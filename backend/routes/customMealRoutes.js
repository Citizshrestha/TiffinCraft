import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    respondToCustomMealRequest,
    cancelCustomMealRequest
} from "../controllers/customMealController.js";

/**
 * Custom meal requests, addressed by request id.
 *
 * Creating and listing live under /api/subscriptions/:id/... because they are
 * scoped to one subscription; answering and withdrawing live here because the
 * cook taps them from a chat card that only knows the request id.
 */
const router = Router();

router.put("/:requestId/respond", protect, roleOnly("cook"), respondToCustomMealRequest);
router.delete("/:requestId", protect, roleOnly("customer"), cancelCustomMealRequest);

export default router;
