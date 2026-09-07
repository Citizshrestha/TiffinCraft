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


router.get("/profile", authMiddleware, roleOnly("customer"), getCustomerProfile);
router.put("/profile", authMiddleware, roleOnly("customer"), updateCustomerProfile);
router.post("/profile/image", authMiddleware, roleOnly("customer"), uploadSingle("profile_image"), uploadCustomerProfileImage);

router.get("/dashboard", authMiddleware, roleOnly("customer"), getCustomerDashboard);

router.get("/notifications", authMiddleware, roleOnly("customer"), getNotifications);
router.put("/notifications/read-all", authMiddleware, roleOnly("customer"), markAllNotificationsAsRead);
router.put("/notifications/:id/read", authMiddleware, roleOnly("customer"), markNotificationAsRead);

// A cook viewing a customer's details from a shared order/chat — must come after the
// literal routes above, or Express would match "profile"/"dashboard" as :customerId.
router.get("/:customerId", authMiddleware, roleOnly("cook"), getCustomerById);

export default router;
