-- Fixes D1: settlements were flagged "overdue" the instant they were created.
--
-- Root cause: listSettlements computed overdue as
--   status='pending' AND (year < YEAR(CURDATE()) OR (year = YEAR(CURDATE()) AND month < MONTH(CURDATE())))
-- The cron generates a period-M row on the 1st of month M+1, so
-- "month < MONTH(CURDATE())" is true the moment the row exists — zero grace period.
--
-- Fix: add a real due_date DATE column, set once at generation time, and
-- redefine overdue as `status = 'pending' AND due_date < CURDATE()`.
--
-- Policy decision (confirmed by product owner): 15-day grace period.
-- due_date = (first day of the month AFTER the period being billed) + 15 days.
-- Example: February's commission (billed on 1 Mar) is due 16 Mar.
--
-- Safe to run multiple times: information_schema guard before ALTER, and the
-- backfill UPDATE only touches rows where due_date IS NULL.

SET @dbname = DATABASE();
SET @tablename = 'commission_settlements';

SET @col = 'due_date';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE commission_settlements ADD COLUMN due_date DATE NULL AFTER order_count',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill any pre-existing rows (created before this column existed) using
-- the same rule the generator now applies: 1st of the month after the
-- billed period, + 15 days. Only touches rows the column guard above would
-- have left NULL — re-running this script is a no-op on rows already set.
UPDATE commission_settlements
SET due_date = DATE_ADD(
    -- first day of the month after (month, year)
    DATE_ADD(MAKEDATE(year, 1), INTERVAL (month) MONTH),
    INTERVAL 15 DAY
)
WHERE due_date IS NULL;

-- Verification query — not part of the migration, run manually to confirm:
-- SELECT id, month, year, due_date, status FROM commission_settlements ORDER BY id;
