-- Adds columns the app writes to but older cook_profiles schemas lack:
--   is_holiday_mode — kitchen open/closed toggle (PUT /cook/holiday-mode)
--   bank_details    — payment QR URLs JSON (PUT /cook/bank-details)
-- Safe to run once per database; skip columns that already exist.

ALTER TABLE cook_profiles
ADD COLUMN is_holiday_mode BOOLEAN NOT NULL DEFAULT FALSE AFTER is_approved;

ALTER TABLE cook_profiles
ADD COLUMN bank_details TEXT NULL AFTER is_holiday_mode;
