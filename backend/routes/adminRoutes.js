import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    getDashboard,
    getPendingCooks,
    approveCook,
    rejectCook,
    getAllUsers,
    deactivateUser,
    getAllOrders
} from "../controllers/adminController.js";

const router = Router();

router.use(protect, roleOnly("admin"));

router.get("/dashboard", getDashboard);
router.get("/cooks/pending", getPendingCooks);
router.put("/cooks/:cookId/approve", approveCook);
router.put("/cooks/:cookId/reject", rejectCook);
router.get("/users", getAllUsers);
router.put("/users/:userId/deactivate", deactivateUser);
router.get("/orders", getAllOrders);

export default router;