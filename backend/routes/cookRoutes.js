import { Router } from "express";
import rateLimit from "express-rate-limit";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { cookAvailabilityLimiter } from "../middleware/rateLimiters.js";
import { uploadSingle } from "../middleware/uploadMiddleware.js";
import {
    setupCookProfile,
    getMyCookProfile,
    updateCookProfile,
    uploadCookProfileImage,
    getAllCooks,
    getCookById,
    getCookDashboard,
    updateCookCompleteProfile,
    updateHolidayMode,
    updateOperatingHours,
    updateBankDetails,
    getNearbyCooks
} from "../controllers/cookController.js";
import {
    setCookDailyUnavailability,
    clearCookDailyUnavailability,
    markSubscriptionDeliveryUnavailable,
    restoreSubscriptionDelivery,
    getTodayDeliveries
} from "../controllers/cookDeliveryController.js";

const router = Router();


const profileUpdateLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 20,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message: "Too many profile updates, try again later." }
});


const nearbyLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 60,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message: "Too many nearby searches, try again later." }
});

router.post("/profile", protect, roleOnly("cook"), setupCookProfile);
router.get("/profile", protect, roleOnly("cook"), getMyCookProfile);
router.put("/profile", protect, roleOnly("cook"), updateCookProfile);
router.put("/profile/complete", profileUpdateLimiter, protect, roleOnly("cook"), updateCookCompleteProfile);

router.post(
    "/profile/image",
    protect,
    roleOnly("cook"),
    uploadSingle("profile_image"),
    uploadCookProfileImage
);

// Profile management endpoints
router.put("/profile/holiday-mode", protect, roleOnly("cook"), updateHolidayMode);
router.put("/profile/operating-hours", protect, roleOnly("cook"), updateOperatingHours);
router.put("/profile/bank-details", protect, roleOnly("cook"), updateBankDetails);

// Dashboard endpoint
router.get("/dashboard", protect, roleOnly("cook"), getCookDashboard);


router.get("/nearby", nearbyLimiter, protect, roleOnly("customer"), getNearbyCooks);


router.get("/today-deliveries", protect, roleOnly("cook"), getTodayDeliveries);
router.post("/subscriptions/:id/unavailable-day", protect, roleOnly("cook"), cookAvailabilityLimiter, markSubscriptionDeliveryUnavailable);
router.delete("/subscriptions/:id/unavailable-day/:date", protect, roleOnly("cook"), cookAvailabilityLimiter, restoreSubscriptionDelivery);
router.post("/daily-availability", protect, roleOnly("cook"), cookAvailabilityLimiter, setCookDailyUnavailability);
router.delete("/daily-availability/:date", protect, roleOnly("cook"), cookAvailabilityLimiter, clearCookDailyUnavailability);

router.get("/", getAllCooks);
router.get("/:cookId", getCookById);

export default router;
