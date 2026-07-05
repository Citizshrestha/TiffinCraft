import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    placeOrder,
    getOrderById,
    getCustomerOrders,
    getCookOrders,
    updateOrderStatus,
    cancelOrder,
    getCookEarnings,
    uploadPaymentScreenshot,
    verifyPayment
} from "../controllers/orderController.js";

const router = Router();

router.post("/", protect, roleOnly("customer"), placeOrder);
router.get("/customer/my", protect, roleOnly("customer"), getCustomerOrders);
router.put("/:orderId/cancel", protect, roleOnly("customer"), cancelOrder);
router.post("/:orderId/payment-screenshot", protect, roleOnly("customer"), uploadPaymentScreenshot);

router.get("/cook/my", protect, roleOnly("cook"), getCookOrders);
router.get("/cook/earnings", protect, roleOnly("cook"), getCookEarnings);
router.put("/:orderId/status", protect, roleOnly("cook"), updateOrderStatus);
router.put("/:orderId/verify-payment", protect, roleOnly("cook"), verifyPayment);

router.get("/:orderId", protect, getOrderById);

export default router;