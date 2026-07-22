-- Add edited_at for chat text message edits.
-- Run after migration_chat_tables.sql / migration_chat_media_messages.sql

ALTER TABLE chat_messages
ADD COLUMN edited_at TIMESTAMP NULL DEFAULT NULL AFTER created_at;
