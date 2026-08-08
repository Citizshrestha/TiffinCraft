-- Subscription payment verification: a subscription no longer activates the
-- instant a customer taps "Subscribe" — it starts as 'pending_payment' and only
-- becomes 'active' once the customer pays the cook (manual QR, same as regular
-- orders), uploads a screenshot, and the cook verifies it. Mirrors the
-- order/commission-settlement "manual QR + screenshot + verify" pattern already
-- used elsewhere — no payment gateway involved.
--
-- subscriptions has 0 rows in production (per migration_subscription_plans.sql's
-- own comment), so widening the status ENUM and adding these columns is a clean
-- schema change, not a data migration.

ALTER TABLE subscriptions
    MODIFY COLUMN status ENUM('pending_payment','active','paused','cancelled') NOT NULL DEFAULT 'pending_payment',
    ADD COLUMN payment_status ENUM('pending','submitted','verified','rejected') NOT NULL DEFAULT 'pending' AFTER status,
    ADD COLUMN payment_screenshot_url VARCHAR(500) NULL AFTER payment_status,
    ADD COLUMN payment_verified_at TIMESTAMP NULL AFTER payment_screenshot_url,
    ADD COLUMN verification_notes TEXT NULL AFTER payment_verified_at;
