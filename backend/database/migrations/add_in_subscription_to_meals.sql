-- Migration: Add in_subscription column to meals table
-- Date: 2026-08-30
-- Description: Adds a boolean column to track which meals can be added to subscription plans

ALTER TABLE meals 
ADD COLUMN in_subscription BOOLEAN DEFAULT FALSE COMMENT 'True if meal can be added to subscription plans' 
AFTER is_available;

-- Add index for better query performance
ALTER TABLE meals 
ADD INDEX idx_in_subscription (in_subscription);
