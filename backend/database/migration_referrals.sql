-- Refer & Earn: referral codes, credits, and referral tracking
-- Run once: node scripts/run_referrals_migration.js

ALTER TABLE users ADD COLUMN referral_code VARCHAR(12) NULL UNIQUE;
ALTER TABLE users ADD COLUMN referral_credits DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS referrals (
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
);
