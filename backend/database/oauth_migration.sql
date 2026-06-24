-- OAuth Migration Script
-- This adds all required columns for Google and Facebook authentication
-- Run this in MySQL Workbench or command line

USE tiffincraft;

-- Add OAuth and verification columns to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE DEFAULT NULL AFTER password_hash,
ADD COLUMN IF NOT EXISTS facebook_id VARCHAR(255) UNIQUE DEFAULT NULL AFTER google_id,
ADD COLUMN IF NOT EXISTS auth_provider ENUM('local', 'google', 'facebook') DEFAULT 'local' AFTER facebook_id,
ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE AFTER auth_provider,
ADD COLUMN IF NOT EXISTS otp_code VARCHAR(6) DEFAULT NULL AFTER is_verified,
ADD COLUMN IF NOT EXISTS otp_expires_at DATETIME DEFAULT NULL AFTER otp_code;

-- Make phone nullable for OAuth users (they might not provide phone)
ALTER TABLE users
MODIFY COLUMN phone VARCHAR(20) UNIQUE DEFAULT NULL;

-- Make password_hash nullable for OAuth users
ALTER TABLE users
MODIFY COLUMN password_hash VARCHAR(255) DEFAULT NULL;

-- Add indexes for OAuth columns
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_google_id (google_id);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_facebook_id (facebook_id);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_auth_provider (auth_provider);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_is_verified (is_verified);

-- Show the updated structure
DESCRIBE users;

SELECT 'OAuth migration completed successfully!' AS Status;
