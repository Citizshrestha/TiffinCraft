import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
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

const router = Router();

router.post("/", protect, roleOnly("customer"), createSubscription);
router.get("/customer/my", protect, roleOnly("customer"), getMySubscriptions);
router.get("/cook/my", protect, roleOnly("cook"), getCookSubscribers);
router.put("/:id/pause", protect, pauseSubscription);
router.put("/:id/resume", protect, resumeSubscription);
router.put("/:id/skip", protect, roleOnly("customer"), skipDay);
router.put("/:id/screenshot", protect, roleOnly("customer"), uploadSubscriptionScreenshot);
router.put("/:id/verify-payment", protect, roleOnly("cook"), verifySubscriptionPayment);
router.delete("/:id", protect, cancelSubscription);

export default router;
