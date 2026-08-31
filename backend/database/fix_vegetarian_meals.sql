-- Fix vegetarian field for non-vegetarian meals
-- Update meals that contain meat/chicken/fish keywords to is_vegetarian = FALSE

UPDATE meals 
SET is_vegetarian = FALSE, is_vegan = FALSE
WHERE is_vegetarian = TRUE 
AND (
    LOWER(name) LIKE '%chicken%' OR
    LOWER(name) LIKE '%mutton%' OR
    LOWER(name) LIKE '%lamb%' OR
    LOWER(name) LIKE '%beef%' OR
    LOWER(name) LIKE '%pork%' OR
    LOWER(name) LIKE '%fish%' OR
    LOWER(name) LIKE '%prawn%' OR
    LOWER(name) LIKE '%shrimp%' OR
    LOWER(name) LIKE '%crab%' OR
    LOWER(name) LIKE '%meat%' OR
    LOWER(name) LIKE '%egg%' OR
    LOWER(description) LIKE '%chicken%' OR
    LOWER(description) LIKE '%mutton%' OR
    LOWER(description) LIKE '%lamb%' OR
    LOWER(description) LIKE '%beef%' OR
    LOWER(description) LIKE '%pork%' OR
    LOWER(description) LIKE '%fish%' OR
    LOWER(description) LIKE '%prawn%' OR
    LOWER(description) LIKE '%shrimp%' OR
    LOWER(description) LIKE '%crab%' OR
    LOWER(description) LIKE '%meat%' OR
    LOWER(description) LIKE '%egg%'
);

-- Show affected meals
SELECT id, name, description, is_vegetarian, is_vegan 
FROM meals 
WHERE is_vegetarian = FALSE
ORDER BY id;
