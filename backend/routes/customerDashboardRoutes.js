import express from "express";
import {
    getCustomerDashboard,
    getNotifications,
    markNotificationAsRead,
    markAllNotificationsAsRead
} from "../controllers/customerDashboardController.js";
import authMiddleware from "../middleware/authMiddleware.js";

const router = express.Router();

// Dashboard endpoint
router.get("/dashboard", authMiddleware, getCustomerDashboard);

// Notifications endpoints
router.get("/notifications", authMiddleware, getNotifications);
router.put("/notifications/:id/read", authMiddleware, markNotificationAsRead);
router.put("/notifications/read-all", authMiddleware, markAllNotificationsAsRead);

export default router;
