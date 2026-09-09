/**
 * Cook-side delivery operations for subscriptions: closing a whole date, and
 * seeing who is expecting a meal.
 *
 * Separate from cookController.js (profile/dashboard) because this is
 * subscription-delivery domain, and separate from subscriptionController.js
 * because those endpoints mount under /api/cook where the cook role is the
 * default rather than the exception.
 */

import db from "../config/db.js";
import {
    notifyCookUnavailable,
    notifyCookDeliveryUnavailable,
    notifyCookDeliveryRestored
} from "../utils/notificationHelper.js";
import {
    isValidDateString,
    getNptToday,
    getNptTomorrow,
    addDays,
    daysBetween,
    isDateLocked,
    msUntilCutoff,
    formatCutoffLabel,
    getCutoffHour
} from "../utils/nptTime.js";
import {
    DAY_STATUS,
    DAY_STATUS_LABELS,
    NON_DELIVERY_STATUSES,
    applyDayStatus,
    isCookClosedOn,
    logDayEvent
} from "../utils/subscriptionDailyLog.js";

/** How far ahead a cook may declare a closure. Matches the customer's start-date
 *  horizon: there is no point closing a date no subscription can reach. */
export const MAX_CLOSURE_DAYS = 30;

/** First not-skipped/not-closed date, bounded by the subscription end date. */
const findNextExpectedDeliveryDate = async (executor, subscriptionId, fromDate, endDate) => {
    if (!endDate || daysBetween(fromDate, endDate) < 0) return null;
    const [rows] = await executor.query(
        `SELECT DATE_FORMAT(delivery_date, '%Y-%m-%d') AS delivery_date
         FROM subscription_daily_log
         WHERE subscription_id = ? AND delivery_date BETWEEN ? AND ?
           AND status IN (?)`,
        [subscriptionId, fromDate, endDate, NON_DELIVERY_STATUSES]
    );
    const blocked = new Set(rows.map(row => row.delivery_date));
    const span = daysBetween(fromDate, endDate);
    for (let i = 0; i <= span; i++) {
        const candidate = addDays(fromDate, i);
        if (!blocked.has(candidate)) return candidate;
    }
    return null;
};

/** Paid calendar span before any customer skip/cook-cancellation extensions. */
const getPlanDurationDays = duration => {
    if (duration === "2_weeks" || duration === "biweekly") return 14;
    if (duration === "monthly" || duration === "1_month") return 30;
    return 7;
};

/**
 * POST /api/cook/subscriptions/:id/unavailable-day — cook only.
 * Body: { date: 'YYYY-MM-DD', reason: string }
 *
 * Cancels exactly one subscriber's future delivery. This deliberately does not
 * write cook_daily_availability, so no other subscriber can be affected. The
 * day is free and the subscription receives one replacement day at its end.
 */
