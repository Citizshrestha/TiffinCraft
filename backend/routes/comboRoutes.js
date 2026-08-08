import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    createCombo,
    getMyCombos,
    getCombosByCook,
    getComboById,
    updateCombo,
    deleteCombo,
    buyCombo
} from "../controllers/comboController.js";

const router = Router();

router.post("/", protect, roleOnly("cook"), createCombo);
router.get("/my", protect, roleOnly("cook"), getMyCombos);
router.get("/cook/:cookId", getCombosByCook); // public — powers the cook's profile page
router.get("/:id", getComboById); // public — single combo detail (edit prefill / deep link)
router.put("/:id", protect, roleOnly("cook"), updateCombo);
router.delete("/:id", protect, roleOnly("cook"), deleteCombo);
router.post("/:id/order", protect, roleOnly("customer"), buyCombo);

export default router;
