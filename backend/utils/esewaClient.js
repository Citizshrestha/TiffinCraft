import { signFields } from "./esewaSignature.js";

/**
 * eSewa Intent Payment API client.
 *
 * Field names/endpoints below are verified against the official docs at
 * https://developer.esewa.com.np/pages/Intent (fetched 2026-07-27) — NOT
 * guessed. Sandbox host is rc-checkout.esewa.com.np; the production host is
 * issued along with your product_code when eSewa approves your merchant
 * application at https://merchant.esewa.com.np — set ESEWA_BASE_URL then.
 *
 * Sandbox defaults (safe to commit — these are eSewa's own published test
 * values, not a real secret): product_code "INTENT", the dev signature
 * access key from eSewa's docs. Production values MUST come from .env only.
 */
const isProd = process.env.NODE_ENV === "production";

const BASE_URL = process.env.ESEWA_BASE_URL
    || (isProd ? null : "https://rc-checkout.esewa.com.np/api/client/intent/payment");

const PRODUCT_CODE = process.env.ESEWA_PRODUCT_CODE || (isProd ? null : "INTENT");

const SECRET_KEY = process.env.ESEWA_SECRET_KEY
    || (isProd ? null : "LB0REg8HUSw3MTYrI1s6JTE8Kyc6JyAqJiA3MQ==");

if (isProd && (!BASE_URL || !PRODUCT_CODE || !SECRET_KEY)) {
    // Fail loud at boot, not silently at the first real payment attempt.
    console.error("❌ ESEWA_BASE_URL / ESEWA_PRODUCT_CODE / ESEWA_SECRET_KEY must be set in production — refusing to fall back to sandbox values.");
}

/**
 * Dev-only escape hatch for when eSewa's own sandbox is down. NOTE: this is
 * a module-load-time constant — editing ESEWA_MOCK_MODE in .env has no
 * effect on an already-running server; nodemon doesn't watch .env by
 * default, so the process needs an actual restart (or any .js file touch)
 * to pick up the new value.
 * Simulates a successful booking/status/cancel using the SAME downstream
 * code paths as a real payment (order gets marked paid, cook gets notified,
 * etc.) — only the actual HTTP call to eSewa is skipped. Explicit opt-in,
 * and hard-disabled in production regardless of the env var's value.
 */
const MOCK_MODE = !isProd && process.env.ESEWA_MOCK_MODE === "true";
if (MOCK_MODE) {
    console.warn("⚠️  ESEWA_MOCK_MODE is ON — eSewa API calls are simulated, no real payment is happening. Turn this off once eSewa's sandbox is back.");
}

let mockCounter = 0;
function nextMockId(prefix) {
    mockCounter += 1;
    return `${prefix}-${Date.now()}-${mockCounter}`;
}

async function callEsewa(path, body) {
    const url = `${BASE_URL}/${path}`;
    let res, text, json;
    try {
        res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        text = await res.text();
        try {
            json = JSON.parse(text);
        } catch {
            // Not JSON — an infra-level error page (502/504 from eSewa's own
            // gateway) rather than an API response. Surface the raw text so
            // it's diagnosable instead of silently becoming a bare null.
            json = null;
        }
    } catch (err) {
        return { ok: false, httpStatus: 0, code: null, data: null, message: `eSewa request failed: ${err.message}`, raw: null };
    }
    if (!json) {
        return { ok: false, httpStatus: res.status, code: null, data: null, message: `eSewa returned a non-JSON response (HTTP ${res.status})`, raw: text.slice(0, 500) };
    }
    return {
        ok: res.ok,
        httpStatus: res.status,
        code: json?.code ?? null,
        data: json?.data ?? null,
        message: json?.message ?? null,
        raw: json
    };
}

/**
 * POST /book — creates a payment booking and returns the deeplink that opens
 * the eSewa app. signed_field_names is fixed per eSewa's spec for this
 * endpoint: product_code,amount,transaction_uuid (order matters).
 */
export async function bookEsewaPayment({ amount, transactionUuid, callbackUrl, redirectUrl, properties }) {
    if (MOCK_MODE) {
        // redirect_url IS tiffincraft://payment-result — reusing it as the
        // "deeplink" means Android's real ACTION_VIEW code opens our own
        // PaymentResultActivity directly, exactly as if eSewa had redirected
        // back, with zero Android-side changes needed to exercise the flow.
        return {
            ok: true, httpStatus: 200, code: "MOCK-201",
            data: { booking_id: nextMockId("mock-booking"), correlation_id: nextMockId("mock-corr"), deeplink: redirectUrl },
            message: "Mocked booking (ESEWA_MOCK_MODE=true)", raw: null
        };
    }

    const fields = { product_code: PRODUCT_CODE, amount: String(amount), transaction_uuid: transactionUuid };
    const signedFieldNames = "product_code,amount,transaction_uuid";
    const signature = signFields(fields, signedFieldNames, SECRET_KEY);

    return callEsewa("book", {
        ...fields,
        signed_field_names: signedFieldNames,
        signature,
        callback_url: callbackUrl,
        redirect_url: redirectUrl,
        properties: properties || {}
    });
}

