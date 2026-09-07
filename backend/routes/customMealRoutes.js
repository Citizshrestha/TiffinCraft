import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    respondToCustomMealRequest,
    cancelCustomMealRequest
} from "../controllers/customMealController.js";


const router = Router();

router.put("/:requestId/respond", protect, roleOnly("cook"), respondToCustomMealRequest);
router.delete("/:requestId", protect, roleOnly("customer"), cancelCustomMealRequest);

export default router;
