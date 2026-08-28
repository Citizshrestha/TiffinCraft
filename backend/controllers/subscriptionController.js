import db from "../config/db.js";
import { fetchPlanWithItems } from "./subscriptionPlanController.js";
import {
    notifySubscriptionPaymentSubmitted,
    notifySubscriptionVerified,
    notifySubscriptionRejected,
    notifySubscriptionScheduled,
    notifySkipDay,
    formatDeliveryDate
} from "../utils/notificationHelper.js";
import {
    dateOnly,
    isValidDateString,
    getNptToday,
    getNptTomorrow,
    addDays,
    daysBetween,
    isDateLocked,
    msUntilCutoff,
    formatCutoffLabel,
    getCutoffHour,
    SQL_NPT_TODAY
} from "../utils/nptTime.js";
import {
    DAY_STATUS,
    DAY_STATUS_LABELS,
    applyDayStatus,
    getDayRowsInRange,
    getCookClosuresInRange,
    isCookClosedOn,
    placeDayOrder,
    logDayEvent
} from "../utils/subscriptionDailyLog.js";

export function getDurationDays(duration) {
    if (duration === "2_weeks" || duration === "biweekly") return 14;
    if (duration === "monthly" || duration === "1_month") return 30;
    return 7;
}

/**
 * How far ahead a customer may choose to start. Enforced server-side, not just
 * by the app's date picker — a hand-rolled request must not be able to park a
 * paid subscription two years out, where the cook's plan and prices no longer
 * exist.
 */
export const MAX_START_DATE_DAYS_AHEAD = 30;

/** Days shown by the delivery calendar endpoint. */
export const CALENDAR_WINDOW_DAYS = 14;

/**
 * Statuses in which a subscription is waiting on the cook's verification. Both
 * exist because 'pending_payment' is where a row starts (money not in yet) and
 * 'pending_verification' is where the money-in-but-unconfirmed rows land; a
 * cook may legitimately verify from either, since the manual-QR flow has no
 * automatic transition between them.
 */
export const AWAITING_VERIFICATION_STATUSES = ["pending_payment", "pending_verification"];

/** A subscription that is running or about to run — blocks a duplicate signup. */
export const LIVE_STATUSES = ["pending_verification", "verified", "scheduled", "active", "paused"];

/**
 * Validates a customer-chosen start date against the Nepal-Time calendar.
 * Returns { ok: true, startDate } or { ok: false, message }.
 *
 * Server-side and NPT-based on purpose. The app's picker enforces the same
 * window, but a request that bypasses the app must not be able to (a) backdate a
 * subscription so the cron thinks it owes deliveries for days that already
 * passed, or (b) push it past the horizon where the plan still exists. And
 * "today" has to mean today IN NEPAL — between 00:00 and 05:45 NPT a UTC server
 * would reject tomorrow's date as being in the past.
 */
export const validateStartDate = (raw) => {
    if (!isValidDateString(raw)) {
        return { ok: false, message: "start_date must be a real calendar date in YYYY-MM-DD form." };
    }
    const startDate = raw.trim();
    const today = getNptToday();
    const offset = daysBetween(today, startDate);

    if (offset < 0) {
        return { ok: false, message: "start_date can't be in the past. Pick today or a later date." };
    }
    if (offset > MAX_START_DATE_DAYS_AHEAD) {
        return {
            ok: false,
            message: `start_date can be at most ${MAX_START_DATE_DAYS_AHEAD} days from today (latest: ${addDays(today, MAX_START_DATE_DAYS_AHEAD)}).`
        };
    }
    return { ok: true, startDate };
};

/**
 * POST /api/subscriptions
 * MANUAL-QR path (customer pays the cook directly and uploads a screenshot).
 * The gateway path is POST /api/subscriptions/initiate — see
 * subscriptionPaymentController.js — which is what the app uses by default.
 *
 * Starts in 'pending_payment'; the subscription does NOT activate here. The
 * customer still has to pay the cook, upload a screenshot via
 * uploadSubscriptionScreenshot, and the cook has to verify it via
 * verifySubscriptionPayment before this ever generates a real delivery.
 *
 * start_date is the customer's CHOICE and is honoured: it is validated against
 * the Nepal-Time calendar here and kept verbatim through verification, rather
 * than being overwritten with the verification day as it used to be. A customer
 * who picks next Monday starts next Monday even if the cook verifies on Friday.
 * Body: { plan_id, delivery_address, start_date }
 */