/** POST /status — signed_field_names: booking_id,product_code,correlation_id. */
export async function checkEsewaStatus({ bookingId, correlationId }) {
    if (MOCK_MODE) {
        return {
            ok: true, httpStatus: 200, code: "MOCK-200",
            data: {
                booking_id: bookingId, product_code: PRODUCT_CODE, status: "SUCCESS",
                correlation_id: correlationId, transaction_id: nextMockId("mock-txn"),
                reference_code: nextMockId("MOCKREF"), updated_at: new Date().toISOString()
            },
            message: "Mocked status (ESEWA_MOCK_MODE=true)", raw: null
        };
    }

    const fields = { booking_id: bookingId, product_code: PRODUCT_CODE, correlation_id: correlationId };
    const signedFieldNames = "booking_id,product_code,correlation_id";
    const signature = signFields(fields, signedFieldNames, SECRET_KEY);

    return callEsewa("status", { ...fields, signed_field_names: signedFieldNames, signature });
}

/** POST /cancel — signed_field_names: booking_id,product_code. Only valid while status is BOOKED. */
export async function cancelEsewaPayment({ bookingId }) {
    if (MOCK_MODE) {
        return {
            ok: true, httpStatus: 200, code: "MOCK-210",
            data: { booking_id: bookingId, status: "CANCELED", correlation_id: null, transaction_id: null },
            message: "Mocked cancel (ESEWA_MOCK_MODE=true)", raw: null
        };
    }

    const fields = { booking_id: bookingId, product_code: PRODUCT_CODE };
    const signedFieldNames = "booking_id,product_code";
    const signature = signFields(fields, signedFieldNames, SECRET_KEY);

    return callEsewa("cancel", { ...fields, signed_field_names: signedFieldNames, signature });
}

/**
 * Turns the https deeplink eSewa returns from /book into the custom-scheme
 * URI that actually opens the eSewa app.
 *
 * Why this is needed: the deeplink (rc-links.esewa.com.np/pay/<slug>) is only
 * a redirector — it 302s to an `intent://d/<slug>?referralId=...#Intent;
 * scheme=esewa;package=com.f1soft.esewa;...;end` URL, which a BROWSER knows
 * how to turn into a real Android intent. The eSewa app itself only claims
 * the verified App Link hosts esewa.com.np and links.esewa.com.np (confirmed
 * via `dumpsys package com.f1soft.esewa`) — NOT the rc-links sandbox host —
 * so launching the https URL straight at eSewa's package resolves to nothing
 * in sandbox. It does register scheme "esewa" with authority "d", which is
 * what the redirect points at.
 *
 * Resolving it here rather than in the app keeps the network hop off the UI
 * thread and lets Android launch a plain, fully-known URI. Deliberately does
 * NOT hand the raw intent:// string to the client: parsing an attacker-
 * influenced intent:// URL is an intent-redirection footgun, so only the
 * scheme/path/query are rebuilt, and only when the scheme is "esewa".
 *
 * Safe to call repeatedly — verified the redirector is idempotent (three
 * fetches of one deeplink returned three clean 302s), so this does not
 * consume the booking.
 *
 * Returns null on any failure; callers must treat it as optional, since the
 * https deeplink still works by itself in production (links.esewa.com.np IS
 * verified by the app there).
 */
export async function resolveEsewaAppDeeplink(deeplink) {
    try {
        // MUST send a mobile-browser User-Agent. eSewa's redirector is
        // UA-sensitive: with Node's default UA it 302s to their marketing
        // site (esewa.com.np/?referrer=...) instead of the intent:// URL,
        // and this whole function silently returns null. Verified both ways.
        const res = await fetch(deeplink, {
            redirect: "manual",
            headers: {
                "User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
        });
        const location = res.headers.get("location");
        if (!location || !location.startsWith("intent://")) return null;

        // intent://d/SLUG?query#Intent;scheme=esewa;package=...;end
        const hashIndex = location.indexOf("#");
        const body = hashIndex === -1 ? location.slice("intent://".length) : location.slice("intent://".length, hashIndex);
        const fragment = hashIndex === -1 ? "" : location.slice(hashIndex + 1);

        const schemeMatch = /(?:^|;)scheme=([^;]+)/.exec(fragment);
        const scheme = schemeMatch ? schemeMatch[1] : null;
        // Only ever emit an eSewa-scheme URI — never blindly trust whatever
        // scheme the redirect asks for.
        if (scheme !== "esewa") return null;

        return `esewa://${body}`;
    } catch (err) {
        console.warn("⚠️  Could not resolve eSewa app deeplink:", err.message);
        return null;
    }
}

export function getEsewaProductCode() {
    return PRODUCT_CODE;
}

export function getEsewaSecretKey() {
    return SECRET_KEY;
}
