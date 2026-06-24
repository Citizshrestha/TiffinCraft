-- ============================================================
-- TIFFINCRAFT OAUTH FIX - Run this in MySQL Workbench
-- Compatible with MySQL 5.7+ (Fixed IF NOT EXISTS syntax)
-- ============================================================

USE tiffincraft;

-- Step 1: Check if table exists, if not create it
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE DEFAULT NULL,
    password_hash VARCHAR(255) DEFAULT NULL,
    role ENUM('customer', 'cook') NOT NULL DEFAULT 'customer',
    is_active BOOLEAN DEFAULT TRUE,
    profile_image VARCHAR(255),
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Add OAuth columns (will skip if they already exist)
SET @dbname = 'tiffincraft';
SET @tablename = 'users';

-- Add google_id
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'google_id');
SET @query = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN google_id VARCHAR(255) UNIQUE DEFAULT NULL AFTER password_hash',
    'SELECT "google_id already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add facebook_id
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'facebook_id');
SET @query = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN facebook_id VARCHAR(255) UNIQUE DEFAULT NULL AFTER google_id',
    'SELECT "facebook_id already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add auth_provider
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'auth_provider');
SET @query = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN auth_provider ENUM(\'local\', \'google\', \'facebook\') DEFAULT \'local\' AFTER facebook_id',
    'SELECT "auth_provider already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_verified
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_verified');
SET @query = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE AFTER auth_provider',
    'SELECT "is_verified already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add otp_code
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'otp_code');
SET @query = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN otp_code VARCHAR(6) DEFAULT NULL AFTER is_verified',
    'SELECT "otp_code already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add otp_expires_at
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'otp_expires_at');
SET @query = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN otp_expires_at DATETIME DEFAULT NULL AFTER otp_code',
    'SELECT "otp_expires_at already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Modify columns to allow NULL for OAuth users
ALTER TABLE users MODIFY COLUMN phone VARCHAR(20) UNIQUE DEFAULT NULL;
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) DEFAULT NULL;

-- Step 4: Add indexes (compatible with MySQL 5.7+)
-- Check and add idx_email
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_email');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_email (email)',
    'SELECT "idx_email already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add idx_phone
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_phone');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_phone (phone)',
    'SELECT "idx_phone already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add idx_role
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_role');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_role (role)',
    'SELECT "idx_role already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add idx_google_id
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_google_id');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_google_id (google_id)',
    'SELECT "idx_google_id already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add idx_facebook_id
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_facebook_id');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_facebook_id (facebook_id)',
    'SELECT "idx_facebook_id already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add idx_auth_provider
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_auth_provider');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_auth_provider (auth_provider)',
    'SELECT "idx_auth_provider already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add idx_is_verified
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_is_verified');
SET @query = IF(@index_exists = 0,
    'ALTER TABLE users ADD INDEX idx_is_verified (is_verified)',
    'SELECT "idx_is_verified already exists" AS Info');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 5: Show final structure
SELECT '✅ OAuth Migration Complete!' AS Status;
SELECT 'Users table structure:' AS Info;
DESCRIBE users;

-- Step 6: Show sample of existing data
SELECT 'Existing users:' AS Info;
SELECT id, full_name, email, role, auth_provider, is_verified,
       CASE WHEN google_id IS NOT NULL THEN '✓' ELSE '' END AS has_google,
       CASE WHEN facebook_id IS NOT NULL THEN '✓' ELSE '' END AS has_facebook
FROM users LIMIT 10;
