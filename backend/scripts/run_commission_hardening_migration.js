// Migration runner for the commission hardening pass.
//
// Mirrors database/migration_commission_hardening.sql. That file is written for
// a MySQL client that can execute PREPARE/EXECUTE blocks; this runner does the
// same guarded work through mysql2 instead, one statement at a time, so it can
// be run from Node on a managed DB with no shell client installed.
//
// Safe to re-run: every step is checked against information_schema first, so a
// second run reports "already present" and writes nothing.
//
// Usage: node scripts/run_commission_hardening_migration.js
import db from "../config/db.js";

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
    // 1. The column every commission period query already depends on.
    //    Deliberately NOT backfilled from updated_at — see the .sql file.
    await step(
        "orders.delivered_at",
        () => columnExists("orders", "delivered_at"),
        "ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP NULL DEFAULT NULL"
    );

    // 2. Covering index for the per-cook monthly rollup.
    await step(
        "orders.idx_orders_commission_period",
        () => indexExists("orders", "idx_orders_commission_period"),
        "ALTER TABLE orders ADD INDEX idx_orders_commission_period (cook_id, status, delivered_at)"
    );

    // 3. Screenshot dedupe — SHA-256 hex, hashed server-side from the real bytes.
    await step(
        "commission_settlements.payment_screenshot_hash",
        () => columnExists("commission_settlements", "payment_screenshot_hash"),
        `ALTER TABLE commission_settlements
            ADD COLUMN payment_screenshot_hash CHAR(64) NULL DEFAULT NULL AFTER payment_screenshot_url`
    );
    await step(
        "commission_settlements.uniq_commission_screenshot_hash",
        () => indexExists("commission_settlements", "uniq_commission_screenshot_hash"),
        `ALTER TABLE commission_settlements
            ADD UNIQUE KEY uniq_commission_screenshot_hash (payment_screenshot_hash)`
    );

    // 4. Reminder throttle so the daily due-date cron can't spam a cook.
    await step(
        "commission_settlements.last_reminder_at",
        () => columnExists("commission_settlements", "last_reminder_at"),
        "ALTER TABLE commission_settlements ADD COLUMN last_reminder_at TIMESTAMP NULL DEFAULT NULL"
    );

    // 5. Commission rate = 5%. The UPDATE is narrowed to rows still on the old
    //    4.00 default so a rate an admin deliberately chose is never clobbered.
    try {
        await conn.query(
            "ALTER TABLE platform_settings MODIFY COLUMN commission_pct DECIMAL(5,2) NOT NULL DEFAULT 5.00"
        );
        await conn.query("INSERT IGNORE INTO platform_settings (id, commission_pct) VALUES (1, 5.00)");
        const [res] = await conn.query(
            "UPDATE platform_settings SET commission_pct = 5.00 WHERE id = 1 AND commission_pct = 4.00"
        );
        console.log("OK:   platform_settings default 5.00" +
            (res.affectedRows > 0 ? " (live rate moved 4.00 → 5.00)" : " (live rate left as-is)"));
    } catch (err) {
        failed++;
        console.error("FAILED: platform_settings rate\n ", err.code, err.message);
    }

    // Read the result back rather than trusting exit statuses. A silently
    // missing delivered_at column is the failure mode that produces ₹0
    // commission with no error anywhere, which is the worst possible outcome.
    try {
        const [[rate]] = await conn.query("SELECT commission_pct FROM platform_settings WHERE id = 1");
        const okDelivered = await columnExists("orders", "delivered_at");
        const okHash = await columnExists("commission_settlements", "payment_screenshot_hash");
        const okReminder = await columnExists("commission_settlements", "last_reminder_at");

        console.log("\n--- verification ---");
        console.log("orders.delivered_at:                       ", okDelivered ? "present" : "MISSING");
        console.log("commission_settlements.screenshot hash:    ", okHash ? "present" : "MISSING");
        console.log("commission_settlements.last_reminder_at:   ", okReminder ? "present" : "MISSING");
        console.log("platform_settings.commission_pct:          ", rate ? rate.commission_pct : "(no row)");

        if (!okDelivered || !okHash || !okReminder) {
            console.error("\n❌ A required column is missing — commission will not work correctly.");
            process.exit(1);
        }
    } catch (err) {
        console.error("Could not verify:", err.message);
        process.exit(1);
    }

    if (failed > 0) process.exit(1);
    console.log("\n✅ Commission hardening migration complete.");
    process.exit(0);
}

run();
