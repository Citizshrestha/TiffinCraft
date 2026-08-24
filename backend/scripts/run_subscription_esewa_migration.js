// One-off migration runner for pay-first (eSewa) subscriptions.
// Mirrors database/migration_subscription_esewa_payment.sql, but per-statement so
// a partial or repeated run is safe.
// Usage: node scripts/run_subscription_esewa_migration.js
import db from "../config/db.js";

const statements = [
    "ALTER TABLE payments MODIFY COLUMN order_id INT NULL",
    "ALTER TABLE payments ADD COLUMN subscription_id INT NULL AFTER order_id",
    `ALTER TABLE payments
        ADD CONSTRAINT fk_payments_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE`,
    "ALTER TABLE payments ADD INDEX idx_payments_subscription (subscription_id)",

    "ALTER TABLE subscriptions ADD COLUMN end_date DATE NULL AFTER start_date",
    `ALTER TABLE subscriptions
        MODIFY COLUMN status ENUM('pending_payment','active','paused','cancelled','completed')
        NOT NULL DEFAULT 'pending_payment'`,
    `ALTER TABLE subscriptions
        MODIFY COLUMN payment_status ENUM('pending','submitted','verified','rejected','failed')
        NOT NULL DEFAULT 'pending'`,
    `ALTER TABLE subscriptions
        ADD COLUMN payment_method ENUM('manual_qr','esewa') NOT NULL DEFAULT 'manual_qr' AFTER payment_status`,

    `CREATE TABLE IF NOT EXISTS subscription_payment_events (
        id INT PRIMARY KEY AUTO_INCREMENT,
        subscription_id INT NOT NULL,
        payment_id INT NULL,
        transaction_uuid VARCHAR(100) NULL,
        event VARCHAR(40) NOT NULL COMMENT 'initiated | reused_pending | payment_success | payment_failed | payment_canceled | payment_reverted | activated | blocked_duplicate | amount_mismatch',
        amount DECIMAL(10,2) NULL,
        detail VARCHAR(500) NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_spe_subscription (subscription_id, created_at),
        INDEX idx_spe_txn (transaction_uuid),
        CONSTRAINT fk_spe_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`
];

// Re-running is safe: these all mean "already applied".
const ALREADY_APPLIED = new Set([
    "ER_DUP_FIELDNAME",
    "ER_DUP_KEYNAME",
    "ER_FK_DUP_NAME",
    "ER_CANT_CREATE_TABLE"
]);

async function run() {
    for (const sql of statements) {
        const label = sql.slice(0, 70).replace(/\s+/g, " ") + "...";
        try {
            await db.promise().query(sql);
            console.log("OK:  ", label);
        } catch (err) {
            if (ALREADY_APPLIED.has(err.code) || /Duplicate (column|key|foreign key)/i.test(err.message)) {
                console.log("SKIP:", label, `(${err.code})`);
            } else {
                console.error("FAILED:", label, "\n ", err.code, err.message);
                process.exit(1);
            }
        }
    }
    console.log("\nSubscription eSewa payment migration complete.");
    process.exit(0);
}

run();
