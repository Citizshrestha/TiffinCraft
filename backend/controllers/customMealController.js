import db from "../config/db.js";
import {
    announceSubscriptionEvent, customMealCardMeta, CARD_TYPES
} from "../utils/subscriptionEvents.js";
import {
    isValidDateString, getNptToday, daysBetween,
    isDateLocked, getCutoffHour, formatCutoffLabel
} from "../utils/nptTime.js";
import { DAY_STATUS, getDayRow, logDayEvent } from "../utils/subscriptionDailyLog.js";
import { notifyCustomMealRequest, notifyCustomMealResponse } from "../utils/notificationHelper.js";

/**
 * Custom meal requests — "give me X instead of the plan default on this day".
 *
 * Structured, not free text: the row carries (subscription, date, meal) so the
 * cook taps Accept/Decline on a card instead of reading a sentence and
 * remembering it. It rides the SAME chat thread and the SAME announce helper as
 * the subscription request card, so the two feel like one feature.
 *
 * One row per (subscription_id, delivery_date), enforced by a UNIQUE key. A day
 * can only have one answer; a declined or cancelled row is REUSED on a
 * re-request rather than stacked, which is what stops a single day from holding
 * two contradictory requests at once.
 */

const nameOf = async (userId) => {
    const [[row]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [userId]);
    return row?.full_name || "Someone";
};

/** Statuses in which the subscription is actually delivering meals. */
const DELIVERING_STATUSES = ["active", "scheduled", "verified"];

/**
 * POST /api/subscriptions/:id/custom-meal — customer only.
 * Body: { delivery_date, meal_id?, note? }
 */
