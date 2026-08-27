import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { subscriptionInitiateLimiter, skipDayLimiter } from "../middleware/rateLimiters.js";
import {
    createSubscription,
    getMySubscriptions,
    getCookSubscribers,
    pauseSubscription,
    resumeSubscription,
    skipDay,
    getSubscriptionCalendar,
    cancelSubscription,
    uploadSubscriptionScreenshot,
    verifySubscriptionPayment
} from "../controllers/subscriptionController.js";
import {
    initiateSubscriptionPayment,
    getSubscriptionPaymentEvents
} from "../controllers/subscriptionPaymentController.js";

const router = Router();

// Pay-first checkout. Rate limiter sits after `protect` because it keys on the
// authenticated user id (see rateLimiters.js).
router.post("/initiate", protect, roleOnly("customer"), subscriptionInitiateLimiter, initiateSubscriptionPayment);

router.post("/", protect, roleOnly("customer"), createSubscription);
router.get("/customer/my", protect, roleOnly("customer"), getMySubscriptions);
router.get("/cook/my", protect, roleOnly("cook"), getCookSubscribers);
router.get("/:id/payment-events", protect, getSubscriptionPaymentEvents);

// Per-day delivery calendar. No roleOnly: the controller authorises the caller
// as either the owning customer OR the owning cook against req.user.id, which
// roleOnly can't express.
router.get("/:id/calendar", protect, getSubscriptionCalendar);

router.put("/:id/pause", protect, pauseSubscription);
router.put("/:id/resume", protect, resumeSubscription);

// Skip a single delivery day. POST, not PUT: each call is a distinct dated event
// in the audit log rather than an idempotent overwrite of one resource, and
// the body carries { date, reason? }.
router.post("/:id/skip-day", protect, roleOnly("customer"), skipDayLimiter, skipDay);

// Legacy alias. The Android app in the field still calls PUT /:id/skip, and the
// rewritten skipDay is backwards-compatible with its body, so removing this
// would break every copy of the app that hasn't updated yet. Retire it once the
// new build is out.
router.put("/:id/skip", protect, roleOnly("customer"), skipDayLimiter, skipDay);

router.put("/:id/screenshot", protect, roleOnly("customer"), uploadSubscriptionScreenshot);
router.put("/:id/verify-payment", protect, roleOnly("cook"), verifySubscriptionPayment);
router.delete("/:id", protect, cancelSubscription);

export default router;
