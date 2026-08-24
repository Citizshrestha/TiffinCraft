import rateLimit, { ipKeyGenerator } from "express-rate-limit";

/**
 * Per-user rate limits for endpoints that create money-moving state.
 *
 * Keyed on the authenticated user id, not the IP — a shared NAT (a hostel, a
 * campus, one mobile carrier) would otherwise let one abusive account throttle
 * everybody else, and a single user on rotating mobile IPs would sail past an
 * IP-keyed limit. That means these MUST be mounted after `protect`.
 *
 * 429 is chosen deliberately: the Android FailoverInterceptor treats 502/503/504
 * as its own dev tunnel misbehaving and swallows the body, so a rate-limit
 * message sent with those codes would never reach the user.
 */
function perUserLimiter({ windowMs, limit, message }) {
    return rateLimit({
        windowMs,
        limit,
        standardHeaders: "draft-7",
        legacyHeaders: false,
        keyGenerator: (req) =>
            req.user?.id ? `u:${req.user.id}` : `ip:${ipKeyGenerator(req.ip)}`,
        handler: (req, res) => res.status(429).json({ success: false, message })
    });
}

/**
 * Subscription checkout. Each call mints a transaction_uuid and a signed eSewa
 * form, so an unbounded loop here would spray dead PENDING rows at the gateway.
 * 6 per 10 minutes still leaves room for genuine retries after a failed or
 * abandoned payment.
 */
export const subscriptionInitiateLimiter = perUserLimiter({
    windowMs: 10 * 60 * 1000,
    limit: 6,
    message: "Too many payment attempts. Please wait a few minutes before trying again."
});
