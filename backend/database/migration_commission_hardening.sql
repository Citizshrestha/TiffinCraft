-- ============================================================================
-- Commission hardening — one migration covering five separate production holes
-- found while auditing the cook⇄admin commission payment flow.
--
-- 1. orders.delivered_at had NO migration file anywhere in database/, despite
--    every commission period query grouping by it (see nepaliTime.js toNpt()).
--    It only existed by live-DB accident. A fresh deploy would have produced
--    zero commission, silently, forever.
--
-- 2. No index covered (cook_id, status, delivered_at) — the exact triple every
--    settlement and earnings query filters on.
--
-- 3. commission_settlements had no screenshot dedupe, so one payment
--    screenshot could be re-submitted to settle a second month. Subscriptions
--    already solved this with a SHA-256 UNIQUE column; mirror it rather than
--    inventing a second scheme.
--
-- 4. The platform rate default was 4.00 in the schema while the code's
--    fallback disagreed with itself (4.00 in the controller, 5.00 in the
--    helper). Confirmed business rate is 5%.
--
-- 5. Due-date reminders had nowhere to record that they had already been sent,
--    so a daily reminder cron would re-notify the same cook every single day.
--
-- Idempotent throughout: every ADD COLUMN / ADD INDEX is guarded on
-- information_schema, so re-running is a no-op.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. orders.delivered_at — the true, immutable delivery moment.
--
-- NOT backfilled to updated_at here. updated_at moves on any later touch to
-- the row, so backfilling it would invent delivery timestamps that are wrong
-- by however long ago the row was last edited, and those wrong timestamps
-- would then decide which month a cook is billed for. Historical orders are
-- handled deliberately, and reviewably, by scripts/backfill_commission.mjs
-- (dry-run by default) instead.
-- ---------------------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivered_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP NULL DEFAULT NULL',
    'SELECT "orders.delivered_at already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Covering index for the commission/earnings period scan.
-- Column order matters: cook_id is the equality filter, status the second
-- equality filter, delivered_at the range/extract. Reversing them would make
-- the index useless for the per-cook monthly rollup.
-- ---------------------------------------------------------------------------
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'orders'
      AND index_name = 'idx_orders_commission_period'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE orders ADD INDEX idx_orders_commission_period (cook_id, status, delivered_at)',
    'SELECT "idx_orders_commission_period already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. Screenshot dedupe. SHA-256 of the actual image bytes, hashed server-side
-- (never trusted from the client). CHAR(64) = hex digest length.
--
-- NULL is allowed and is NOT deduped: MySQL/TiDB unique keys permit unlimited
-- NULLs, which is exactly right — every settlement that has not been submitted
-- yet shares "no screenshot", and that must not collide.
-- ---------------------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'commission_settlements'
      AND column_name = 'payment_screenshot_hash'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE commission_settlements
        ADD COLUMN payment_screenshot_hash CHAR(64) NULL DEFAULT NULL AFTER payment_screenshot_url',
    'SELECT "payment_screenshot_hash already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'commission_settlements'
      AND index_name = 'uniq_commission_screenshot_hash'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE commission_settlements
        ADD UNIQUE KEY uniq_commission_screenshot_hash (payment_screenshot_hash)',
    'SELECT "uniq_commission_screenshot_hash already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5. Reminder throttle. Stores the calendar day a due/overdue reminder last
-- went out for this settlement, so the daily cron can send at most one per day
-- per settlement even across restarts and duplicate cron fires.
-- ---------------------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'commission_settlements'
      AND column_name = 'last_reminder_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE commission_settlements
        ADD COLUMN last_reminder_at TIMESTAMP NULL DEFAULT NULL',
    'SELECT "last_reminder_at already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. Commission rate = 5% (confirmed business decision).
--
-- The UPDATE is deliberately narrowed to rows still sitting on the old 4.00
-- default. An admin who has intentionally set some other rate must not have it
-- silently overwritten by a migration, and a rate that is already 5.00 needs
-- no write. This is why it is not a blanket `SET commission_pct = 5.00`.
-- ---------------------------------------------------------------------------
ALTER TABLE platform_settings MODIFY COLUMN commission_pct DECIMAL(5,2) NOT NULL DEFAULT 5.00;

UPDATE platform_settings SET commission_pct = 5.00 WHERE id = 1 AND commission_pct = 4.00;

INSERT IGNORE INTO platform_settings (id, commission_pct) VALUES (1, 5.00);

-- ---------------------------------------------------------------------------
-- Verification readout
-- ---------------------------------------------------------------------------
SELECT 'orders' AS tbl, column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivered_at'
UNION ALL
SELECT 'commission_settlements', column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'commission_settlements'
  AND column_name IN ('payment_screenshot_hash', 'last_reminder_at');

SELECT id, commission_pct FROM platform_settings WHERE id = 1;
