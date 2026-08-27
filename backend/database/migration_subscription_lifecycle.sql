-- ============================================================================
-- Subscription lifecycle redesign — schema changes on `subscriptions`
--
-- Before this, a cook tapping "verify" flipped the subscription straight to
-- 'active' AND overwrote start_date with the verification day, so a customer
-- who chose to start next Monday started today instead. And once active, the
-- whole subscription was a single on/off flag — there was no per-day record,
-- so "I skipped Thursday" and "the cook was closed Friday" were unrepresentable.
--
-- New lifecycle:
--   pending_payment      -> customer hasn't paid yet (unchanged, still needed
--                           by the create-then-pay flow)
--   pending_verification -> money is in, cook hasn't confirmed it
--   verified             -> cook confirmed payment; NOT yet delivering
--   scheduled            -> verified + start_date is in the future
--   active               -> start_date has arrived; daily log rows exist
--   paused               -> INDEFINITE customer-initiated hold (distinct from
--                           a one-day skip, which lives in subscription_daily_log)
--   completed / cancelled-> terminal
--
-- Idempotent: every change is guarded by an information_schema check, so this
-- can be re-run safely (unlike migration_subscription_esewa_payment.sql).
--   mysql -u <user> -p <database> < migration_subscription_lifecycle.sql
-- ============================================================================

-- 1) Widen the status ENUM. MODIFY is naturally idempotent (it just restates
--    the target definition), but it must preserve every existing value so no
--    live row is silently truncated to ''.
ALTER TABLE subscriptions
    MODIFY COLUMN status ENUM(
        'pending_payment',
        'pending_verification',
        'verified',
        'scheduled',
        'active',
        'paused',
        'completed',
        'cancelled'
    ) NOT NULL DEFAULT 'pending_payment';

-- 2) Who verified the payment, and when. payment_verified_at already records
--    the timestamp for the manual-QR flow; verified_at is the lifecycle
--    timestamp (set on every path, including eSewa auto-verification) and
--    verified_by is the actor, needed for dispute resolution.
SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'verified_by');
SET @sql := IF(@exists = 0,
    'ALTER TABLE subscriptions ADD COLUMN verified_by INT NULL COMMENT ''users.id of the cook (or admin) who verified payment'' AFTER verification_notes',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'verified_at');
SET @sql := IF(@exists = 0,
    'ALTER TABLE subscriptions ADD COLUMN verified_at TIMESTAMP NULL COMMENT ''when the payment was verified (UTC)'' AFTER verified_by',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK on verified_by. Named so re-runs can detect it. ON DELETE SET NULL rather
-- than CASCADE: deleting a cook account must never delete a customer's paid
-- subscription record — we lose the attribution, not the subscription.
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'subscriptions'
                  AND CONSTRAINT_NAME = 'fk_subscriptions_verified_by');
SET @sql := IF(@exists = 0,
    'ALTER TABLE subscriptions ADD CONSTRAINT fk_subscriptions_verified_by FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Meal credits.
--    The per-day log needs a notion of "how many paid deliveries are left" so a
--    skipped day can be NOT charged (credit_deducted = FALSE) and the
--    subscription can auto-complete when the paid meals run out. The schema had
--    no such counter — end_date alone can't express it, because a customer who
--    skips three days should still receive their full paid meal count.
--
--    Seeded from the plan duration (7 / 14 / 30 = one meal per day) at
--    verification time; NULL on legacy rows, which the cron treats as
--    "unlimited within end_date" so nothing pre-existing breaks.
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'meals_total');
SET @sql := IF(@exists = 0,
    'ALTER TABLE subscriptions ADD COLUMN meals_total INT NULL COMMENT ''paid deliveries in this cycle; NULL = legacy row, bounded by end_date only'' AFTER end_date',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'meals_remaining');
SET @sql := IF(@exists = 0,
    'ALTER TABLE subscriptions ADD COLUMN meals_remaining INT NULL COMMENT ''decremented only when a delivery day is actually charged'' AFTER meals_total',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) The daily activation cron scans by (status, start_date). Without this
--    index it full-scans `subscriptions` every morning.
SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'subscriptions' AND INDEX_NAME = 'idx_sub_status_start');
SET @sql := IF(@exists = 0,
    'ALTER TABLE subscriptions ADD INDEX idx_sub_status_start (status, start_date)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) next_delivery_date must become nullable.
--
--    It was declared DATE NOT NULL back when a subscription was only ever active
--    or paused, and there was always a next delivery. That is no longer true:
--    a 'completed' or 'cancelled' subscription has no next delivery, and neither
--    does one whose every remaining day the customer has already skipped.
--    Writing a stale date there means the app shows "Next delivery: Sep 12" for
--    a subscription that has finished; writing 0000-00-00 (what NOT NULL + a
--    NULL parameter degrades to outside strict mode) means it shows a date in
--    the year zero.
--
--    MODIFY restates the whole definition, so it is idempotent — but the DEFAULT
--    is deliberately omitted rather than set: an INSERT that forgets this column
--    should still fail loudly rather than quietly create a subscription with no
--    delivery date.
ALTER TABLE subscriptions
    MODIFY COLUMN next_delivery_date DATE NULL
        COMMENT 'next date a meal is expected; NULL once completed/cancelled or no days remain';

