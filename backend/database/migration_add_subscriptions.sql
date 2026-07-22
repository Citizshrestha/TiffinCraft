-- Migration: Create subscriptions table for weekly/monthly meal subscriptions
-- Safe to run multiple times

CREATE TABLE IF NOT EXISTS subscriptions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    cook_id INT NOT NULL,
    meal_id INT NOT NULL,
    frequency ENUM('weekly','monthly') NOT NULL,
    status ENUM('active','paused','cancelled') DEFAULT 'active',
    delivery_address TEXT,
    start_date DATE NOT NULL,
    next_delivery_date DATE NOT NULL,
    skipped_dates JSON DEFAULT NULL COMMENT 'array of ISO date strings',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    INDEX idx_sub_cook (cook_id),
    INDEX idx_next_delivery (next_delivery_date)
);
