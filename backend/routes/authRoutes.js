import express from "express";
import {
    registerUser,
    loginUser,
    getCurrentUser,
    verifyOTP,
    resendOTP,
    forgotPassword,
    resetPassword,
    getCustomerProfile,
    updateCustomerProfile,
    uploadCustomerProfileImage
} from "../controllers/authController.js";
import {
    verifyGoogleToken
} from "../controllers/oauthController.js";
import authMiddleware from "../middleware/authMiddleware.js";
import { uploadSingle } from "../middleware/uploadMiddleware.js";

const router = express.Router();

router.post("/register", registerUser);
router.post("/login", loginUser);
router.post("/verify-otp", verifyOTP);
router.post("/resend-otp", resendOTP);
router.post("/forgot-password", forgotPassword);
router.post("/reset-password", resetPassword);
router.get("/me", authMiddleware, getCurrentUser);

router.get("/profile", authMiddleware, getCustomerProfile);
router.put("/profile", authMiddleware, updateCustomerProfile);
router.post("/profile/image", authMiddleware, uploadSingle("profile_image"), uploadCustomerProfileImage);

router.post("/google/verify", verifyGoogleToken);

export default router;