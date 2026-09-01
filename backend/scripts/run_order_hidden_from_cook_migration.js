// One-off migration runner for orders.hidden_from_cook.
//
// Mirrors database/migration_add_order_hidden_from_cook.sql. A cook's order
// "delete" now sets this flag instead of DELETE-ing the row, because earnings
// are SUM(total_amount) live off the orders table — a hard delete was
// subtracting the order's amount from earnings too.
//
// Usage: node scripts/run_order_hidden_from_cook_migration.js
import db from "../config/db.js";

const sql = `ALTER TABLE orders ADD COLUMN hidden_from_cook BOOLEAN NOT NULL DEFAULT FALSE AFTER status`;

async function run() {
    try {
        await db.promise().query(sql);
        console.log("OK: added orders.hidden_from_cook");
    } catch (err) {
        if (err.code === "ER_DUP_FIELDNAME" || /Duplicate column/i.test(err.message)) {
            console.log("SKIP: orders.hidden_from_cook already exists");
        } else {
            console.error("FAILED:", err.code, err.message);
            process.exit(1);
        }
    }
    process.exit(0);
}

run();
