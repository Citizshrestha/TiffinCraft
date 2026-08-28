import db from "../config/db.js";
import { getDurationDays } from "../controllers/subscriptionController.js";
import {
    notifySubscriptionVerified,
    notifySubscriptionPaidToCook,
    notifySubscriptionScheduled
} from "./notificationHelper.js";
import { dateOnly, getNptToday, addDays, daysBetween } from "./nptTime.js";
import {
    DAY_STATUS,
    applyDayStatus,
    isCookClosedOn,
    placeDayOrder,
    logDayEvent
} from "./subscriptionDailyLog.js";

/**
 * Subscription payment state machine — the ONLY place a subscription is
 * allowed to become active off the back of an eSewa payment.
 *
 * Lives in utils/ rather than a controller because both paymentController
 * (callback + status-poll paths) and subscriptionPaymentController (initiate)
 * need it; importing it from a controller in either direction would create a
 * cycle.
 *
 * Two rules everything here exists to enforce:
 *  1. Activation happens ONLY after the payment row already reached SUCCESS via
 *     a server-side re-check against eSewa (see handleEpayReturn /
 *     getEsewaPaymentStatus) — never from a redirect URL or a client claim.
 *  2. Every transition is applied with a conditional UPDATE + affectedRows
 *     check, so a duplicated or replayed callback can't activate the same
 *     subscription (or double-notify) twice.
 */

/**
 * Append-only audit record of a subscription payment transition. Never throws
 * — an unloggable event must not be allowed to fail the payment itself, but it
 * does get shouted about in the server log.
 */
