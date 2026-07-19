-- ============================================================================
-- Chat System Migration
-- Creates conversations and chat_messages tables for 1-to-1 chat
-- between customers and cooks, with support for text and call log messages.
-- Run: mysql -u <user> -p <database> < migration_chat_tables.sql
-- ============================================================================

-- Conversations: one row per unique customer <-> cook pair
CREATE TABLE IF NOT EXISTS conversations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    cook_id INT NOT NULL,
    last_message_id INT NULL,
    last_message_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_conversation_pair (customer_id, cook_id),
    INDEX idx_conversations_customer (customer_id, last_message_at),
    INDEX idx_conversations_cook (cook_id, last_message_at),
    CONSTRAINT fk_conversations_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_cook FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat messages: text/media messages and call logs
CREATE TABLE IF NOT EXISTS chat_messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    message_type ENUM('text', 'image', 'video', 'call_ended', 'call_declined', 'call_missed') NOT NULL DEFAULT 'text',
    content TEXT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    call_duration_seconds INT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_chat_messages_conversation (conversation_id, id),
    INDEX idx_chat_messages_unread (conversation_id, is_read),
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Link last_message_id after chat_messages exists
ALTER TABLE conversations
ADD CONSTRAINT fk_conversations_last_message
FOREIGN KEY (last_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;

