import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { uploadProfileImage } from "../middleware/uploadMiddleware.js";
import {
    setupCookProfile,
    getMyCookProfile,
    updateCookProfile,
    uploadCookProfileImage,
    getAllCooks,
    getCookById,
    getCookDashboard,
    updateCookCompleteProfile
} from "../controllers/cookController.js";

const router = Router();

router.post("/profile", protect, roleOnly("cook"), setupCookProfile);
router.get("/profile", protect, roleOnly("cook"), getMyCookProfile);
router.put("/profile", protect, roleOnly("cook"), updateCookProfile);

router.post(
    "/profile/image",
    protect,
    roleOnly("cook"),
    uploadProfileImage,
    uploadCookProfileImage
);

// Dashboard endpoint
router.get("/dashboard", protect, roleOnly("cook"), getCookDashboard);

router.get("/", getAllCooks);
router.get("/:cookId", getCookById);

export default router;