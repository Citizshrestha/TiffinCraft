/**
 * Gate 10 — the earnings screen and the commission bill must agree.
 *
 * Problem 6 was that earningsController bucketed by created_at in the DB
 * session's timezone while the commission ledger bucketed by NPT delivered_at,
 * so around a month boundary a cook saw one month's earnings next to another
 * month's bill. This seeds an order that lands on the WRONG side of the
 * boundary under the old basis (delivered 2026-07-31 18:30 UTC = 2026-08-01
 * 00:15 NPT) and asserts both queries now put it in the same NPT month with the
 * same commission figure.
 *
 * Self-cleaning: every seeded row is deleted in the finally block.
 * Run from backend/: node scripts/verify_earnings_agreement.mjs
 */
import assert from "node:assert/strict";
import db from "../config/db.js";
import { toNpt } from "../utils/nepaliTime.js";

const p = db.promise();
const orders = [];

// Same expressions the two controllers use, side by side.
const earnedAt = toNpt("COALESCE(delivered_at, created_at)");          // earningsController
const ledgerAt = toNpt("delivered_at");                                 // commissionController

try {
    const [[cook]] = await p.query("SELECT id FROM users WHERE role='cook' LIMIT 1");
    const [[customer]] = await p.query("SELECT id FROM users WHERE role='customer' LIMIT 1");
    const [[st]] = await p.query("SELECT commission_pct FROM platform_settings WHERE id = 1");
    const pct = parseFloat(st.commission_pct);

    // Two orders, both in NPT August 2026. The first is the boundary case: in
    // raw UTC it is still July, so a UTC-basis query would bill it to July.
    const seeds = [
        ["2026-07-31 18:30:00", 800.00],   // 2026-08-01 00:15 NPT
        ["2026-08-14 09:00:00", 1250.00],  // safely mid-month either way
    ];
    for (const [deliveredAt, total] of seeds) {
        const [r] = await p.query(
            `INSERT INTO orders (customer_id, cook_id, total_amount, status, payment_method,
                                 delivered_at, commission_pct, commission_amount)
             VALUES (?, ?, ?, 'delivered', 'cod', ?, ?, ROUND(? * ? / 100, 2))`,
            [customer.id, cook.id, total, deliveredAt, pct, total, pct]
        );
        orders.push(r.insertId);
    }
    const ids = orders.join(",");
    const expected = seeds.reduce((s, [, t]) => s + Math.round(t * pct) / 100, 0);

    // Earnings screen's monthly aggregate (earningsController.getCookEarningsSummary).
    const [[earn]] = await p.query(
        `SELECT COALESCE(SUM(total_amount), 0) AS gross,
                COALESCE(SUM(commission_amount), 0) AS commission,
                COUNT(*) AS n
         FROM orders
         WHERE id IN (${ids}) AND cook_id = ? AND status = 'delivered'
           AND (refund_status IS NULL OR refund_status <> 'refunded')
           AND MONTH(${earnedAt}) = 8 AND YEAR(${earnedAt}) = 2026`,
        [cook.id]
    );

    // Commission ledger's monthly aggregate (commissionController.getCommissionByCook).
    const [[bill]] = await p.query(
        `SELECT COALESCE(SUM(total_amount), 0) AS gross,
                COALESCE(SUM(commission_amount), 0) AS commission,
                COUNT(*) AS n
         FROM orders
         WHERE id IN (${ids}) AND cook_id = ? AND status = 'delivered'
           AND commission_amount IS NOT NULL
           AND (refund_status IS NULL OR refund_status != 'refunded')
           AND MONTH(${ledgerAt}) = 8 AND YEAR(${ledgerAt}) = 2026`,
        [cook.id]
    );

    console.log(`earnings : ${earn.n} order(s), gross ${earn.gross}, commission ${earn.commission}`);
    console.log(`bill     : ${bill.n} order(s), gross ${bill.gross}, commission ${bill.commission}`);

    assert.equal(Number(earn.n), 2, "both orders must fall in NPT August (boundary order included)");
    assert.equal(Number(bill.n), 2, "the ledger must see the same two orders");
    assert.equal(parseFloat(earn.commission), expected, `earnings commission should be ${expected}`);
    assert.equal(parseFloat(bill.commission), parseFloat(earn.commission), "the two screens must show the same commission");
    assert.equal(parseFloat(bill.gross), parseFloat(earn.gross), "the two screens must show the same gross");

    // The boundary order under the OLD basis, to prove the test is not vacuous.
    const [[old]] = await p.query(
        `SELECT COUNT(*) AS n FROM orders
         WHERE id IN (${ids}) AND MONTH(delivered_at) = 8 AND YEAR(delivered_at) = 2026`
    );
    assert.equal(Number(old.n), 1, "raw-UTC bucketing should have found only 1 — this is the bug being guarded");

    // EC1: a refunded order drops out of BOTH views, not just one.
    await p.query(`UPDATE orders SET refund_status = 'refunded' WHERE id = ${orders[0]}`);
    const [[eRef]] = await p.query(
        `SELECT COUNT(*) AS n FROM orders
         WHERE id IN (${ids}) AND status = 'delivered'
           AND (refund_status IS NULL OR refund_status <> 'refunded')
           AND MONTH(${earnedAt}) = 8 AND YEAR(${earnedAt}) = 2026`
    );
    const [[bRef]] = await p.query(
        `SELECT COUNT(*) AS n FROM orders
         WHERE id IN (${ids}) AND status = 'delivered' AND commission_amount IS NOT NULL
           AND (refund_status IS NULL OR refund_status != 'refunded')
           AND MONTH(${ledgerAt}) = 8 AND YEAR(${ledgerAt}) = 2026`
    );
    assert.equal(Number(eRef.n), 1, "EC1: refunded order must leave the earnings view");
    assert.equal(Number(bRef.n), 1, "EC1: refunded order must leave the commission view");

    console.log("\nALL CHECKS PASSED — earnings and commission agree, refunds excluded from both");
} catch (err) {
    console.error("FAILED:", err.message);
    process.exitCode = 1;
} finally {
    for (const id of orders) await p.query("DELETE FROM orders WHERE id = ?", [id]);
    console.log(`cleaned up: ${orders.length} order(s)`);
    process.exit(process.exitCode || 0);
}
