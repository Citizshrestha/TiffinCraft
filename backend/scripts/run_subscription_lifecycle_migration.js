// One-off migration runner for the subscription lifecycle redesign.
//
// Mirrors, in order:
//   database/migration_subscription_lifecycle.sql
//   database/migration_subscription_daily_log.sql
//   database/migration_cook_daily_availability.sql
//   database/migration_delivery_cutoff_hour.sql
//
// Per-statement rather than piping the .sql files through a client, for the same
// reason as run_subscription_esewa_migration.js: a partial or repeated run stays
// safe, and each step reports individually instead of the whole file aborting on
// a change that was already applied.
//
// Usage: node scripts/run_subscription_lifecycle_migration.js
import db from "../config/db.js";

const statements = [
    // ── subscriptions: new lifecycle states ──────────────────────────────────
    `ALTER TABLE subscriptions
        MODIFY COLUMN status ENUM(
            'pending_payment','pending_verification','verified','scheduled',
            'active','paused','completed','cancelled'
        ) NOT NULL DEFAULT 'pending_payment'`,

    // ── subscriptions: who verified, and when ────────────────────────────────
    `ALTER TABLE subscriptions ADD COLUMN verified_by INT NULL
        COMMENT 'users.id of the cook (or admin) who verified payment; NULL for eSewa auto-verification'`,
    `ALTER TABLE subscriptions ADD COLUMN verified_at TIMESTAMP NULL
        COMMENT 'when the payment was verified (UTC)'`,
    `ALTER TABLE subscriptions
        ADD CONSTRAINT fk_subscriptions_verified_by
        FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL`,

    // ── subscriptions: meal credits ──────────────────────────────────────────
    `ALTER TABLE subscriptions ADD COLUMN meals_total INT NULL
        COMMENT 'paid deliveries in this cycle; NULL = legacy row, bounded by end_date only'`,
    `ALTER TABLE subscriptions ADD COLUMN meals_remaining INT NULL
        COMMENT 'decremented only when a delivery day is actually charged'`,

    "ALTER TABLE subscriptions ADD INDEX idx_sub_status_start (status, start_date)",

    // A completed/cancelled subscription has no next delivery, and neither does
    // one whose remaining days are all skipped. See the migration file for why
    // NOT NULL here was actively harmful.
    `ALTER TABLE subscriptions MODIFY COLUMN next_delivery_date DATE NULL
        COMMENT 'next date a meal is expected; NULL once completed/cancelled or no days remain'`,

    // ── the per-day log ──────────────────────────────────────────────────────
    `CREATE TABLE IF NOT EXISTS subscription_daily_log (
        id INT PRIMARY KEY AUTO_INCREMENT,
        subscription_id INT NOT NULL,
        delivery_date DATE NOT NULL COMMENT 'the NPT calendar date this row is about, never a timestamp',
        status ENUM('scheduled','customer_skipped','cook_unavailable','delivered','missed')
            NOT NULL DEFAULT 'scheduled',
        toggled_by ENUM('customer','cook','system') NULL COMMENT 'actor for dispute resolution; system = cron',
        reason TEXT NULL,
        credit_deducted BOOLEAN NOT NULL DEFAULT FALSE
            COMMENT 'TRUE only when this day consumed one of the paid meals',
        order_id INT NULL COMMENT 'the order placed for this day, if any',
        toggled_at TIMESTAMP NULL COMMENT 'when the status last changed (UTC)',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uk_sdl_sub_date (subscription_id, delivery_date),
        INDEX idx_sdl_date_status (delivery_date, status),
        CONSTRAINT fk_sdl_subscription FOREIGN KEY (subscription_id)
            REFERENCES subscriptions(id) ON DELETE CASCADE,
        CONSTRAINT fk_sdl_order FOREIGN KEY (order_id)
            REFERENCES orders(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`,

    // ── cook closures ────────────────────────────────────────────────────────
    `CREATE TABLE IF NOT EXISTS cook_daily_availability (
        id INT PRIMARY KEY AUTO_INCREMENT,
        cook_id INT NOT NULL COMMENT 'users.id of the cook — same key space as subscriptions.cook_id',
        unavailable_date DATE NOT NULL COMMENT 'NPT calendar date the cook is closed',
        reason TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uk_cda_cook_date (cook_id, unavailable_date),
        INDEX idx_cda_date (unavailable_date),
        CONSTRAINT fk_cda_cook FOREIGN KEY (cook_id)
            REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`,

    // ── configurable cutoff ──────────────────────────────────────────────────
    `ALTER TABLE platform_settings ADD COLUMN delivery_cutoff_hour TINYINT UNSIGNED NOT NULL DEFAULT 20
        COMMENT 'hour-of-day in Nepal Time (0-23) after which the NEXT day''s deliveries are locked; 20 = 8pm NPT'`,
    `ALTER TABLE platform_settings
        ADD CONSTRAINT chk_delivery_cutoff_hour CHECK (delivery_cutoff_hour BETWEEN 0 AND 23)`,
    "INSERT IGNORE INTO platform_settings (id, commission_pct, delivery_cutoff_hour) VALUES (1, 4.00, 20)"
];

// Re-running is safe: these all mean "already applied".
const ALREADY_APPLIED = new Set([
    "ER_DUP_FIELDNAME",
    "ER_DUP_KEYNAME",
    "ER_FK_DUP_NAME",
    "ER_CANT_CREATE_TABLE",
    "ER_CHECK_CONSTRAINT_DUP_NAME",
    "ER_CONSTRAINT_EXISTS"
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
                || /Duplicate (column|key|foreign key|check constraint)/i.test(err.message)
                || /already exists/i.test(err.message)) {
                console.log("SKIP:", label, `(${err.code})`);
            } else {
                // Kept going rather than exiting on the first failure: these
                // statements are independent, and knowing that 2 of 14 failed is
                // far more useful than stopping at the first and re-running to
                // discover the rest one at a time.
                failed++;
                console.error("FAILED:", label, "\n ", err.code, err.message);
            }
        }
    }

    if (failed > 0) {
        console.error(`\n❌ ${failed} statement(s) failed — schema is INCOMPLETE. Fix and re-run.`);
        process.exit(1);
    }
    console.log("\n✅ Subscription lifecycle migration complete.");
    process.exit(0);
}

run();
