-- TiffinCraft Database Schema Fix Migration
-- This migration adds all missing columns and tables required by the controllers
-- Run this AFTER running complete_schema.sql

USE tiffincraft;

-- ============================================================
-- 1. Add missing columns to users table
-- ============================================================

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS otp_code VARCHAR(6),
  ADD COLUMN IF NOT EXISTS otp_expires_at DATETIME,
  ADD COLUMN IF NOT EXISTS auth_provider ENUM('local', 'google', 'facebook') DEFAULT 'local',
  ADD COLUMN IF NOT EXISTS google_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;

-- Add indexes for performance
ALTER TABLE users
  ADD INDEX IF NOT EXISTS idx_google_id (google_id),
  ADD INDEX IF NOT EXISTS idx_verified (is_verified),
  ADD INDEX IF NOT EXISTS idx_auth_provider (auth_provider);

-- ============================================================
-- 2. Add missing column to cook_profiles table
-- ============================================================

ALTER TABLE cook_profiles
  ADD COLUMN IF NOT EXISTS total_reviews INT DEFAULT 0;

-- ============================================================
-- 3. Fix order status enum to match controllers
-- ============================================================
-- Controllers use: placed, accepted, preparing, out_for_delivery, delivered, cancelled
-- Schema has: pending, confirmed, preparing, ready, delivered, cancelled

ALTER TABLE orders
  MODIFY COLUMN status ENUM('placed', 'accepted', 'preparing', 'out_for_delivery', 'delivered', 'cancelled') DEFAULT 'placed';

-- ============================================================
-- 4. Add admin role to users enum
-- ============================================================

ALTER TABLE users
  MODIFY COLUMN role ENUM('customer', 'cook', 'admin') NOT NULL DEFAULT 'customer';

-- ============================================================
-- 5. Create missing admin_records table
-- ============================================================

CREATE TABLE IF NOT EXISTS admin_records (
    id INT PRIMARY KEY AUTO_INCREMENT,
    admin_id INT NOT NULL,
    action VARCHAR(255) NOT NULL,
    target_type VARCHAR(50),
    target_id INT,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admin (admin_id),
    INDEX idx_action (action),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Verification queries
-- ============================================================

SELECT 'Migration completed successfully!' AS Status;

-- Show updated table structures
SHOW COLUMNS FROM users;
SHOW COLUMNS FROM cook_profiles;
SHOW COLUMNS FROM orders;
SHOW TABLES;
