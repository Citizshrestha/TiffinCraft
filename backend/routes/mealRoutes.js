import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    addMeal,
    getMyMeals,
    getMealsByCook,
    getAllMeals,
    getMealById,
    updateMeal,
    deleteMeal,
    uploadMealImage as uploadMealImageController
} from "../controllers/mealController.js";

const router = Router();

router.post("/", protect, roleOnly("cook"), addMeal);
router.get("/my", protect, roleOnly("cook"), getMyMeals);
router.put("/:mealId", protect, roleOnly("cook"), updateMeal);
router.delete("/:mealId", protect, roleOnly("cook"), deleteMeal);

router.post(
    "/:mealId/image",
    protect,
    roleOnly("cook"),
    uploadMealImage,
    uploadMealImageController
);

router.get("/", getAllMeals);
router.get("/cook/:cookId", getMealsByCook);
router.get("/:mealId", getMealById);

export default router;