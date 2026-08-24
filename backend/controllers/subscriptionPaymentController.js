import db from "../config/db.js";
import { buildEpayFormFields } from "../utils/esewaEpayClient.js";
import { getRedirectBaseUrl } from "../utils/publicUrl.js";
import { fetchPlanWithItems } from "./subscriptionPlanController.js";
import { logSubscriptionPaymentEvent } from "../utils/subscriptionPaymentState.js";

/**
 * Pay-first subscription checkout.
 *
 * The old flow created a subscription row and *then* asked for money (manual
 * QR + screenshot + cook verification). This one inverts that: the row is
 * created as pending_payment and stays inert until eSewa confirms the money
 * server-side — see applySubscriptionPaymentOutcome, which is the only code
 * path that can activate it.
 *
 * Deliberately reuses the ePay v2 plumbing built for orders
 * (buildEpayFormFields → signed form → /api/payments/esewa-epay/return →
 * applyConfirmedPaymentStatus) instead of a second signing/verifying stack.
 * The `payments` row it creates has subscription_id set and order_id NULL;
 * everything downstream branches on that.
 */

/** Trust boundary: the amount charged comes from the plan row, never the request. */
function resolvePlanAmount(fullPlan) {
    // fetchPlanWithItems already falls back to the summed menu total for legacy
    // plans that never had a price set, so this is always a real number.
    return Number(fullPlan.price_per_delivery);
}

