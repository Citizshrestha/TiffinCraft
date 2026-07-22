-- Add media message types for chat image/video uploads.
-- Run after migration_chat_tables.sql on existing databases.

ALTER TABLE chat_messages
MODIFY message_type ENUM('text', 'image', 'video', 'call_ended', 'call_declined', 'call_missed') NOT NULL DEFAULT 'text';
