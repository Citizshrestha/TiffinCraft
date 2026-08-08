import cron from "node-cron";
import { generateMonthlySettlements } from "../controllers/commissionController.js";

/**
 * Runs at 01:00 on the 1st of every month: generates commission_settlements
 * rows for the month that just closed (so cooks get a "commission due"
 * notification once the month's orders are final). Idempotent — safe if
 * the server restarts mid-run or this fires more than once for the same
 * period (see generateMonthlySettlements's INSERT IGNORE).
 *
 * For dev/QA, POST /api/commission/settlements/generate (admin-only) runs
 * the same logic on demand instead of waiting for a real month boundary.
 */
export const startCommissionSettlementJob = () => {
    cron.schedule("0 1 1 * *", async () => {
        try {
            const now = new Date();
            const prevMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
            const month = prevMonthDate.getMonth() + 1;
            const year = prevMonthDate.getFullYear();

            const created = await generateMonthlySettlements(month, year);
            console.log(`🧾 Commission settlements: generated ${created} new row(s) for ${month}/${year}.`);
        } catch (err) {
            console.error("❌ Commission settlement job error:", err.message);
        }
    });
    console.log("✅ Commission settlement cron job scheduled (01:00 on the 1st of each month)");
};
