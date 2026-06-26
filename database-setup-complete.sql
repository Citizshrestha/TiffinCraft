-- ========================================
-- TiffinCraft Complete Database Setup
-- Execute this via phpMyAdmin or MySQL Workbench
-- ========================================

-- Drop and create database
DROP DATABASE IF EXISTS tiffincraft;
CREATE DATABASE tiffincraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tiffincraft;

-- ========================================
-- Users Table
-- ========================================
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('customer', 'cook', 'admin') NOT NULL DEFAULT 'customer',
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    profile_image VARCHAR(255),
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    otp_code VARCHAR(6),
    otp_expires_at DATETIME,
    auth_provider ENUM('local', 'google', 'facebook') DEFAULT 'local',
    google_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_role (role),
    INDEX idx_verified (is_verified),
    INDEX idx_google_id (google_id),
    INDEX idx_auth_provider (auth_provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Cook Profiles Table
-- ========================================
CREATE TABLE cook_profiles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE NOT NULL,
    kitchen_name VARCHAR(255),
    food_type VARCHAR(255),
    description TEXT,
    capacity_per_day INT DEFAULT 0,
    bio TEXT,
    specialties TEXT,
    rating DECIMAL(3,2) DEFAULT 0.00,
    total_orders INT DEFAULT 0,
    total_reviews INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_approved (is_approved),
    INDEX idx_verified (is_verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Meals Table
-- ========================================
CREATE TABLE meals (
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

-- ========================================
-- Orders Table
-- ========================================
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    cook_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('placed', 'accepted', 'preparing', 'out_for_delivery', 'delivered', 'cancelled') DEFAULT 'placed',
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

-- ========================================
-- Order Items Table
-- ========================================
CREATE TABLE order_items (
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

-- ========================================
-- Reviews Table
-- ========================================
CREATE TABLE reviews (
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

-- ========================================
-- Admin Records Table
-- ========================================
CREATE TABLE admin_records (
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

-- ========================================
-- Insert Test Data
-- ========================================

-- Test Admin User (password: admin123)
INSERT INTO users (full_name, email, phone, password_hash, role, is_active, is_verified, auth_provider)
VALUES ('Admin User', 'admin@tiffincraft.com', '9999999999', '$2a$10$YourHashedPasswordHere', 'admin', TRUE, TRUE, 'local');

-- Test Customer (password: test123)
INSERT INTO users (full_name, email, phone, password_hash, role, is_active, is_verified, auth_provider)
VALUES ('Test Customer', 'customer@test.com', '1234567890', '$2a$10$YourHashedPasswordHere', 'customer', TRUE, TRUE, 'local');

-- Test Cook (password: test123)
INSERT INTO users (full_name, email, phone, password_hash, role, is_active, is_verified, auth_provider)
VALUES ('Test Cook', 'cook@test.com', '9876543210', '$2a$10$YourHashedPasswordHere', 'cook', TRUE, TRUE, 'local');

-- Cook Profile
INSERT INTO cook_profiles (user_id, kitchen_name, food_type, description, capacity_per_day, is_approved)
VALUES (3, 'Home Kitchen', 'Indian', 'Delicious homemade meals', 20, TRUE);

-- ========================================
-- Verification
-- ========================================
SELECT 'Database setup completed!' AS Status;
SHOW TABLES;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS cook_count FROM cook_profiles;
