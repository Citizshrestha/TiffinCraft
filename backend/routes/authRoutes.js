import express from "express";
import {
	loginUser,
	registerUser,
	getCurrentUser
} from "../controllers/authController.js";

const router = express.Router();

router.post("/register", registerUser);
router.post("/login", loginUser);
router.get("/me", getCurrentUser);

export default router;