export const markSubscriptionDeliveryUnavailable = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const target = String(req.body.date || "").trim();
        const reason = typeof req.body.reason === "string" ? req.body.reason.trim().slice(0, 500) : "";

        if (!isValidDateString(target)) {
            return res.status(400).json({
                success: false,
                message: "date is required, as a real calendar date in YYYY-MM-DD form."
            });
        }
        if (!reason) {
            return res.status(400).json({ success: false, message: "Tell the customer why this delivery is unavailable." });
        }

        const cutoffHour = await getCutoffHour();
        const today = getNptToday();
        if (daysBetween(today, target) < 0) {
            return res.status(400).json({ success: false, message: "That delivery date has already passed." });
        }
        if (isDateLocked(target, cutoffHour)) {
            return res.status(409).json({
                success: false,
                code: "cutoff_passed",
                message: target === today
                    ? `Too late to cancel today's delivery — the cutoff was ${formatCutoffLabel(cutoffHour)} yesterday. Contact the customer directly.`
                    : `Too late to cancel ${target === getNptTomorrow() ? "tomorrow's" : `the delivery on ${target}`} — cutoff was ${formatCutoffLabel(cutoffHour)} on ${addDays(target, -1)}.`,
                cutoff: { hour: cutoffHour, label: formatCutoffLabel(cutoffHour) }
            });
        }

        const connection = await db.promise().getConnection();
        let sub;
        let newEndDate = null;
        let newNextDeliveryDate = null;
        try {
            await connection.beginTransaction();
            const [subs] = await connection.query(
                `SELECT s.id, s.customer_id, s.cook_id, s.status, s.payment_status,
                        DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                        DATE_FORMAT(s.end_date, '%Y-%m-%d') AS end_date,
                        DATE_FORMAT(s.next_delivery_date, '%Y-%m-%d') AS next_delivery_date,
                        p.name AS plan_name
                 FROM subscriptions s
                 JOIN subscription_plans p ON p.id = s.plan_id
                 WHERE s.id = ?
                 FOR UPDATE`,
                [id]
            );
            if (subs.length === 0) {
                await connection.rollback();
                return res.status(404).json({ success: false, message: "Subscription not found." });
            }
            sub = subs[0];
            if (sub.cook_id !== cookId) {
                await connection.rollback();
                return res.status(403).json({ success: false, message: "This subscription is not assigned to your kitchen." });
            }
            if (!["active", "scheduled"].includes(sub.status) || sub.payment_status !== "verified") {
                await connection.rollback();
                return res.status(409).json({ success: false, message: "Only a verified running or scheduled subscription has deliveries to change." });
            }
            if ((sub.start_date && daysBetween(sub.start_date, target) < 0)
                    || (sub.end_date && daysBetween(sub.end_date, target) > 0)) {
                await connection.rollback();
                return res.status(400).json({ success: false, message: `${target} is outside this subscription's delivery dates.` });
            }
            if (!sub.end_date) {
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    message: "This legacy subscription has no end date, so a replacement day cannot be added safely. Contact support."
                });
            }

            const [[existingDay]] = await connection.query(
                `SELECT status, credit_deducted, order_id
                 FROM subscription_daily_log
                 WHERE subscription_id = ? AND delivery_date = ?
                 FOR UPDATE`,
                [sub.id, target]
            );
            if (existingDay && (existingDay.credit_deducted || existingDay.order_id)) {
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    code: "delivery_already_prepared",
                    message: "This delivery already has a charged meal or order attached, so it cannot be cancelled here. Contact the customer and support."
                });
            }

            // A date-wide closure is a separate bulk operation. A late-activated
            // subscription can inherit it without having a day row yet, so check
            // the closure source before creating a per-customer record.
            if (await isCookClosedOn(connection, cookId, target)) {
                await connection.rollback();
                return res.status(200).json({
                    success: true,
                    code: "kitchen_already_closed",
                    message: `Your kitchen is already marked unavailable on ${target}; this customer is already covered. Nothing else changed.`
                });
            }

            const outcome = await applyDayStatus(connection, {
                subscriptionId: sub.id,
                deliveryDate: target,
                status: DAY_STATUS.COOK_DELIVERY_UNAVAILABLE,
                toggledBy: "cook",
                reason,
                creditDeducted: false,
                onlyFrom: [DAY_STATUS.SCHEDULED]
            });

            if (!outcome.applied) {
                await connection.rollback();
                if (outcome.previousStatus === DAY_STATUS.COOK_DELIVERY_UNAVAILABLE) {
                    return res.status(200).json({
                        success: true,
                        code: "already_unavailable",
                        message: `${target} is already marked unavailable for this customer. Nothing changed.`
                    });
                }
                if (outcome.previousStatus === DAY_STATUS.CUSTOMER_SKIPPED) {
                    return res.status(200).json({
                        success: true,
                        code: "customer_already_skipped",
                        message: `The customer already skipped ${target}, so no delivery was expected and no extra day was added.`
                    });
                }
                return res.status(409).json({
                    success: false,
                    code: "day_not_changeable",
                    message: `${target} cannot be marked unavailable because it is currently "${outcome.previousStatus || "scheduled"}".`
                });
            }

            // Do not destroy a pending/accepted meal-change request here. The
            // request is temporarily non-actionable while this day is closed,
            // and becomes valid again if the cook restores the delivery.

            newEndDate = sub.end_date ? addDays(sub.end_date, 1) : null;
            newNextDeliveryDate = sub.next_delivery_date;
            if (!newNextDeliveryDate || newNextDeliveryDate === target) {
                newNextDeliveryDate = await findNextExpectedDeliveryDate(
                    connection, sub.id, target, newEndDate
                );
            }
            await connection.query(
                `UPDATE subscriptions
                 SET end_date = ?, next_delivery_date = ?
                 WHERE id = ?`,
                [newEndDate, newNextDeliveryDate, sub.id]
            );

            await logDayEvent({
                subscriptionId: sub.id,
                event: "cook_delivery_unavailable",
                actor: "cook",
                detail: `Cook cancelled only this subscription's ${target} delivery — "${reason}". `
                    + `No credit charged.${newEndDate ? ` End date extended ${sub.end_date} → ${newEndDate}.` : ""}`,
                executor: connection
            });
            await connection.commit();
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }

        const [[cook]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [cookId]);
        const cookName = cook?.full_name || "Your cook";
        await notifyCookDeliveryUnavailable(
            sub.customer_id, sub.id, cookName, sub.plan_name, target, reason, newEndDate
        );

        const io = req.app.get("io");
        if (io) {
            io.to(`user_${sub.customer_id}`).emit("newNotification", {
                type: "cook_delivery_unavailable",
                title: "Delivery Unavailable",
                message: `${cookName} can't deliver on ${target}. A replacement day was added to your plan.`,
                reference_id: Number(sub.id),
                reference_type: "subscription",
                created_at: new Date().toISOString()
            });
        }

        return res.status(200).json({
            success: true,
            message: newEndDate
                ? `${target} marked unavailable for this customer only. Their replacement delivery extends the plan to ${newEndDate}.`
                : `${target} marked unavailable for this customer only.`,
            day: { date: target, status: DAY_STATUS.COOK_DELIVERY_UNAVAILABLE, credit_deducted: false },
            end_date: newEndDate,
            next_delivery_date: newNextDeliveryDate,
            affected_subscriptions: 1
        });
    } catch (error) {
        console.error("markSubscriptionDeliveryUnavailable error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * DELETE /api/cook/subscriptions/:id/unavailable-day/:date — cook only.
 *
 * Reverses only this cook's per-customer cancellation. It cannot reopen a
 * whole-kitchen closure, a customer skip, or a settled delivery. Exactly one
 * replacement-day extension is removed when the guarded status transition
 * succeeds; retries are therefore harmless.
 */
export const restoreSubscriptionDelivery = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const target = String(req.params.date || "").trim();

        if (!isValidDateString(target)) {
            return res.status(400).json({
                success: false,
                message: "date must be a real calendar date in YYYY-MM-DD form."
            });
        }

        const cutoffHour = await getCutoffHour();
        const today = getNptToday();
        if (daysBetween(today, target) < 0) {
            return res.status(400).json({ success: false, message: "That delivery date has already passed." });
        }
        if (isDateLocked(target, cutoffHour)) {
            return res.status(409).json({
                success: false,
                code: "cutoff_passed",
                message: `Too late to restore ${target} — the delivery-change cutoff was ${formatCutoffLabel(cutoffHour)} on ${addDays(target, -1)}. Contact the customer directly.`,
                cutoff: { hour: cutoffHour, label: formatCutoffLabel(cutoffHour) }
            });
        }

        const connection = await db.promise().getConnection();
        let sub;
        let newEndDate;
        let newNextDeliveryDate;
        try {
            await connection.beginTransaction();
            const [subs] = await connection.query(
                `SELECT s.id, s.customer_id, s.cook_id, s.status, s.payment_status,
                        DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                        DATE_FORMAT(s.end_date, '%Y-%m-%d') AS end_date,
                        DATE_FORMAT(s.next_delivery_date, '%Y-%m-%d') AS next_delivery_date,
                        p.name AS plan_name, p.duration
                 FROM subscriptions s
                 JOIN subscription_plans p ON p.id = s.plan_id
                 WHERE s.id = ?
                 FOR UPDATE`,
                [id]
            );
            if (subs.length === 0) {
                await connection.rollback();
                return res.status(404).json({ success: false, message: "Subscription not found." });
            }
            sub = subs[0];
            if (sub.cook_id !== cookId) {
                await connection.rollback();
                return res.status(403).json({ success: false, message: "This subscription is not assigned to your kitchen." });
            }
            if (!["active", "scheduled"].includes(sub.status) || sub.payment_status !== "verified") {
                await connection.rollback();
                return res.status(409).json({ success: false, message: "Only a verified running or scheduled subscription has deliveries to restore." });
            }

            const [[existingDay]] = await connection.query(
                `SELECT status, toggled_by, credit_deducted, order_id
                 FROM subscription_daily_log
                 WHERE subscription_id = ? AND delivery_date = ?
                 FOR UPDATE`,
                [sub.id, target]
            );
            if (!existingDay && await isCookClosedOn(connection, cookId, target)) {
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    code: "day_not_restorable",
                    message: `${target} is part of a whole-kitchen closure. Reopen it from Today's Deliveries instead.`
                });
            }
            if (!existingDay || existingDay.status === DAY_STATUS.SCHEDULED) {
                await connection.rollback();
                return res.status(200).json({
                    success: true,
                    code: "already_restored",
                    message: `${target} is already scheduled. Nothing changed.`
                });
            }
            if (existingDay.status !== DAY_STATUS.COOK_DELIVERY_UNAVAILABLE
                    || existingDay.toggled_by !== "cook") {
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    code: "day_not_restorable",
                    message: existingDay.status === DAY_STATUS.COOK_UNAVAILABLE
                        ? `${target} is part of a whole-kitchen closure. Reopen it from Today's Deliveries instead.`
                        : `${target} cannot be restored here because it is currently "${existingDay.status}".`
                });
            }
            if (existingDay.credit_deducted || existingDay.order_id) {
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    code: "delivery_already_prepared",
                    message: "This delivery has a charged meal or order attached and cannot be restored safely. Contact support."
                });
            }

            const outcome = await applyDayStatus(connection, {
                subscriptionId: sub.id,
                deliveryDate: target,
                status: DAY_STATUS.SCHEDULED,
                toggledBy: "cook",
                reason: null,
                creditDeducted: false,
                onlyFrom: [DAY_STATUS.COOK_DELIVERY_UNAVAILABLE]
            });
            if (!outcome.applied) {
                await connection.rollback();
                if (outcome.previousStatus === DAY_STATUS.SCHEDULED) {
                    return res.status(200).json({
                        success: true,
                        code: "already_restored",
                        message: `${target} was already restored. Nothing changed.`
                    });
                }
                return res.status(409).json({
                    success: false,
                    code: "day_not_restorable",
                    message: `${target} changed while you were restoring it. Refresh the schedule and try again.`
                });
            }

            const originalEndDate = addDays(sub.start_date, getPlanDurationDays(sub.duration) - 1);
            const shortenedEndDate = addDays(sub.end_date, -1);
            newEndDate = daysBetween(originalEndDate, shortenedEndDate) < 0
                ? originalEndDate
                : shortenedEndDate;

            newNextDeliveryDate = sub.next_delivery_date;
            if (!newNextDeliveryDate || daysBetween(target, newNextDeliveryDate) > 0) {
                newNextDeliveryDate = target;
            }
            if (newNextDeliveryDate && daysBetween(newNextDeliveryDate, newEndDate) < 0) {
                newNextDeliveryDate = await findNextExpectedDeliveryDate(connection, sub.id, today, newEndDate);
            }

            await connection.query(
                `UPDATE subscriptions SET end_date = ?, next_delivery_date = ? WHERE id = ?`,
                [newEndDate, newNextDeliveryDate, sub.id]
            );
            await logDayEvent({
                subscriptionId: sub.id,
                event: "cook_delivery_restored",
                actor: "cook",
                detail: `Cook restored only this subscription's ${target} delivery. `
                    + `End date shortened ${sub.end_date} → ${newEndDate}.`,
                executor: connection
            });
            await connection.commit();
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }

        const [[cook]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [cookId]);
        const cookName = cook?.full_name || "Your cook";
        await notifyCookDeliveryRestored(
            sub.customer_id, sub.id, cookName, sub.plan_name, target, newEndDate
        );

        const io = req.app.get("io");
        if (io) {
            io.to(`user_${sub.customer_id}`).emit("newNotification", {
                type: "cook_delivery_restored",
                title: "Delivery Restored",
                message: `${cookName} can deliver on ${target} after all. Your meal is scheduled again.`,
                reference_id: Number(sub.id),
                reference_type: "subscription",
                created_at: new Date().toISOString()
            });
        }

        return res.status(200).json({
            success: true,
            message: `${target} is scheduled again for this customer. Their plan now ends on ${newEndDate}.`,
            day: { date: target, status: DAY_STATUS.SCHEDULED, credit_deducted: false },
            end_date: newEndDate,
            next_delivery_date: newNextDeliveryDate,
            affected_subscriptions: 1
        });
    } catch (error) {
        console.error("restoreSubscriptionDelivery error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * POST /api/cook/daily-availability — cook only.
 * Body: { date: 'YYYY-MM-DD', reason? }
 *
 * A BULK operation: it closes the date for EVERY one of this cook's running
 * subscriptions, in a single transaction. Either all of the affected subscribers
 * get a cook_unavailable day, or none do — a partial
 * fan-out would leave some customers expecting food from a shut kitchen and
 * others correctly told it's closed, with nothing to distinguish them.
 *
 * The cook_daily_availability row is kept alongside the fan-out because the
 * fan-out can only reach subscriptions that EXIST right now. A subscription that
 * activates tomorrow for this date has to be able to discover the closure, which
 * it does by reading that row (see getSubscriptionCalendar and the activation
 * path in verifySubscriptionPayment).
 *
 * Note on the cutoff: the earliest closable date is TOMORROW, never today.
 * Today's cutoff passed yesterday evening, and customers have already been told
 * a meal is coming. That is why the cook UI's bulk button targets tomorrow and
 * counts down to tonight's cutoff.
 */
/**
 * What actually happened, in words the cook can act on.
 *
 * Kept honest about the days a closure could NOT touch. "No subscribers were
 * expecting a meal that day" is flatly wrong when one of them had already been
 * delivered to — the cook needs to know the closure didn't reach those people,
 * because contacting them is then a manual job.
 */
function buildClosureMessage({ date, notified, closureCreated, skippedDelivered, alreadySettled }) {
    const untouched = [];
    if (skippedDelivered > 0) {
        untouched.push(`${skippedDelivered} had already been delivered to`);
    }
    if (alreadySettled > 0) {
        untouched.push(`${alreadySettled} had already skipped it or were already closed`);
    }
    const tail = untouched.length > 0
        ? ` Left unchanged: ${untouched.join(", ")} — no extra day was added for those.`
        : "";

    const head = closureCreated ? `${date} marked unavailable.` : `${date} was already marked unavailable.`;

    if (notified > 0) {
        return `${head} ${notified} subscriber${notified === 1 ? "" : "s"} notified`
            + ` — none of them were charged for that day.${tail}`;
    }
    if (untouched.length > 0) {
        return `${head} Nobody was notified.${tail}`;
    }
    return `${head} No subscribers were expecting a meal that day.`;
}

export const setCookDailyUnavailability = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { date, reason } = req.body;

        if (!isValidDateString(date)) {
            return res.status(400).json({
                success: false,
                message: "date is required, as a real calendar date in YYYY-MM-DD form."
            });
        }
        const target = date.trim();

        const cutoffHour = await getCutoffHour();
        const today = getNptToday();
        const tomorrow = getNptTomorrow();
        const offset = daysBetween(today, target);

        if (offset < 0) {
            return res.status(400).json({ success: false, message: "That date has already passed." });
        }
        if (offset > MAX_CLOSURE_DAYS) {
            return res.status(400).json({
                success: false,
                message: `You can close dates up to ${MAX_CLOSURE_DAYS} days ahead (latest: ${addDays(today, MAX_CLOSURE_DAYS)}).`
            });
        }
        // Rejects today automatically — today's cutoff is always in the past.
        if (isDateLocked(target, cutoffHour)) {
            return res.status(409).json({
                success: false,
                code: "cutoff_passed",
                message: target === today
                    ? `Too late to close today — the cutoff was ${formatCutoffLabel(cutoffHour)} yesterday. Contact affected customers directly.`
                    : `Too late to close ${target === tomorrow ? "tomorrow" : target} — cutoff was ${formatCutoffLabel(cutoffHour)} on ${addDays(target, -1)}.`,
                cutoff: { hour: cutoffHour, label: formatCutoffLabel(cutoffHour) }
            });
        }

        const trimmedReason = reason ? String(reason).slice(0, 500) : null;

        const connection = await db.promise().getConnection();
        // Collected inside the transaction, notified after it commits: a push
        // sent from inside a transaction that then rolls back tells customers
        // about a closure that never happened, and it can't be recalled.
        const notifyQueue = [];
        let closureCreated = false;
        let skippedDelivered = 0;
        let alreadySettled = 0;

        try {
            await connection.beginTransaction();

            // UNIQUE (cook_id, unavailable_date) makes a double tap idempotent
            // rather than a second fan-out.
            const [ins] = await connection.query(
                `INSERT IGNORE INTO cook_daily_availability (cook_id, unavailable_date, reason)
                 VALUES (?, ?, ?)`,
                [cookId, target, trimmedReason]
            );
            closureCreated = ins.affectedRows === 1;

            // Locked FOR UPDATE so a subscription can't slip from 'scheduled' to
            // 'active' (or have its own skip written) between this read and the
            // fan-out below. Deliberately no join to subscription_plans: nothing
            // here needs the plan name, and FOR UPDATE would take locks on it
            // that could contend with a cook editing a plan mid-closure.
            const [affected] = await connection.query(
                `SELECT s.id, s.customer_id, s.end_date, s.status
                 FROM subscriptions s
                 WHERE s.cook_id = ?
                   AND s.status IN ('active', 'scheduled')
                   AND s.payment_status = 'verified'
                   AND (s.start_date IS NULL OR s.start_date <= ?)
                   AND (s.end_date IS NULL OR s.end_date >= ?)
                 FOR UPDATE`,
                [cookId, target, target]
            );

            for (const sub of affected) {
                const outcome = await applyDayStatus(connection, {
                    subscriptionId: sub.id,
                    deliveryDate: target,
                    status: DAY_STATUS.COOK_UNAVAILABLE,
                    toggledBy: "cook",
                    reason: trimmedReason,
                    creditDeducted: false,
                    // Only a day that was actually going to be delivered. A day
                    // already settled as customer_skipped / cook_unavailable /
                    // delivered / missed is left exactly as it is: rewriting the
                    // reason on a day that is already settled would only muddy
                    // the audit trail.
                    onlyFrom: [DAY_STATUS.SCHEDULED]
                });

                if (!outcome.applied) {
                    // 'delivered' is terminal. A closure must never rewrite a
                    // meal the customer actually received — that would erase the
                    // record of food they got and the credit it cost.
                    if (outcome.blockedBy === "delivered") {
                        skippedDelivered++;
                        await logDayEvent({
                            subscriptionId: sub.id,
                            event: "closure_skipped",
                            actor: "cook",
                            detail: `Kitchen closure for ${target} not applied — that day was already delivered.`,
                            executor: connection
                        });
                    } else if (outcome.blockedBy === "not_overwritable" || outcome.blockedBy === "raced") {
                        // Already settled as not-being-delivered — most often the
                        // customer had skipped it themselves. Left untouched, and
                        // NOT compensated again. Audited because "the kitchen was
                        // closed but my day still says I skipped it" is a question
                        // someone will eventually ask.
                        alreadySettled++;
                        await logDayEvent({
                            subscriptionId: sub.id,
                            event: "closure_skipped",
                            actor: "cook",
                            detail: `Kitchen closure for ${target} not applied — day was already "${outcome.previousStatus}", `
                                + `which already means no meal that day.`,
                            executor: connection
                        });
                    }
                    // already_set: this date was already closed for them. Nothing
                    // changed and nothing to tell them.
                    continue;
                }

                // ── Calendar-day model: end_date does NOT move ────────────────
                // A subscription is a window of N calendar days fixed at
                // verification, not a pool of N meal credits. A closed kitchen
                // day means no meal that day; it does not push the end date out.
                // Extending here made the end date the customer agreed to drift
                // every time the cook closed a day, and let two independent
                // actors (cook closures, customer skips) both stretch the window.

                await logDayEvent({
                    subscriptionId: sub.id,
                    event: "cook_unavailable",
                    actor: "cook",
                    detail: `Cook closed ${target}${trimmedReason ? ` — "${trimmedReason}"` : ""}. `
                        + `Previous day status: ${outcome.previousStatus || "none"}. `
                        + `No meal that day; the ${sub.end_date || "subscription"} end date is unchanged.`,
                    executor: connection
                });

                notifyQueue.push({ customerId: sub.customer_id, subscriptionId: sub.id });
            }

            await connection.commit();
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }

        const [[cook]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [cookId]);
        const cookName = cook?.full_name || "Your cook";
        // Sequential rather than Promise.all: each of these fires an FCM push,
        // and a hundred at once is how a cook with a hundred subscribers gets
        // their pushes rate-limited into the void.
        for (const item of notifyQueue) {
            await notifyCookUnavailable(item.customerId, item.subscriptionId, cookName, target, trimmedReason);
        }

        return res.status(200).json({
            success: true,
            message: buildClosureMessage({
                date: target,
                notified: notifyQueue.length,
                closureCreated,
                skippedDelivered,
                alreadySettled
            }),
            date: target,
            already_marked: !closureCreated,
            affected_subscriptions: notifyQueue.length,
            skipped_already_delivered: skippedDelivered,
            skipped_already_settled: alreadySettled
        });
    } catch (error) {
        console.error("setCookDailyUnavailability error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * DELETE /api/cook/daily-availability/:date — cook only.
 * Reopens a date the cook had closed, provided its cutoff hasn't passed.
 *
 * Included because a closure is exactly the kind of thing that gets tapped by
 * mistake, and without this the only recovery is a support request. Days already
 * delivered are left alone; days the CUSTOMER skipped stay skipped — reopening
 * the kitchen doesn't un-skip someone's day for them.
 */
export const clearCookDailyUnavailability = async (req, res) => {
    try {
        const cookId = req.user.id;
        const target = (req.params.date || "").trim();

        if (!isValidDateString(target)) {
            return res.status(400).json({ success: false, message: "date must be a real calendar date in YYYY-MM-DD form." });
        }

        const cutoffHour = await getCutoffHour();
        if (isDateLocked(target, cutoffHour)) {
            return res.status(409).json({
                success: false,
                code: "cutoff_passed",
                message: `Too late to reopen ${target} — cutoff was ${formatCutoffLabel(cutoffHour)} on ${addDays(target, -1)}. Customers have already been told you're closed.`
            });
        }

        const connection = await db.promise().getConnection();
        let restored = 0;
        try {
            await connection.beginTransaction();

            const [del] = await connection.query(
                `DELETE FROM cook_daily_availability WHERE cook_id = ? AND unavailable_date = ?`,
                [cookId, target]
            );
            if (del.affectedRows === 0) {
                await connection.rollback();
                return res.status(404).json({ success: false, message: `${target} wasn't marked unavailable.` });
            }

            // Identified BEFORE the flip because afterwards these rows are
            // indistinguishable from days that were always scheduled; the list
            // is also the audit scope for this reopen operation.
            //
            // Only rows the CLOSURE created (toggled_by = 'cook') are in scope. A
            // customer_skipped day is theirs, not the cook's: reopening the
            // kitchen doesn't un-skip someone's day for them.
            const [closedRows] = await connection.query(
                `SELECT sdl.id, sdl.subscription_id
                 FROM subscription_daily_log sdl
                 JOIN subscriptions s ON s.id = sdl.subscription_id
                 WHERE s.cook_id = ? AND sdl.delivery_date = ?
                   AND sdl.status = 'cook_unavailable' AND sdl.toggled_by = 'cook'
                 FOR UPDATE`,
                [cookId, target]
            );

            if (closedRows.length > 0) {
                const rowIds = closedRows.map(r => r.id);
                const subIds = [...new Set(closedRows.map(r => r.subscription_id))];

                const [upd] = await connection.query(
                    `UPDATE subscription_daily_log
                     SET status = 'scheduled', toggled_by = 'cook',
                         reason = 'Kitchen reopened for this date', toggled_at = NOW()
                     WHERE id IN (?)`,
                    [rowIds]
                );
                restored = upd.affectedRows;

                for (const subId of subIds) {
                    await logDayEvent({
                        subscriptionId: subId,
                        event: "closure_reversed",
                        actor: "cook",
                        detail: `Cook reopened ${target}. Day back to scheduled; end date unchanged.`,
                        executor: connection
                    });
                }
            }

            await connection.commit();
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }

        return res.status(200).json({
            success: true,
            message: restored === 0
                ? `${target} reopened. No subscriber days needed restoring — nobody had been switched off for that date.`
                : `${target} reopened. ${restored} subscriber${restored === 1 ? " is" : "s are"} back on schedule for that day.`,
            date: target,
            restored_subscriptions: restored
        });
    } catch (error) {
        console.error("clearCookDailyUnavailability error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * GET /api/cook/today-deliveries — cook only.
 * Optional ?date=YYYY-MM-DD to look at another day (defaults to today NPT).
 *
 * The cook's operational answer to "what am I cooking, and for whom". Every
 * running subscription that has a day row for the date, with its real status —
 * so a customer who skipped shows as skipped instead of quietly disappearing
 * from the list, which is how a cook ends up making a meal nobody wants.
 *
 * Also returns the state of the bulk toggle for the next changeable date
 * (tomorrow) and the countdown to its cutoff, because that button and this list
 * are the same screen.
 */
export const getTodayDeliveries = async (req, res) => {
    try {
        const cookId = req.user.id;

        const requested = req.query.date;
        if (requested !== undefined && !isValidDateString(requested)) {
            return res.status(400).json({ success: false, message: "date must be a real calendar date in YYYY-MM-DD form." });
        }

        const today = getNptToday();
        const target = requested ? requested.trim() : today;
        const cutoffHour = await getCutoffHour();
        const tomorrow = getNptTomorrow();

        // LEFT JOIN, not INNER: a subscription that is active for this date but
        // whose row the cron hasn't created yet still owes a meal, and must
        // appear as 'scheduled' rather than vanish from the cook's list.
        const [rows] = await db.promise().query(
            `SELECT s.id AS subscription_id,
                    s.status AS subscription_status,
                    s.delivery_address,
                    s.meals_remaining,
                    u.id AS customer_id,
                    u.full_name AS customer_name,
                    u.phone AS customer_phone,
                    p.name AS plan_name,
                    p.duration,
                    COALESCE(sdl.status, 'scheduled') AS day_status,
                    sdl.toggled_by,
                    sdl.reason,
                    sdl.credit_deducted,
                    sdl.order_id
             FROM subscriptions s
             JOIN users u ON u.id = s.customer_id
             JOIN subscription_plans p ON p.id = s.plan_id
             LEFT JOIN subscription_daily_log sdl
                    ON sdl.subscription_id = s.id AND sdl.delivery_date = ?
             WHERE s.cook_id = ?
               AND s.status = 'active'
               AND s.payment_status = 'verified'
               AND (s.start_date IS NULL OR s.start_date <= ?)
               AND (s.end_date IS NULL OR s.end_date >= ?)
             ORDER BY u.full_name ASC`,
            [target, cookId, target, target]
        );

        const deliveries = rows.map(row => ({
            subscription_id: row.subscription_id,
            customer_id: row.customer_id,
            customer_name: row.customer_name,
            customer_phone: row.customer_phone,
            plan_name: row.plan_name,
            duration: row.duration,
            delivery_address: row.delivery_address,
            meals_remaining: row.meals_remaining,
            status: row.day_status,
            label: DAY_STATUS_LABELS[row.day_status] || row.day_status,
            toggled_by: row.toggled_by,
            reason: row.reason,
            credit_deducted: !!row.credit_deducted,
            order_id: row.order_id
        }));

        // ── Custom meal swaps for this date ───────────────────────────────────
        // The cook's daily list has to answer "what am I actually cooking today",
        // and an accepted swap changes that answer. A still-pending swap is
        // included too, flagged separately: it is work the cook has to decide on
        // before the cutoff, and hiding it here is how a request gets missed.
        // Attached onto the matching delivery row rather than listed apart, so
        // one customer is one line on the screen.
        const [swapRows] = await db.promise().query(
            `SELECT r.id, r.subscription_id, r.status, r.note, r.meal_id,
                    m.name AS meal_name, m.image_url AS meal_image
             FROM custom_meal_requests r
             LEFT JOIN meals m ON m.id = r.meal_id
             WHERE r.cook_id = ? AND r.delivery_date = ?
               AND r.status IN ('pending', 'accepted')`,
            [cookId, target]
        );
        const swapBySub = new Map(swapRows.map(r => [r.subscription_id, r]));

        deliveries.forEach(d => {
            const swap = swapBySub.get(d.subscription_id);
            d.custom_meal = swap && (d.status === DAY_STATUS.SCHEDULED || d.status === DAY_STATUS.SENT)
                ? {
                    request_id: swap.id,
                    status: swap.status,
                    meal_id: swap.meal_id,
                    meal_name: swap.meal_name,
                    meal_image: swap.meal_image,
                    note: swap.note,
                    // Drives the "Cook this instead" vs "Decide before cutoff"
                    // treatment on the card without the app re-deriving it.
                    is_confirmed: swap.status === "accepted"
                }
                : null;
        });

        const cookingCount = deliveries.filter(
            d => d.status === DAY_STATUS.SCHEDULED || d.status === DAY_STATUS.DELIVERED
        ).length;

        const [closureRows] = await db.promise().query(
            `SELECT DATE_FORMAT(unavailable_date, '%Y-%m-%d') AS date, reason
             FROM cook_daily_availability
             WHERE cook_id = ? AND unavailable_date IN (?, ?)`,
            [cookId, target, tomorrow]
        );
        const closures = new Map(closureRows.map(r => [r.date, r.reason]));

        return res.status(200).json({
            success: true,
            date: target,
            is_today: target === today,
            summary: {
                total: deliveries.length,
                cooking: cookingCount,
                customer_skipped: deliveries.filter(d => d.status === DAY_STATUS.CUSTOMER_SKIPPED).length,
                cook_unavailable: deliveries.filter(d =>
                    d.status === DAY_STATUS.COOK_UNAVAILABLE
                    || d.status === DAY_STATUS.COOK_DELIVERY_UNAVAILABLE
                ).length,
                delivered: deliveries.filter(d => d.status === DAY_STATUS.DELIVERED).length,
                missed: deliveries.filter(d => d.status === DAY_STATUS.MISSED).length,
                custom_meals_confirmed: deliveries.filter(d => d.custom_meal?.is_confirmed).length,
                custom_meals_pending: deliveries.filter(d => d.custom_meal && !d.custom_meal.is_confirmed).length
            },
            // The date the cook can still act on, and how long they have. The
            // bulk close button targets THIS date, not `date` above — today is
            // already past its cutoff and can't be closed.
            next_changeable: {
                date: tomorrow,
                is_marked_unavailable: closures.has(tomorrow),
                reason: closures.get(tomorrow) || null,
                ms_until_cutoff: msUntilCutoff(tomorrow, cutoffHour),
                is_locked: isDateLocked(tomorrow, cutoffHour)
            },
            cutoff: { hour: cutoffHour, label: formatCutoffLabel(cutoffHour) },
            viewed_date_unavailable: closures.has(target),
            deliveries
        });
    } catch (error) {
        console.error("getTodayDeliveries error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

export default {
    setCookDailyUnavailability,
    clearCookDailyUnavailability,
    markSubscriptionDeliveryUnavailable,
    restoreSubscriptionDelivery,
    getTodayDeliveries
};
