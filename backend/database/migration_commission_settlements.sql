-- Commission settlement feature: admin's own QR (for cooks to pay commission into)
-- and a monthly per-cook settlement ledger. See commissionController.js for the
-- accompanying logic. Amounts here are frozen at generation time (same convention
-- as orders.commission_amount) so a later rate change or order edit never
-- silently shifts what a cook already owes.

ALTER TABLE platform_settings ADD COLUMN bank_details TEXT NULL AFTER commission_pct;

CREATE TABLE IF NOT EXISTS commission_settlements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cook_id INT NOT NULL,
    month TINYINT NOT NULL,
    year SMALLINT NOT NULL,
    amount_due DECIMAL(10,2) NOT NULL,
    order_count INT NOT NULL DEFAULT 0,
    status ENUM('pending','submitted','verified','rejected') NOT NULL DEFAULT 'pending',
    payment_screenshot_url VARCHAR(500) NULL,
    submitted_at TIMESTAMP NULL,
    verified_at TIMESTAMP NULL,
    verified_by INT NULL,
    admin_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_cook_period (cook_id, month, year),
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_period (year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
