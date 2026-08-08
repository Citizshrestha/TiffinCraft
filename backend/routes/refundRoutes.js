import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { requestRefund, listRefunds, processRefund } from "../controllers/refundController.js";

const router = Router();

router.post("/request", protect, roleOnly("customer", "cook"), requestRefund);
router.get("/", protect, roleOnly("admin"), listRefunds);
router.put("/:id/process", protect, roleOnly("admin"), processRefund);

export default router;
