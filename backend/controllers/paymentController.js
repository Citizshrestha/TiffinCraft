import db from "../config/db.js";
import { bookEsewaPayment, checkEsewaStatus, cancelEsewaPayment as cancelEsewaBooking, getEsewaSecretKey, resolveEsewaAppDeeplink } from "../utils/esewaClient.js";
import { checkEpayStatus, mapEpayStatus } from "../utils/esewaEpayClient.js";
import { verifyEsewaSignature } from "../utils/esewaSignature.js";
import { getPublicBaseUrl } from "../utils/publicUrl.js";
import { notifyPaymentConfirmed } from "../utils/notificationHelper.js";
import { applySubscriptionPaymentOutcome, logSubscriptionPaymentEvent } from "../utils/subscriptionPaymentState.js";

export const REDIRECT_URL = "tiffincraft://payment-result";

/** eSewa status values are UPPERCASE; keep the mapping in one place. */
const TERMINAL_STATUSES = new Set(["SUCCESS", "FAILED", "CANCELED", "REVERTED"]);

/**
 * The amount eSewa says it actually collected, or null when the payload didn't
 * carry one. eSewa formats amounts for humans in some responses ("1,200.0"), so
 * separators are stripped before parsing.
 */
function extractEsewaTotalAmount(rawPayload) {
    const raw = rawPayload?.total_amount ?? rawPayload?.totalAmount;
    if (raw === undefined || raw === null || raw === "") return null;
    const parsed = Number(String(raw).replace(/,/g, "").trim());
    return Number.isFinite(parsed) ? parsed : null;
}

/**
 * Applies a confirmed (server-verified, never client/callback-trusted) eSewa
 * status to a payment + whatever it's paying for. Idempotent — safe to call
 * from both the callback handler and the status-polling endpoint for the same
 * event without double-crediting or double-notifying, since it only acts when
 * the stored payment status actually changes.
 *
 * A payments row belongs to EITHER an order (order_id) or a subscription
 * (subscription_id) — never both. The subscription branch delegates to
 * applySubscriptionPaymentOutcome, which owns that side's own conditional
 * activation guard.
 */
export async function applyConfirmedPaymentStatus(payment, confirmedStatus, referenceCode, rawPayload) {
    if (payment.status === confirmedStatus) {
        // Already applied — duplicate callback/poll, nothing to do.
        return payment;
    }

    // Conditional on the status not already being what we're about to write:
    // two concurrent confirmations (callback + poll) both pass the check above,
    // and this makes exactly one of them the winner.
    const [applied] = await db.promise().query(
        `UPDATE payments
         SET status = ?, reference_code = COALESCE(?, reference_code), raw_callback_payload = ?, updated_at = NOW()
         WHERE id = ? AND status <> ?`,
        [confirmedStatus, referenceCode || null, rawPayload ? JSON.stringify(rawPayload) : null, payment.id, confirmedStatus]
    );
    if (applied.affectedRows === 0) {
        return { ...payment, status: confirmedStatus };
    }

    if (payment.subscription_id) {
        // Server-side amount validation, defence in depth. `payment.amount` was
        // computed at initiate from the plan price in the DB — never from
        // anything the app sent — and the ePay signature binds total_amount, so
        // a short amount here means something is wrong upstream. Treat it as an
        // unpaid attempt rather than activate a subscription that wasn't paid
        // for. Tolerance covers float/decimal representation only.
        const paidAmount = extractEsewaTotalAmount(rawPayload);
        const expectedAmount = Number(payment.amount);
        if (confirmedStatus === "SUCCESS" && paidAmount !== null && Number.isFinite(expectedAmount)
                && paidAmount + 0.01 < expectedAmount) {
            await logSubscriptionPaymentEvent({
                subscriptionId: payment.subscription_id,
                paymentId: payment.id,
                transactionUuid: payment.transaction_uuid,
                event: "amount_mismatch",
                amount: paidAmount,
                detail: `eSewa confirmed ${paidAmount} but this plan costs ${expectedAmount} — subscription NOT activated.`
            });
            console.error(`❌ Amount mismatch on subscription ${payment.subscription_id}: eSewa=${paidAmount}, expected=${expectedAmount}. Refusing to activate.`);
            await applySubscriptionPaymentOutcome({ ...payment, status: "FAILED" }, "FAILED");
            return { ...payment, status: confirmedStatus };
        }

        await applySubscriptionPaymentOutcome({ ...payment, status: confirmedStatus }, confirmedStatus);
        return { ...payment, status: confirmedStatus };
    }

    if (confirmedStatus === "SUCCESS") {
        const [orders] = await db.promise().query("SELECT * FROM orders WHERE id = ?", [payment.order_id]);
        const order = orders[0];
        if (order && order.payment_status !== "paid") {
            await db.promise().query(
                `UPDATE orders
                 SET payment_status = 'paid',
                     status = CASE WHEN status = 'pending' THEN 'confirmed' ELSE status END,
                     updated_at = NOW()
                 WHERE id = ?`,
                [order.id]
            );

            const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [order.customer_id]);
            await notifyPaymentConfirmed(order.cook_id, order.id, customer?.full_name || "A customer", payment.amount);
        }
    } else if (confirmedStatus === "REVERTED") {
        // eSewa-side refund already happened — reflect it even if no
        // refund_requests row exists yet (e.g. reversed directly by eSewa support).
        await db.promise().query(
            `UPDATE orders SET payment_status = 'refunded', refund_status = 'refunded', updated_at = NOW()
             WHERE id = ? AND payment_status = 'paid'`,
            [payment.order_id]
        );
    }

    return { ...payment, status: confirmedStatus };
}

