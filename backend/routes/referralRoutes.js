import { Router } from "express";
import { protect } from "../middleware/authMiddleware.js";
import { getMyReferralInfo, applyReferralCode } from "../controllers/referralController.js";

const router = Router();

router.use(protect);

router.get("/my", getMyReferralInfo);
router.post("/apply", applyReferralCode);

export default router;
