import express from "express";
import passport from "../config/passport.js";
import {
    registerUser,
    loginUser,
    getCurrentUser,
    verifyOTP,
    resendOTP,
    forgotPassword,
    resetPassword
} from "../controllers/authController.js";
import {
    oauthSuccess,
    oauthFailure,
    verifyGoogleToken
} from "../controllers/oauthController.js";
import authMiddleware from "../middleware/authMiddleware.js";

const router = express.Router();

router.post("/register", registerUser);
router.post("/login", loginUser);
router.post("/verify-otp", verifyOTP);
router.post("/resend-otp", resendOTP);
router.post("/forgot-password", forgotPassword);
router.post("/reset-password", resetPassword);
router.get("/me", authMiddleware, getCurrentUser);

// Mobile OAuth token verification endpoints
router.post("/google/verify", verifyGoogleToken);

router.get("/google",
    passport.authenticate("google", {
        scope: ["profile", "email"],
        session: false
    })
);

router.get("/google/callback",
    passport.authenticate("google", {
        session: false,
        failureRedirect: "/api/auth/failure"
    }),
    oauthSuccess
);

router.get("/failure", oauthFailure);

export default router;