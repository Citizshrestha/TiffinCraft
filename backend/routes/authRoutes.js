import express from "express";
import {
    registerUser,
    loginUser,
    getCurrentUser,
    verifyOTP,
    resendOTP,
    forgotPassword,
    resetPassword,
    changePassword,
    getCustomerProfile,
    updateCustomerProfile,
    uploadCustomerProfileImage,
    updateFcmToken,
    deleteOwnAccount,
    logoutUser
} from "../controllers/authController.js";
import {
    verifyGoogleToken
} from "../controllers/oauthController.js";
import authMiddleware from "../middleware/authMiddleware.js";
import { uploadSingle } from "../middleware/uploadMiddleware.js";
import { validateRegister, validateLogin } from "../middleware/validation.js";

const router = express.Router();

// ponytail: rate limiting deliberately deferred — heavy manual testing is in
// progress and per-IP throttling would block that. Add express-rate-limit
// here (see cookRoutes.js's profileUpdateLimiter for the pattern) once testing
// winds down and before this goes anywhere near production.
router.post("/register", validateRegister, registerUser);
router.post("/login", validateLogin, loginUser);
router.post("/verify-otp", verifyOTP);
router.post("/resend-otp", resendOTP);
router.post("/forgot-password", forgotPassword);
router.post("/reset-password", resetPassword);
router.put("/change-password", authMiddleware, changePassword);
router.get("/me", authMiddleware, getCurrentUser);

router.get("/profile", authMiddleware, getCustomerProfile);
router.put("/profile", authMiddleware, updateCustomerProfile);
router.post("/profile/image", authMiddleware, uploadSingle("profile_image"), uploadCustomerProfileImage);

router.put("/fcm-token", authMiddleware, updateFcmToken);
router.post("/logout", authMiddleware, logoutUser);

// Self-service account deletion — asks for current password, then deletes the
// user row (ON DELETE CASCADE handles all related tables).
router.delete("/account", authMiddleware, deleteOwnAccount);

router.post("/google/verify", verifyGoogleToken);

export default router;
