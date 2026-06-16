import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    setupCookProfile,
    getMyCookProfile,
    updateCookProfile,
    getAllCooks,
    getCookById
} from "../controllers/cookController.js";

const router = Router();

// Cook sets up their profile after registering (protected, cook only)
router.post("/profile", protect, roleOnly("cook"), setupCookProfile);

// Cook views their own profile
router.get("/profile", protect, roleOnly("cook"), getMyCookProfile);

// Cook updates their profile
router.put("/profile", protect, roleOnly("cook"), updateCookProfile);

// Public routes - customers browse cooks
router.get("/", getAllCooks);
router.get("/:cookId", getCookById);

export default router;
