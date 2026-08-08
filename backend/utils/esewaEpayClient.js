import { signFields } from "./esewaSignature.js";

/**
 * eSewa ePay v2 client — a DIFFERENT, OLDER eSewa product than Intent
 * Payment (esewaClient.js), with its own host, credentials, and field
 * names. This is NOT the deprecated native Android SDK — that deprecation
 * only covers eSewa's own .aar wrapper around this same web API. The
 * underlying HTTP form-POST + signature flow below is eSewa's current,
 * documented ePay v2 integration path (developer.esewa.com.np/pages/Epay,
 * fetched 2026-07-27), consumed here via a WebView-hosted form instead of
 * their deprecated SDK.
 *
 * Exists as a fallback: as of this writing, Intent Payment's sandbox
 * (rc-checkout.esewa.com.np) is down (bare 502 from nginx, confirmed at the
 * domain root too), while ePay v2's sandbox (rc-epay.esewa.com.np) responds
 * normally. Keep BOTH — Intent Payment is still the better long-term choice
 * once eSewa's sandbox recovers.
 */
const isProd = process.env.NODE_ENV === "production";

const FORM_URL = process.env.ESEWA_EPAY_FORM_URL
    || (isProd ? null : "https://rc-epay.esewa.com.np/api/epay/main/v2/form");
const STATUS_URL = process.env.ESEWA_EPAY_STATUS_URL
    || (isProd ? null : "https://rc.esewa.com.np/api/epay/transaction/status/");

// Sandbox values below are eSewa's own long-published, universally-known
// test constants for ePay v2 (same ones used across their official docs and
// every third-party integration guide) — not a real secret, safe to commit.
const PRODUCT_CODE = process.env.ESEWA_EPAY_PRODUCT_CODE || (isProd ? null : "EPAYTEST");
const SECRET_KEY = process.env.ESEWA_EPAY_SECRET_KEY || (isProd ? null : "8gBm/:&EnhH.1/q");

if (isProd && (!FORM_URL || !STATUS_URL || !PRODUCT_CODE || !SECRET_KEY)) {
    console.error("❌ ESEWA_EPAY_FORM_URL / ESEWA_EPAY_STATUS_URL / ESEWA_EPAY_PRODUCT_CODE / ESEWA_EPAY_SECRET_KEY must be set in production.");
}

/**
 * Builds the complete, signed field set for eSewa's ePay v2 form. The
 * caller (Android WebView) auto-submits these as a real POST to FORM_URL —
 * nothing here talks to eSewa directly, since this product is a hosted
 * redirect flow, not a JSON API like Intent Payment.
 */
export function buildEpayFormFields({ amount, transactionUuid, successUrl, failureUrl }) {
    const totalAmount = Number(amount);
    const fields = {
        amount: String(totalAmount),
        tax_amount: "0",
        total_amount: String(totalAmount),
        transaction_uuid: transactionUuid,
        product_code: PRODUCT_CODE,
        product_service_charge: "0",
        product_delivery_charge: "0",
        success_url: successUrl,
        failure_url: failureUrl
    };
    const signedFieldNames = "total_amount,transaction_uuid,product_code";
    const signature = signFields(fields, signedFieldNames, SECRET_KEY);

    return { formUrl: FORM_URL, fields: { ...fields, signed_field_names: signedFieldNames, signature } };
}

/**
 * GET status check — ePay v2 takes query params, not a signed JSON body
 * (unlike Intent Payment's /status endpoint).
 */
export async function checkEpayStatus({ transactionUuid, totalAmount }) {
    const url = `${STATUS_URL}?product_code=${encodeURIComponent(PRODUCT_CODE)}`
        + `&total_amount=${encodeURIComponent(totalAmount)}`
        + `&transaction_uuid=${encodeURIComponent(transactionUuid)}`;
    let res, json;
    try {
        res = await fetch(url);
        json = await res.json().catch(() => null);
    } catch (err) {
        return { ok: false, message: `eSewa status request failed: ${err.message}`, data: null };
    }
    if (!json) {
        return { ok: false, message: `eSewa status returned a non-JSON response (HTTP ${res.status})`, data: null };
    }
    return { ok: res.ok, message: null, data: json };
}

export function getEpaySecretKey() {
    return SECRET_KEY;
}

export function getEpayProductCode() {
    return PRODUCT_CODE;
}

/**
 * ePay v2 uses a completely different status vocabulary than Intent
 * Payment — COMPLETE/PENDING/CANCELED/NOT_FOUND/AMBIGUOUS/FULL_REFUND
 * (confirmed via eSewa's own docs + cross-checked against multiple
 * independent public integration guides) — mapped here onto the shared
 * `payments.status` ENUM (BOOKED/PENDING/SUCCESS/FAILED/CANCELED/REVERTED)
 * so both products can share the same table and confirmation logic.
 */
export function mapEpayStatus(rawStatus) {
    switch (rawStatus) {
        case "COMPLETE": return "SUCCESS";
        case "PENDING": return "PENDING";
        case "CANCELED": return "CANCELED";
        case "FULL_REFUND": return "REVERTED";
        case "NOT_FOUND": return "FAILED";
        // AMBIGUOUS is genuinely unclear — never treat it as a success;
        // leave it PENDING so it resolves on a later poll/manual review
        // rather than risk crediting an order that didn't actually pay.
        case "AMBIGUOUS": return "PENDING";
        default: return "PENDING";
    }
}
