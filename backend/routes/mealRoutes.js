import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    addMeal,
    getMealsByCook,
    getMyMeals,
    getAllMeals,
    getMealById,
    updateMeal,
    deleteMeal
} from "../controllers/mealController.js";

const router = Router();

// Public routes - customers browse meals
router.get("/", getAllMeals);
router.get("/cook/:cookId", getMealsByCook);
router.get("/:mealId", getMealById);

// Protected routes - cook manages their meals
router.post("/", protect, roleOnly("cook"), addMeal);
router.get("/my/list", protect, roleOnly("cook"), getMyMeals);
router.put("/:mealId", protect, roleOnly("cook"), updateMeal);
router.delete("/:mealId", protect, roleOnly("cook"), deleteMeal);

export default router;
