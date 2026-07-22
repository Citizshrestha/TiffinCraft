import cron from "node-cron";
import db from "../config/db.js";

/**
 * Daily cron job (06:00) that auto-places orders for active subscriptions
 * whose next_delivery_date is today, then advances the date.
 * Skipped dates are respected.
 */
export const startSubscriptionJob = () => {
    // Run daily at 06:00
    cron.schedule("0 6 * * *", async () => {
        console.log("🔁 Subscription job: checking for orders to auto-place...");
        try {
            const [subs] = await db.promise().query(
                `SELECT s.*, m.price, m.name as meal_name
                 FROM subscriptions s
                 JOIN meals m ON s.meal_id = m.id
                 WHERE s.status = 'active'
                 AND s.next_delivery_date = CURDATE()
                 AND (s.skipped_dates IS NULL OR JSON_CONTAINS(s.skipped_dates, CAST(CURDATE() AS JSON)) = 0)`
            );

            for (const sub of subs) {
                const connection = await db.promise().getConnection();
                try {
                    await connection.beginTransaction();

                    const [result] = await connection.query(
                        `INSERT INTO orders (customer_id, cook_id, total_amount, delivery_address, status, payment_method, payment_status, special_instructions)
                         VALUES (?, ?, ?, ?, 'pending', 'cod', 'pending', 'Auto-subscription order')`,
                        [sub.customer_id, sub.cook_id, sub.price, sub.delivery_address]
                    );
                    const orderId = result.insertId;

                    await connection.query(
                        `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time) VALUES (?, ?, 1, ?)`,
                        [orderId, sub.meal_id, sub.price]
                    );

                    // Advance next_delivery_date
                    const interval = sub.frequency === 'weekly' ? 7 : 30;
                    await connection.query(
                        `UPDATE subscriptions SET next_delivery_date = DATE_ADD(next_delivery_date, INTERVAL ? DAY) WHERE id = ?`,
                        [interval, sub.id]
                    );

                    await connection.commit();
                    console.log(`✅ Auto-placed order ${orderId} for subscription ${sub.id} (${sub.meal_name})`);
                } catch (err) {
                    await connection.rollback();
                    console.error(`❌ Failed to place order for subscription ${sub.id}:`, err.message);
                } finally {
                    connection.release();
                }
            }

            if (subs.length === 0) {
                console.log("ℹ️  No subscriptions due today.");
            }
        } catch (err) {
            console.error("❌ Subscription job error:", err.message);
        }
    });
    console.log("✅ Subscription cron job scheduled (daily at 06:00)");
};
