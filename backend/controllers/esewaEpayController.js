import db from "../config/db.js";
import { buildEpayFormFields, checkEpayStatus, mapEpayStatus, getEpaySecretKey } from "../utils/esewaEpayClient.js";
import { verifyEsewaSignature } from "../utils/esewaSignature.js";
import { getRedirectBaseUrl } from "../utils/publicUrl.js";
import { applyConfirmedPaymentStatus, REDIRECT_URL } from "./paymentController.js";

/**
 * eSewa ePay v2 — fallback payment path (see esewaEpayClient.js header for
 * why this exists alongside Intent Payment). Shares the `payments` table:
 * a NULL booking_id is how the rest of the code tells these rows apart from
 * Intent Payment rows (see getEsewaPaymentStatus in paymentController.js).
 */

// POST /api/payments/esewa-epay/initiate
export const initiateEpayPayment = async (req, res) => {
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

        // Browser-redirect target, not a server-to-server callback — the
        // WebView on the user's own phone loads this, so the LAN address is
        // used in dev rather than a fragile tunnel URL (see getRedirectBaseUrl).
        const redirectBaseUrl = getRedirectBaseUrl();

        const transactionUuid = `tc-epay-${order_id}-${Date.now()}`;
        // Our own return endpoint verifies everything server-side, then
        // redirects into the app — eSewa appends its own params to whichever
        // of these two it hits, we never trust which one fired on its own.
        const successUrl = `${redirectBaseUrl}/api/payments/esewa-epay/return?transaction_uuid=${encodeURIComponent(transactionUuid)}`;
        const failureUrl = `${redirectBaseUrl}/api/payments/esewa-epay/return?transaction_uuid=${encodeURIComponent(transactionUuid)}`;

        const { formUrl, fields } = buildEpayFormFields({
            amount: order.total_amount,
            transactionUuid,
            successUrl,
            failureUrl
        });

        await db.promise().query(
            `INSERT INTO payments (order_id, transaction_uuid, amount, status)
             VALUES (?, ?, ?, 'PENDING')`,
            [order_id, transactionUuid, order.total_amount]
        );

        return res.status(200).json({
            success: true,
            form_url: formUrl,
            fields,
            transaction_uuid: transactionUuid
        });
    } catch (error) {
        console.error("initiateEpayPayment error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * GET /api/payments/esewa-epay/checkout/:transactionUuid — serves a
 * self-submitting form so the checkout can be opened in a REAL browser
 * (Chrome) instead of an in-app WebView. Exists because eSewa's login gates
 * on reCAPTCHA, which is unreliable inside a WebView; Chrome handles it
 * natively, and the tiffincraft:// return redirect still hands control back
 * to the app via its manifest intent-filter.
 *
 * Deliberately unauthenticated: it's opened by an external browser that
 * carries no JWT. It's scoped tightly instead — only a still-PENDING,
 * under-15-minute payment resolves, and the page exposes nothing beyond the
 * amount the payer is about to pay anyway. It cannot move money by itself;
 * confirmation still requires signature verification in handleEpayReturn.
 */
export const serveEpayCheckoutForm = async (req, res) => {
    try {
        const { transactionUuid } = req.params;

        const [payments] = await db.promise().query(
            `SELECT * FROM payments
             WHERE transaction_uuid = ? AND status = 'PENDING'
               AND booking_id IS NULL
               AND created_at > DATE_SUB(NOW(), INTERVAL 15 MINUTE)`,
            [transactionUuid]
        );
        if (payments.length === 0) {
            return res.status(404).send("<html><body style='font-family:sans-serif;padding:24px'><h3>This payment link has expired.</h3><p>Please start the payment again from the TiffinCraft app.</p></body></html>");
        }
        const payment = payments[0];

        const redirectBaseUrl = getRedirectBaseUrl();
        const returnUrl = `${redirectBaseUrl}/api/payments/esewa-epay/return?transaction_uuid=${encodeURIComponent(transactionUuid)}`;
        const { formUrl, fields } = buildEpayFormFields({
            amount: payment.amount,
            transactionUuid,
            successUrl: returnUrl,
            failureUrl: returnUrl
        });

        const escapeHtml = (s) => String(s)
            .replace(/&/g, "&amp;").replace(/"/g, "&quot;")
            .replace(/</g, "&lt;").replace(/>/g, "&gt;");

        const inputs = Object.entries(fields)
            .map(([k, v]) => `<input type="hidden" name="${escapeHtml(k)}" value="${escapeHtml(v)}">`)
            .join("");

        res.set("Content-Type", "text/html");
        return res.send(
            `<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">`
            + `<title>Redirecting to eSewa…</title></head>`
            + `<body style="font-family:sans-serif;padding:24px;text-align:center" onload="document.forms[0].submit()">`
            + `<p>Redirecting you to eSewa…</p>`
            + `<form method="POST" action="${escapeHtml(formUrl)}">${inputs}`
            + `<noscript><button type="submit">Continue to eSewa</button></noscript></form>`
            + `</body></html>`
        );
    } catch (error) {
        console.error("serveEpayCheckoutForm error:", error);
        return res.status(500).send("<html><body>Something went wrong. Please try again from the app.</body></html>");
    }
};

/**
 * GET /api/payments/esewa-epay/return — public, hit by the WebView after
 * eSewa redirects back (success or failure both land here). Always ends by
 * redirecting to the app's own deeplink — the WebView's WebViewClient
 * intercepts that scheme and hands off to PaymentResultActivity, which
 * re-verifies via GET /esewa/status/:transactionUuid regardless of what
 * happened here. This handler's real job is just to confirm-and-record.
 */
export const handleEpayReturn = async (req, res) => {
    const { transaction_uuid: transactionUuid, data } = req.query;

    try {
        if (!transactionUuid) {
            console.warn("⚠️  eSewa ePay return hit with no transaction_uuid:", req.query);
            return res.redirect(302, REDIRECT_URL);
        }

        const [payments] = await db.promise().query("SELECT * FROM payments WHERE transaction_uuid = ?", [transactionUuid]);
        if (payments.length === 0) {
            console.warn("⚠️  eSewa ePay return for unknown transaction_uuid:", transactionUuid);
            return res.redirect(302, REDIRECT_URL);
        }
        const payment = payments[0];

        if (data) {
            let decoded;
            try {
                decoded = JSON.parse(Buffer.from(data, "base64").toString("utf-8"));
            } catch {
                console.warn("⚠️  eSewa ePay return: malformed base64 'data' param");
                decoded = null;
            }

            if (decoded && decoded.signature && decoded.signed_field_names) {
                const validSignature = verifyEsewaSignature(decoded, decoded.signed_field_names, decoded.signature, getEpaySecretKey());
                if (!validSignature) {
                    console.warn("⚠️  eSewa ePay callback signature verification FAILED:", decoded);
                } else if (decoded.transaction_uuid !== transactionUuid) {
                    console.warn(`⚠️  eSewa ePay callback transaction_uuid mismatch: expected ${transactionUuid}, got ${decoded.transaction_uuid}`);
                } else {
                    const mappedStatus = mapEpayStatus(decoded.status);
                    if (mappedStatus === "SUCCESS" && Math.abs(Number(payment.amount) - Number(decoded.total_amount)) > 0.01) {
                        console.error(`❌ eSewa ePay amount mismatch for payment ${payment.id}: expected=${payment.amount} got=${decoded.total_amount}`);
                    } else {
                        // Independently re-confirm via the status API rather than
                        // trusting this redirect alone, same as the Intent flow.
                        const statusResult = await checkEpayStatus({ transactionUuid, totalAmount: payment.amount });
                        const confirmedStatus = statusResult.ok && statusResult.data?.status
                            ? mapEpayStatus(statusResult.data.status)
                            : mappedStatus; // status API unreachable — fall back to the (already signature-verified) redirect data
                        await applyConfirmedPaymentStatus(payment, confirmedStatus, statusResult.data?.ref_id || decoded.transaction_code, decoded);
                    }
                }
            }
        } else {
            // No data param — failure_url path, or the user backed out.
            // Ask the status API directly rather than assuming FAILED,
            // since a slow-completing payment could still land here.
            const statusResult = await checkEpayStatus({ transactionUuid, totalAmount: payment.amount });
            if (statusResult.ok && statusResult.data?.status) {
                await applyConfirmedPaymentStatus(payment, mapEpayStatus(statusResult.data.status), statusResult.data.ref_id, statusResult.data);
            }
        }
    } catch (error) {
        console.error("handleEpayReturn error:", error);
        // Fall through to the redirect regardless — PaymentResultActivity's
        // own status poll is the real source of truth for the customer.
    }

    return res.redirect(302, REDIRECT_URL);
};
