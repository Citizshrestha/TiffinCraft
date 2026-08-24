-- Migration: Expand subscription plan duration to support 1 week, 2 weeks, and 1 month
ALTER TABLE subscription_plans MODIFY COLUMN duration VARCHAR(32) NOT NULL;
