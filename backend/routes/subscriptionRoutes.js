import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { subscriptionInitiateLimiter } from "../middleware/rateLimiters.js";
import {
    createSubscription,
    getMySubscriptions,
    getCookSubscribers,
    pauseSubscription,
    resumeSubscription,
    skipDay,
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
router.put("/:id/pause", protect, pauseSubscription);
router.put("/:id/resume", protect, resumeSubscription);
router.put("/:id/skip", protect, roleOnly("customer"), skipDay);
router.put("/:id/screenshot", protect, roleOnly("customer"), uploadSubscriptionScreenshot);
router.put("/:id/verify-payment", protect, roleOnly("cook"), verifySubscriptionPayment);
router.delete("/:id", protect, cancelSubscription);

export default router;
