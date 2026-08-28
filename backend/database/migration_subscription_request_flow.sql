-- ===========================================================================
-- Subscription request-to-active flow
-- ===========================================================================
-- Inverts the previous pay-first ordering. A subscription now begins as a
-- REQUEST the cook must accept before any money is asked for:
--
--   requested -> accepted -> pending_verification -> verified/scheduled
--             -> active -> completed
--   requested -> rejected                       (terminal, closed cleanly)
--
-- Applied by scripts/run_subscription_request_migration.js, which runs each
-- statement independently so a repeated run is safe.
-- ===========================================================================

-- ── subscriptions: the request gate ────────────────────────────────────────
-- 'requested' and 'accepted' sit BEFORE 'pending_verification'. 'rejected' is
-- terminal and distinct from 'cancelled' so a cook's "no thanks" is never
-- confused with a customer walking away — the two have different meanings in a
-- dispute, and different next steps for the customer.
ALTER TABLE subscriptions
    MODIFY COLUMN status ENUM(
        'requested','accepted','rejected',
        'pending_payment','pending_verification','verified','scheduled',
        'active','paused','completed','cancelled'
    ) NOT NULL DEFAULT 'pending_payment';

ALTER TABLE subscriptions ADD COLUMN request_note VARCHAR(300) NULL
    COMMENT 'optional note the customer sent with the request';
ALTER TABLE subscriptions ADD COLUMN requested_at TIMESTAMP NULL
    COMMENT 'when the customer created the request';
ALTER TABLE subscriptions ADD COLUMN responded_at TIMESTAMP NULL
    COMMENT 'when the cook accepted or rejected the request';
ALTER TABLE subscriptions ADD COLUMN response_note VARCHAR(300) NULL
    COMMENT 'the cook''s reason for accepting/rejecting, shown to the customer';

-- Manual, trust-based payment verification. The hash is the ONLY mechanical
-- safeguard in that step: it stops the same image being re-used across two
-- subscriptions. It cannot tell a genuine screenshot from a forged one.
ALTER TABLE subscriptions ADD COLUMN payment_screenshot_hash CHAR(64) NULL
    COMMENT 'SHA-256 of the uploaded proof image; UNIQUE so one image cannot pay for two subscriptions';
ALTER TABLE subscriptions ADD COLUMN payment_submitted_at TIMESTAMP NULL;
ALTER TABLE subscriptions ADD COLUMN payment_rejection_reason VARCHAR(300) NULL
    COMMENT 'why the cook rejected the proof; shown to the customer so they can resubmit';
ALTER TABLE subscriptions ADD COLUMN payment_proof_attempts INT NOT NULL DEFAULT 0
    COMMENT 'how many proofs have been submitted; a rising count is a dispute signal';

ALTER TABLE subscriptions
    ADD UNIQUE KEY uk_sub_screenshot_hash (payment_screenshot_hash);
ALTER TABLE subscriptions ADD INDEX idx_sub_cook_status (cook_id, status);

-- ── chat_messages: structured cards ───────────────────────────────────────
-- A request/custom-meal card IS a chat message, not a parallel channel, so it
-- lands in the thread the two people already use. metadata holds the snapshot
-- rendered inside the bubble; reference_id/_type point at the live row so the
-- card can show CURRENT status rather than a frozen one.
ALTER TABLE chat_messages
    MODIFY COLUMN message_type ENUM(
        'text','image','video','call_ended','call_declined','call_missed',
        'subscription_request','subscription_update','custom_meal_request'
    ) NOT NULL DEFAULT 'text';

ALTER TABLE chat_messages ADD COLUMN metadata JSON NULL
    COMMENT 'card payload for structured message_types; NULL for plain text/media';
ALTER TABLE chat_messages ADD COLUMN reference_id INT NULL
    COMMENT 'subscriptions.id or custom_meal_requests.id the card is about';
ALTER TABLE chat_messages ADD COLUMN reference_type VARCHAR(40) NULL
    COMMENT 'subscription | custom_meal_request';
ALTER TABLE chat_messages ADD INDEX idx_cm_reference (reference_type, reference_id);

-- ── custom_meal_requests ──────────────────────────────────────────────────
-- One row per (subscription, date). Deliberately UNIQUE on that pair rather
-- than append-only: a day can only have one answer, and a declined row is
-- REUSED on a re-request instead of stacking rows that would let a single day
-- hold two contradictory states at once.
CREATE TABLE IF NOT EXISTS custom_meal_requests (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
