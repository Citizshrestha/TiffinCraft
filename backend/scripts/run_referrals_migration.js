// One-off migration runner for the Refer & Earn feature.
// Usage: node scripts/run_referrals_migration.js
import db from "../config/db.js";

const statements = [
    "ALTER TABLE users ADD COLUMN referral_code VARCHAR(12) NULL UNIQUE",
    "ALTER TABLE users ADD COLUMN referral_credits DECIMAL(10,2) NOT NULL DEFAULT 0",
    `CREATE TABLE IF NOT EXISTS referrals (
        id INT AUTO_INCREMENT PRIMARY KEY,
        referrer_id INT NOT NULL,
        referred_id INT NOT NULL UNIQUE,
        code_used VARCHAR(12) NOT NULL,
        referrer_reward DECIMAL(10,2) NOT NULL DEFAULT 100.00,
        referred_reward DECIMAL(10,2) NOT NULL DEFAULT 50.00,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (referrer_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (referred_id) REFERENCES users(id) ON DELETE CASCADE,
        INDEX idx_referrer (referrer_id)
    )`
];

async function run() {
    for (const sql of statements) {
        try {
            await db.promise().query(sql);
            console.log("OK:", sql.slice(0, 60).replace(/\s+/g, " ") + "...");
        } catch (err) {
            // Re-running is safe: duplicate column/key errors mean it's already applied.
            if (err.code === "ER_DUP_FIELDNAME" || err.code === "ER_DUP_KEYNAME") {
                console.log("SKIP (already applied):", sql.slice(0, 60).replace(/\s+/g, " ") + "...");
            } else {
                console.error("FAILED:", sql.slice(0, 60), "\n", err.message);
                process.exit(1);
            }
        }
    }
    console.log("Referrals migration complete.");
    process.exit(0);
}

run();
