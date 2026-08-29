import cron from "node-cron";
import { generateMonthlySettlements, sendCommissionDueReminders } from "../controllers/commissionController.js";
import { getNptPreviousMonthYear } from "../utils/nepaliTime.js";

/**
 * Two crons, both about the same thing — making sure a cook always knows what
 * they owe and by when.
 *
 * 1. Monthly generation, 01:00 on the 1st: creates commission_settlements rows
 *    for the month that just closed and notifies each cook. Idempotent — safe
 *    if the server restarts mid-run or this fires twice for the same period
 *    (see generateMonthlySettlements's INSERT IGNORE).
 *
 * 2. Daily reminders, 09:00: nudges cooks 3 days before the due date, on the
 *    due date, then weekly once overdue. Throttled in the DB via
 *    last_reminder_at, so a restart cannot re-send.
 *
 * Both months are computed in Nepal time, not server-local time. This was a
 * real bug: `new Date().getMonth() - 1` on a UTC-clock host (Render, most
 * managed hosts) still reads the previous calendar day for the first 5h45m of
 * every NPT day, so a 01:00-NPT run on the 1st billed the month BEFORE the one
 * that had just closed. Every commission query buckets by NPT via CONVERT_TZ;
 * the cron now agrees with them. See utils/nepaliTime.js.
 *
 * For dev/QA, POST /api/commission/settlements/generate (admin-only) runs the
 * generation on demand instead of waiting for a real month boundary.
 *
 * Note (DEPLOYMENT.md): crons only fire while the process is alive, so on a
 * free-tier host that sleeps, the 1st-of-month run can be missed entirely —
 * the manual generate endpoint is the recovery path, and it is idempotent.
 */
export const startCommissionSettlementJob = () => {
    cron.schedule("0 1 1 * *", async () => {
        try {
            const { month, year } = getNptPreviousMonthYear();
            const created = await generateMonthlySettlements(month, year);
            console.log(`🧾 Commission settlements: generated ${created} new row(s) for ${month}/${year}.`);
        } catch (err) {
            console.error("❌ Commission settlement job error:", err.message);
        }
    });

    // 09:00 rather than the small hours: a payment reminder is only useful at a
    // time the cook can actually act on it, and a push at 01:00 gets swiped away.
    cron.schedule("0 9 * * *", async () => {
        try {
            const { candidates, sent } = await sendCommissionDueReminders();
            if (sent > 0 || candidates > 0) {
                console.log(`🔔 Commission reminders: ${sent} sent (${candidates} unpaid settlement(s) checked).`);
            }
        } catch (err) {
            console.error("❌ Commission reminder job error:", err.message);
        }
    });

    console.log("✅ Commission crons scheduled (settlements 01:00 on the 1st, reminders daily 09:00)");
};
