-- Customer Dashboard Related Tables

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('order', 'promo', 'system', 'cook') DEFAULT 'system',
    is_read BOOLEAN DEFAULT FALSE,
    order_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    INDEX idx_user_notifications (user_id, is_read, created_at DESC)
);

-- Cart Table
CREATE TABLE IF NOT EXISTS cart (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    meal_id INT NOT NULL,
    quantity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    UNIQUE KEY unique_cart_item (customer_id, meal_id)
);

-- Favorites Table
CREATE TABLE IF NOT EXISTS favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    cook_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_favorite (customer_id, cook_id)
);

-- Insert sample notifications for testing
INSERT INTO notifications (user_id, title, message, type, is_read, order_id) VALUES
(1, 'Order Delivered', 'Your order from Anita\'s Kitchen has been delivered successfully!', 'order', FALSE, NULL),
(1, 'Special Offer', 'Get 20% off on your next order! Use code WELCOME20', 'promo', FALSE, NULL),
(1, 'New Cook Available', 'Shashi Meals is now available in your area. Check out their menu!', 'cook', TRUE, NULL);
