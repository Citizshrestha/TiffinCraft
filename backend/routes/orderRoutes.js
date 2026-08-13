import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    placeOrder,
    getOrderById,
    getCustomerOrders,
    getCookOrders,
    updateOrderStatus,
    cancelOrder,
    cookCancelOrder,
    getCookEarnings,
    uploadPaymentScreenshot,
    verifyPayment,
    deleteOrder
} from "../controllers/orderController.js";
import {
    getCookEarningsSummary,
    getCookEarningsTransactions
} from "../controllers/earningsController.js";

const router = Router();

router.post("/", protect, roleOnly("customer"), placeOrder);
router.get("/customer/my", protect, roleOnly("customer"), getCustomerOrders);
router.put("/:orderId/cancel", protect, roleOnly("customer"), cancelOrder);
router.post("/:orderId/payment-screenshot", protect, roleOnly("customer"), uploadPaymentScreenshot);

router.get("/cook/my", protect, roleOnly("cook"), getCookOrders);
router.get("/cook/earnings", protect, roleOnly("cook"), getCookEarnings);
router.get("/cook/earnings/summary", protect, roleOnly("cook"), getCookEarningsSummary);
router.get("/cook/earnings/transactions", protect, roleOnly("cook"), getCookEarningsTransactions);
router.put("/:orderId/status", protect, roleOnly("cook"), updateOrderStatus);
router.put("/:orderId/verify-payment", protect, roleOnly("cook"), verifyPayment);
router.put("/:orderId/cook-cancel", protect, roleOnly("cook"), cookCancelOrder);
router.delete("/:orderId", protect, roleOnly("cook"), deleteOrder);

router.get("/:orderId", protect, getOrderById);

export default router;
