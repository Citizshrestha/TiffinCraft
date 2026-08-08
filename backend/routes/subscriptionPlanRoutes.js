import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    createPlan,
    getMyPlans,
    getPlansByCook,
    getPlanById,
    updatePlan,
    deletePlan
} from "../controllers/subscriptionPlanController.js";

const router = Router();

router.post("/", protect, roleOnly("cook"), createPlan);
router.get("/my", protect, roleOnly("cook"), getMyPlans);
router.get("/cook/:cookId", getPlansByCook); // public — powers the cook's profile page
router.get("/:id", getPlanById); // public — single plan detail (edit prefill / deep link)
router.put("/:id", protect, roleOnly("cook"), updatePlan);
router.delete("/:id", protect, roleOnly("cook"), deletePlan);

export default router;
