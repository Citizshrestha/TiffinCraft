import db from "../config/db.js";
import { getDurationDays } from "../controllers/subscriptionController.js";
import { notifySubscriptionVerified, notifySubscriptionPaidToCook } from "./notificationHelper.js";

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
 * once: the UPDATE is gated on the row still being 'pending_payment', so a
 * replayed callback finds affectedRows = 0 and returns without re-notifying or
 * re-computing the delivery window.
 */
export async function applySubscriptionPaymentOutcome(payment, confirmedStatus) {
    const subscriptionId = payment.subscription_id;
    if (!subscriptionId) return;

    const [subs] = await db.promise().query(
        `SELECT s.*, p.name AS plan_name, p.duration
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
        const intervalDays = getDurationDays(sub.duration);
        const today = new Date();
        const nextDelivery = new Date(today);
        nextDelivery.setDate(nextDelivery.getDate() + intervalDays);
        // Local calendar date, NOT toISOString(). Nepal is UTC+05:45, so
        // toISOString() on any local time before 05:45 returns the previous
        // day — which would write an end_date a day short of the cycle the
        // customer actually paid for.
        const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

        // The window this single up-front payment buys. Delivery generation
        // stops at end_date (subscriptionOrderJob.js) — a one-time payment
        // must not fund an unbounded stream of deliveries.
        const endDate = new Date(today);
        endDate.setDate(endDate.getDate() + intervalDays);

        // Conditional on 'pending_payment': the atomic gate. A duplicate
        // callback, a concurrent status poll and a manual retry can all land
        // here at once; only one of them gets affectedRows = 1.
        const [result] = await db.promise().query(
            `UPDATE subscriptions
             SET status = 'active',
                 payment_status = 'verified',
                 payment_method = 'esewa',
                 payment_verified_at = NOW(),
                 start_date = ?,
                 end_date = ?,
                 next_delivery_date = ?
             WHERE id = ? AND status = 'pending_payment'`,
            [iso(today), iso(endDate), iso(nextDelivery), subscriptionId]
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

        await logSubscriptionPaymentEvent({
            subscriptionId,
            paymentId: payment.id,
            transactionUuid: payment.transaction_uuid,
            event: "activated",
            amount: payment.amount,
            detail: `Paid via eSewa and activated. Deliveries run to ${iso(endDate)}; first on ${iso(nextDelivery)}.`
        });

        const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [sub.customer_id]);
        await notifySubscriptionVerified(sub.customer_id, subscriptionId, sub.plan_name);
        await notifySubscriptionPaidToCook(sub.cook_id, subscriptionId, customer?.full_name || "A customer", sub.plan_name, payment.amount);
        return;
    }

    if (["FAILED", "CANCELED"].includes(confirmedStatus)) {
        // The subscription row stays inactive and grants nothing. It's marked
        // 'failed' rather than deleted so the customer can retry against the
        // same row (see initiateSubscriptionPayment) and so the attempt stays
        // on record.
        const [result] = await db.promise().query(
            `UPDATE subscriptions SET payment_status = 'failed'
             WHERE id = ? AND status = 'pending_payment' AND payment_status <> 'verified'`,
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
        const [result] = await db.promise().query(
            `UPDATE subscriptions SET status = 'cancelled', payment_status = 'failed'
             WHERE id = ? AND status IN ('active', 'paused', 'pending_payment')`,
            [subscriptionId]
        );
        await logSubscriptionPaymentEvent({
            subscriptionId,
            paymentId: payment.id,
            transactionUuid: payment.transaction_uuid,
            event: "payment_reverted",
            amount: payment.amount,
            detail: result.affectedRows === 1
                ? "eSewa reverted the payment — subscription cancelled."
                : `No state change (subscription is "${sub.status}").`
        });
    }
}