// POST /api/subscriptions/initiate  { cook_id, plan_id, delivery_address }
export const initiateSubscriptionPayment = async (req, res) => {
    try {
        // From the JWT only. A customer_id in the body is ignored on purpose.
        const customerId = req.user.id;
        const { cook_id, plan_id, delivery_address } = req.body;

        if (!plan_id) {
            return res.status(400).json({ success: false, message: "plan_id is required." });
        }
        const address = typeof delivery_address === "string" ? delivery_address.trim() : "";
        if (!address) {
            return res.status(400).json({ success: false, message: "A delivery address is required." });
        }

        const fullPlan = await fetchPlanWithItems(plan_id);
        if (!fullPlan) {
            return res.status(404).json({ success: false, message: "Subscription plan not found." });
        }
        // cook_id is supplied by the app for its own routing; treat a mismatch
        // as a bug/tamper rather than silently trusting either side.
        if (cook_id !== undefined && cook_id !== null && Number(cook_id) !== Number(fullPlan.cook_id)) {
            return res.status(400).json({ success: false, message: "This plan doesn't belong to that cook." });
        }
        if (!fullPlan.is_active) {
            return res.status(400).json({ success: false, message: "This plan is no longer offered." });
        }
        if (!fullPlan.is_available) {
            return res.status(400).json({
                success: false,
                message: "This plan includes an item the cook has marked unavailable. Please try again once it's back."
            });
        }

        const cookId = Number(fullPlan.cook_id);
        if (cookId === customerId) {
            return res.status(400).json({ success: false, message: "You can't subscribe to your own plan." });
        }

        const amount = resolvePlanAmount(fullPlan);
        if (!Number.isFinite(amount) || amount <= 0) {
            console.error(`❌ Plan ${plan_id} has an unusable price:`, fullPlan.price_per_delivery);
            return res.status(400).json({ success: false, message: "This plan's price isn't set correctly. Please ask the cook to fix it." });
        }

        // ── Duplicate guard ──────────────────────────────────────────────
        // An already-running (not expired) subscription with the SAME COOK
        // blocks a new checkout, so a double-tap or a replayed request can't
        // buy the same thing twice. Checked before any booking is generated.
        const [live] = await db.promise().query(
            `SELECT s.id, s.status, s.end_date, p.name AS plan_name
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.customer_id = ? AND s.cook_id = ?
               AND s.status IN ('active', 'paused')
               AND (s.end_date IS NULL OR s.end_date >= CURDATE())
             LIMIT 1`,
            [customerId, cookId]
        );
        if (live.length > 0) {
            await logSubscriptionPaymentEvent({
                subscriptionId: live[0].id,
                event: "blocked_duplicate",
                amount,
                detail: `Blocked a new checkout for plan ${plan_id} — "${live[0].plan_name}" is still ${live[0].status}.`
            });
            return res.status(409).json({
                success: false,
                // Phrased so it reads correctly for every status value in the
                // enum ("a active"/"a paused" would both be wrong).
                message: `You already have a subscription with this cook ("${live[0].plan_name}") that is currently ${live[0].status}. Cancel it first, or wait for it to finish.`
            });
        }

        // ── Subscription row ─────────────────────────────────────────────
        // Reuse an abandoned/failed attempt for the same plan instead of
        // stacking up dead rows — the old code left these behind and then
        // refused every future attempt because of them.
        const [pending] = await db.promise().query(
            `SELECT id FROM subscriptions
             WHERE customer_id = ? AND plan_id = ? AND status = 'pending_payment'
             ORDER BY id DESC LIMIT 1`,
            [customerId, plan_id]
        );

        let subscriptionId;
        if (pending.length > 0) {
            subscriptionId = pending[0].id;
            await db.promise().query(
                `UPDATE subscriptions
                 SET delivery_address = ?, payment_status = 'pending', payment_method = 'esewa'
                 WHERE id = ? AND status = 'pending_payment'`,
                [address, subscriptionId]
            );
            await logSubscriptionPaymentEvent({
                subscriptionId, event: "reused_pending", amount,
                detail: "Retrying payment against the existing pending subscription."
            });
        } else {
            // start_date / next_delivery_date are placeholders — both are
            // recomputed from the real payment date on activation, so a
            // customer who pays two days later doesn't get a delivery date
            // already in the past.
            // Local calendar date — toISOString() would give the previous day
            // for any local time before 05:45 (Nepal is UTC+05:45).
            const now = new Date();
            const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
            const [created] = await db.promise().query(
                `INSERT INTO subscriptions
                   (customer_id, cook_id, plan_id, delivery_address, start_date, next_delivery_date, status, payment_status, payment_method)
                 VALUES (?, ?, ?, ?, ?, ?, 'pending_payment', 'pending', 'esewa')`,
                [customerId, cookId, plan_id, address, today, today]
            );
            subscriptionId = created.insertId;
        }

        // Any earlier in-flight attempt on this subscription is dead the moment
        // a new one is issued — retire it so only one PENDING row is ever
        // checkout-able (serveEpayCheckoutForm resolves by PENDING status).
        await db.promise().query(
            `UPDATE payments SET status = 'CANCELED', updated_at = NOW()
             WHERE subscription_id = ? AND status = 'PENDING'`,
            [subscriptionId]
        );

        const redirectBaseUrl = getRedirectBaseUrl();
        // Unique per attempt (same guarantee as the order flow, backed by the
        // UNIQUE index on payments.transaction_uuid).
        const transactionUuid = `tc-sub-${subscriptionId}-${Date.now()}`;
        const returnUrl = `${redirectBaseUrl}/api/payments/esewa-epay/return?transaction_uuid=${encodeURIComponent(transactionUuid)}`;

        const { formUrl, fields } = buildEpayFormFields({
            amount,
            transactionUuid,
            successUrl: returnUrl,
            failureUrl: returnUrl
        });

        await db.promise().query(
            `INSERT INTO payments (subscription_id, transaction_uuid, amount, status)
             VALUES (?, ?, ?, 'PENDING')`,
            [subscriptionId, transactionUuid, amount]
        );
        const [[{ paymentId }]] = await db.promise().query(
            "SELECT id AS paymentId FROM payments WHERE transaction_uuid = ?",
            [transactionUuid]
        );

        await logSubscriptionPaymentEvent({
            subscriptionId,
            paymentId,
            transactionUuid,
            event: "initiated",
            amount,
            detail: `eSewa ePay checkout issued for plan ${plan_id} ("${fullPlan.name}").`
        });

        return res.status(200).json({
            success: true,
            subscription_id: subscriptionId,
            transaction_uuid: transactionUuid,
            amount,
            form_url: formUrl,
            fields
        });
    } catch (error) {
        console.error("initiateSubscriptionPayment error:", error);
        // Not 502/503/504 — the Android FailoverInterceptor treats those as its
        // own tunnel misbehaving and swallows the message.
        return res.status(500).json({ success: false, message: "Couldn't start the payment. Please try again.", error: error.message });
    }
};

/**
 * GET /api/subscriptions/:id/payment-events — customer or cook.
 * The audit trail for one subscription, newest first. Exists so a payment
 * dispute can be answered from the app instead of the server log.
 */
export const getSubscriptionPaymentEvents = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            "SELECT customer_id, cook_id FROM subscriptions WHERE id = ?",
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        if (subs[0].customer_id !== userId && subs[0].cook_id !== userId) {
            return res.status(403).json({ success: false, message: "Access denied." });
        }

        const [events] = await db.promise().query(
            `SELECT id, payment_id, transaction_uuid, event, amount, detail, created_at
             FROM subscription_payment_events
             WHERE subscription_id = ?
             ORDER BY created_at DESC, id DESC`,
            [id]
        );
        return res.status(200).json({ success: true, events });
    } catch (error) {
        console.error("getSubscriptionPaymentEvents error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};
