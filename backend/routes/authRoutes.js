import { Router } from "express";
import {
    registerUser,
    loginUser,
    getCurrentUser,
    logoutUser
} from "../controllers/authController.js";

const router = Router();

// Test route
router.get('/test', (req, res) => {
    res.json({
        status: "success",
        message: "Auth routes are working",
        timestamp: new Date().toISOString()
    });
});

// Registration and login
router.post("/register", registerUser);
router.post("/login", loginUser);

// Logout
router.post("/logout", logoutUser);

// Get current user
router.get("/me", getCurrentUser);

export default router;
