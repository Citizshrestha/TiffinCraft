-- Quick fix for production database
-- Run this if meals.in_subscription column is missing

-- Check if column exists and add if missing
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'meals'
    AND COLUMN_NAME = 'in_subscription'
);

-- Only add if it doesn't exist
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE meals ADD COLUMN in_subscription BOOLEAN DEFAULT FALSE COMMENT ''True if meal can be added to subscription plans'' AFTER is_available',
    'SELECT ''Column in_subscription already exists'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index if column was just created
SET @sql2 = IF(@column_exists = 0,
    'ALTER TABLE meals ADD INDEX idx_in_subscription (in_subscription)',
    'SELECT ''Index already exists'' as message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Verify
SELECT 'SUCCESS: in_subscription column is now present' as status;
DESCRIBE meals;
