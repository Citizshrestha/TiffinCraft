import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    addReview,
    getCookReviews,
    getMyReviews,
    getMyCookReviews,
    replyToReview
} from "../controllers/reviewController.js";

const router = Router();

router.post("/", protect, roleOnly("customer"), addReview);
router.get("/my", protect, roleOnly("customer"), getMyReviews);

router.get("/cook/my", protect, roleOnly("cook"), getMyCookReviews);
router.put("/:reviewId/reply", protect, roleOnly("cook"), replyToReview);

router.get("/cook/:cookId", getCookReviews);

export default router;