export async function logSubscriptionPaymentEvent({ subscriptionId, paymentId = null, transactionUuid = null, event, amount = null, detail = null }) {
    try {
        await db.promise().query(
            `INSERT INTO subscription_payment_events
             (subscription_id, payment_id, transaction_uuid, event, amount, detail)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [subscriptionId, paymentId, transactionUuid, event, amount, detail ? String(detail).slice(0, 500) : null]
        );
    } catch (err) {
        console.error(`❌ Could not log subscription payment event "${event}" for subscription ${subscriptionId}:`, err.message);
    }
}

/**
 * Applies a server-confirmed eSewa status to the subscription attached to a
 * payment row. Called from applyConfirmedPaymentStatus, so `confirmedStatus`
 * has already been re-verified against eSewa's own status API.
 *
 * SUCCESS is the only status that grants anything, and it grants it exactly
 * once: the UPDATE is gated on the row still being in a pre-delivery state, so a
 * replayed callback finds affectedRows = 0 and returns without re-notifying or
 * re-computing the delivery window.
 */
export async function applySubscriptionPaymentOutcome(payment, confirmedStatus) {
    const subscriptionId = payment.subscription_id;
    if (!subscriptionId) return;

    const [subs] = await db.promise().query(
        `SELECT s.*, p.name AS plan_name, p.duration, p.price_per_delivery
         FROM subscriptions s
         JOIN subscription_plans p ON s.plan_id = p.id
         WHERE s.id = ?`,
        [subscriptionId]
    );
    if (subs.length === 0) {
        console.warn(`⚠️  Payment ${payment.id} references missing subscription ${subscriptionId}.`);
        return;
    }
    const sub = subs[0];

    if (confirmedStatus === "SUCCESS") {
        await activateAfterSuccessfulPayment(sub, payment);
        return;
    }

    if (["FAILED", "CANCELED"].includes(confirmedStatus)) {
        // The subscription row stays inactive and grants nothing. It's marked
        // 'failed' rather than deleted so the customer can retry against the
        // same row (see initiateSubscriptionPayment) and so the attempt stays
        // on record.
        const [result] = await db.promise().query(
            `UPDATE subscriptions SET payment_status = 'failed'
             WHERE id = ? AND status IN ('pending_payment', 'pending_verification') AND payment_status <> 'verified'`,
            [subscriptionId]
        );
        await logSubscriptionPaymentEvent({
            subscriptionId,
            paymentId: payment.id,
            transactionUuid: payment.transaction_uuid,
            event: confirmedStatus === "FAILED" ? "payment_failed" : "payment_canceled",
            amount: payment.amount,
            detail: result.affectedRows === 1
                ? "Payment did not complete — no meals granted, retry allowed."
                : `No state change (subscription is "${sub.status}"/"${sub.payment_status}").`
        });
        return;
    }

    if (confirmedStatus === "REVERTED") {
        // eSewa refunded a payment we'd already activated on. Pull the
        // subscription back out of service rather than keep delivering food
        // that is no longer paid for.
        //
        // The gate lists every state a paid-then-refunded subscription can be
        // sitting in. 'scheduled' and 'verified' matter especially: a refund
        // that lands before the start date is the COMMON case (the customer
        // changed their mind), and the old gate — 'active','paused',
        // 'pending_payment' — would have let those keep their start date and
        // silently begin delivering food nobody had paid for.
        const [result] = await db.promise().query(
            `UPDATE subscriptions
             SET status = 'cancelled', payment_status = 'failed', next_delivery_date = NULL
             WHERE id = ? AND status IN
                 ('pending_payment', 'pending_verification', 'verified', 'scheduled', 'active', 'paused')`,
            [subscriptionId]
        );
        await logSubscriptionPaymentEvent({
            subscriptionId,
            paymentId: payment.id,
            transactionUuid: payment.transaction_uuid,
            event: "payment_reverted",
            amount: payment.amount,
            detail: result.affectedRows === 1
                ? `eSewa reverted the payment — subscription cancelled from "${sub.status}". No further delivery days will be created.`
                : `No state change (subscription is "${sub.status}").`
        });
    }
}

/**
 * The eSewa half of "payment confirmed → subscription enters the delivery
 * lifecycle". The manual-QR half is verifySubscriptionPayment in
 * subscriptionController.js; the two must agree on every field they write, or a
 * customer's experience depends on which way they happened to pay.
 *
 * Three things changed here when the per-day lifecycle landed:
 *
 *  - The customer's CHOSEN start_date is honoured. This used to overwrite
 *    start_date with the payment day and force status 'active', so someone who
 *    picked "start next Monday" started immediately.
 *  - Deliveries are daily, so a plan is worth getDurationDays(duration) meals
 *    and the window is start_date + (meals - 1). It previously set
 *    end_date = today + intervalDays with a single delivery in it.
 *  - Dates are NPT. `new Date()` on a UTC server names yesterday for the first
 *    5h45m of every Nepali day, which is a live window for eSewa traffic.
 */
async function activateAfterSuccessfulPayment(sub, payment) {
    const subscriptionId = sub.id;
    const today = getNptToday();

    // A start_date already in the past (the customer paid days after creating
    // the row, or sat on the eSewa screen overnight) is clamped forward. Nobody
    // is retroactively owed meals for days that have already gone by.
    const chosen = dateOnly(sub.start_date);
    const startsInFuture = chosen !== null && daysBetween(today, chosen) > 0;
    const effectiveStart = startsInFuture ? chosen : today;

    const mealsTotal = getDurationDays(sub.duration);
    // Inclusive window: a 7-meal plan starting the 1st runs through the 7th.
    const endDate = addDays(effectiveStart, mealsTotal - 1);
    const newStatus = startsInFuture ? "scheduled" : "active";

    // Conditional on a pre-delivery state: the atomic gate. A duplicate
    // callback, a concurrent status poll and a manual retry can all land here at
    // once; only one of them gets affectedRows = 1.
    const [result] = await db.promise().query(
        `UPDATE subscriptions
         SET status = ?,
             payment_status = 'verified',
             payment_method = 'esewa',
             payment_verified_at = NOW(),
             verified_at = NOW(),
             start_date = ?,
             end_date = ?,
             meals_total = ?,
             meals_remaining = ?,
             next_delivery_date = ?
         WHERE id = ? AND status IN ('pending_payment', 'pending_verification')`,
        [newStatus, effectiveStart, endDate, mealsTotal, mealsTotal, effectiveStart, subscriptionId]
    );

    if (result.affectedRows === 0) {
        await logSubscriptionPaymentEvent({
            subscriptionId,
            paymentId: payment.id,
            transactionUuid: payment.transaction_uuid,
            event: "payment_success",
            amount: payment.amount,
            detail: `Duplicate confirmation ignored — subscription already "${sub.status}".`
        });
        return;
    }

    // verified_by stays NULL: this path had no human verifier. That is the
    // distinction the column exists to record — "eSewa confirmed it" versus
    // "cook X said they saw the money".

    await logSubscriptionPaymentEvent({
        subscriptionId,
        paymentId: payment.id,
        transactionUuid: payment.transaction_uuid,
        event: newStatus === "active" ? "activated" : "scheduled",
        amount: payment.amount,
        detail: `Paid via eSewa. ${mealsTotal} meals, ${effectiveStart} to ${endDate}.`
            + (startsInFuture ? ` Starts on the customer's chosen date.` : ` Starting today.`)
            + (chosen && chosen !== effectiveStart ? ` Chosen start ${chosen} had passed and was clamped forward.` : "")
    });

    // Starting today means today's meal is owed NOW. The 06:00 batch has already
    // run, so without this a customer who pays at 2pm for a same-day start
    // silently loses the first day they paid for.
    if (newStatus === "active") {
        await orderFirstDay({ ...sub, meals_total: mealsTotal, end_date: endDate }, today);
    }

    const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [sub.customer_id]);
    if (newStatus === "active") {
        await notifySubscriptionVerified(sub.customer_id, subscriptionId, sub.plan_name);
    } else {
        await notifySubscriptionScheduled(sub.customer_id, subscriptionId, sub.plan_name, effectiveStart);
    }
    await notifySubscriptionPaidToCook(sub.cook_id, subscriptionId, customer?.full_name || "A customer", sub.plan_name, payment.amount);
}

