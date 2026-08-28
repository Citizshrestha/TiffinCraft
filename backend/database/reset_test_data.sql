-- ============================================================================
-- Reset Test Data Script
-- Purpose: Reset all subscription, order, payment, and earnings data
--          while keeping users, cook profiles, and meals intact
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. SUBSCRIPTION DATA
-- ============================================================================

-- Clear subscription daily logs
TRUNCATE TABLE subscription_daily_log;

-- Clear subscription payment events
TRUNCATE TABLE subscription_payment_events;

-- Clear subscriptions
TRUNCATE TABLE subscriptions;

-- Clear subscription plan items
TRUNCATE TABLE subscription_plan_items;

-- Clear subscription plans
TRUNCATE TABLE subscription_plans;

-- ============================================================================
-- 2. ORDER DATA
-- ============================================================================

-- Clear order items
TRUNCATE TABLE order_items;

-- Clear orders
TRUNCATE TABLE orders;

-- ============================================================================
-- 3. CART DATA
-- ============================================================================

-- Clear cart items
TRUNCATE TABLE cart_items;

-- Clear cart
TRUNCATE TABLE cart;

-- ============================================================================
-- 4. PAYMENT & FINANCIAL DATA
-- ============================================================================

-- Clear payments
TRUNCATE TABLE payments;

-- Clear refund requests
TRUNCATE TABLE refund_requests;

-- Clear commission settlements
TRUNCATE TABLE commission_settlements;

-- Reset commission rate history (keep only the current rate)
-- Keeping the most recent commission rate entry
DELETE FROM commission_rate_history 
WHERE id NOT IN (
    SELECT id FROM (
        SELECT id FROM commission_rate_history 
        ORDER BY effective_from DESC 
        LIMIT 1
    ) AS temp
);

-- ============================================================================
-- 5. ENGAGEMENT & COMMUNICATION DATA
-- ============================================================================

-- Clear reviews
TRUNCATE TABLE reviews;

-- Clear favorites
TRUNCATE TABLE favorites;

-- Clear notifications
TRUNCATE TABLE notifications;

-- Clear chat messages
TRUNCATE TABLE chat_messages;

-- Clear conversations
TRUNCATE TABLE conversations;

-- ============================================================================
-- 6. CUSTOM REQUESTS & DEALS
-- ============================================================================

-- Clear custom meal requests
TRUNCATE TABLE custom_meal_requests;

-- Clear combo deal items
TRUNCATE TABLE combo_deal_items;

-- Clear combo deals
TRUNCATE TABLE combo_deals;

-- ============================================================================
-- 7. COOK AVAILABILITY
-- ============================================================================

-- Clear cook daily availability (optional - uncomment if needed)
-- TRUNCATE TABLE cook_daily_availability;

-- ============================================================================
-- 8. REFERRALS
-- ============================================================================

-- Clear referrals
TRUNCATE TABLE referrals;

-- ============================================================================
-- 9. RESET AUTO-INCREMENT COUNTERS
-- ============================================================================

ALTER TABLE subscriptions AUTO_INCREMENT = 1;
ALTER TABLE subscription_plans AUTO_INCREMENT = 1;
ALTER TABLE subscription_plan_items AUTO_INCREMENT = 1;
ALTER TABLE subscription_daily_log AUTO_INCREMENT = 1;
ALTER TABLE subscription_payment_events AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE refund_requests AUTO_INCREMENT = 1;
ALTER TABLE commission_settlements AUTO_INCREMENT = 1;
ALTER TABLE reviews AUTO_INCREMENT = 1;
ALTER TABLE favorites AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE conversations AUTO_INCREMENT = 1;
ALTER TABLE chat_messages AUTO_INCREMENT = 1;
ALTER TABLE custom_meal_requests AUTO_INCREMENT = 1;
ALTER TABLE combo_deals AUTO_INCREMENT = 1;
ALTER TABLE combo_deal_items AUTO_INCREMENT = 1;
ALTER TABLE referrals AUTO_INCREMENT = 1;
ALTER TABLE cart AUTO_INCREMENT = 1;
ALTER TABLE cart_items AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- VERIFICATION QUERIES (Optional - uncomment to verify)
-- ============================================================================

-- SELECT 'Subscriptions' AS table_name, COUNT(*) AS count FROM subscriptions
-- UNION ALL
-- SELECT 'Orders', COUNT(*) FROM orders
-- UNION ALL
-- SELECT 'Payments', COUNT(*) FROM payments
-- UNION ALL
-- SELECT 'Commission Settlements', COUNT(*) FROM commission_settlements
-- UNION ALL
-- SELECT 'Reviews', COUNT(*) FROM reviews
-- UNION ALL
-- SELECT 'Notifications', COUNT(*) FROM notifications
-- UNION ALL
-- SELECT 'Chat Messages', COUNT(*) FROM chat_messages
-- UNION ALL
-- SELECT 'Users', COUNT(*) FROM users
-- UNION ALL
-- SELECT 'Cook Profiles', COUNT(*) FROM cook_profiles
-- UNION ALL
-- SELECT 'Meals', COUNT(*) FROM meals;

-- ============================================================================
-- SUMMARY
-- ============================================================================
-- This script has reset:
-- ✓ All subscriptions and subscription plans
-- ✓ All orders and order items
-- ✓ All payments and refund requests
-- ✓ All commission settlements (earnings data)
-- ✓ All reviews and favorites
-- ✓ All notifications
-- ✓ All chat conversations and messages
-- ✓ All custom meal requests and combo deals
-- ✓ All cart data
-- ✓ All referrals
--
-- Preserved:
-- ✓ Users (customers, cooks, admins)
-- ✓ Cook profiles
-- ✓ Meals
-- ✓ Platform settings
-- ✓ Most recent commission rate
-- ============================================================================

SELECT 'Test data reset complete. System ready for fresh testing.' AS status;
