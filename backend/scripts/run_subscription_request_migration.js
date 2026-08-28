// One-off migration runner for the subscription request-to-active flow.
//
// Mirrors database/migration_subscription_request_flow.sql, one statement per
// array entry for the same reason as the other runners in this folder: a
// partial or repeated run stays safe, and each step reports individually
// instead of the whole file aborting on a change that was already applied.
//
// Usage: node scripts/run_subscription_request_migration.js
import db from "../config/db.js";

const statements = [
    // ── subscriptions: the request gate ──────────────────────────────────────
    `ALTER TABLE subscriptions
        MODIFY COLUMN status ENUM(
            'requested','accepted','rejected',
            'pending_payment','pending_verification','verified','scheduled',
            'active','paused','completed','cancelled'
        ) NOT NULL DEFAULT 'pending_payment'`,

    `ALTER TABLE subscriptions ADD COLUMN request_note VARCHAR(300) NULL
        COMMENT 'optional note the customer sent with the request'`,
    `ALTER TABLE subscriptions ADD COLUMN requested_at TIMESTAMP NULL
        COMMENT 'when the customer created the request'`,
    `ALTER TABLE subscriptions ADD COLUMN responded_at TIMESTAMP NULL
        COMMENT 'when the cook accepted or rejected the request'`,
    `ALTER TABLE subscriptions ADD COLUMN response_note VARCHAR(300) NULL
        COMMENT 'the cook''s reason for accepting/rejecting, shown to the customer'`,

    // Manual, trust-based verification. The hash is the ONLY mechanical
    // safeguard: it stops one image paying for two subscriptions. It cannot
    // tell a genuine screenshot from a forged one.
    `ALTER TABLE subscriptions ADD COLUMN payment_screenshot_hash CHAR(64) NULL
        COMMENT 'SHA-256 of the proof image; UNIQUE so one image cannot pay for two subscriptions'`,
    `ALTER TABLE subscriptions ADD COLUMN payment_submitted_at TIMESTAMP NULL`,
    `ALTER TABLE subscriptions ADD COLUMN payment_rejection_reason VARCHAR(300) NULL
        COMMENT 'why the cook rejected the proof; shown to the customer so they can resubmit'`,
    `ALTER TABLE subscriptions ADD COLUMN payment_proof_attempts INT NOT NULL DEFAULT 0
        COMMENT 'how many proofs have been submitted; a rising count is a dispute signal'`,

    `ALTER TABLE subscriptions ADD UNIQUE KEY uk_sub_screenshot_hash (payment_screenshot_hash)`,
    `ALTER TABLE subscriptions ADD INDEX idx_sub_cook_status (cook_id, status)`,

    // ── chat_messages: structured cards ─────────────────────────────────────
    `ALTER TABLE chat_messages
        MODIFY COLUMN message_type ENUM(
            'text','image','video','call_ended','call_declined','call_missed',
            'subscription_request','subscription_update','custom_meal_request'
        ) NOT NULL DEFAULT 'text'`,

    `ALTER TABLE chat_messages ADD COLUMN metadata JSON NULL
        COMMENT 'card payload for structured message_types; NULL for plain text/media'`,
    `ALTER TABLE chat_messages ADD COLUMN reference_id INT NULL
        COMMENT 'subscriptions.id or custom_meal_requests.id the card is about'`,
    `ALTER TABLE chat_messages ADD COLUMN reference_type VARCHAR(40) NULL
        COMMENT 'subscription | custom_meal_request'`,
    `ALTER TABLE chat_messages ADD INDEX idx_cm_reference (reference_type, reference_id)`,

    // ── custom_meal_requests ────────────────────────────────────────────────
    `CREATE TABLE IF NOT EXISTS custom_meal_requests (
        id INT AUTO_INCREMENT PRIMARY KEY,
        subscription_id INT NOT NULL,
        customer_id INT NOT NULL,
        cook_id INT NOT NULL,
        delivery_date DATE NOT NULL,
        meal_id INT NULL COMMENT 'the meal asked for instead of the plan default; NULL = free-text only',
        note VARCHAR(300) NULL,
        status ENUM('pending','accepted','declined','cancelled','expired') NOT NULL DEFAULT 'pending',
        responded_by INT NULL,
        responded_at TIMESTAMP NULL,
        response_note VARCHAR(300) NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uk_cmr_sub_date (subscription_id, delivery_date),
        KEY idx_cmr_cook_date (cook_id, delivery_date, status),
        CONSTRAINT fk_cmr_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE,
        CONSTRAINT fk_cmr_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_cmr_cook FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_cmr_meal FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
];

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
                failed++;
                console.error("FAILED:", label, "\n ", err.code, err.message);
            }
        }
    }

    if (failed > 0) {
        console.error(`\n❌ ${failed} statement(s) failed — schema is INCOMPLETE. Fix and re-run.`);
        process.exit(1);
    }
    console.log("\n✅ Subscription request-flow migration complete.");
    process.exit(0);
}

run();
