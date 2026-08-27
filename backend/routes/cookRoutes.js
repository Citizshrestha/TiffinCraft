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
    getTodayDeliveries
} from "../controllers/cookDeliveryController.js";

const router = Router();

// Location writes ride on profile updates — throttle them so a compromised
// token can't hammer coordinate changes; generous enough for normal editing.
const profileUpdateLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 20,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message: "Too many profile updates, try again later." }
});

// Blunts scraping of distance-ranked cook positions.
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

// CRITICAL: /nearby must precede the greedy "/:cookId" route below, or Express
// matches the literal string "nearby" as :cookId and this route is dead.
router.get("/nearby", nearbyLimiter, protect, roleOnly("customer"), getNearbyCooks);

// Subscription delivery operations. Same CRITICAL ordering constraint as
// /nearby above — these are literal paths and must be declared before
// "/:cookId", or "today-deliveries" is matched as a cook id.
router.get("/today-deliveries", protect, roleOnly("cook"), getTodayDeliveries);
router.post("/daily-availability", protect, roleOnly("cook"), cookAvailabilityLimiter, setCookDailyUnavailability);
router.delete("/daily-availability/:date", protect, roleOnly("cook"), cookAvailabilityLimiter, clearCookDailyUnavailability);

router.get("/", getAllCooks);
router.get("/:cookId", getCookById);

export default router;
