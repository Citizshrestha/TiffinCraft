-- ============================================
-- TiffinCraft Cart Table Setup
-- ============================================
-- Run this script in MySQL Workbench or phpMyAdmin
-- Or via command line: mysql -u root -p < setup_cart_table.sql

USE tiffincraft;

-- Drop existing cart table if you need to recreate it
-- DROP TABLE IF EXISTS cart;

-- Create cart table
CREATE TABLE IF NOT EXISTS cart (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    meal_id INT NOT NULL,
    quantity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    UNIQUE KEY unique_cart_item (customer_id, meal_id),
    INDEX idx_customer_cart (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify table was created
SELECT 'Cart table created successfully!' AS Status;

-- Show table structure
DESCRIBE cart;

-- Show current cart data (should be empty initially)
SELECT COUNT(*) as cart_items_count FROM cart;
