import cron from "node-cron";
import { autoCancelStaleBookings } from "../controllers/paymentController.js";

/**
 * Runs every 5 minutes: cancels eSewa payment bookings stuck in 'BOOKED'
 * for 15+ minutes (session timeout / user never completed payment), so
 * they don't block re-initiating payment on the same order forever.
 */
export const startEsewaBookingCleanupJob = () => {
    cron.schedule("*/5 * * * *", async () => {
        try {
            const count = await autoCancelStaleBookings();
            if (count > 0) {
                console.log(`🧹 eSewa cleanup: auto-cancelled ${count} stale booking(s).`);
            }
        } catch (err) {
            console.error("❌ eSewa cleanup job error:", err.message);
        }
    });
    console.log("✅ eSewa booking cleanup cron job scheduled (every 5 minutes)");
};
