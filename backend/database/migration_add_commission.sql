-- Adds platform-wide commission support: a single global commission_pct
-- setting admins can change, and per-order commission_pct/commission_amount
-- snapshotted once an order is marked 'delivered' (so a later rate change
-- never rewrites the commission owed on already-completed orders).
-- Safe to run multiple times.

CREATE TABLE IF NOT EXISTS platform_settings (
    id TINYINT PRIMARY KEY DEFAULT 1,
    commission_pct DECIMAL(5,2) NOT NULL DEFAULT 4.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INT NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT single_row CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed the one settings row if it doesn't exist yet. 4% matches the rate the
-- Admin dashboard's Earnings page already displayed before this was wired
-- to a real, editable value.
INSERT IGNORE INTO platform_settings (id, commission_pct) VALUES (1, 4.00);

SET @dbname = DATABASE();
SET @tablename = 'orders';

SET @col = 'commission_pct';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN commission_pct DECIMAL(5,2) NULL AFTER payment_verified_at',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'commission_amount';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN commission_amount DECIMAL(10,2) NULL AFTER commission_pct',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
