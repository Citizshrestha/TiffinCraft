-- ============================================================================
-- Commission Rate History & Dynamic Updates
-- ============================================================================
-- Tracks all commission rate changes so the system can notify cooks and
-- show historical rates for transparency. When rate changes, all active cooks
-- receive notifications AND automated chat messages with the details.

CREATE TABLE IF NOT EXISTS commission_rate_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    old_rate DECIMAL(5,2) NOT NULL,
    new_rate DECIMAL(5,2) NOT NULL,
    changed_by INT NOT NULL COMMENT 'admin user_id who made the change',
    change_reason TEXT NULL COMMENT 'optional admin note about why rate changed',
    affected_cooks_count INT DEFAULT 0 COMMENT 'how many cooks were notified',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add a column to platform_settings to track last notification sent
ALTER TABLE platform_settings 
ADD COLUMN IF NOT EXISTS last_rate_notification_at TIMESTAMP NULL AFTER bank_details;

-- Index for faster cook queries
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_role_active (role, is_active);