// POST /api/payments/esewa/initiate
export const initiateEsewaPayment = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { order_id } = req.body;

        if (!order_id) {
            return res.status(400).json({ success: false, message: "order_id is required." });
        }

        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND customer_id = ?",
            [order_id, customerId]
        );
        if (orders.length === 0) {
            return res.status(404).json({ success: false, message: "Order not found." });
        }

        const order = orders[0];
        if (order.payment_method !== "online") {
            return res.status(400).json({ success: false, message: "This order is not using online payment method." });
        }
        if (order.payment_status !== "pending") {
            return res.status(400).json({ success: false, message: `Order payment is already "${order.payment_status}".` });
        }

        const publicBaseUrl = getPublicBaseUrl();
        if (!publicBaseUrl) {
            console.error("❌ initiateEsewaPayment: no reachable public base URL configured (PUBLIC_BASE_URL / tunnel-config.json missing).");
            // Not 502/503/504 — the Android app's FailoverInterceptor treats
            // those as "our own dev tunnel is flaky" and retries/rewrites the
            // host, swallowing this message. This is a real API error, not a
            // transport-layer one, so it needs a status outside that set.
            return res.status(500).json({ success: false, message: "Payment service is temporarily unavailable. Try again shortly." });
        }

        const transactionUuid = `tc-${order_id}-${Date.now()}`;
        const callbackUrl = `${publicBaseUrl}/api/payments/esewa/callback`;

        const result = await bookEsewaPayment({
            amount: order.total_amount,
            transactionUuid,
            callbackUrl,
            redirectUrl: REDIRECT_URL,
            properties: { order_id: String(order_id) }
        });

        if (!result.ok || !result.data?.deeplink) {
            // Full technical detail (raw eSewa error body, HTTP status, code)
            // goes to server logs only — result.message is a debug string
            // ("eSewa returned a non-JSON response (HTTP 502)") never meant
            // for a customer to read. The client always gets a plain,
            // actionable message instead.
            console.error("❌ eSewa book failed:", result.code, result.message, result.raw);
            // See note above — avoid 502/503/504, the Android FailoverInterceptor
            // treats those as our own tunnel infrastructure failing, not eSewa's.
            return res.status(500).json({
                success: false,
                message: "eSewa isn't responding right now. Please try again in a few minutes, or pay via the QR code instead."
            });
        }

        await db.promise().query(
            `INSERT INTO payments (order_id, transaction_uuid, booking_id, correlation_id, amount, status)
             VALUES (?, ?, ?, ?, ?, 'BOOKED')`,
            [order_id, transactionUuid, result.data.booking_id, result.data.correlation_id, order.total_amount]
        );

        // The https deeplink only opens the eSewa app directly on hosts the
        // app has verified (production's links.esewa.com.np). In sandbox it's
        // a bare redirector, so resolve it to the esewa:// URI the app really
        // handles. Optional — null just means the client uses the https one.
        const appDeeplink = await resolveEsewaAppDeeplink(result.data.deeplink);

        return res.status(200).json({
            success: true,
            deeplink: result.data.deeplink,
            app_deeplink: appDeeplink,
            booking_id: result.data.booking_id,
            transaction_uuid: transactionUuid
        });
    } catch (error) {
        console.error("initiateEsewaPayment error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// POST /api/payments/esewa/callback — called BY eSewa, no auth.
export const esewaCallback = async (req, res) => {
    try {
        const body = req.body || {};
        const { product_code, amount, reference_code, correlation_id, status, signature, signed_field_names } = body;

        if (!correlation_id || !status || !signature || !signed_field_names) {
            console.warn("⚠️  eSewa callback missing required fields:", body);
            return res.status(400).json({ success: false, message: "Malformed callback payload." });
        }

        const fields = { product_code, amount, reference_code, correlation_id, status };
        const validSignature = verifyEsewaSignature(fields, signed_field_names, signature, getEsewaSecretKey());
        if (!validSignature) {
            console.warn("⚠️  eSewa callback signature verification FAILED — payload rejected:", body);
            return res.status(400).json({ success: false, message: "Invalid signature." });
        }

        const [payments] = await db.promise().query("SELECT * FROM payments WHERE correlation_id = ?", [correlation_id]);
        if (payments.length === 0) {
            console.warn("⚠️  eSewa callback for unknown correlation_id:", correlation_id);
            return res.status(404).json({ success: false, message: "Payment not found." });
        }
        const payment = payments[0];

        if (TERMINAL_STATUSES.has(payment.status)) {
            // Already resolved (by an earlier callback or a status poll) —
            // idempotent no-op so a replayed/duplicate callback can't
            // double-credit or double-notify.
            return res.status(200).json({ success: true, message: "Already processed." });
        }

        // Never trust the callback body's status alone — independently
        // re-confirm with eSewa's own status-check API before crediting anything.
        const statusResult = await checkEsewaStatus({ bookingId: payment.booking_id, correlationId: correlation_id });
        if (!statusResult.ok || !statusResult.data?.status) {
            console.error("❌ eSewa status re-check failed after callback:", statusResult.code, statusResult.message);
            return res.status(500).json({ success: false, message: "Could not confirm payment status." });
        }

        const confirmedStatus = statusResult.data.status;

        if (confirmedStatus === "SUCCESS") {
            const [orders] = await db.promise().query("SELECT total_amount FROM orders WHERE id = ?", [payment.order_id]);
            const orderAmount = orders[0] ? Number(orders[0].total_amount) : null;
            if (orderAmount === null || Math.abs(orderAmount - Number(amount)) > 0.01) {
                console.error(`❌ eSewa callback amount mismatch for order ${payment.order_id}: order=${orderAmount} callback=${amount}`);
                return res.status(400).json({ success: false, message: "Amount mismatch." });
            }
        }

        await applyConfirmedPaymentStatus(payment, confirmedStatus, statusResult.data.reference_code || reference_code, body);

        const io = req.app.get("io");
        if (io) {
            io.to(`order_${payment.order_id}`).emit("paymentStatusUpdated", {
                orderId: payment.order_id,
                transaction_uuid: payment.transaction_uuid,
                status: confirmedStatus
            });
        }

        return res.status(200).json({ success: true });
    } catch (error) {
        console.error("esewaCallback error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// GET /api/payments/esewa/status/:transactionUuid
export const getEsewaPaymentStatus = async (req, res) => {
    try {
        const userId = req.user.id;
        const { transactionUuid } = req.params;

        // LEFT JOINs, not INNER: a payments row hangs off an order OR a
        // subscription, so requiring `orders` would 404 every subscription
        // checkout. COALESCE picks whichever side actually exists for the
        // ownership check below.
        const [payments] = await db.promise().query(
            `SELECT p.*,
                    COALESCE(o.customer_id, s.customer_id) AS customer_id,
                    COALESCE(o.cook_id, s.cook_id)         AS cook_id
             FROM payments p
             LEFT JOIN orders o ON o.id = p.order_id
             LEFT JOIN subscriptions s ON s.id = p.subscription_id
             WHERE p.transaction_uuid = ?`,
            [transactionUuid]
        );
        if (payments.length === 0) {
            return res.status(404).json({ success: false, message: "Payment not found." });
        }
        const payment = payments[0];

        if (payment.customer_id !== userId && payment.cook_id !== userId) {
            return res.status(403).json({ success: false, message: "Access denied." });
        }

        let currentStatus = payment.status;

        if (!TERMINAL_STATUSES.has(currentStatus)) {
            // booking_id only exists for Intent Payment rows — ePay v2 rows
            // (created via /esewa-epay/initiate) never have one, since that
            // product has no booking step. That's the reliable signal for
            // which product's (differently-shaped) status API to call.
            const statusResult = payment.booking_id
                ? await checkEsewaStatus({ bookingId: payment.booking_id, correlationId: payment.correlation_id })
                : await checkEpayStatus({ transactionUuid: payment.transaction_uuid, totalAmount: payment.amount });

            const rawStatus = payment.booking_id ? statusResult.data?.status : statusResult.data?.status;
            const mappedStatus = payment.booking_id ? rawStatus : (rawStatus ? mapEpayStatus(rawStatus) : null);
            const referenceCode = payment.booking_id ? statusResult.data?.reference_code : statusResult.data?.ref_id;

            if (statusResult.ok && mappedStatus) {
                const updated = await applyConfirmedPaymentStatus(payment, mappedStatus, referenceCode, statusResult.data);
                currentStatus = updated.status;
            }
            // If the eSewa call itself fails transiently, fall back to
            // whatever we already had stored rather than erroring the poll.
        }

        return res.status(200).json({
            success: true,
            status: currentStatus,
            order_id: payment.order_id,
            subscription_id: payment.subscription_id,
            transaction_uuid: payment.transaction_uuid
        });
    } catch (error) {
        console.error("getEsewaPaymentStatus error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// POST /api/payments/esewa/cancel
export const cancelEsewaPaymentBooking = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { transaction_uuid } = req.body;
        if (!transaction_uuid) {
            return res.status(400).json({ success: false, message: "transaction_uuid is required." });
        }

        const [payments] = await db.promise().query(
            `SELECT p.*, o.customer_id
             FROM payments p
             JOIN orders o ON o.id = p.order_id
             WHERE p.transaction_uuid = ?`,
            [transaction_uuid]
        );
        if (payments.length === 0) {
            return res.status(404).json({ success: false, message: "Payment not found." });
        }
        const payment = payments[0];

        if (payment.customer_id !== customerId) {
            return res.status(403).json({ success: false, message: "Access denied." });
        }
        if (payment.status !== "BOOKED") {
            return res.status(400).json({ success: false, message: `Cannot cancel a payment with status "${payment.status}".` });
        }

        const result = await cancelEsewaBooking({ bookingId: payment.booking_id });
        if (!result.ok) {
            console.error("❌ eSewa cancel failed:", result.code, result.message);
            return res.status(500).json({ success: false, message: "Couldn't cancel this payment right now. Please try again in a few minutes." });
        }

        await db.promise().query("UPDATE payments SET status = 'CANCELED', updated_at = NOW() WHERE id = ?", [payment.id]);

        return res.status(200).json({ success: true, message: "Payment cancelled." });
    } catch (error) {
        console.error("cancelEsewaPaymentBooking error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/** Used by the cleanup cron job — not an HTTP handler. */
export const autoCancelStaleBookings = async () => {
    // BOOKED = stale Intent Payment bookings (have a booking_id to cancel
    // against). PENDING with no booking_id = stale ePay v2 attempts (that
    // product has no cancel API — nothing to call, just stop tracking it).
    const [stale] = await db.promise().query(
        `SELECT * FROM payments
         WHERE status IN ('BOOKED', 'PENDING') AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE)`
    );
    for (const payment of stale) {
        try {
            if (payment.booking_id) {
                const result = await cancelEsewaBooking({ bookingId: payment.booking_id });
                // Even if eSewa's own cancel call fails (e.g. it already expired
                // their side), still mark it CANCELED locally so it stops
                // blocking retries and stops showing up in this sweep.
                if (!result.ok) {
                    console.warn(`⚠️  eSewa-side cancel failed for stale booking ${payment.booking_id}, marking CANCELED locally anyway:`, result.message);
                }
            }
            await db.promise().query("UPDATE payments SET status = 'CANCELED', updated_at = NOW() WHERE id = ?", [payment.id]);
            if (payment.subscription_id) {
                // Leaves the subscription inert and marks it 'failed' so the
                // customer sees "Payment failed — retry" instead of a
                // permanently "pending" subscription that grants nothing.
                await applySubscriptionPaymentOutcome({ ...payment, status: "CANCELED" }, "CANCELED");
            }
            console.log(`🧹 Auto-cancelled stale eSewa payment: payment #${payment.id} (${payment.order_id ? `order #${payment.order_id}` : `subscription #${payment.subscription_id}`})`);
        } catch (err) {
            console.error(`❌ autoCancelStaleBookings failed for payment #${payment.id}:`, err.message);
        }
    }
    return stale.length;
};
