-- Subscription plans had no cook-controlled pricing at all — price_per_delivery
-- was always just the live sum of the plan's meal prices, i.e. exactly what a
-- customer would pay ordering the same items separately, with zero benefit to
-- subscribing. This gives cooks the same explicit-price override combo_deals
-- already has: NULL keeps the old auto-summed behavior (existing plans are
-- unaffected), a set value becomes the real per-delivery charge and the
-- summed total is shown alongside it as "compare at" / savings context.
ALTER TABLE subscription_plans
    ADD COLUMN price_per_delivery DECIMAL(10,2) NULL AFTER duration;