export const createCustomMealRequest = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { delivery_date, meal_id } = req.body;
        const note = typeof req.body.note === "string" ? req.body.note.trim().slice(0, 300) : null;

        if (!isValidDateString(delivery_date)) {
            return res.status(400).json({ success: false, message: "delivery_date must be a real calendar date in YYYY-MM-DD form." });
        }
        const date = String(delivery_date).trim();

        const [subs] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id, s.plan_id, s.status,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')   AS end_date,
                    p.name AS plan_name
             FROM subscriptions s JOIN subscription_plans p ON p.id = s.plan_id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];
        if (sub.customer_id !== customerId) {
            return res.status(403).json({ success: false, message: "This subscription does not belong to you." });
        }
        if (!DELIVERING_STATUSES.includes(sub.status)) {
            return res.status(409).json({
                success: false,
                message: `You can only request a specific meal on an active subscription — this one is "${sub.status}".`
            });
        }

        // ── The date has to be inside the paid window ────────────────────────
        // Checked against end_date, which under the calendar-day model is fixed
        // at verification and never moves. This is also the guard that stops a
        // request being created for a day after the window closes, which is
        // half of the "period ends with a request still pending" edge case; the
        // other half is expireDanglingCustomMealRequests below.
        if (!sub.start_date || !sub.end_date) {
            return res.status(409).json({ success: false, message: "This subscription's delivery window isn't set yet." });
        }
        if (daysBetween(sub.start_date, date) < 0 || daysBetween(date, sub.end_date) < 0) {
            return res.status(400).json({
                success: false,
                message: `Pick a day inside your subscription window (${sub.start_date} to ${sub.end_date}).`
            });
        }

        // ── Cutoff ──────────────────────────────────────────────────────────
        // The same deadline that governs skipping. A cook who has already shut
        // the books on tomorrow cannot act on a swap for it, so asking is worse
        // than being told no.
        const cutoffHour = await getCutoffHour();
        if (isDateLocked(date, cutoffHour)) {
            return res.status(400).json({
                success: false,
                message: `Too late to change ${date} — the cutoff was ${formatCutoffLabel(cutoffHour)} the day before.`
            });
        }

        // ── Contradictory states ────────────────────────────────────────────
        // A day the customer already skipped, the cook already closed, or that
        // is already delivered cannot also take a swap. Rejected with the actual
        // reason rather than a generic 400, because each one needs a different
        // action from the customer.
        const existingDay = await getDayRow(db.promise(), id, date);
        if (existingDay) {
            if (existingDay.status === DAY_STATUS.DELIVERED) {
                return res.status(409).json({ success: false, message: `${date} has already been delivered — there's nothing left to change.` });
            }
            if (existingDay.status === DAY_STATUS.CUSTOMER_SKIPPED) {
                return res.status(409).json({ success: false, message: `You've already skipped ${date}. Un-skipping isn't possible, so there's no meal to swap.` });
            }
            if (existingDay.status === DAY_STATUS.COOK_UNAVAILABLE) {
                return res.status(409).json({ success: false, message: `The kitchen is closed on ${date}, so there's no meal to swap.` });
            }
            if (existingDay.status === DAY_STATUS.MISSED) {
                return res.status(409).json({ success: false, message: `${date} was marked missed. Please talk to the cook in chat about it.` });
            }
        }

        // Optional meal_id must belong to this cook — otherwise a customer could
        // request another kitchen's dish, which the cook can't make.
        let mealId = null, mealName = null;
        if (meal_id !== undefined && meal_id !== null && meal_id !== "") {
            const [[meal]] = await db.promise().query(
                "SELECT id, name, cook_id, is_available FROM meals WHERE id = ?",
                [meal_id]
            );
            if (!meal) return res.status(404).json({ success: false, message: "That meal doesn't exist." });
            if (Number(meal.cook_id) !== Number(sub.cook_id)) {
                return res.status(400).json({ success: false, message: "That meal is from a different kitchen. Pick one from this cook's menu." });
            }
            if (!meal.is_available) {
                return res.status(400).json({ success: false, message: `"${meal.name}" is marked unavailable right now.` });
            }
            mealId = meal.id;
            mealName = meal.name;
        }
        if (!mealId && !note) {
            return res.status(400).json({ success: false, message: "Pick a meal or write what you'd like instead." });
        }

        // Reuse a settled row for the same day rather than stacking. A pending
        // or already-accepted one blocks: the cook has answered or is about to.
        const [[existingReq]] = await db.promise().query(
            "SELECT id, status FROM custom_meal_requests WHERE subscription_id = ? AND delivery_date = ?",
            [id, date]
        );
        let requestId;
        if (existingReq && ["pending", "accepted"].includes(existingReq.status)) {
            return res.status(409).json({
                success: false,
                message: existingReq.status === "pending"
                    ? `You already have a request for ${date} waiting on the cook.`
                    : `The cook already agreed to a different meal for ${date}.`,
                request_id: existingReq.id
            });
        }
        if (existingReq) {
            requestId = existingReq.id;
            await db.promise().query(
                `UPDATE custom_meal_requests
                 SET meal_id = ?, note = ?, status = 'pending',
                     responded_by = NULL, responded_at = NULL, response_note = NULL
                 WHERE id = ? AND customer_id = ?`,
                [mealId, note, requestId, customerId]
            );
        } else {
            const [ins] = await db.promise().query(
                `INSERT INTO custom_meal_requests
                    (subscription_id, customer_id, cook_id, delivery_date, meal_id, note, status)
                 VALUES (?, ?, ?, ?, ?, ?, 'pending')`,
                [id, customerId, sub.cook_id, date, mealId, note]
            );
            requestId = ins.insertId;
        }

        await logDayEvent({
            subscriptionId: id, event: "custom_meal_requested", actor: "customer",
            detail: `Asked for ${mealName || "a specific meal"} on ${date}.${note ? ` Note: ${note}` : ""}`
        });

        const customerName = await nameOf(customerId);
        const { conversationId } = await announceSubscriptionEvent({
            io: req.app.get("io"),
            customerId, cookId: sub.cook_id,
            senderId: customerId, recipientId: sub.cook_id, senderName: customerName,
            cardType: CARD_TYPES.CUSTOM_MEAL_REQUEST,
            cardText: `Meal request for ${date}: ${mealName || note}`,
            metadata: customMealCardMeta({
                requestId, subscriptionId: Number(id), deliveryDate: date,
                mealId, mealName, note, status: "pending", customerName
            }),
            referenceId: requestId, referenceType: "custom_meal_request",
            title: "Custom meal request",
            body: `${customerName} asked for ${mealName || "a different meal"} on ${date}.`,
            notifType: "custom_meal_request",
            pushData: { subscriptionId: String(id), deliveryDate: date }
        });

        // Dedicated FCM push with full payload — announceSubscriptionEvent's
        // internal createNotification already wrote the in-app DB row; this
        // ensures the push data (subscriptionId, requestId, deliveryDate,
        // customerName) is always complete so FcmService can deep-link correctly.
        notifyCustomMealRequest(
            sub.cook_id, Number(id), customerName, sub.plan_name,
            date, mealName, note, requestId
        ).catch(err => console.error("notifyCustomMealRequest push failed:", err.message));

        return res.status(201).json({
            success: true,
            message: `Request sent for ${date}. The cook will accept or decline it.`,
            request_id: requestId,
            conversation_id: conversationId,
            status: "pending",
            delivery_date: date,
            meal_id: mealId,
            meal_name: mealName
        });
    } catch (error) {
        console.error("createCustomMealRequest error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * PUT /api/custom-meals/:requestId/respond — cook only.
 * Body: { action: 'accept' | 'decline', note? }
 *
 * Guarded on `status = 'pending'` so answering twice (chat card + daily view)
 * loses the second time rather than overwriting the first answer.
 */
export const respondToCustomMealRequest = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { requestId } = req.params;
        const action = String(req.body.action || "").toLowerCase();
        const note = typeof req.body.note === "string" ? req.body.note.trim().slice(0, 300) : null;

        if (!["accept", "decline"].includes(action)) {
            return res.status(400).json({ success: false, message: "action must be 'accept' or 'decline'." });
        }

        const [[reqRow]] = await db.promise().query(
            `SELECT r.*, DATE_FORMAT(r.delivery_date, '%Y-%m-%d') AS date_str,
                    m.name AS meal_name, p.name AS plan_name,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d') AS sub_end_date, s.status AS sub_status
             FROM custom_meal_requests r
             JOIN subscriptions s ON s.id = r.subscription_id
             JOIN subscription_plans p ON p.id = s.plan_id
             LEFT JOIN meals m ON m.id = r.meal_id
             WHERE r.id = ?`,
            [requestId]
        );
        if (!reqRow) {
            return res.status(404).json({ success: false, message: "Request not found." });
        }
        if (Number(reqRow.cook_id) !== cookId) {
            return res.status(403).json({ success: false, message: "This request isn't for your kitchen." });
        }
        if (reqRow.status !== "pending") {
            return res.status(409).json({
                success: false,
                message: `This request is already "${reqRow.status}".`,
                status: reqRow.status
            });
        }

        const accepted = action === "accept";
        const newStatus = accepted ? "accepted" : "declined";

        const [upd] = await db.promise().query(
            `UPDATE custom_meal_requests
             SET status = ?, responded_by = ?, responded_at = NOW(), response_note = ?
             WHERE id = ? AND cook_id = ? AND status = 'pending'`,
            [newStatus, cookId, note, requestId, cookId]
        );
        if (upd.affectedRows === 0) {
            return res.status(409).json({ success: false, message: "That request was just answered from another screen. Pull to refresh." });
        }

        await logDayEvent({
            subscriptionId: reqRow.subscription_id,
            event: accepted ? "custom_meal_accepted" : "custom_meal_declined",
            actor: "cook",
            detail: `${accepted ? "Agreed to" : "Declined"} ${reqRow.meal_name || "the requested meal"} on ${reqRow.date_str}.${note ? ` ${note}` : ""}`
        });

        const cookName = await nameOf(cookId);
        await announceSubscriptionEvent({
            io: req.app.get("io"),
            customerId: reqRow.customer_id, cookId,
            senderId: cookId, recipientId: reqRow.customer_id, senderName: cookName,
            cardType: CARD_TYPES.CUSTOM_MEAL_REQUEST,
            cardText: accepted
                ? `Agreed — you'll get ${reqRow.meal_name || "your requested meal"} on ${reqRow.date_str}.`
                : `Can't do ${reqRow.meal_name || "that meal"} on ${reqRow.date_str}.${note ? " " + note : " You'll get the usual plan meal."}`,
            metadata: customMealCardMeta({
                requestId: Number(requestId), subscriptionId: reqRow.subscription_id,
                deliveryDate: reqRow.date_str, mealId: reqRow.meal_id,
                mealName: reqRow.meal_name, note, status: newStatus
            }),
            referenceId: Number(requestId), referenceType: "custom_meal_request",
            title: accepted ? "Meal request accepted" : "Meal request declined",
            body: accepted
                ? `${cookName} will make ${reqRow.meal_name || "your requested meal"} on ${reqRow.date_str}.`
                : `${cookName} can't make that on ${reqRow.date_str}.${note ? " Reason: " + note : " You'll get the usual plan meal."}`,
            notifType: accepted ? "custom_meal_accepted" : "custom_meal_declined",
            pushData: { subscriptionId: String(reqRow.subscription_id), deliveryDate: reqRow.date_str }
        });

        // Dedicated FCM push to the customer with complete payload so the
        // Android client can deep-link to the correct subscription calendar.
        notifyCustomMealResponse(
            reqRow.customer_id, reqRow.subscription_id, cookName,
            reqRow.date_str, reqRow.meal_name, accepted, note, Number(requestId)
        ).catch(err => console.error("notifyCustomMealResponse push failed:", err.message));

        return res.status(200).json({
            success: true,
            status: newStatus,
            message: accepted
                ? `Noted — ${reqRow.meal_name || "the requested meal"} on ${reqRow.date_str}.`
                : "Declined. The customer knows they'll get the usual plan meal."
        });
    } catch (error) {
        console.error("respondToCustomMealRequest error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * GET /api/subscriptions/:id/custom-meals — customer or cook.
 * Every request on this subscription, newest day first, so the calendar can
 * mark days that already carry a swap.
 */
export const getCustomMealRequests = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [[sub]] = await db.promise().query(
            "SELECT customer_id, cook_id FROM subscriptions WHERE id = ?",
            [id]
        );
        if (!sub) return res.status(404).json({ success: false, message: "Subscription not found." });
        if (sub.customer_id !== userId && sub.cook_id !== userId) {
            return res.status(403).json({ success: false, message: "Access denied." });
        }

        const [rows] = await db.promise().query(
            `SELECT r.id, DATE_FORMAT(r.delivery_date, '%Y-%m-%d') AS delivery_date,
                    r.meal_id, m.name AS meal_name, m.image_url AS meal_image,
                    r.note, r.status, r.response_note, r.responded_at, r.created_at
             FROM custom_meal_requests r
             LEFT JOIN meals m ON m.id = r.meal_id
             WHERE r.subscription_id = ?
             ORDER BY r.delivery_date DESC`,
            [id]
        );

        return res.status(200).json({
            success: true,
            viewer: sub.cook_id === userId ? "cook" : "customer",
            today: getNptToday(),
            requests: rows
        });
    } catch (error) {
        console.error("getCustomMealRequests error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * DELETE /api/custom-meals/:requestId — customer withdraws a pending request.
 * Only from 'pending': once the cook has agreed, the swap is part of their prep
 * plan and pulling it silently would waste food.
 */
export const cancelCustomMealRequest = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { requestId } = req.params;

        const [upd] = await db.promise().query(
            `UPDATE custom_meal_requests SET status = 'cancelled'
             WHERE id = ? AND customer_id = ? AND status = 'pending'`,
            [requestId, customerId]
        );
        if (upd.affectedRows === 0) {
            return res.status(409).json({
                success: false,
                message: "That request can't be withdrawn — it's either already answered or not yours."
            });
        }
        return res.status(200).json({ success: true, message: "Request withdrawn." });
    } catch (error) {
        console.error("cancelCustomMealRequest error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * Close out requests the calendar has run past.
 *
 * Handles the "period ends while a request is still pending" edge case from the
 * other end: creation already refuses dates outside the window, and this sweeps
 * anything that was legitimately pending when the day arrived and went by.
 * Marked 'expired' rather than deleted — the customer asked, nobody answered,
 * and that is worth being able to see afterwards.
 *
 * Called by the daily subscription job; safe to run repeatedly.
 */
export const expireDanglingCustomMealRequests = async () => {
    const today = getNptToday();
    const [res1] = await db.promise().query(
        `UPDATE custom_meal_requests
         SET status = 'expired'
         WHERE status = 'pending' AND delivery_date < ?`,
        [today]
    );
    const [res2] = await db.promise().query(
        `UPDATE custom_meal_requests r
         JOIN subscriptions s ON s.id = r.subscription_id
         SET r.status = 'expired'
         WHERE r.status = 'pending'
           AND s.status IN ('completed','cancelled','rejected')`
    );
    const total = res1.affectedRows + res2.affectedRows;
    if (total > 0) console.log(`🧹 Expired ${total} dangling custom meal request(s).`);
    return total;
};

export default {
    createCustomMealRequest,
    respondToCustomMealRequest,
    getCustomMealRequests,
    cancelCustomMealRequest,
    expireDanglingCustomMealRequests
};
