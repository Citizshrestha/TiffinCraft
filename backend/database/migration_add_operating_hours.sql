-- Adds operating_hours to cook_profiles if it doesn't already exist.
-- The app writes to this via PUT /api/cook/profile/operating-hours.
-- Safe to run multiple times.

SET @dbname = DATABASE();
SET @tablename = 'cook_profiles';
SET @col = 'operating_hours';

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE cook_profiles ADD COLUMN operating_hours JSON NULL AFTER bank_details',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
