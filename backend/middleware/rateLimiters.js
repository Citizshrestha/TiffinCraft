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

/**
 * Cook daily-availability toggle. One call fans a push notification out to every
 * one of that cook's active subscribers, so an unbounded loop of
 * close/reopen/close is a way to spam a hundred customers' phones at will — and
 * to burn the FCM quota the whole platform shares. 12 per hour is far more than
 * a real kitchen needs (a cook closes a day, occasionally undoes a mistake)
 * while capping the blast radius of a stolen token.
 */
export const cookAvailabilityLimiter = perUserLimiter({
    windowMs: 60 * 60 * 1000,
    limit: 12,
    message: "Too many availability changes. Please wait a while before changing more dates."
});

/**
 * Per-day skip toggle. Cheap for us, but it notifies the cook every time, and a
 * customer flipping one day back and forth is a pager for someone's kitchen.
 */
export const skipDayLimiter = perUserLimiter({
    windowMs: 60 * 60 * 1000,
    limit: 20,
    message: "Too many changes to your delivery days. Please wait a while before making more."
});
