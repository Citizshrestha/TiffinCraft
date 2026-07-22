-- Add is_deleted for chat message soft-delete (keeps the row so "This message
-- was deleted" placeholders can render for both participants).
-- Run after migration_chat_tables.sql / migration_chat_message_edit.sql

ALTER TABLE chat_messages
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER content;
