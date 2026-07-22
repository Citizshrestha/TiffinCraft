-- The live notifications table predates complete_schema.sql and only has order_id.
-- notificationHelper.js / orderController.js / adminController.js insert into
-- reference_id + reference_type, so those columns must exist. order_id is kept
-- for backward compatibility with existing rows and seed scripts.
ALTER TABLE notifications
    ADD COLUMN reference_id INT NULL COMMENT 'ID of related entity (order_id, review_id, cook_profile id, etc)' AFTER type,
    ADD COLUMN reference_type VARCHAR(50) NULL COMMENT 'order, review, meal, cook_profile, etc' AFTER reference_id;

-- Backfill existing order notifications into the generic columns
UPDATE notifications SET reference_id = order_id, reference_type = 'order'
WHERE order_id IS NOT NULL AND reference_id IS NULL;
