/**
 * settle-now top-up check — the money path, run against the real DB.
 *
 * A cook who pays this month early and then delivers more orders has an
 * already-verified bill for the period, and the unique (cook, month, year) key
 * means the new commission cannot get a bill of its own. settle-now must top the
 * existing bill up instead of 409-ing, without ever un-banking what was paid.
 *
 * Every row it creates is deleted again in the finally block, including on failure.
 * Run from backend/: node scripts/verify_commission_topup.mjs
 */
import assert from "node:assert/strict";
import db from "../config/db.js";
import { settleAccruedNow } from "../controllers/commissionController.js";

const p = db.promise();
const created = { orders: [], settlements: [], users: [] };

const pick = async (sql, args = []) => (await p.query(sql, args))[0][0];

// Minimal express doubles — the controller only ever touches these.
const call = async (cookId) => {
    let captured = {};
    const res = {
        status(code) { captured.code = code; return this; },
        json(body) { captured.body = body; return this; },
    };
    await settleAccruedNow({ user: { id: cookId } }, res);
    return captured;
};

const deliverOrder = async (cookId, customerId, amount, commission) => {
    const [o] = await p.query(
        `INSERT INTO orders (customer_id, cook_id, total_amount, status, payment_method,
                             commission_amount, commission_pct, delivered_at)
         VALUES (?, ?, ?, 'delivered', 'cod', ?, 5.00, NOW())`,
        [customerId, cookId, amount, commission]
    );
    created.orders.push(o.insertId);
    return o.insertId;
};

try {
    const customer = await pick("SELECT id FROM users WHERE role='customer' LIMIT 1");
    // Runs on a throwaway cook, not a real one: the whole point of this path is
    // that it mutates an existing bill, and doing that to live money to prove it
    // works is exactly the kind of test that costs somebody a rupee.
    const [tmp] = await p.query(
        `INSERT INTO users (full_name, email, role, is_active)
         VALUES ('topup check cook', ?, 'cook', TRUE)`,
        [`topup-check-${Date.now()}@example.invalid`]
    );
    created.users.push(tmp.insertId);
    const cook = { id: tmp.insertId };

    // ---- first bill: ₹45 of accrual, paid early ----
    await deliverOrder(cook.id, customer.id, 900.0, 45.0);
    let r = await call(cook.id);
    assert.equal(r.code, 201, `expected a bill to be created, got ${r.code}: ${r.body?.message}`);
    const id = r.body.settlement.id;
    created.settlements.push(id);
    assert.equal(Number(r.body.settlement.amount_due), 45);
    let stamped = await pick("SELECT COUNT(*) AS n FROM orders WHERE commission_settlement_id = ?", [id]);
    assert.equal(stamped.n, 1, "the billed order must be stamped so the cron cannot bill it twice");

    // admin verifies it in full
    await p.query(
        `UPDATE commission_settlements
         SET status='verified', amount_paid=amount_due, verified_at=NOW(),
             payment_screenshot_url='https://example.test/proof.png'
         WHERE id = ?`, [id]);

    // nothing new delivered yet -> still nothing to pay
    r = await call(cook.id);
    assert.equal(r.code, 409, "a settled month with no new accrual must not reopen");

    // ---- more deliveries after the early payment ----
    await deliverOrder(cook.id, customer.id, 1300.0, 65.0);
    r = await call(cook.id);
    assert.equal(r.code, 200, `expected a top-up, got ${r.code}: ${r.body?.message}`);

    const row = await pick("SELECT * FROM commission_settlements WHERE id = ?", [id]);
    assert.equal(Number(row.amount_due), 110, "amount_due must grow by the new accrual");
    assert.equal(Number(row.amount_paid), 45, "the verified ₹45 must stay banked");
    assert.equal(Number(row.amount_due) - Number(row.amount_paid), 65, "remaining must be exactly the new accrual");
    assert.equal(row.status, "pending", "a topped-up bill is payable again");
    assert.equal(row.order_count, 2);
    assert.equal(row.payment_screenshot_url, null, "the already-verified proof must not be re-verifiable");
    stamped = await pick("SELECT COUNT(*) AS n FROM orders WHERE commission_settlement_id = ?", [id]);
    assert.equal(stamped.n, 2, "the new order must be stamped too, or the cron bills it again");

    const unbilled = await pick(
        `SELECT COUNT(*) AS n FROM orders WHERE cook_id = ? AND commission_settlement_id IS NULL
           AND status='delivered' AND id IN (?)`, [cook.id, created.orders]);
    assert.equal(unbilled.n, 0, "nothing may be left unbilled after a top-up");

    console.log("PASS — settle-now tops up a verified bill: 45 paid, 65 remaining, both orders stamped.");
} finally {
    if (created.settlements.length) {
        await p.query("DELETE FROM notifications WHERE related_id IN (?) AND type LIKE 'commission%'", [created.settlements]);
    }
    if (created.orders.length) await p.query("DELETE FROM orders WHERE id IN (?)", [created.orders]);
    if (created.settlements.length) await p.query("DELETE FROM commission_settlements WHERE id IN (?)", [created.settlements]);
    if (created.users.length) {
        await p.query("DELETE FROM notifications WHERE user_id IN (?)", [created.users]);
        await p.query("DELETE FROM users WHERE id IN (?)", [created.users]);
    }
    db.end();
}
