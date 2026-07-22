-- Add fcm_token column to users table
-- Run: mysql -u root -p tiffincraft < backend/database/migration_add_fcm_token.sql

ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255) NULL AFTER profile_image;
