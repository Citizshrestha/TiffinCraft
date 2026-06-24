import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    addReview,
    getCookReviews,
    getMyReviews
} from "../controllers/reviewController.js";

const router = Router();

// Customer only
router.post("/", protect, roleOnly("customer"), addReview);
router.get("/my", protect, roleOnly("customer"), getMyReviews);

// Public
router.get("/cook/:cookId", getCookReviews);

export default router;