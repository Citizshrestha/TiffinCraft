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
import authMiddleware, { roleOnly } from "../middleware/authMiddleware.js";
import { uploadSingle } from "../middleware/uploadMiddleware.js";
import { validateRegister, validateLogin } from "../middleware/validation.js";

const router = express.Router();


router.post("/register", validateRegister, registerUser);
router.post("/login", validateLogin, loginUser);
router.post("/verify-otp", verifyOTP);
router.post("/resend-otp", resendOTP);
router.post("/forgot-password", forgotPassword);
router.post("/reset-password", resetPassword);
router.put("/change-password", authMiddleware, changePassword);
router.get("/me", authMiddleware, getCurrentUser);

router.get("/profile", authMiddleware, roleOnly("customer"), getCustomerProfile);
router.put("/profile", authMiddleware, roleOnly("customer"), updateCustomerProfile);
router.post("/profile/image", authMiddleware, roleOnly("customer"), uploadSingle("profile_image"), uploadCustomerProfileImage);

router.put("/fcm-token", authMiddleware, updateFcmToken);
router.post("/logout", authMiddleware, logoutUser);


router.delete("/account", authMiddleware, deleteOwnAccount);

router.post("/google/verify", verifyGoogleToken);

export default router;
