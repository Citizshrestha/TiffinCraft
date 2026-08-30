// One-off migration runner for the order payment-rejection columns.
//
// Mirrors database/migration_order_payment_rejection.sql, one statement per
// array entry for the same reason as the other runners in this folder: a
// partial or repeated run stays safe, and each step reports individually
// instead of the whole file aborting on a change that was already applied.
//
// Usage: node scripts/run_order_payment_rejection_migration.js
import db from "../config/db.js";

const statements = [
    `ALTER TABLE orders ADD COLUMN payment_rejection_reason VARCHAR(500) NULL
        COMMENT 'why the cook rejected the proof; shown to the customer so they can resubmit'
        AFTER payment_verified_at`,

    `ALTER TABLE orders ADD COLUMN payment_rejected_at TIMESTAMP NULL
        AFTER payment_rejection_reason`,

    `ALTER TABLE orders ADD COLUMN payment_proof_attempts INT NOT NULL DEFAULT 0
        COMMENT 'how many proofs have been submitted; a rising count is a dispute signal'
        AFTER payment_rejected_at`,

    // Backfill. An online order that already carries a screenshot has had
    // exactly one proof submitted; leaving it at 0 would make the cook's
    // "Attempt N" warning read wrong the first time they reject one.
    `UPDATE orders
        SET payment_proof_attempts = 1
      WHERE payment_proof_attempts = 0
        AND payment_screenshot_url IS NOT NULL
        AND payment_screenshot_url <> ''`
];

const ALREADY_APPLIED = new Set([
    "ER_DUP_FIELDNAME",
    "ER_DUP_KEYNAME"
]);

async function run() {
    let failed = 0;
    for (const sql of statements) {
        const label = sql.slice(0, 70).replace(/\s+/g, " ") + "...";
        try {
            await db.promise().query(sql);
            console.log("OK:  ", label);
        } catch (err) {
            if (ALREADY_APPLIED.has(err.code)
                || /Duplicate (column|key)/i.test(err.message)
                || /already exists/i.test(err.message)) {
                console.log("SKIP:", label, `(${err.code})`);
            } else {
                failed++;
                console.error("FAILED:", label, "\n ", err.code, err.message);
            }
        }
    }

    if (failed > 0) {
        console.error(`\n❌ ${failed} statement(s) failed — schema is INCOMPLETE. Fix and re-run.`);
        process.exit(1);
    }
    console.log("\n✅ Order payment-rejection migration complete.");
    process.exit(0);
}

run();
