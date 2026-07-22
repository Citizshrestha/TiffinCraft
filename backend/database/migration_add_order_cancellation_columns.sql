-- Migration: Add cancellation + refund tracking columns to orders table
-- Safe to run multiple times (idempotent checks)

SET @dbname = DATABASE();
SET @tablename = 'orders';

SET @col1 = 'cancelled_by';
SET @sql1 = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col1) = 0,
    'ALTER TABLE orders ADD COLUMN cancelled_by ENUM(''customer'',''cook'') NULL AFTER status',
    'SELECT 1'
));
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

SET @col2 = 'cancellation_reason';
SET @sql2 = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col2) = 0,
    'ALTER TABLE orders ADD COLUMN cancellation_reason TEXT NULL AFTER cancelled_by',
    'SELECT 1'
));
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @col3 = 'refund_status';
SET @sql3 = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col3) = 0,
    'ALTER TABLE orders ADD COLUMN refund_status ENUM(''not_applicable'',''pending'',''refunded'') DEFAULT ''not_applicable'' AFTER cancellation_reason',
    'SELECT 1'
));
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;
