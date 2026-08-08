-- Combo Deals: a cook-authored ONE-TIME bundle at a fixed cook-set price
-- (which can be less than the sum of its parts — that's the whole point of a
-- "deal"). Structurally similar to subscription_plans/subscription_plan_items
-- but deliberately has no duration/recurrence — buying a combo just places a
-- normal order, reusing the entire existing order/payment infrastructure
-- (COD or the same manual eSewa-QR flow), instead of the subscription's
-- recurring-delivery engine. This is what makes it a genuinely different
-- feature from Subscriptions, not just a renamed copy of it.

CREATE TABLE IF NOT EXISTS combo_deals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cook_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_combo_cook (cook_id, is_active)
);

CREATE TABLE IF NOT EXISTS combo_deal_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    combo_id INT NOT NULL,
    meal_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    FOREIGN KEY (combo_id) REFERENCES combo_deals(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    INDEX idx_combo_item (combo_id)
);

-- Tags an order as having come from a combo purchase (nullable — regular
-- orders leave this NULL). Lets the order's total_amount legitimately differ
-- from summing its order_items' price_at_time (the combo's own fixed price
-- wins), and lets both sides see "this was a combo deal" in order history.
ALTER TABLE orders ADD COLUMN combo_id INT NULL AFTER special_instructions,
    ADD CONSTRAINT fk_orders_combo FOREIGN KEY (combo_id) REFERENCES combo_deals(id) ON DELETE SET NULL;
