import express from "express";
import {
    getCustomerDashboard,
    getCustomerById,
    getNotifications,
    markNotificationAsRead,
    markAllNotificationsAsRead
} from "../controllers/customerDashboardController.js";
import {
    getCustomerProfile,
    updateCustomerProfile,
    uploadCustomerProfileImage
} from "../controllers/authController.js";
import authMiddleware, { roleOnly } from "../middleware/authMiddleware.js";
import { uploadSingle } from "../middleware/uploadMiddleware.js";

const router = express.Router();

// The Android app calls these under /api/customer/* (see ApiService); the handlers
// live in authController.js and are also reachable under /api/auth/* — mounted here
// too so the existing app build works without a rebuild.
router.get("/profile", authMiddleware, getCustomerProfile);
router.put("/profile", authMiddleware, updateCustomerProfile);
router.post("/profile/image", authMiddleware, uploadSingle("profile_image"), uploadCustomerProfileImage);

router.get("/dashboard", authMiddleware, getCustomerDashboard);

router.get("/notifications", authMiddleware, getNotifications);
router.put("/notifications/:id/read", authMiddleware, markNotificationAsRead);
router.put("/notifications/read-all", authMiddleware, markAllNotificationsAsRead);

// A cook viewing a customer's details from a shared order/chat — must come after the
// literal routes above, or Express would match "profile"/"dashboard" as :customerId.
router.get("/:customerId", authMiddleware, roleOnly("cook"), getCustomerById);

export default router;
