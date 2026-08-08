import crypto from "crypto";

/**
 * eSewa Intent Payment signatures are HMAC-SHA256, base64-encoded, over a
 * comma-separated "key=value" message whose field order must exactly match
 * the signed_field_names sent alongside it (order matters — it's part of
 * what gets signed, not just a hint to the verifier).
 */
export function generateEsewaSignature(message, secretKey) {
    const hmac = crypto.createHmac("sha256", secretKey);
    hmac.update(message);
    return hmac.digest("base64");
}

/**
 * Builds the "field=value,field=value" message from a fields object and the
 * signed_field_names order, then signs it. Used identically for outgoing
 * requests (book/status/cancel) and for verifying eSewa's incoming callback.
 */
export function signFields(fields, signedFieldNames, secretKey) {
    const order = signedFieldNames.split(",").map((f) => f.trim());
    const message = order.map((field) => `${field}=${fields[field]}`).join(",");
    return generateEsewaSignature(message, secretKey);
}

/** Recomputes the signature over `fields` and compares it to `signature`. */
export function verifyEsewaSignature(fields, signedFieldNames, signature, secretKey) {
    if (!signature || !signedFieldNames) return false;
    const expected = signFields(fields, signedFieldNames, secretKey);
    // Both are base64 HMACs of the same fixed length — constant-time compare
    // avoids leaking timing info about how much of the signature matched.
    const a = Buffer.from(expected);
    const b = Buffer.from(signature);
    if (a.length !== b.length) return false;
    return crypto.timingSafeEqual(a, b);
}
