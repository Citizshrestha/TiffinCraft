import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
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
    updateBankDetails
} from "../controllers/cookController.js";

const router = Router();

router.post("/profile", protect, roleOnly("cook"), setupCookProfile);
router.get("/profile", protect, roleOnly("cook"), getMyCookProfile);
router.put("/profile", protect, roleOnly("cook"), updateCookProfile);
router.put("/profile/complete", protect, roleOnly("cook"), updateCookCompleteProfile);

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

router.get("/", getAllCooks);
router.get("/:cookId", getCookById);

export default router;