import { Router } from "express";
import rateLimit from "express-rate-limit";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { initiateEpayPayment, handleEpayReturn, serveEpayCheckoutForm } from "../controllers/esewaEpayController.js";

const router = Router();

const initiateLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message: "Too many payment attempts, please try again later." }
});

router.post("/esewa-epay/initiate", protect, roleOnly("customer"), initiateLimiter, initiateEpayPayment);


router.get("/esewa-epay/checkout/:transactionUuid", serveEpayCheckoutForm);


router.get("/esewa-epay/return", handleEpayReturn);

export default router;
