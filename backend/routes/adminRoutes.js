import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    getDashboard,
    getPendingCooks,
    approveCook,
    rejectCook,
    getAllUsers,
    createUser,
    updateUser,
    deactivateUser,
    deleteUser,
    getAllOrders,
    updateOrderStatus,
    deleteOrder,
    getAllCooks,
    createCook,
    updateCook,
    getAllMeals,
    createMeal,
    updateMeal,
    deleteMeal
} from "../controllers/adminController.js";

const router = Router();

router.use(protect, roleOnly("admin"));

router.get("/dashboard", getDashboard);
router.get("/cooks/pending", getPendingCooks);
router.get("/cooks", getAllCooks);
router.post("/cooks", createCook);
router.put("/cooks/:cookId/approve", approveCook);
router.put("/cooks/:cookId/reject", rejectCook);
router.put("/cooks/:userId", updateCook);
router.get("/users", getAllUsers);
router.post("/users", createUser);
router.put("/users/:userId", updateUser);
router.put("/users/:userId/deactivate", deactivateUser);
router.delete("/users/:userId", deleteUser);
router.get("/orders", getAllOrders);
router.put("/orders/:orderId/status", updateOrderStatus);
router.delete("/orders/:orderId", deleteOrder);
router.get("/meals", getAllMeals);
router.post("/meals", createMeal);
router.put("/meals/:mealId", updateMeal);
router.delete("/meals/:mealId", deleteMeal);

export default router;