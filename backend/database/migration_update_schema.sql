-- TiffinCraft Database Migration Script
-- Run this in MySQL Workbench to update the existing database schema
-- This adds missing columns and tables without dropping existing data

USE tiffincraft;

-- ============================================
-- Step 1: Update cook_profiles table
-- ============================================

-- Check if columns exist before adding them
SET @dbname = 'tiffincraft';
SET @tablename = 'cook_profiles';

-- Add kitchen_name column if it doesn't exist
SET @query = CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN kitchen_name VARCHAR(255) AFTER user_id');
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = 'kitchen_name'
);
SET @query = IF(@column_exists = 0, @query, 'SELECT "kitchen_name already exists"');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add food_type column if it doesn't exist
SET @query = CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN food_type VARCHAR(255) AFTER kitchen_name');
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = 'food_type'
);
SET @query = IF(@column_exists = 0, @query, 'SELECT "food_type already exists"');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add description column if it doesn't exist
SET @query = CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN description TEXT AFTER food_type');
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = 'description'
);
SET @query = IF(@column_exists = 0, @query, 'SELECT "description already exists"');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add capacity_per_day column if it doesn't exist
SET @query = CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN capacity_per_day INT DEFAULT 0 AFTER description');
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = 'capacity_per_day'
);
SET @query = IF(@column_exists = 0, @query, 'SELECT "capacity_per_day already exists"');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_approved column if it doesn't exist
SET @query = CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN is_approved BOOLEAN DEFAULT FALSE AFTER is_verified');
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = 'is_approved'
);
SET @query = IF(@column_exists = 0, @query, 'SELECT "is_approved already exists"');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add indexes
ALTER TABLE cook_profiles ADD INDEX IF NOT EXISTS idx_approved (is_approved);
ALTER TABLE cook_profiles ADD INDEX IF NOT EXISTS idx_verified (is_verified);


-- ============================================
-- Step 2: Create meals table if it doesn't exist
-- ============================================

CREATE TABLE IF NOT EXISTS meals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cook_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    cuisine_type VARCHAR(100),
    is_available BOOLEAN DEFAULT TRUE,
    preparation_time INT COMMENT 'in minutes',
    spice_level ENUM('mild', 'medium', 'hot', 'very_hot') DEFAULT 'medium',
    is_vegetarian BOOLEAN DEFAULT FALSE,
    is_vegan BOOLEAN DEFAULT FALSE,
    allergens TEXT COMMENT 'comma-separated list',
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_cook_id (cook_id),
    INDEX idx_category (category),
    INDEX idx_available (is_available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================
-- Step 3: Create orders table if it doesn't exist
-- ============================================

CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    cook_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('pending', 'confirmed', 'preparing', 'ready', 'delivered', 'cancelled') DEFAULT 'pending',
    delivery_address TEXT,
    delivery_latitude DECIMAL(10, 8),
    delivery_longitude DECIMAL(11, 8),
    special_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_cook (cook_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================
-- Step 4: Create order_items table if it doesn't exist
-- ============================================

CREATE TABLE IF NOT EXISTS order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    meal_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price_at_time DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    INDEX idx_order (order_id),
    INDEX idx_meal (meal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================
-- Step 5: Create reviews table if it doesn't exist
-- ============================================

CREATE TABLE IF NOT EXISTS reviews (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    customer_id INT NOT NULL,
    cook_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_cook (cook_id),
    INDEX idx_customer (customer_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================
-- Verify Migration
-- ============================================

SELECT 'Migration completed!' AS Status;
SHOW TABLES;
DESCRIBE cook_profiles;
DESCRIBE meals;
