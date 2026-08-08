import { Router } from "express";
import rateLimit from "express-rate-limit";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    initiateEsewaPayment,
    esewaCallback,
    getEsewaPaymentStatus,
    cancelEsewaPaymentBooking
} from "../controllers/paymentController.js";

const router = Router();

// Blunts booking-spam (each call hits eSewa's live API) without blocking a
// customer who's legitimately retrying after a failed/cancelled attempt.
const initiateLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message: "Too many payment attempts, please try again later." }
});

router.post("/esewa/initiate", protect, roleOnly("customer"), initiateLimiter, initiateEsewaPayment);

// Called by eSewa's servers — no JWT, authenticity comes from the
// HMAC signature verified inside the controller.
router.post("/esewa/callback", esewaCallback);

router.get("/esewa/status/:transactionUuid", protect, getEsewaPaymentStatus);
router.post("/esewa/cancel", protect, roleOnly("customer"), cancelEsewaPaymentBooking);

export default router;
