-- ============================================================================
-- platform_settings.delivery_cutoff_hour
--
-- The deadline for changing tomorrow's meal (customer skip, cook closure) is
-- 8:00 PM Nepal Time on the day BEFORE the delivery date. That is a business
-- rule the admin will want to move (festival season, cook feedback), so it
-- lives in platform_settings next to commission_pct rather than as a constant
-- compiled into the API.
--
-- Stored as an hour-of-day integer in NPT, 0-23. 20 = 8:00 PM NPT.
-- Deliberately hour-granular: a minute-precision cutoff would need a second
-- column and buys nothing — no one schedules a tiffin deadline at 20:37.
--
-- platform_settings is a single-row table (CHECK (id = 1)) created by
-- migration_add_commission.sql; this only adds a column to it.
--
-- Idempotent.
--   mysql -u <user> -p <database> < migration_delivery_cutoff_hour.sql
-- ============================================================================

SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'platform_settings'
                  AND COLUMN_NAME = 'delivery_cutoff_hour');
SET @sql := IF(@exists = 0,
    'ALTER TABLE platform_settings ADD COLUMN delivery_cutoff_hour TINYINT UNSIGNED NOT NULL DEFAULT 20 COMMENT ''hour-of-day in Nepal Time (0-23) after which the NEXT day''''s deliveries are locked; 20 = 8pm NPT''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Guard the range at the DB level so a bad admin write can't produce an
-- unenforceable cutoff (e.g. hour 30) that silently locks or unlocks every day.
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'platform_settings'
                  AND CONSTRAINT_NAME = 'chk_delivery_cutoff_hour');
SET @sql := IF(@exists = 0,
    'ALTER TABLE platform_settings ADD CONSTRAINT chk_delivery_cutoff_hour CHECK (delivery_cutoff_hour BETWEEN 0 AND 23)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Ensure the singleton row exists (no-op if migration_add_commission.sql already
-- inserted it) so reads never have to cope with an empty table.
INSERT IGNORE INTO platform_settings (id, commission_pct, delivery_cutoff_hour) VALUES (1, 4.00, 20);
