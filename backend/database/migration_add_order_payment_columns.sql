-- Adds payment-related columns to orders table if they don't already exist
-- Safe to run multiple times

SET @dbname = DATABASE();
SET @tablename = 'orders';

SET @col = 'payment_method';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_method ENUM(''cod'', ''online'') DEFAULT ''cod'' AFTER special_instructions',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'payment_status';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_status ENUM(''pending'', ''paid'', ''verified'', ''refunded'') DEFAULT ''pending'' AFTER payment_method',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'payment_screenshot_url';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_screenshot_url VARCHAR(500) AFTER payment_status',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'payment_verified_at';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_verified_at TIMESTAMP NULL AFTER payment_screenshot_url',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
