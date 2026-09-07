import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { subscriptionInitiateLimiter, skipDayLimiter, dayHandshakeLimiter } from "../middleware/rateLimiters.js";
import {
    createSubscription,
    getMySubscriptions,
    getCookSubscribers,
    pauseSubscription,
    resumeSubscription,
    skipDay,
    markDaySent,
    markDayReceived,
    getSubscriptionCalendar,
    cancelSubscription,
    uploadSubscriptionScreenshot,
    verifySubscriptionPayment
} from "../controllers/subscriptionController.js";
import {
    initiateSubscriptionPayment,
    getSubscriptionPaymentEvents
} from "../controllers/subscriptionPaymentController.js";
import {
    createSubscriptionRequest,
    respondToSubscriptionRequest,
    submitPaymentProof,
    verifySubscriptionProof,
    getCookSubscriptionRequests,
    getSubscriptionDetail
} from "../controllers/subscriptionRequestController.js";
import {
    createCustomMealRequest,
    getCustomMealRequests,
    getCookCustomMealRequests
} from "../controllers/customMealController.js";
import { uploadSingle, handleUploadError } from "../middleware/uploadMiddleware.js";

const router = Router();


router.get("/cook/requests", protect, roleOnly("cook"), getCookSubscriptionRequests);
router.get("/cook/custom-meal-requests", protect, roleOnly("cook"), getCookCustomMealRequests);

router.post("/request", protect, roleOnly("customer"), subscriptionInitiateLimiter, createSubscriptionRequest);
router.put("/:id/respond", protect, roleOnly("cook"), respondToSubscriptionRequest);


router.post("/:id/payment-proof", protect, roleOnly("customer"), uploadSingle("proof"), submitPaymentProof);
router.put("/:id/verify-proof", protect, roleOnly("cook"), verifySubscriptionProof);


router.get("/:id/detail", protect, getSubscriptionDetail);


router.post("/:id/custom-meal", protect, roleOnly("customer"), createCustomMealRequest);
router.get("/:id/custom-meals", protect, getCustomMealRequests);


router.post("/initiate", protect, roleOnly("customer"), subscriptionInitiateLimiter, initiateSubscriptionPayment);

router.post("/", protect, roleOnly("customer"), createSubscription);
router.get("/customer/my", protect, roleOnly("customer"), getMySubscriptions);
router.get("/cook/my", protect, roleOnly("cook"), getCookSubscribers);
router.get("/:id/payment-events", protect, getSubscriptionPaymentEvents);


router.get("/:id/calendar", protect, getSubscriptionCalendar);

router.put("/:id/pause", protect, pauseSubscription);
router.put("/:id/resume", protect, resumeSubscription);


router.post("/:id/skip-day", protect, roleOnly("customer"), skipDayLimiter, skipDay);


router.put("/:id/skip", protect, roleOnly("customer"), skipDayLimiter, skipDay);


router.post("/:id/mark-sent", protect, roleOnly("cook"), dayHandshakeLimiter, markDaySent);
router.post("/:id/mark-received", protect, roleOnly("customer"), dayHandshakeLimiter, markDayReceived);

router.put("/:id/screenshot", protect, roleOnly("customer"), uploadSubscriptionScreenshot);
router.put("/:id/verify-payment", protect, roleOnly("cook"), verifySubscriptionPayment);
router.delete("/:id", protect, cancelSubscription);


router.use(handleUploadError);

export default router;
