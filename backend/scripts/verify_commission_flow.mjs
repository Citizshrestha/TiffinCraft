/**
 * Commission snapshot end-to-end check — the money path, run against the real DB.
 *
 * Gate 5: a normal order marked delivered gets commission = ROUND(total * pct/100, 2),
 *         pct snapshotted, delivered_at set — and marking it again changes nothing.
 * Gate 6: a subscription day walked scheduled -> sent -> delivered promotes its
 *         linked order to 'delivered' AND snapshots commission (the hole Phase 2a
 *         closed), 'delivered' stays terminal, and a second attempt does not
 *         double-charge.
 *
 * Every row it creates is deleted again in the finally block, including on failure.
 * Run from backend/: node scripts/verify_commission_flow.mjs
 */
import assert from "node:assert/strict";
import db from "../config/db.js";
import { applyDeliveryCommission } from "../utils/commissionSnapshot.js";
import { applyDayStatus, getDayRow } from "../utils/subscriptionDailyLog.js";

const p = db.promise();
const created = { orders: [], subs: [], days: [] };

const pick = async (sql) => (await p.query(sql))[0][0];

try {
    const cook = await pick("SELECT id FROM users WHERE role='cook' LIMIT 1");
    const customer = await pick("SELECT id FROM users WHERE role='customer' LIMIT 1");
    const plan = await pick(`SELECT id FROM subscription_plans WHERE cook_id = ${cook.id} LIMIT 1`)
        || await pick("SELECT id FROM subscription_plans LIMIT 1");
    const settings = await pick("SELECT commission_pct FROM platform_settings WHERE id = 1");
    const pct = parseFloat(settings.commission_pct);
    console.log(`cook=${cook.id} customer=${customer.id} plan=${plan?.id} rate=${pct}%`);
    assert.equal(pct, 5, "platform rate should be 5% after the hardening migration");

    // ---------------- Gate 5: plain order ----------------
    const [o1] = await p.query(
        `INSERT INTO orders (customer_id, cook_id, total_amount, status, payment_method)
         VALUES (?, ?, 1000.00, 'delivered', 'cod')`,
        [customer.id, cook.id]
    );
    created.orders.push(o1.insertId);

    await applyDeliveryCommission(o1.insertId);
    let row = await pick(`SELECT commission_amount, commission_pct, delivered_at FROM orders WHERE id = ${o1.insertId}`);
    assert.equal(parseFloat(row.commission_amount), 50.00, "1000 @ 5% = 50");
    assert.equal(parseFloat(row.commission_pct), pct);
    assert.ok(row.delivered_at, "delivered_at must be stamped");
    const firstStamp = String(row.delivered_at);

    await applyDeliveryCommission(o1.insertId);   // idempotency
    row = await pick(`SELECT commission_amount, delivered_at FROM orders WHERE id = ${o1.insertId}`);
    assert.equal(parseFloat(row.commission_amount), 50.00, "re-marking must not re-charge");
    assert.equal(String(row.delivered_at), firstStamp, "delivered_at must not move");
    console.log("gate 5 OK — plain order snapshot + idempotent");

    // ---------------- Gate 6: subscription day ----------------
    const [s1] = await p.query(
        `INSERT INTO subscriptions (customer_id, cook_id, plan_id, start_date)
         VALUES (?, ?, ?, CURDATE())`,
        [customer.id, cook.id, plan.id]
    );
    created.subs.push(s1.insertId);

    const [o2] = await p.query(
        `INSERT INTO orders (customer_id, cook_id, total_amount, status, payment_method)
         VALUES (?, ?, 249.00, 'confirmed', 'cod')`,
        [customer.id, cook.id]
    );
    created.orders.push(o2.insertId);

    const day = "2026-08-20";
    created.days.push([s1.insertId, day]);

    let r = await applyDayStatus(p, { subscriptionId: s1.insertId, deliveryDate: day, status: "scheduled", orderId: o2.insertId });
    assert.ok(r.applied && r.created, "scheduled should create the day row");
    r = await applyDayStatus(p, { subscriptionId: s1.insertId, deliveryDate: day, status: "sent" });
    assert.ok(r.applied, "sent should apply");
    r = await applyDayStatus(p, { subscriptionId: s1.insertId, deliveryDate: day, status: "delivered" });
    assert.ok(r.applied, "delivered should apply");

    row = await pick(`SELECT status, commission_amount, commission_pct, delivered_at FROM orders WHERE id = ${o2.insertId}`);
    assert.equal(row.status, "delivered", "the linked order must be promoted to delivered");
    assert.equal(parseFloat(row.commission_amount), 12.45, "249 @ 5% = 12.45");
    assert.equal(parseFloat(row.commission_pct), pct);
    assert.ok(row.delivered_at, "delivered_at must be stamped on the subscription order too");
    console.log("gate 6 OK — subscription day charges commission");

    // Terminal + no double charge.
    r = await applyDayStatus(p, { subscriptionId: s1.insertId, deliveryDate: day, status: "cancelled" });
    assert.equal(r.blockedBy, "delivered", "'delivered' must be terminal");
    r = await applyDayStatus(p, { subscriptionId: s1.insertId, deliveryDate: day, status: "delivered" });
    assert.equal(r.blockedBy, "delivered", "re-delivering must be blocked, not re-charged");
    row = await pick(`SELECT commission_amount FROM orders WHERE id = ${o2.insertId}`);
    assert.equal(parseFloat(row.commission_amount), 12.45, "commission must not double");
    const dayRow = await getDayRow(p, s1.insertId, day);
    assert.equal(dayRow.status, "delivered");
    console.log("gate 6 OK — terminal, no double charge");

    console.log("\nALL CHECKS PASSED");
} catch (err) {
    console.error("FAILED:", err.message);
    process.exitCode = 1;
} finally {
    for (const [sub, day] of created.days) {
        await p.query("DELETE FROM subscription_daily_log WHERE subscription_id = ? AND delivery_date = ?", [sub, day]);
    }
    for (const id of created.subs) await p.query("DELETE FROM subscriptions WHERE id = ?", [id]);
    for (const id of created.orders) await p.query("DELETE FROM orders WHERE id = ?", [id]);
    console.log(`cleaned up: ${created.orders.length} order(s), ${created.subs.length} subscription(s), ${created.days.length} day row(s)`);
    process.exit(process.exitCode || 0);
}
