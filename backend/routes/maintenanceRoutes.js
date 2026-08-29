import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { normalizeEmails } from "../controllers/maintenanceController.js";

const router = Router();

// Admin-only maintenance endpoints
router.post("/normalize-emails", protect, roleOnly("admin"), normalizeEmails);

export default router;
