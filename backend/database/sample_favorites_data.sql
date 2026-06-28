-- Sample Favorites Data for Testing

-- Make sure you have at least one customer user and multiple cook users
-- Assuming customer user_id = 1 (from existing data)
-- And we have multiple cooks in the system

-- Add sample favorites (customer favorites some cooks)
-- Note: Adjust the customer_id and cook_id based on your actual data
INSERT INTO favorites (customer_id, cook_id) VALUES
(1, 2),  -- Customer 1 favorites Cook 2
(1, 3)   -- Customer 1 favorites Cook 3
ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP;

-- If you need to create sample cook users (run only if needed):
/*
INSERT INTO users (name, email, password, role, phone, is_verified) VALUES
('Anita Kumar', 'anita@cook.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'cook', '9876543210', TRUE),
('Shashi Meals', 'shashi@cook.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'cook', '9876543211', TRUE),
('Rajesh Kitchen', 'rajesh@cook.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'cook', '9876543212', TRUE);

-- Add cook profiles for the above users
INSERT INTO cook_profiles (user_id, address, description, specialties, experience_years, certifications) VALUES
(2, '123 Main St, Delhi', 'Specializing in North Indian cuisine', 'North Indian,Chinese,Continental', 5, 'Food Safety,Hygiene'),
(3, '456 Park Ave, Mumbai', 'Authentic home-style cooking', 'South Indian,Maharashtrian', 3, 'Food Safety'),
(4, '789 Lake Rd, Bangalore', 'Healthy and tasty meals', 'North Indian,South Indian', 7, 'Food Safety,Nutrition');
*/

-- Verify favorites
SELECT 
    f.id,
    f.customer_id,
    c.name as customer_name,
    f.cook_id,
    ck.name as cook_name,
    f.created_at
FROM favorites f
INNER JOIN users c ON f.customer_id = c.id
INNER JOIN users ck ON f.cook_id = ck.id;
