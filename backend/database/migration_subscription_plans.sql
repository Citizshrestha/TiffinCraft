-- Migration: Subscription plans (cook-authored, multi-item, weekly/monthly)
-- CREATE TABLE statements are safe to re-run; the ALTER TABLE block at the
-- bottom is a one-time repoint of `subscriptions` and is not re-run-safe.

CREATE TABLE IF NOT EXISTS subscription_plans (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cook_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    duration ENUM('weekly','monthly') NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_plan_cook (cook_id, is_active)
);

-- Each item is one of the cook's own meals + a quantity per delivery.
-- Price/description/name are read live from `meals` (no duplicated, driftable copy).
CREATE TABLE IF NOT EXISTS subscription_plan_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    plan_id INT NOT NULL,
    meal_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    INDEX idx_item_plan (plan_id)
);

-- Repoint `subscriptions` (customer -> subscribed plan) at a plan instead of a
-- single meal. The table has 0 rows in production (feature was never wired to
-- a purchase flow before now), so this is a clean repoint, not a data migration.
ALTER TABLE subscriptions
    DROP FOREIGN KEY subscriptions_ibfk_3,
    DROP COLUMN meal_id,
    DROP COLUMN frequency,
    ADD COLUMN plan_id INT NOT NULL AFTER cook_id;
ALTER TABLE subscriptions ADD CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE;