export const createSubscription = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { plan_id, delivery_address, start_date } = req.body;

        if (!plan_id || !delivery_address || !start_date) {
            return res.status(400).json({
                success: false,
                message: "plan_id, delivery_address, and start_date are required."
            });
        }

        const [plans] = await db.promise().query(
            "SELECT id, cook_id, duration, is_active FROM subscription_plans WHERE id = ?",
            [plan_id]
        );
        if (plans.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription plan not found." });
        }
        const plan = plans[0];
        if (!plan.is_active) {
            return res.status(400).json({ success: false, message: "This plan is no longer available." });
        }
        if (plan.cook_id === customerId) {
            return res.status(400).json({ success: false, message: "You can't subscribe to your own plan." });
        }

        // One or more of this plan's meals is currently marked unavailable by
        // the cook — block signup rather than accept payment for something
        // that can't actually be delivered (same check the recurring-order
        // job re-runs on every cycle — see subscriptionOrderJob.js).
        const fullPlan = await fetchPlanWithItems(plan_id);
        if (!fullPlan || !fullPlan.is_available) {
            return res.status(400).json({
                success: false,
                message: "This plan includes an item that's currently unavailable. Please try again once the cook restocks it."
            });
        }

        // Only a subscription that's actually RUNNING (or queued to run) blocks
        // a new one. The original check was `status != 'cancelled'`, which also
        // matched abandoned 'pending_payment' rows — so one tap that never got
        // paid for permanently locked the customer out of that plan.
        const [live] = await db.promise().query(
            `SELECT id, status FROM subscriptions
             WHERE customer_id = ? AND plan_id = ? AND status IN (?)
               AND (end_date IS NULL OR end_date >= ${SQL_NPT_TODAY})
             LIMIT 1`,
            [customerId, plan_id, LIVE_STATUSES]
        );
        if (live.length > 0) {
            return res.status(409).json({
                success: false,
                message: `You already have a ${live[0].status.replace(/_/g, " ")} subscription to this plan. Cancel it first, or wait for it to finish.`
            });
        }

        const startCheck = validateStartDate(start_date);
        if (!startCheck.ok) {
            return res.status(400).json({ success: false, message: startCheck.message });
        }
        const startDate = startCheck.startDate;
        // next_delivery_date IS the start date: deliveries are daily from day
        // one. end_date is left for verification to set, because the number of
        // paid meals is only fixed once the payment is confirmed.
        const nextDelivery = startDate;

        // Reuse an unpaid attempt for this plan instead of stacking dead rows.
        const [pending] = await db.promise().query(
            `SELECT id FROM subscriptions
             WHERE customer_id = ? AND plan_id = ? AND status = 'pending_payment'
             ORDER BY id DESC LIMIT 1`,
            [customerId, plan_id]
        );
        if (pending.length > 0) {
            await db.promise().query(
                `UPDATE subscriptions
                 SET delivery_address = ?, start_date = ?, next_delivery_date = ?,
                     payment_status = 'pending', payment_method = 'manual_qr',
                     payment_screenshot_url = NULL
                 WHERE id = ? AND status = 'pending_payment'`,
                [delivery_address, startDate, nextDelivery, pending[0].id]
            );
            return res.status(200).json({
                success: true,
                message: "Continuing your unpaid subscription — pay the cook and upload proof to activate it.",
                subscriptionId: pending[0].id,
                start_date: startDate
            });
        }

        const [result] = await db.promise().query(
            `INSERT INTO subscriptions (customer_id, cook_id, plan_id, delivery_address, start_date, next_delivery_date, status, payment_status, payment_method)
             VALUES (?, ?, ?, ?, ?, ?, 'pending_payment', 'pending', 'manual_qr')`,
            [customerId, plan.cook_id, plan_id, delivery_address, startDate, nextDelivery]
        );

        return res.status(201).json({
            success: true,
            message: "Subscription created — pay the cook and upload proof to activate it.",
            subscriptionId: result.insertId,
            start_date: startDate
        });

    } catch (error) {
        console.error("createSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/screenshot — customer only.
 * Uploads to /api/upload/document first (same two-step pattern as orders and
 * commission settlements), then posts the resulting URL here.
 */
export const uploadSubscriptionScreenshot = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { payment_screenshot_url } = req.body;

        if (!payment_screenshot_url) {
            return res.status(400).json({ success: false, message: "payment_screenshot_url is required." });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
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
        if (!["pending", "rejected", "failed"].includes(sub.payment_status)) {
            return res.status(400).json({ success: false, message: `Cannot submit payment proof — this subscription's payment is already "${sub.payment_status}".` });
        }

        // Moves the lifecycle to 'pending_verification': the customer's side is
        // done and the ball is entirely in the cook's court. Distinct from
        // 'pending_payment' so the cook's subscriber list can show "N waiting on
        // you" without also counting people who never paid.
        await db.promise().query(
            `UPDATE subscriptions
             SET payment_screenshot_url = ?, payment_status = 'submitted',
                 status = CASE WHEN status = 'pending_payment' THEN 'pending_verification' ELSE status END
             WHERE id = ?`,
            [payment_screenshot_url, id]
        );

        await logDayEvent({
            subscriptionId: id,
            event: "proof_submitted",
            actor: "customer",
            detail: `Payment proof uploaded; awaiting cook verification. Requested start ${dateOnly(sub.start_date)}.`
        });

        const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [customerId]);
        await notifySubscriptionPaymentSubmitted(sub.cook_id, id, customer?.full_name || "A customer", sub.plan_name);

        return res.status(200).json({ success: true, message: "Payment proof submitted. The cook will verify it shortly." });
    } catch (error) {
        console.error("uploadSubscriptionScreenshot error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * PUT /api/subscriptions/:id/verify-payment — cook only.
 * Body: { status: 'verified'|'rejected', notes }
 *
 * Verifying confirms the MONEY. It does not, by itself, start deliveries.
 *
 *   start_date in the future  -> 'scheduled'. The 06:00 cron flips it to
 *                                'active' on the morning of start_date.
 *   start_date today (or past)-> 'active' immediately, plus today's log row and,
 *                                if the 06:00 batch has already run, today's
 *                                order placed on the spot so a paid day isn't
 *                                lost to the cook's verification timing.
 *
 * A past start_date is CLAMPED to today rather than honoured. Honouring it would
 * mean the cron owes deliveries for days that already went by — it would either
 * dump several days of orders at once or mark them all missed, and either way
 * the customer's meal credits drain for food nobody cooked.
 */
export const verifySubscriptionPayment = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const { status, notes } = req.body;

        const ALLOWED = ["verified", "rejected"];
        if (!status || !ALLOWED.includes(status)) {
            return res.status(400).json({ success: false, message: `status must be one of: ${ALLOWED.join(", ")}` });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name, p.duration, p.price_per_delivery
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];

        // Ownership: the cook_id on the row must be the authenticated user. The
        // :id in the URL is never trusted on its own.
        if (sub.cook_id !== cookId) {
            return res.status(403).json({ success: false, message: "This subscription does not belong to your kitchen." });
        }
        if (sub.payment_status !== "submitted") {
            return res.status(400).json({
                success: false,
                message: `Cannot ${status === "verified" ? "verify" : "reject"} — payment proof hasn't been submitted (current status: ${sub.payment_status}).`
            });
        }

        if (status === "rejected") {
            await db.promise().query(
                `UPDATE subscriptions
                 SET payment_status = 'rejected', verification_notes = ?,
                     status = CASE WHEN status = 'pending_verification' THEN 'pending_payment' ELSE status END
                 WHERE id = ?`,
                [notes || null, id]
            );
            await logDayEvent({
                subscriptionId: id,
                event: "verification_rejected",
                actor: "cook",
                detail: notes ? `Rejected: ${notes}` : "Rejected by cook."
            });
            await notifySubscriptionRejected(sub.customer_id, sub.id, sub.plan_name, notes);
            return res.status(200).json({ success: true, message: "Subscription payment marked rejected." });
        }

        // ---- verified ----------------------------------------------------
        const today = getNptToday();
        const requestedStart = dateOnly(sub.start_date);
        const clamped = !requestedStart || daysBetween(today, requestedStart) < 0;
        const effectiveStart = clamped ? today : requestedStart;

        // One paid meal per delivery day. meals_total is the authoritative bound
        // on the cycle; end_date is the date the last of them lands if nothing is
        // skipped, and is pushed out by a day each time a day is skipped so the
        // customer never loses a meal they paid for.
        const mealsTotal = getDurationDays(sub.duration);
        const endDate = addDays(effectiveStart, mealsTotal - 1);
        const startsNow = effectiveStart === today;
        const newStatus = startsNow ? "active" : "scheduled";

        const connection = await db.promise().getConnection();
        let dayOutcome = null;
        try {
            await connection.beginTransaction();

            // Gated on the row still awaiting verification: two cooks on two
            // devices (or a double tap) can both reach here, and only one may
            // seed the meal credits. The loser gets affectedRows = 0.
            const [upd] = await connection.query(
                `UPDATE subscriptions
                 SET status = ?, payment_status = 'verified', payment_verified_at = NOW(),
                     verified_by = ?, verified_at = NOW(), verification_notes = ?,
                     start_date = ?, end_date = ?, next_delivery_date = ?,
                     meals_total = ?, meals_remaining = ?
                 WHERE id = ? AND status IN (?) AND payment_status = 'submitted'`,
                [newStatus, cookId, notes || null, effectiveStart, endDate, effectiveStart,
                 mealsTotal, mealsTotal, id, AWAITING_VERIFICATION_STATUSES]
            );

            if (upd.affectedRows === 0) {
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    message: "This subscription was already processed — refresh to see its current state."
                });
            }

            if (startsNow) {
                // Today's log row. A cook who closed today BEFORE this
                // subscription existed never got a fan-out row for it, so the
                // closure has to be re-checked here — otherwise the customer's
                // very first day would be scheduled into a shut kitchen.
                const closedToday = await isCookClosedOn(connection, cookId, today);

                if (closedToday) {
                    await applyDayStatus(connection, {
                        subscriptionId: sub.id,
                        deliveryDate: today,
                        status: DAY_STATUS.COOK_UNAVAILABLE,
                        toggledBy: "cook",
                        reason: "Kitchen already closed for this date when the subscription activated",
                        creditDeducted: false
                    });
                    // The day costs nothing, but the window does NOT move: an
                    // N-calendar-day subscription ends on the date fixed at
                    // verification regardless of how many days went undelivered.
                    // Only next_delivery_date advances, so tomorrow's pass looks
                    // at tomorrow.
                    await connection.query(
                        `UPDATE subscriptions SET next_delivery_date = DATE_ADD(next_delivery_date, INTERVAL 1 DAY)
                         WHERE id = ?`,
                        [sub.id]
                    );
                    dayOutcome = "cook_unavailable";
                } else {
                    // Row first, order second: placeDayOrder writes the order_id
                    // back onto this row, so it has to exist. The credit is
                    // deducted only once the order actually exists — a plan whose
                    // meal the cook has 86'd produces no order, and charging a
                    // meal credit for food nobody cooked is the one outcome the
                    // credit system exists to prevent.
                    await applyDayStatus(connection, {
                        subscriptionId: sub.id,
                        deliveryDate: today,
                        status: DAY_STATUS.SCHEDULED,
                        toggledBy: "system",
                        reason: "Activated on verification",
                        creditDeducted: false
                    });

                    const placement = await placeDayOrder(connection, sub, today);

                    if (placement.placed) {
                        await connection.query(
                            `UPDATE subscription_daily_log SET credit_deducted = TRUE
                             WHERE subscription_id = ? AND delivery_date = ?`,
                            [sub.id, today]
                        );
                        await connection.query(
                            `UPDATE subscriptions
                             SET meals_remaining = GREATEST(meals_remaining - 1, 0),
                                 next_delivery_date = DATE_ADD(next_delivery_date, INTERVAL 1 DAY)
                             WHERE id = ?`,
                            [sub.id]
                        );
                        dayOutcome = "order_placed";
                    } else {
                        // Treated exactly like a kitchen closure: no meal, no
                        // charge. The end date stays where verification put it.
                        const names = (placement.items || []).map(i => i.meal_name).filter(Boolean).join(", ");
                        await connection.query(
                            `UPDATE subscription_daily_log
                             SET status = 'cook_unavailable', toggled_by = 'system', reason = ?, toggled_at = NOW()
                             WHERE subscription_id = ? AND delivery_date = ? AND status <> 'delivered'`,
                            [`Plan item unavailable on activation day${names ? `: ${names}` : ""}`, sub.id, today]
                        );
                        await connection.query(
                            `UPDATE subscriptions
                             SET next_delivery_date = DATE_ADD(next_delivery_date, INTERVAL 1 DAY)
                             WHERE id = ?`,
                            [sub.id]
                        );
                        dayOutcome = "unavailable_items";
                    }
                }
            }

            await connection.commit();
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }

        await logDayEvent({
            subscriptionId: id,
            event: startsNow ? "activated" : "scheduled",
            actor: "cook",
            detail: `Payment verified by cook ${cookId}. ${mealsTotal} meals, ${effectiveStart} → ${endDate}.`
                + (clamped ? ` Requested start ${requestedStart || "(none)"} was in the past and was clamped to today.` : "")
                + (dayOutcome ? ` Day one: ${dayOutcome}.` : "")
        });

        if (startsNow) {
            await notifySubscriptionVerified(sub.customer_id, sub.id, sub.plan_name);
        } else {
            await notifySubscriptionScheduled(sub.customer_id, sub.id, sub.plan_name, effectiveStart);
        }

        return res.status(200).json({
            success: true,
            message: startsNow
                ? "Payment verified — this subscription is active and today's delivery is scheduled."
                : `Payment verified — deliveries start ${formatDeliveryDate(effectiveStart)}. Nothing to do until then.`,
            subscription: {
                id: Number(id),
                status: newStatus,
                start_date: effectiveStart,
                end_date: endDate,
                meals_total: mealsTotal,
                start_date_clamped: clamped
            }
        });
    } catch (error) {
        console.error("verifySubscriptionPayment error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * Every DATE column on `subscriptions`, normalised to 'YYYY-MM-DD' before the
 * row leaves the server.
 *
 * This is the fix for the raw "2026-09-03T00:00:00.000Z" the app was rendering
 * in "Next delivery": mysql2 hydrates a DATE into a JS Date at local midnight,
 * JSON.stringify turns that into a full UTC timestamp, and the client had no way
 * to tell a date from an instant. Normalising here means no current or future
 * screen can receive one by accident.
 */
const DATE_FIELDS = ["start_date", "end_date", "next_delivery_date"];
const withDateOnlyFields = (row) => {
    const out = { ...row };
    for (const field of DATE_FIELDS) {
        if (field in out) out[field] = dateOnly(out[field]);
    }
    return out;
};

const formatSubscriptionRow = async (row) => {
    const plan = await fetchPlanWithItems(row.plan_id);

    // Cook's eSewa QR for the manual pay flow — the customer scans it in their
    // own eSewa app. Same field name and shape the order detail endpoint
    // already exposes (see orderController.js), so the two manual-pay screens
    // consume one contract. bank_details is a JSON blob (esewa_qr_url /
    // khalti_qr_url / bank_qr_url); pull just eSewa's, tolerating rows that
    // are null (cook never set one up) or malformed.
    let cookEsewaQrUrl = null;
    try {
        if (row.cook_bank_details) cookEsewaQrUrl = JSON.parse(row.cook_bank_details).esewa_qr_url || null;
    } catch (_) { /* legacy/malformed JSON — no QR */ }

    const { cook_bank_details, ...rest } = row;
    return { ...withDateOnlyFields(rest), plan, cook_esewa_qr_url: cookEsewaQrUrl };
};

/**
 * GET /api/subscriptions/customer/my
 * Customer views their subscriptions (active, paused, cancelled)
 */
export const getMySubscriptions = async (req, res) => {
    try {
        const customerId = req.user.id;

        const [subscriptions] = await db.promise().query(
            `SELECT s.*, cp.bank_details AS cook_bank_details
             FROM subscriptions s
             LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
             WHERE s.customer_id = ? ORDER BY s.created_at DESC`,
            [customerId]
        );

        const formatted = await Promise.all(subscriptions.map(formatSubscriptionRow));

        return res.status(200).json({
            success: true,
            subscriptions: formatted
        });

    } catch (error) {
        console.error("getMySubscriptions error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /api/subscriptions/cook/my
 * Cook views their subscribers across all plans
 */
export const getCookSubscribers = async (req, res) => {
    try {
        const cookId = req.user.id;

        const [subscriptions] = await db.promise().query(
            `SELECT s.*, p.name as plan_name, p.duration, u.full_name as customer_name,
                    u.phone as customer_phone
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             JOIN users u ON s.customer_id = u.id
             WHERE s.cook_id = ?
             ORDER BY s.created_at DESC`,
            [cookId]
        );

        // 'active' is no longer the only live state — a verified subscription
        // waiting for its start date is a real subscriber the cook has been paid
        // by, and counting only 'active' would under-report them.
        const [[counts]] = await db.promise().query(
            `SELECT
                SUM(status = 'active')    AS active_count,
                SUM(status = 'scheduled') AS scheduled_count,
                SUM(status = 'pending_verification' OR payment_status = 'submitted') AS awaiting_verification_count
             FROM subscriptions WHERE cook_id = ?`,
            [cookId]
        );

        return res.status(200).json({
            success: true,
            activeSubscriberCount: Number(counts?.active_count || 0),
            scheduledSubscriberCount: Number(counts?.scheduled_count || 0),
            awaitingVerificationCount: Number(counts?.awaiting_verification_count || 0),
            subscriptions: subscriptions.map(withDateOnlyFields)
        });

    } catch (error) {
        console.error("getCookSubscribers error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/pause
 * An INDEFINITE hold — deliberately distinct from skipping a single day, which
 * is a subscription_daily_log row. Pausing stops the cron creating any further
 * day rows; the remaining meal credits are untouched and wait for a resume.
 */
export const pauseSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        // Verify ownership
        const [subs] = await db.promise().query(
            "SELECT id, cook_id, status FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        if (!["active", "scheduled"].includes(subs[0].status)) {
            return res.status(400).json({
                success: false,
                message: `Only a running or scheduled subscription can be paused — this one is "${subs[0].status.replace(/_/g, " ")}".`
            });
        }

        await db.promise().query(
            "UPDATE subscriptions SET status = 'paused' WHERE id = ? AND status IN ('active','scheduled')",
            [id]
        );

        await logDayEvent({
            subscriptionId: id,
            event: "paused",
            actor: userId === subs[0].cook_id ? "cook" : "customer",
            detail: `Paused indefinitely from "${subs[0].status}". Remaining meal credits preserved.`
        });

        return res.status(200).json({
            success: true,
            message: "Subscription paused. Today's and tomorrow's deliveries already past their cutoff are unaffected."
        });

    } catch (error) {
        console.error("pauseSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/resume
 * Resume a paused subscription.
 *
 * Deliveries restart TOMORROW (Nepal time), never today: today's cutoff passed
 * yesterday evening, the cook has already planned their day, and quietly adding
 * a meal to it would be exactly the surprise the cutoff exists to prevent.
 * A subscription whose start date is still ahead goes back to 'scheduled'.
 */
export const resumeSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            `SELECT id, cook_id, status,
                    DATE_FORMAT(start_date, '%Y-%m-%d') AS start_date,
                    DATE_FORMAT(end_date, '%Y-%m-%d')   AS end_date
             FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)`,
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        const sub = subs[0];
        if (sub.status !== "paused") {
            return res.status(400).json({
                success: false,
                message: `Only a paused subscription can be resumed — this one is "${sub.status.replace(/_/g, " ")}".`
            });
        }
        // Calendar-day model: what stops a resume is the window having run out,
        // not a meal count. A subscription paused near its end date can be
        // resumed only if there is still a day left inside the original window —
        // pausing never bought extra days at the end.
        if (sub.end_date && daysBetween(getNptToday(), sub.end_date) < 0) {
            return res.status(400).json({
                success: false,
                message: `This subscription's period ended on ${sub.end_date}. Subscribe again to keep going.`
            });
        }

        const today = getNptToday();
        const tomorrow = getNptTomorrow();
        const startsLater = sub.start_date && daysBetween(today, sub.start_date) > 0;
        // Resuming after the original start date passed means deliveries pick up
        // tomorrow; resuming before it means they still wait for it.
        const resumeDate = startsLater ? sub.start_date : tomorrow;
        const resumeStatus = startsLater ? "scheduled" : "active";

        await db.promise().query(
            "UPDATE subscriptions SET status = ?, next_delivery_date = ? WHERE id = ? AND status = 'paused'",
            [resumeStatus, resumeDate, id]
        );

        await logDayEvent({
            subscriptionId: id,
            event: "resumed",
            actor: userId === sub.cook_id ? "cook" : "customer",
            detail: `Resumed as "${resumeStatus}"; next delivery ${resumeDate}.`
        });

        return res.status(200).json({
            success: true,
            message: `Subscription resumed — next delivery ${resumeDate}.`,
            next_delivery_date: resumeDate,
            status: resumeStatus
        });

    } catch (error) {
        console.error("resumeSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /api/subscriptions/:id/calendar — customer OR cook (owner of either side).
 *
 * The per-day view that replaces the single "Active" flag. Returns a window of
 * delivery days, each with its REAL logged status where one exists and a
 * projected 'scheduled' where none does yet, plus everything the UI needs to
 * decide whether a day can still be changed — so the client never has to
 * recompute the cutoff and get it wrong.
 *
 * All dates are emitted as 'YYYY-MM-DD' strings. Nothing here returns a DATE
 * column raw: mysql2 turns those into Date objects that JSON-serialise as
 * "2026-09-03T00:00:00.000Z", which is what the app used to display verbatim.
 */
export const getSubscriptionCalendar = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id, s.plan_id, s.status, s.payment_status,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d')        AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')          AS end_date,
                    DATE_FORMAT(s.next_delivery_date, '%Y-%m-%d') AS next_delivery_date,
                    s.meals_total, s.meals_remaining,
                    p.name AS plan_name, p.duration,
                    cu.full_name AS customer_name,
                    ck.full_name AS cook_name
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             JOIN users cu ON cu.id = s.customer_id
             JOIN users ck ON ck.id = s.cook_id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];

        // Ownership: only the customer who bought it or the cook who serves it.
        // Checked against req.user.id, never against anything in the request.
        const isCustomer = sub.customer_id === userId;
        const isCook = sub.cook_id === userId;
        if (!isCustomer && !isCook) {
            return res.status(403).json({ success: false, message: "This subscription isn't yours to view." });
        }

        const cutoffHour = await getCutoffHour();
        const today = getNptToday();

        // A scheduled subscription's window starts on its first delivery day —
        // showing 14 mostly-empty days before it starts tells the customer
        // nothing. An active one starts today.
        const windowStart = sub.start_date && daysBetween(today, sub.start_date) > 0 ? sub.start_date : today;
        let windowEnd = addDays(windowStart, CALENDAR_WINDOW_DAYS - 1);
        if (sub.end_date && daysBetween(sub.end_date, windowEnd) > 0) windowEnd = sub.end_date;

        const [logRows, closures, swapRows] = await Promise.all([
            getDayRowsInRange(db.promise(), sub.id, windowStart, windowEnd),
            getCookClosuresInRange(db.promise(), sub.cook_id, windowStart, windowEnd),
            // Custom meal swaps inside the same window. Fetched here so the
            // calendar — the one screen that shows a customer their whole
            // upcoming week — marks the days that already carry a swap. Without
            // this the customer has no way to see a request they already sent,
            // and would send it again into the UNIQUE (subscription, date) key.
            db.promise().query(
                `SELECT r.id, DATE_FORMAT(r.delivery_date, '%Y-%m-%d') AS delivery_date,
                        r.status, r.note, r.meal_id, m.name AS meal_name
                 FROM custom_meal_requests r
                 LEFT JOIN meals m ON m.id = r.meal_id
                 WHERE r.subscription_id = ? AND r.delivery_date BETWEEN ? AND ?
                   AND r.status IN ('pending', 'accepted')`,
                [sub.id, windowStart, windowEnd]
            ).then(([rows]) => new Map(rows.map(r => [r.delivery_date, r])))
        ]);

        // Only a running (or about-to-run) subscription has changeable days at
        // all. A paused/cancelled/completed one is shown read-only.
        const isLive = ["active", "scheduled"].includes(sub.status);

        const days = [];
        const total = Math.max(0, (daysBetween(windowStart, windowEnd) ?? -1) + 1);
        for (let i = 0; i < total; i++) {
            const date = addDays(windowStart, i);
            const row = logRows.get(date) || null;
            const locked = isDateLocked(date, cutoffHour);

            let status;
            let toggledBy = null;
            let reason = null;
            let creditDeducted = false;
            let orderId = null;

            if (row) {
                status = row.status;
                toggledBy = row.toggled_by;
                reason = row.reason;
                creditDeducted = !!row.credit_deducted;
                orderId = row.order_id;
            } else if (closures.has(date)) {
                // Projected from the cook's closure list. There is no row yet
                // because the fan-out only reaches subscribers who existed when
                // the cook closed the date — a subscription that activates later
                // must still show the closure rather than a false "Scheduled".
                status = DAY_STATUS.COOK_UNAVAILABLE;
                toggledBy = "cook";
                reason = closures.get(date);
            } else {
                status = DAY_STATUS.SCHEDULED;
            }

            days.push({
                date,
                status,
                label: DAY_STATUS_LABELS[status] || status,
                toggled_by: toggledBy,
                reason,
                credit_deducted: creditDeducted,
                order_id: orderId,
                is_today: date === today,
                is_locked: locked,
                // A day is skippable only if a meal is actually still expected on
                // it AND the cutoff hasn't passed. Days the cook already closed
                // are excluded — there's nothing left to skip.
                can_skip: isLive && !locked && status === DAY_STATUS.SCHEDULED,
                // The swap already on this day, if any. `can_request_custom` uses
                // exactly the same conditions the create endpoint enforces
                // (delivering day, before cutoff, nothing already asked for), so
                // the UI never offers a button the server will refuse.
                custom_meal: (() => {
                    const swap = swapRows.get(date);
                    if (!swap) return null;
                    return {
                        request_id: swap.id,
                        status: swap.status,
                        meal_id: swap.meal_id,
                        meal_name: swap.meal_name,
                        note: swap.note
                    };
                })(),
                can_request_custom: isLive && !locked
                    && status === DAY_STATUS.SCHEDULED
                    && !swapRows.has(date),
                // Surfaced even when false so the UI can grey the toggle out with
                // "Cutoff passed" rather than hiding it and looking broken.
                locked_message: locked
                    ? `Too late to change this day — the cutoff was ${formatCutoffLabel(cutoffHour)} on ${addDays(date, -1)}.`
                    : null
            });
        }

        const nextEditableDate = getNptTomorrow();
        return res.status(200).json({
            success: true,
            viewer: isCustomer ? "customer" : "cook",
            today,
            subscription: {
                id: sub.id,
                status: sub.status,
                payment_status: sub.payment_status,
                plan_name: sub.plan_name,
                duration: sub.duration,
                customer_name: sub.customer_name,
                cook_name: sub.cook_name,
                start_date: sub.start_date,
                end_date: sub.end_date,
                next_delivery_date: sub.next_delivery_date,
                meals_total: sub.meals_total,
                meals_remaining: sub.meals_remaining,
                // Drives "Starts Sep 3, 2026 · in 7 days" for a scheduled
                // subscription, which must read differently from an active one.
                days_until_start: sub.start_date ? Math.max(0, daysBetween(today, sub.start_date)) : null
            },
            cutoff: {
                hour: cutoffHour,
                label: formatCutoffLabel(cutoffHour),
                next_editable_date: nextEditableDate,
                ms_until_cutoff: msUntilCutoff(nextEditableDate, cutoffHour)
            },
            window: { from: windowStart, to: windowEnd },
            days
        });
    } catch (error) {
        console.error("getSubscriptionCalendar error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * POST /api/subscriptions/:id/skip-day — customer only.
 * Body: { date: 'YYYY-MM-DD', reason? }
 *
 * Skipping is free: credit_deducted stays FALSE and end_date slides out by a
 * day, so the customer still receives every meal they paid for — just not on
 * that date. This replaces the old skipped_dates JSON append, which recorded no
 * actor, no reason and no timestamp, and which two concurrent writes could
 * clobber (read-modify-write on a JSON column).
 *
 * Rejections are specific on purpose. "Too late" and "the kitchen is closed
 * anyway" call for completely different reactions from the customer, and a
 * generic failure teaches them to just retry.
 */
export const skipDay = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { date, reason } = req.body;

        if (!isValidDateString(date)) {
            return res.status(400).json({
                success: false,
                message: "date is required, as a real calendar date in YYYY-MM-DD form."
            });
        }
        const target = date.trim();

        const [subs] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id, s.status, s.meals_remaining,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')   AS end_date,
                    p.name AS plan_name
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];

        // Ownership against req.user.id — the :id alone proves nothing.
        if (sub.customer_id !== customerId) {
            return res.status(403).json({ success: false, message: "This subscription does not belong to you." });
        }
        if (!["active", "scheduled"].includes(sub.status)) {
            return res.status(400).json({
                success: false,
                message: `Only a running or scheduled subscription has days to skip — this one is "${sub.status.replace(/_/g, " ")}".`
            });
        }

        const cutoffHour = await getCutoffHour();
        const today = getNptToday();

        if (daysBetween(today, target) < 0) {
            return res.status(400).json({ success: false, message: "That date has already passed." });
        }
        if (sub.start_date && daysBetween(sub.start_date, target) < 0) {
            return res.status(400).json({
                success: false,
                message: `Deliveries don't start until ${sub.start_date} — there's nothing to skip on ${target}.`
            });
        }
        if (sub.end_date && daysBetween(sub.end_date, target) > 0) {
            return res.status(400).json({
                success: false,
                message: `${target} is past the end of this subscription (${sub.end_date}).`
            });
        }
        // The cutoff. Note this also rejects TODAY without a separate branch —
        // today's cutoff was yesterday evening, which is unavoidably past.
        if (isDateLocked(target, cutoffHour)) {
            return res.status(409).json({
                success: false,
                code: "cutoff_passed",
                message: target === today
                    ? `Too late to change today's meal — the cutoff was ${formatCutoffLabel(cutoffHour)} yesterday.`
                    : `Too late to change ${target === getNptTomorrow() ? "tomorrow's" : `the meal on ${target}`} — cutoff was ${formatCutoffLabel(cutoffHour)} on ${addDays(target, -1)}.`,
                cutoff: { hour: cutoffHour, label: formatCutoffLabel(cutoffHour) }
            });
        }

        const connection = await db.promise().getConnection();
        let extendedTo = sub.end_date;
        try {
            await connection.beginTransaction();

            const outcome = await applyDayStatus(connection, {
                subscriptionId: sub.id,
                deliveryDate: target,
                status: DAY_STATUS.CUSTOMER_SKIPPED,
                toggledBy: "customer",
                reason: reason ? String(reason).slice(0, 500) : null,
                creditDeducted: false,
                // Only a day that was actually going to be delivered. Without
                // this, skipping a day the cook had already closed would overwrite
                // the cook's reason AND extend the cycle a second time for a
                // single non-delivered day. It is also what makes the
                // 'cook_unavailable' branch below reachable at all.
                onlyFrom: [DAY_STATUS.SCHEDULED]
            });

            if (!outcome.applied) {
                await connection.rollback();

                // Both attempts are recorded even when the write loses, so a
                // later dispute shows what each side tried to do and when.
                await logDayEvent({
                    subscriptionId: sub.id,
                    event: "skip_rejected",
                    actor: "customer",
                    detail: `Skip of ${target} not applied — day is "${outcome.previousStatus}" (${outcome.blockedBy}).`
                });

                if (outcome.blockedBy === "delivered") {
                    return res.status(409).json({
                        success: false,
                        code: "already_delivered",
                        message: `${target}'s meal has already been delivered, so it can't be skipped.`
                    });
                }
                if (outcome.previousStatus === DAY_STATUS.CUSTOMER_SKIPPED) {
                    return res.status(200).json({
                        success: true,
                        code: "already_skipped",
                        message: `You'd already skipped ${target}. Nothing changed.`,
                        day: { date: target, status: DAY_STATUS.CUSTOMER_SKIPPED }
                    });
                }
                if (outcome.previousStatus === DAY_STATUS.COOK_UNAVAILABLE) {
                    // The customer's goal (no meal, no charge) already holds.
                    // Overwriting would erase the cook's reason for closing.
                    return res.status(200).json({
                        success: true,
                        code: "cook_unavailable",
                        message: `The kitchen is already closed on ${target}, so no meal was coming and no credit was used.`,
                        day: { date: target, status: DAY_STATUS.COOK_UNAVAILABLE }
                    });
                }
                return res.status(409).json({
                    success: false,
                    code: "not_applied",
                    message: `Couldn't skip ${target} — it's currently "${outcome.previousStatus}".`
                });
            }

            // ── A skip in advance moves the meal to the end ────────────────────
            // The rule: a day skipped BEFORE that day arrives is a meal the cook
            // never cooked and the customer never got, so the window grows by one
            // day and they still receive what they paid for. A day that simply
            // passed undelivered (cook closed, or the cutoff went by) does not
            // extend anything — and this endpoint can't produce that case, because
            // isDateLocked() above already rejected every locked date. So every
            // skip that reaches here is an advance skip.
            //
            // ponytail: no cap on total extensions. A customer who skips every day
            // stretches the window indefinitely, one day at a time. Add a ceiling
            // (e.g. extensions <= duration_days) if that turns out to be abused.
            let newEndDate = sub.end_date;
            if (sub.end_date) {
                newEndDate = addDays(sub.end_date, 1);
                await connection.query(
                    `UPDATE subscriptions SET end_date = ? WHERE id = ?`,
                    [newEndDate, sub.id]
                );
            }
            await connection.commit();
            extendedTo = newEndDate;
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }

        await logDayEvent({
            subscriptionId: sub.id,
            event: "day_skipped",
            actor: "customer",
            detail: `Customer skipped ${target}${reason ? ` — "${String(reason).slice(0, 200)}"` : ""}. No meal, no charge.`
                + (extendedTo && extendedTo !== sub.end_date
                    ? ` End date extended ${sub.end_date} → ${extendedTo}.`
                    : " End date unchanged (none set yet).")
        });

        const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [customerId]);
        await notifySkipDay(sub.cook_id, sub.id, customer?.full_name || "A customer", sub.plan_name, target, reason);

        return res.status(200).json({
            success: true,
            message: extendedTo && extendedTo !== sub.end_date
                ? `${target} skipped — the cook won't prepare it, and your subscription now runs one day longer, to ${extendedTo}.`
                : `${target} skipped — the cook has been told not to prepare it.`,
            day: { date: target, status: DAY_STATUS.CUSTOMER_SKIPPED, credit_deducted: false },
            end_date: extendedTo || null,
            extended: !!(extendedTo && extendedTo !== sub.end_date)
        });
    } catch (error) {
        console.error("skipDay error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * DELETE /api/subscriptions/:id
 * Cancel a subscription — and say what is owed.
 *
 * THE POLICY, as specified: once the cook has confirmed the payment the customer
 * owes the FULL plan amount, however early they cancel and however many days
 * they skipped. Before that confirmation nothing is owed. There is no proration
 * either way — the cook committed their kitchen to the whole window. (Skipping a
 * day in advance moves that meal to the end of the window rather than refunding
 * it, which is why a skip never reduces what is owed.)
 *
 * ASSUMPTION, because the rule as given has two readings for the gap between
 * "cook confirmed" and "first delivery": a verified/scheduled subscription is
 * treated as CHARGEABLE IN FULL. The money is already with the cook by then (this
 * is a manual transfer the cook verified by eye) and they have blocked out the
 * days. The alternative reading — free until the first meal — would let a
 * customer tie up a cook's capacity for a month and walk away, and would require
 * clawing money back from the cook rather than never taking it.
 *
 * NOT DONE, and deliberately: no refund_requests row is created when the customer
 * had already paid but the cook never confirmed. refund_requests.order_id is NOT
 * NULL with a foreign key to orders(id), so a subscription refund has nowhere to
 * live in that table without a schema change. The amount is recorded in the audit
 * trail and returned to the caller instead, and settling it is an admin action.
 */
export const cancelSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            `SELECT s.id, s.cook_id, s.status, s.payment_status,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')   AS end_date,
                    p.duration, p.price_per_delivery
             FROM subscriptions s
             JOIN subscription_plans p ON p.id = s.plan_id
             WHERE s.id = ? AND (s.customer_id = ? OR s.cook_id = ?)`,
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        const sub = subs[0];
        const actor = userId === sub.cook_id ? "cook" : "customer";

        // Already over. Cancelling again would rewrite a finished record and
        // re-log a charge decision that was settled at the time.
        if (["cancelled", "completed"].includes(sub.status)) {
            return res.status(409).json({
                success: false,
                message: sub.status === "completed"
                    ? "This subscription already ran to its end date."
                    : "This subscription was already cancelled."
            });
        }

        const confirmed = ["verified", "scheduled", "active", "paused"].includes(sub.status);
        // price_per_delivery is the plan's ONE-TIME price for the whole window, not
        // a daily rate — the column name is legacy. Multiplying it by the day count
        // billed a 7-day plan seven times over.
        const fullAmount = sub.price_per_delivery === null
            ? null
            : Number(Number(sub.price_per_delivery).toFixed(2));

        // Paid, but the cook never confirmed it. Nothing is owed, so whatever was
        // transferred is owed back — the one case that needs a human.
        const paidButUnconfirmed = !confirmed
            && ["submitted", "verified"].includes(sub.payment_status);

        const amountOwed = confirmed ? fullAmount : 0;
        const refundDue = paidButUnconfirmed ? fullAmount : 0;

        await db.promise().query(
            "UPDATE subscriptions SET status = 'cancelled' WHERE id = ?",
            [id]
        );

        const moneyNote = confirmed
            ? `Full plan amount is payable (${fullAmount === null ? "price not set" : "Rs. " + fullAmount}) — no proration, cancelled from "${sub.status}".`
            : refundDue
                ? `Nothing is owed: the cook never confirmed the payment. Rs. ${fullAmount} is refundable and needs an admin to settle it.`
                : "Nothing is owed — the cook had not confirmed this subscription.";

        await logDayEvent({
            subscriptionId: id,
            event: "cancelled",
            actor,
            detail: `Cancelled by ${actor} from "${sub.status}". No further delivery days will be created. ${moneyNote}`
        });

        return res.status(200).json({
            success: true,
            message: confirmed
                ? `Cancelled. ${fullAmount === null ? "The full plan amount" : "Rs. " + fullAmount} still applies — cancelling part-way through doesn't reduce it.`
                : refundDue
                    ? `Cancelled. Nothing is owed, and Rs. ${fullAmount} is refundable — support will settle it.`
                    : `Cancelled. Nothing is owed.`,
            amount_owed: amountOwed,
            refund_due: refundDue,
            cancelled_from: sub.status
        });

    } catch (error) {
        console.error("cancelSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
