-- ============================================================================
-- Subscriptions: pay-first (eSewa) support
--
-- Before this, subscribing created an ACTIVE-pending row and the customer paid
-- the cook manually by QR + screenshot. This migration lets a subscription be
-- gated behind a real, server-verified eSewa payment, reusing the SAME
-- `payments` table (and therefore the same signature verification, status
-- re-check and idempotent-apply code) as order payments — rather than a
-- parallel table that would duplicate all of that logic.
--
-- NOT idempotent: plain ALTERs, run once.
--   mysql -u <user> -p <database> < migration_subscription_esewa_payment.sql
-- ============================================================================

-- 1) payments can now belong to EITHER an order or a subscription (exactly one).
--    order_id becomes nullable; its FK stays and simply permits NULL.
ALTER TABLE payments MODIFY COLUMN order_id INT NULL;

ALTER TABLE payments ADD COLUMN subscription_id INT NULL AFTER order_id;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE;

ALTER TABLE payments ADD INDEX idx_payments_subscription (subscription_id);

-- 2) Subscription-side state.
--    end_date: a subscription is a SINGLE up-front payment, so recurring
--    delivery generation has to stop. Set on activation from the plan duration
--    (see subscriptionPaymentState.js) and enforced in subscriptionOrderJob.js.
ALTER TABLE subscriptions ADD COLUMN end_date DATE NULL AFTER start_date;

--    'completed' is the natural end of a paid subscription whose delivery
--    window has run out — without it such a row sits 'active' forever, showing
--    "Next delivery" dates that will never happen.
ALTER TABLE subscriptions
    MODIFY COLUMN status ENUM('pending_payment','active','paused','cancelled','completed')
    NOT NULL DEFAULT 'pending_payment';

--    'failed' distinguishes "the gateway declined / the customer abandoned it"
--    from 'rejected' (the cook rejected a manual QR screenshot).
ALTER TABLE subscriptions
    MODIFY COLUMN payment_status ENUM('pending','submitted','verified','rejected','failed')
    NOT NULL DEFAULT 'pending';

--    Which path this subscription is paying through. Manual QR stays the
--    default so existing rows keep their current meaning; the cook's
--    verify-proof screen only applies to those.
ALTER TABLE subscriptions
    ADD COLUMN payment_method ENUM('manual_qr','esewa') NOT NULL DEFAULT 'manual_qr' AFTER payment_status;

-- 3) Append-only audit trail of every subscription payment state transition,
--    with timestamps, for dispute resolution. Deliberately separate from
--    `subscriptions` (which only holds current state) so a failed attempt
--    followed by a successful retry leaves both events on record.
CREATE TABLE IF NOT EXISTS subscription_payment_events (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
