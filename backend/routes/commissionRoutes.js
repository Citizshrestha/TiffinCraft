import { Router } from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    getCommissionSettings,
    updateCommissionSettings,
    getCommissionSummary,
    updateAdminBankDetails,
    getAdminQr,
    generateSettlementsNow,
    listSettlements,
    verifySettlement,
    getMyCurrentSettlement,
    listMySettlements,
    uploadSettlementScreenshot,
    getCommissionRateHistoryEndpoint
} from "../controllers/commissionController.js";

const router = Router();

// Admin: rate configuration + reporting
router.get("/settings", protect, roleOnly("admin"), getCommissionSettings);
router.put("/settings", protect, roleOnly("admin"), updateCommissionSettings);
router.get("/summary", protect, roleOnly("admin"), getCommissionSummary);
router.get("/rate-history", protect, roleOnly("admin"), getCommissionRateHistoryEndpoint);

// Admin's own payment QR — admin manages it, cooks (and admin) can view it
router.put("/admin-qr", protect, roleOnly("admin"), updateAdminBankDetails);
router.get("/admin-qr", protect, roleOnly("admin", "cook"), getAdminQr);

// Admin: settlement queue
router.post("/settlements/generate", protect, roleOnly("admin"), generateSettlementsNow);
router.get("/settlements", protect, roleOnly("admin"), listSettlements);
router.put("/settlements/:id/verify", protect, roleOnly("admin"), verifySettlement);

// Cook: view what they owe, pay, upload proof
router.get("/settlements/current", protect, roleOnly("cook"), getMyCurrentSettlement);
router.get("/settlements/mine", protect, roleOnly("cook"), listMySettlements);
router.put("/settlements/:id/screenshot", protect, roleOnly("cook"), uploadSettlementScreenshot);

export default router;
