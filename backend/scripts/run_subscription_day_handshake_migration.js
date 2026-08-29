// Migration runner for the per-day sent → received handshake.
//
// Mirrors database/migration_subscription_day_handshake.sql.
//
// Safe to re-run: MODIFY COLUMN restates the whole ENUM, so a second run sets
// the definition it already has. 'sent' is appended LAST on purpose — an ENUM is
// stored as the ordinal of its value, so putting it next to 'scheduled' would
// renumber every existing row.
//
// Usage: node scripts/run_subscription_day_handshake_migration.js
import db from "../config/db.js";

const statements = [
    `ALTER TABLE subscription_daily_log
        MODIFY COLUMN status
            ENUM('scheduled','customer_skipped','cook_unavailable','delivered','missed','sent')
            NOT NULL DEFAULT 'scheduled'`
];

async function run() {
    let failed = 0;
    for (const sql of statements) {
        const label = sql.slice(0, 70).replace(/\s+/g, " ") + "...";
        try {
            await db.promise().query(sql);
            console.log("OK:  ", label);
        } catch (err) {
            failed++;
            console.error("FAILED:", label, "\n ", err.code, err.message);
        }
    }

    // Read the definition back rather than trusting the ALTER's exit status: the
    // whole feature writes a value that does not exist until this runs, and a
    // silently-unchanged ENUM would surface later as a truncated-data error on a
    // cook pressing "Mark as sent".
    try {
        const [[col]] = await db.promise().query(
            `SELECT COLUMN_TYPE AS type
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'subscription_daily_log'
               AND COLUMN_NAME = 'status'`
        );
        console.log("\nstatus is now:", col ? col.type : "(column not found)");
        if (!col || !/'sent'/.test(col.type)) {
            console.error("❌ 'sent' is NOT in the ENUM — the handshake endpoints will fail.");
            process.exit(1);
        }
    } catch (err) {
        console.error("Could not verify the column:", err.message);
        process.exit(1);
    }

    if (failed > 0) process.exit(1);
    console.log("✅ Day handshake migration complete.");
    process.exit(0);
}

run();
