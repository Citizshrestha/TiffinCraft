import cron from "node-cron";
import db from "../config/db.js";
import { notifySubscriptionDeliverySkipped, notifySubscriptionDeliverySkippedToCook } from "../utils/notificationHelper.js";

/**
 * Processes every subscription whose next_delivery_date is today: auto-places
 * an order (or skips + notifies if a plan meal is currently unavailable), then
 * advances the date. Exported separately from the cron wiring below so it can
 * be invoked directly — by tests, or by an admin "run it now" trigger — same
 * reasoning as commissionSettlementJob.js's generateMonthlySettlements.
 */
export const processDueSubscriptions = async () => {
    console.log("🔁 Subscription job: checking for orders to auto-place...");
    try {
        const [subs] = await db.promise().query(
            `SELECT s.*, p.name as plan_name, p.duration, p.price_per_delivery
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.status = 'active'
             AND s.next_delivery_date = CURDATE()
             AND (s.skipped_dates IS NULL OR JSON_CONTAINS(s.skipped_dates, CAST(CURDATE() AS JSON)) = 0)`
        );

        for (const sub of subs) {
            const connection = await db.promise().getConnection();
            try {
                await connection.beginTransaction();

                const [allItems] = await connection.query(
                    `SELECT spi.meal_id, spi.quantity, m.price, m.is_available
                     FROM subscription_plan_items spi
                     JOIN meals m ON spi.meal_id = m.id
                     WHERE spi.plan_id = ?`,
                    [sub.plan_id]
                );

                if (allItems.length === 0) {
                    throw new Error(`plan ${sub.plan_id} has no items`);
                }

                // A meal the cook has 86'd shouldn't silently keep getting
                // delivered (or silently dropped from the order without the
                // customer's say-so) — skip this cycle entirely rather than
                // ship a partial order, and notify both sides. The date still
                // advances normally so the subscription just tries again next
                // cycle, same as any other delivery.
                const unavailable = allItems.filter(i => !i.is_available);
                if (unavailable.length > 0) {
                    const interval = sub.duration === 'weekly' ? 7 : 30;
                    await connection.query(
                        `UPDATE subscriptions SET next_delivery_date = DATE_ADD(next_delivery_date, INTERVAL ? DAY) WHERE id = ?`,
                        [interval, sub.id]
                    );
                    await connection.commit();

                    const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [sub.customer_id]);
                    await notifySubscriptionDeliverySkipped(sub.customer_id, sub.id, sub.plan_name);
                    await notifySubscriptionDeliverySkippedToCook(sub.cook_id, sub.id, sub.plan_name, customer?.full_name || "A subscriber");

                    console.log(`⏭️  Skipped delivery for subscription ${sub.id} (${sub.plan_name}) — unavailable meal(s) in plan.`);
                    continue;
                }

                const items = allItems;
                // Charge the plan's actual set price when the cook gave it one —
                // that's the number the customer saw and subscribed at. Only
                // falls back to summing item prices for plans that never had a
                // price set (auto-summed, pre-existing behavior). order_items
                // below still records each item's real menu price for kitchen
                // reference, same as buyCombo does for combo orders.
                const totalAmount = sub.price_per_delivery !== null
                    ? parseFloat(sub.price_per_delivery)
                    : items.reduce((sum, i) => sum + parseFloat(i.price) * i.quantity, 0);

                const [result] = await connection.query(
                    `INSERT INTO orders (customer_id, cook_id, total_amount, delivery_address, status, payment_method, payment_status, special_instructions)
                     VALUES (?, ?, ?, ?, 'pending', 'cod', 'pending', 'Auto-subscription order')`,
                    [sub.customer_id, sub.cook_id, totalAmount, sub.delivery_address]
                );
                const orderId = result.insertId;

                for (const item of items) {
                    await connection.query(
                        `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time) VALUES (?, ?, ?, ?)`,
                        [orderId, item.meal_id, item.quantity, item.price]
                    );
                }

                // Advance next_delivery_date
                const interval = sub.duration === 'weekly' ? 7 : 30;
                await connection.query(
                    `UPDATE subscriptions SET next_delivery_date = DATE_ADD(next_delivery_date, INTERVAL ? DAY) WHERE id = ?`,
                    [interval, sub.id]
                );

                await connection.commit();
                console.log(`✅ Auto-placed order ${orderId} for subscription ${sub.id} (${sub.plan_name})`);
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
};

/** Wires processDueSubscriptions into the actual daily 06:00 cron schedule. */
export const startSubscriptionJob = () => {
    cron.schedule("0 6 * * *", processDueSubscriptions);
    console.log("✅ Subscription cron job scheduled (daily at 06:00)");
};
