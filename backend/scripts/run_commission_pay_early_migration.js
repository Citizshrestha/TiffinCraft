// Migration runner for "pay commission early".
//
// Adds orders.commission_settlement_id — the marker that says "this order's
// commission has already been billed on settlement #N". Without it, a cook who
// settles the OPEN month early would have those same orders summed again at
// month close (double-billed), and orders delivered after the early payment
// would have no way to be told apart from the ones already paid for.
//
// The backfill stamps every delivered order that an EXISTING settlement already
// covers (same cook, same NPT month/year). This must run before the new
// generation path goes live: an unstamped historical order looks unbilled, and
// the carry-in rule in generateMonthlySettlements would re-bill it.
//
// Safe to re-run: guarded on information_schema, and the backfill only touches
// rows where commission_settlement_id IS NULL.
//
// Usage: node scripts/run_commission_pay_early_migration.js
import db from "../config/db.js";
import { toNpt } from "../utils/nepaliTime.js";

const conn = db.promise();

async function columnExists(table, column) {
    const [[row]] = await conn.query(
        `SELECT COUNT(*) AS n FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?`,
        [table, column]
    );
    return row.n > 0;
}

async function indexExists(table, index) {
    const [[row]] = await conn.query(
        `SELECT COUNT(*) AS n FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?`,
        [table, index]
    );
    return row.n > 0;
}

let failed = 0;

async function step(label, guard, sql) {
    try {
        if (await guard()) {
            console.log("SKIP:", label, "(already present)");
            return;
        }
        await conn.query(sql);
        console.log("OK:  ", label);
    } catch (err) {
        failed++;
        console.error("FAILED:", label, "\n ", err.code, err.message);
    }
}

async function run() {
    await step(
        "orders.commission_settlement_id",
        () => columnExists("orders", "commission_settlement_id"),
        "ALTER TABLE orders ADD COLUMN commission_settlement_id INT NULL DEFAULT NULL"
    );

    // Every new query filters on it, and the stamping UPDATE looks rows up by
    // (cook_id, commission_settlement_id IS NULL).
    await step(
        "orders.idx_orders_commission_settlement",
        () => indexExists("orders", "idx_orders_commission_settlement"),
        "ALTER TABLE orders ADD INDEX idx_orders_commission_settlement (commission_settlement_id)"
    );

    if (!(await columnExists("orders", "commission_settlement_id"))) {
        console.error("\n❌ commission_settlement_id is missing — cannot backfill.");
        process.exit(1);
    }

    // Backfill: link each delivered order to the settlement whose period it
    // falls in. NPT boundaries, same rule as every other commission query.
    try {
        const [res] = await conn.query(
            `UPDATE orders o
             JOIN commission_settlements s
               ON s.cook_id = o.cook_id
              AND s.month = MONTH(${toNpt("o.delivered_at")})
              AND s.year  = YEAR(${toNpt("o.delivered_at")})
             SET o.commission_settlement_id = s.id
             WHERE o.commission_settlement_id IS NULL
               AND o.status = 'delivered'
               AND o.commission_amount IS NOT NULL
               AND o.delivered_at IS NOT NULL`
        );
        console.log("OK:   backfill stamped", res.affectedRows, "already-billed order(s)");
    } catch (err) {
        failed++;
        console.error("FAILED: backfill\n ", err.code, err.message);
    }

    const [[unbilled]] = await conn.query(
        `SELECT COUNT(*) AS n, COALESCE(SUM(commission_amount), 0) AS amount
         FROM orders
         WHERE status = 'delivered' AND commission_amount IS NOT NULL
           AND (refund_status IS NULL OR refund_status != 'refunded')
           AND commission_settlement_id IS NULL`
    );

    console.log("\n--- verification ---");
    console.log("orders.commission_settlement_id:", await columnExists("orders", "commission_settlement_id") ? "present" : "MISSING");
    console.log("unbilled delivered orders:      ", unbilled.n, "(₹" + Number(unbilled.amount).toFixed(2) + ")");
    console.log("  ^ these are orders no settlement covers yet — the open month, plus");
    console.log("    any period whose generation never ran. Review before the next cycle.");

    if (failed > 0) process.exit(1);
    console.log("\n✅ Pay-early migration complete.");
    process.exit(0);
}

run();
