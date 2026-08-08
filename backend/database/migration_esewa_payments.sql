-- ============================================================================
-- eSewa Intent Payment Migration
-- Creates payments (one row per eSewa payment attempt) and refund_requests
-- (cook/customer-flagged refund workflow, manually processed by an admin via
-- the eSewa merchant dashboard — see refundController.js for why this is a
-- tracking table, not an automatic money-movement system).
-- Run: mysql -u <user> -p <database> < migration_esewa_payments.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    transaction_uuid VARCHAR(100) NOT NULL UNIQUE,
    booking_id VARCHAR(100) NULL,
    correlation_id VARCHAR(100) NULL,
    reference_code VARCHAR(100) NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status ENUM('BOOKED', 'PENDING', 'SUCCESS', 'FAILED', 'CANCELED', 'REVERTED') NOT NULL DEFAULT 'BOOKED',
    raw_callback_payload JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_correlation (correlation_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refund_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    payment_id INT NULL,
    requested_by INT NOT NULL,
    reason ENUM('failed_delivery', 'cook_mistake', 'customer_cancelled', 'other') NOT NULL,
    reason_notes TEXT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('requested', 'under_review', 'approved', 'rejected', 'processed') NOT NULL DEFAULT 'requested',
    admin_notes TEXT NULL,
    processed_by INT NULL,
    processed_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refund_requests_order (order_id),
    INDEX idx_refund_requests_status (status),
    CONSTRAINT fk_refund_requests_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_requests_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL,
    CONSTRAINT fk_refund_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_requests_processed_by FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