/**
 * Create day one's log row and order for a subscription that activated mid-day.
 *
 * Fully swallowed on failure, deliberately: the money is already taken and the
 * subscription is already active, so throwing here would surface as a failed
 * payment to a customer who has in fact paid successfully. A missing first-day
 * order costs one day; a payment the customer believes failed costs their trust.
 * The failure is logged loudly and the credit is left unspent.
 */
async function orderFirstDay(sub, date) {
    const connection = await db.promise().getConnection();
    try {
        await connection.beginTransaction();

        // A closure the cook declared before this subscription existed has no
        // fan-out row, so the closure table is the only record of it.
        if (await isCookClosedOn(connection, sub.cook_id, date)) {
            await applyDayStatus(connection, {
                subscriptionId: sub.id,
                deliveryDate: date,
                status: DAY_STATUS.COOK_UNAVAILABLE,
                toggledBy: "cook",
                reason: "Kitchen closed on the day this subscription started",
                creditDeducted: false
            });
            // Calendar-day model: only next_delivery_date advances. end_date was
            // fixed when the subscription was verified and never moves — an
            // undelivered day is a lost day, not a deferred one.
            await connection.query(
                `UPDATE subscriptions SET next_delivery_date = ? WHERE id = ?`,
                [addDays(date, 1), sub.id]
            );
            await connection.commit();
            await logDayEvent({
                subscriptionId: sub.id,
                event: "cook_unavailable",
                actor: "system",
                detail: `${date} not delivered — cook had marked the date unavailable. No charge; the subscription end date is unchanged.`
            });
            return;
        }

        // Row first with credit_deducted = FALSE, order second, credit charged
        // only once the order exists — a plan with an unavailable meal must not
        // bill for food nobody cooked.
        await applyDayStatus(connection, {
            subscriptionId: sub.id,
            deliveryDate: date,
            status: DAY_STATUS.SCHEDULED,
            toggledBy: "system",
            reason: null,
            creditDeducted: false
        });

        const order = await placeDayOrder(connection, sub, date);

        if (!order.placed) {
            const names = order.items.map(i => i.meal_name).join(", ");
            await applyDayStatus(connection, {
                subscriptionId: sub.id,
                deliveryDate: date,
                status: DAY_STATUS.COOK_UNAVAILABLE,
                toggledBy: "system",
                reason: `Unavailable in the kitchen today: ${names}`,
                creditDeducted: false
            });
            await connection.query(
                `UPDATE subscriptions SET next_delivery_date = ? WHERE id = ?`,
                [addDays(date, 1), sub.id]
            );
            await connection.commit();
            await logDayEvent({
                subscriptionId: sub.id,
                event: "day_unavailable",
                actor: "system",
                detail: `${date} not delivered — unavailable meal(s): ${names}. No charge; the subscription end date is unchanged.`
            });
            return;
        }

        await connection.query(
            `UPDATE subscription_daily_log SET credit_deducted = TRUE
             WHERE subscription_id = ? AND delivery_date = ?`,
            [sub.id, date]
        );
        await connection.query(
            `UPDATE subscriptions
             SET meals_remaining = GREATEST(meals_remaining - 1, 0), next_delivery_date = ?
             WHERE id = ?`,
            [addDays(date, 1), sub.id]
        );

        await connection.commit();
        console.log(`✅ Subscription ${sub.id}: first-day order ${order.orderId} placed for ${date}.`);
    } catch (err) {
        await connection.rollback();
        console.error(
            `❌ Subscription ${sub.id} activated but its first-day order for ${date} failed:`,
            err.message,
            "— the credit was NOT spent; tomorrow's cron will resume from tomorrow."
        );
        await logDayEvent({
            subscriptionId: sub.id,
            event: "first_day_failed",
            actor: "system",
            detail: `Could not create day one (${date}): ${err.message}. Credit not charged.`
        });
    } finally {
        connection.release();
    }
}
