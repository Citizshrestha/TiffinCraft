import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import { uploadSingle } from "../middleware/uploadMiddleware.js";
import {
    addMeal,
    getMyMeals,
    getMealsByCook,
    getAllMeals,
    getMealById,
    updateMeal,
    deleteMeal,
    uploadMealImage as uploadMealImageController,
    addMealToSubscription
} from "../controllers/mealController.js";

const router = Router();

router.post("/", protect, roleOnly("cook"), addMeal);
router.get("/my", protect, roleOnly("cook"), getMyMeals);
router.put("/:mealId", protect, roleOnly("cook"), updateMeal);
router.delete("/:mealId", protect, roleOnly("cook"), deleteMeal);
router.post("/:mealId/subscription", protect, roleOnly("cook"), addMealToSubscription);

router.post(
    "/:mealId/image",
    protect,
    roleOnly("cook"),
    uploadSingle("meal_image"),
    uploadMealImageController
);

router.get("/", getAllMeals);
router.get("/cook/:cookId", getMealsByCook);
router.get("/:mealId", getMealById);

export default router;