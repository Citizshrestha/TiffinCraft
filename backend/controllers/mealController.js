import db from "../config/db.js";
import { uploadToCloudinary, deleteFromCloudinary, extractPublicId } from "../services/uploadService.js";
import { legacyCategorySlugs, rankMealDiscovery } from "../utils/mealDiscoveryRanking.js";

const normalizeCategorySlugs = (value) => {
    const raw = Array.isArray(value) ? value : (typeof value === "string" ? value.split(",") : []);
    return [...new Set(raw.map(v => String(v).trim().toLowerCase()).filter(Boolean))];
};

async function resolveCategoryRows(executor, value) {
    const slugs = normalizeCategorySlugs(value);
    if (slugs.length === 0) return { slugs, rows: [], invalid: [] };
    const [rows] = await executor.query(
        `SELECT id, slug, name FROM meal_categories
         WHERE is_active = TRUE AND slug IN (${slugs.map(() => "?").join(",")})`,
        slugs
    );
    const found = new Set(rows.map(row => row.slug));
    return { slugs, rows, invalid: slugs.filter(slug => !found.has(slug)) };
}

async function replaceMealCategories(executor, mealId, categoryRows) {
    await executor.query("DELETE FROM meal_category_map WHERE meal_id = ?", [mealId]);
    for (const category of categoryRows) {
        await executor.query(
            "INSERT INTO meal_category_map (meal_id, category_id) VALUES (?, ?)",
            [mealId, category.id]
        );
    }
}

// The subscription-plan-items migration is deployed separately from the API
// on some environments. Keep meal endpoints compatible while that migration
// is pending, but use the soft-delete flag whenever the column is available.
async function hasSubscriptionItemActiveColumn() {
    const [rows] = await db.promise().query(
        `SELECT COUNT(*) AS column_count
         FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'subscription_plan_items'
           AND COLUMN_NAME = 'is_active'`
    );
    return Number(rows[0]?.column_count || 0) > 0;
}

async function attachMealCategories(meals) {
    if (!Array.isArray(meals) || meals.length === 0) return meals;
    const mealIds = meals.map(meal => meal.id);
    const [rows] = await db.promise().query(
        `SELECT mcm.meal_id, mc.slug
         FROM meal_category_map mcm
         JOIN meal_categories mc ON mc.id = mcm.category_id
         WHERE mc.is_active = TRUE
           AND mcm.meal_id IN (${mealIds.map(() => "?").join(",")})
         ORDER BY mc.sort_order ASC, mc.id ASC`,
        mealIds
    );
    const byMeal = new Map(mealIds.map(id => [id, []]));
    for (const row of rows) {
        if (byMeal.has(row.meal_id)) byMeal.get(row.meal_id).push(row.slug);
    }
    return meals.map(meal => ({ ...meal, category_slugs: byMeal.get(meal.id) || [] }));
}

export const addMeal = async (req, res) => {
    try {
        const cookId = req.user.id;
        const {
            name,
            description,
            price,
            category,
            cuisine_type,
            is_available,
            preparation_time,
            spice_level,
            is_vegetarian,
            is_vegan,
            allergens,
            image_url,
            categories
        } = req.body;

        console.log("=== addMeal called ===");
        console.log("Cook ID:", cookId);
        console.log("Request body:", JSON.stringify(req.body, null, 2));

        if (!name || !price) {
            return res.status(400).json({
                success: false,
                message: "Meal name and price are required."
            });
        }

        if (price <= 0) {
            return res.status(400).json({
                success: false,
                message: "Price must be greater than zero."
            });
        }

        const categorySelection = await resolveCategoryRows(db.promise(), categories);
        if (categories !== undefined && categorySelection.slugs.length === 0) {
            return res.status(400).json({ success: false, message: "Select at least one meal category." });
        }
        if (categorySelection.invalid.length > 0) {
            return res.status(400).json({
                success: false,
                message: `Unknown meal categories: ${categorySelection.invalid.join(", ")}`
            });
        }

        const legacyCategory = category || (categorySelection.rows[0] ? categorySelection.rows[0].name : null);
        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();

        const [result] = await connection.query(
            `INSERT INTO meals (
                cook_id, name, description, price, category, cuisine_type,
                is_available, preparation_time, spice_level, is_vegetarian,
                is_vegan, allergens, image_url
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [
                cookId,
                name,
                description || null,
                price,
                legacyCategory,
                cuisine_type || null,
                is_available !== undefined ? is_available : true,
                preparation_time || null,
                spice_level || 'medium',
                is_vegetarian || false,
                is_vegan || false,
                allergens || null,
                image_url || null
            ]
        );

        if (categories !== undefined) {
            await replaceMealCategories(connection, result.insertId, categorySelection.rows);
        }

        await connection.commit();

        console.log("Meal inserted with ID:", result.insertId);

        return res.status(201).json({
            success: true,
            message: "Meal added successfully!",
            mealId: result.insertId
        });
        } catch (error) {
            await connection.rollback();
            throw error;
        } finally {
            connection.release();
        }

    } catch (error) {
        console.error("addMeal error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const getMyMeals = async (req, res) => {
    try {
        const cookId = req.user.id;

        console.log("=== getMyMeals called ===");
        console.log("Cook ID:", cookId);
        console.log("User info:", req.user);

        const hasItemActiveColumn = await hasSubscriptionItemActiveColumn();
        const itemActivePredicate = hasItemActiveColumn ? "AND spi.is_active = TRUE" : "";

        // is_in_subscription is derived from: meals.in_subscription flag
        // (set by "Add to Subscription") OR active plan membership
        const [meals] = await db.promise().query(
            `SELECT m.id, m.cook_id, m.name, m.description, m.price, m.category,
                    m.cuisine_type, m.is_available, m.preparation_time, m.spice_level,
                    m.is_vegetarian, m.is_vegan, m.allergens, m.image_url,
                    m.created_at, m.updated_at,
                    (
                        m.in_subscription = TRUE
                        OR EXISTS(
                            SELECT 1 FROM subscription_plan_items spi
                            JOIN subscription_plans sp ON spi.plan_id = sp.id
                            WHERE spi.meal_id = m.id
                            ${itemActivePredicate}
                            AND sp.cook_id = ?
                            AND sp.is_active = TRUE
                        )
                    ) AS is_in_subscription
             FROM meals m
             WHERE m.cook_id = ?
             ORDER BY m.created_at DESC`,
            [cookId, cookId]
        );

        console.log("Meals found:", meals.length);
        console.log("Meals data:", JSON.stringify(meals, null, 2));

        // Convert DECIMAL price to real number and boolean is_in_subscription
        const formattedMeals = await attachMealCategories(meals.map(meal => ({
            ...meal,
            price: parseFloat(meal.price),
            is_in_subscription: !!meal.is_in_subscription
        })));

        return res.status(200).json({
            success: true,
            meals: formattedMeals
        });

    } catch (error) {
        console.error("getMyMeals error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const getMealsByCook = async (req, res) => {
    try {
        const { cookId } = req.params;

        const [meals] = await db.promise().query(
            `SELECT m.id, m.cook_id, m.name, m.description, m.price, m.category,
                    m.cuisine_type, m.is_available, m.preparation_time, m.spice_level,
                    m.is_vegetarian, m.is_vegan, m.allergens, m.image_url,
                    m.created_at, m.updated_at,
                    u.full_name as cook_name
             FROM meals m
             JOIN users u ON m.cook_id = u.id
             WHERE m.cook_id = ? AND m.is_available = TRUE
             ORDER BY m.created_at DESC`,
            [cookId]
        );

        // Convert DECIMAL price to real number
        const formattedMeals = await attachMealCategories(meals.map(meal => ({
            ...meal,
            price: parseFloat(meal.price)
        })));

        return res.status(200).json({
            success: true,
            meals: formattedMeals
        });

    } catch (error) {
        console.error("getMealsByCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const uploadMealImage = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { mealId } = req.params;

        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: "No image file provided."
            });
        }

        // Check if meal exists and belongs to this cook
        const [meals] = await db.promise().query(
            "SELECT id, image_url FROM meals WHERE id = ? AND cook_id = ?",
            [mealId, cookId]
        );

        if (meals.length === 0) {
            return res.status(403).json({
                success: false,
                message: "Meal not found or access denied."
            });
        }

        // Delete old Cloudinary image if it exists
        if (meals[0].image_url) {
            const oldPublicId = extractPublicId(meals[0].image_url);
            if (oldPublicId) {
                await deleteFromCloudinary(oldPublicId).catch(() => {});
            }
        }

        // Build folder: tiffincraft/meals/<cookId>/
        const folder = `tiffincraft/meals/${cookId}`;

        // Upload to Cloudinary
        const result = await uploadToCloudinary(req.file.buffer, folder, {
            transformation: [
                { width: 800, height: 600, crop: 'fill', gravity: 'auto' },
                { quality: 'auto:good' },
                { fetch_format: 'auto' },
            ],
        });

        const imageUrl = result.secure_url;

        // Update meal image_url
        await db.promise().query(
            "UPDATE meals SET image_url = ? WHERE id = ?",
            [imageUrl, mealId]
        );

        return res.status(200).json({
            success: true,
            message: "Meal image uploaded successfully.",
            image_url: imageUrl
        });

    } catch (error) {
        console.error("uploadMealImage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// Customer home discovery: real demand drives Popular, while order-category
// history and favorite cooks drive Recommended. Scores are computed server-side
// so every client sees the same ordering and cannot manufacture popularity.
export const getMealDiscovery = async (req, res) => {
    try {
        const customerId = req.user.id;
        const hasCurrentLocation = req.query.lat !== undefined || req.query.lng !== undefined;
        const currentLatitude = parseFloat(req.query.lat);
        const currentLongitude = parseFloat(req.query.lng);
        if (hasCurrentLocation && (Number.isNaN(currentLatitude) || Number.isNaN(currentLongitude)
                || currentLatitude < -90 || currentLatitude > 90
                || currentLongitude < -180 || currentLongitude > 180)) {
            return res.status(400).json({ success: false, message: "lat and lng must be valid coordinates." });
        }
        const [customerResult, mealResult, preferenceResult, affinityResult, favoriteResult] = await Promise.all([
            db.promise().query(
                "SELECT latitude, longitude FROM users WHERE id = ? LIMIT 1",
                [customerId]
            ),
            db.promise().query(
                `SELECT m.id, m.cook_id, m.name, m.description, m.price, m.category,
                        m.cuisine_type, m.is_available, m.preparation_time, m.spice_level,
                        m.is_vegetarian, m.is_vegan, m.allergens, m.image_url,
                        m.created_at, m.updated_at,
                        u.full_name AS cook_name, u.profile_image AS cook_image,
                        u.latitude, u.longitude, cp.kitchen_name,
                        CASE WHEN review_stats.review_count > 0
                             THEN review_stats.average_rating ELSE cp.rating END AS cook_rating,
                        COALESCE(review_stats.review_count, 0) AS cook_review_count,
                        COALESCE(order_stats.completed_order_units, 0) AS completed_order_units,
                        COALESCE(order_stats.recent_order_units, 0) AS recent_order_units,
                        COALESCE(favorite_stats.cook_favorite_count, 0) AS cook_favorite_count
                 FROM meals m
                 JOIN users u ON u.id = m.cook_id
                 JOIN cook_profiles cp ON cp.user_id = m.cook_id
                 LEFT JOIN (
                    SELECT oi.meal_id,
                           SUM(CASE WHEN o.status IN ('delivered', 'completed')
                                    THEN oi.quantity ELSE 0 END) AS completed_order_units,
                           SUM(CASE WHEN o.status IN ('delivered', 'completed')
                                         AND COALESCE(o.delivered_at, o.updated_at, o.created_at)
                                             >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)
                                    THEN oi.quantity ELSE 0 END) AS recent_order_units
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    GROUP BY oi.meal_id
                 ) order_stats ON order_stats.meal_id = m.id
                 LEFT JOIN (
                    SELECT cook_id, COUNT(*) AS review_count, AVG(rating) AS average_rating
                    FROM reviews
                    GROUP BY cook_id
                 ) review_stats ON review_stats.cook_id = m.cook_id
                 LEFT JOIN (
                    SELECT cook_id, COUNT(*) AS cook_favorite_count
                    FROM favorites
                    GROUP BY cook_id
                 ) favorite_stats ON favorite_stats.cook_id = m.cook_id
                 WHERE m.is_available = TRUE
                   AND u.is_active = TRUE
                   AND cp.is_approved = TRUE
                   AND COALESCE(cp.is_holiday_mode, FALSE) = FALSE`
            ),
            db.promise().query(
                `SELECT mc.slug, m.category, m.cuisine_type, SUM(oi.quantity) AS units
                 FROM orders o
                 JOIN order_items oi ON oi.order_id = o.id
                 JOIN meals m ON m.id = oi.meal_id
                 LEFT JOIN meal_category_map mcm ON mcm.meal_id = m.id
                 LEFT JOIN meal_categories mc ON mc.id = mcm.category_id AND mc.is_active = TRUE
                 WHERE o.customer_id = ? AND o.status IN ('delivered', 'completed')
                 GROUP BY mc.slug, m.category, m.cuisine_type`,
                [customerId]
            ),
            db.promise().query(
                `SELECT cook_id, COUNT(*) AS orders_count
                 FROM orders
                 WHERE customer_id = ? AND status IN ('delivered', 'completed')
                 GROUP BY cook_id`,
                [customerId]
            ),
            db.promise().query(
                "SELECT cook_id FROM favorites WHERE customer_id = ?",
                [customerId]
            )
        ]);

        const customer = customerResult[0][0] || {};
        const meals = await attachMealCategories(mealResult[0].map(meal => ({
            ...meal,
            price: parseFloat(meal.price),
            cook_rating: parseFloat(meal.cook_rating) || 0,
            cook_review_count: Number(meal.cook_review_count) || 0,
            completed_order_units: Number(meal.completed_order_units) || 0,
            recent_order_units: Number(meal.recent_order_units) || 0,
            cook_favorite_count: Number(meal.cook_favorite_count) || 0
        })));

        const categoryPreferences = {};
        for (const row of preferenceResult[0]) {
            const slugs = row.slug
                ? [row.slug]
                : legacyCategorySlugs(row.category, row.cuisine_type);
            for (const slug of slugs) {
                categoryPreferences[slug] = (categoryPreferences[slug] || 0) + Number(row.units || 0);
            }
        }
        const cookAffinity = Object.fromEntries(
            affinityResult[0].map(row => [row.cook_id, Number(row.orders_count) || 0])
        );
        const favoriteCookIds = favoriteResult[0].map(row => Number(row.cook_id));
        const ranked = rankMealDiscovery(meals, {
            latitude: hasCurrentLocation ? currentLatitude : customer.latitude,
            longitude: hasCurrentLocation ? currentLongitude : customer.longitude,
            categoryPreferences,
            cookAffinity,
            favoriteCookIds
        });

        return res.status(200).json({
            success: true,
            personalized: ranked.personalized,
            location_applied: ranked.locationApplied,
            popular: ranked.popular,
            recommended: ranked.recommended
        });
    } catch (error) {
        console.error("getMealDiscovery error:", error);
        return res.status(500).json({
            success: false,
            message: "Unable to load meal recommendations"
        });
    }
};

export const getAllMeals = async (req, res) => {
    try {
        const { category, categories, cuisine_type, is_vegetarian, is_vegan, max_price, search, sort, lat, lng, radius_km } = req.query;
        const hasLocation = lat !== undefined || lng !== undefined;
        const nearbyOnly = radius_km !== undefined;
        const latitude = parseFloat(lat);
        const longitude = parseFloat(lng);
        let radiusKm = radius_km !== undefined ? parseFloat(radius_km) : 10;

        if (hasLocation && (Number.isNaN(latitude) || Number.isNaN(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            return res.status(400).json({ success: false, message: "lat and lng must be valid coordinates." });
        }
        if (nearbyOnly && (Number.isNaN(radiusKm) || radiusKm <= 0)) {
            return res.status(400).json({ success: false, message: "radius_km must be a positive number." });
        }
        radiusKm = Math.min(radiusKm, 20);

        const distanceSelect = hasLocation
            ? `, (6371 * ACOS(LEAST(1, GREATEST(-1,
                    COS(RADIANS(?)) * COS(RADIANS(u.latitude)) *
                    COS(RADIANS(u.longitude) - RADIANS(?)) +
                    SIN(RADIANS(?)) * SIN(RADIANS(u.latitude))
                )))) AS distance_km`
            : "";
        const params = hasLocation ? [latitude, longitude, latitude] : [];

        let query = `SELECT m.id, m.cook_id, m.name, m.description, m.price, m.category,
                            m.cuisine_type, m.is_available, m.preparation_time, m.spice_level,
                            m.is_vegetarian, m.is_vegan, m.allergens, m.image_url,
                            m.created_at, m.updated_at,
                            u.full_name as cook_name, u.profile_image as cook_image,
                            cp.rating as cook_rating, cp.kitchen_name as kitchen_name${distanceSelect}
                     FROM meals m
                     JOIN users u ON m.cook_id = u.id
                     JOIN cook_profiles cp ON u.id = cp.user_id
                     WHERE m.is_available = TRUE AND u.is_active = TRUE`;

        if (hasLocation) {
            query += " AND u.latitude IS NOT NULL AND u.longitude IS NOT NULL";
        }

        if (category) {
            query += ` AND m.category = ?`;
            params.push(category);
        }

        const categorySlugs = normalizeCategorySlugs(categories);
        if (categorySlugs.length > 0) {
            query += ` AND EXISTS (
                SELECT 1
                FROM meal_category_map category_map
                JOIN meal_categories category_def ON category_def.id = category_map.category_id
                WHERE category_map.meal_id = m.id
                  AND category_def.is_active = TRUE
                  AND category_def.slug IN (${categorySlugs.map(() => "?").join(",")})
            )`;
            params.push(...categorySlugs);
        }

        if (cuisine_type) {
            query += ` AND m.cuisine_type = ?`;
            params.push(cuisine_type);
        }

        if (is_vegetarian === 'true') {
            query += ` AND m.is_vegetarian = TRUE`;
        }

        if (is_vegan === 'true') {
            query += ` AND m.is_vegan = TRUE`;
        }

        if (max_price) {
            query += ` AND m.price <= ?`;
            params.push(parseFloat(max_price));
        }

        if (search) {
            query += ` AND (m.name LIKE CONCAT('%', ?, '%') OR m.description LIKE CONCAT('%', ?, '%') OR u.full_name LIKE CONCAT('%', ?, '%') OR cp.kitchen_name LIKE CONCAT('%', ?, '%'))`;
            params.push(search, search, search, search);
        }

        if (nearbyOnly) {
            query += " HAVING distance_km <= ?";
            params.push(radiusKm);
        }

        if (hasLocation) {
            query += ` ORDER BY distance_km ASC, cp.rating DESC, m.created_at DESC`;
        } else if (sort === 'price_asc') {
            query += ` ORDER BY m.price ASC`;
        } else if (sort === 'price_desc') {
            query += ` ORDER BY m.price DESC`;
        } else if (sort === 'rating') {
            query += ` ORDER BY cp.rating DESC`;
        } else {
            query += ` ORDER BY cp.rating DESC, m.created_at DESC`;
        }

        const [meals] = await db.promise().query(query, params);

        // Convert DECIMAL price and cook_rating to real numbers
        const formattedMeals = await attachMealCategories(meals.map(meal => ({
            ...meal,
            price: parseFloat(meal.price),
            cook_rating: meal.cook_rating ? parseFloat(meal.cook_rating) : null,
            distance_km: meal.distance_km == null ? null : Math.round(parseFloat(meal.distance_km) * 100) / 100
        })));

        return res.status(200).json({
            success: true,
            meals: formattedMeals
        });

    } catch (error) {
        console.error("getAllMeals error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/meals/:mealId
// Public - get single meal details
export const getMealById = async (req, res) => {
    try {
        const { mealId } = req.params;

        const [meals] = await db.promise().query(
            `SELECT m.id, m.cook_id, m.name, m.description, m.price, m.category,
                    m.cuisine_type, m.is_available, m.preparation_time, m.spice_level,
                    m.is_vegetarian, m.is_vegan, m.allergens, m.image_url,
                    m.created_at, m.updated_at,
                    u.full_name as cook_name, u.profile_image as cook_image,
                    cp.rating as cook_rating, cp.total_orders as cook_total_orders
             FROM meals m
             JOIN users u ON m.cook_id = u.id
             JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE m.id = ?`,
            [mealId]
        );

        if (meals.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Meal not found."
            });
        }

        // Convert DECIMAL price and cook_rating to real numbers
        const [formattedMeal] = await attachMealCategories([{
            ...meals[0],
            price: parseFloat(meals[0].price),
            cook_rating: meals[0].cook_rating ? parseFloat(meals[0].cook_rating) : null
        }]);

        return res.status(200).json({
            success: true,
            meal: formattedMeal
        });

    } catch (error) {
        console.error("getMealById error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/meals/:mealId
// Cook updates their meal
export const updateMeal = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { mealId } = req.params;
        const {
            name,
            description,
            price,
            category,
            cuisine_type,
            is_available,
            preparation_time,
            spice_level,
            is_vegetarian,
            is_vegan,
            allergens,
            image_url,
            categories
        } = req.body;

        // Check if meal exists and belongs to this cook
        const [ownerCheck] = await db.promise().query(
            "SELECT id FROM meals WHERE id = ? AND cook_id = ?",
            [mealId, cookId]
        );

        if (ownerCheck.length === 0) {
            return res.status(403).json({
                success: false,
                message: "Meal not found or you don't have permission."
            });
        }


        const categorySelection = await resolveCategoryRows(db.promise(), categories);
        if (categories !== undefined && categorySelection.slugs.length === 0) {
            return res.status(400).json({ success: false, message: "Select at least one meal category." });
        }
        if (categorySelection.invalid.length > 0) {
            return res.status(400).json({
                success: false,
                message: `Unknown meal categories: ${categorySelection.invalid.join(", ")}`
            });
        }

        // Build dynamic update query
        const updates = [];
        const values = [];

        if (name !== undefined) {
            updates.push("name = ?");
            values.push(name);
        }
        if (description !== undefined) {
            updates.push("description = ?");
            values.push(description);
        }
        if (price !== undefined) {
            if (price <= 0) {
                return res.status(400).json({
                    success: false,
                    message: "Price must be greater than zero."
                });
            }
            updates.push("price = ?");
            values.push(price);
        }
        if (category !== undefined) {
            updates.push("category = ?");
            values.push(category);
        } else if (categories !== undefined && categorySelection.rows[0]) {
            // Keep the legacy single-value column meaningful for older clients.
            updates.push("category = ?");
            values.push(categorySelection.rows[0].name);
        }
        if (cuisine_type !== undefined) {
            updates.push("cuisine_type = ?");
            values.push(cuisine_type);
        }
        if (is_available !== undefined) {
            updates.push("is_available = ?");
            values.push(is_available);
        }
        if (preparation_time !== undefined) {
            updates.push("preparation_time = ?");
            values.push(preparation_time);
        }
        if (spice_level !== undefined) {
            updates.push("spice_level = ?");
            values.push(spice_level);
        }
        if (is_vegetarian !== undefined) {
            updates.push("is_vegetarian = ?");
            values.push(is_vegetarian);
        }
        if (is_vegan !== undefined) {
            updates.push("is_vegan = ?");
            values.push(is_vegan);
        }
        if (allergens !== undefined) {
            updates.push("allergens = ?");
            values.push(allergens);
        }
        if (image_url !== undefined) {
            updates.push("image_url = ?");
            values.push(image_url);
        }

        if (updates.length === 0 && categories === undefined) {
            return res.status(400).json({
                success: false,
                message: "No fields to update."
            });
        }

        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();
            if (updates.length > 0) {
                values.push(mealId);
                await connection.query(
                    `UPDATE meals SET ${updates.join(", ")} WHERE id = ?`,
                    values
                );
            }
            if (categories !== undefined) {
                await replaceMealCategories(connection, mealId, categorySelection.rows);
            }
            await connection.commit();
        } catch (error) {
            await connection.rollback();
            throw error;
        } finally {
            connection.release();
        }

        return res.status(200).json({
            success: true,
            message: "Meal updated successfully."
        });

    } catch (error) {
        console.error("updateMeal error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// DELETE /api/meals/:mealId
// Cook deletes their meal
export const deleteMeal = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { mealId } = req.params;

        // Check if meal exists and belongs to this cook
        const [ownerCheck] = await db.promise().query(
            "SELECT id FROM meals WHERE id = ? AND cook_id = ?",
            [mealId, cookId]
        );

        if (ownerCheck.length === 0) {
            return res.status(403).json({
                success: false,
                message: "Meal not found or you don't have permission."
            });
        }

        await db.promise().query(
            "DELETE FROM meals WHERE id = ?",
            [mealId]
        );

        return res.status(200).json({
            success: true,
            message: "Meal deleted successfully."
        });

    } catch (error) {
        console.error("deleteMeal error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};


export const addMealToSubscription = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { mealId } = req.params;

        console.log("=== addMealToSubscription called ===");
        console.log("Cook ID:", cookId);
        console.log("Meal ID:", mealId);

        // Verify the meal belongs to this cook
        const [meals] = await db.promise().query(
            `SELECT id FROM meals WHERE id = ? AND cook_id = ?`,
            [mealId, cookId]
        );

        if (meals.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Meal not found or you don't have permission to modify it."
            });
        }

        // Update the meal to mark it as available for subscriptions
        await db.promise().query(
            `UPDATE meals SET in_subscription = true WHERE id = ?`,
            [mealId]
        );

        // Re-enable the preserved item rows created when this meal was
        // temporarily removed from subscription availability. This restores
        // each plan's original quantity instead of merely changing the meal
        // flag and leaving the plan short one item.
        let restored = { affectedRows: 0 };
        if (await hasSubscriptionItemActiveColumn()) {
            [restored] = await db.promise().query(
                `UPDATE subscription_plan_items spi
                 JOIN subscription_plans sp ON spi.plan_id = sp.id
                 SET spi.is_active = TRUE
                 WHERE spi.meal_id = ? AND sp.cook_id = ? AND spi.is_active = FALSE`,
                [mealId, cookId]
            );
        }

        res.json({
            success: true,
            message: restored.affectedRows > 0
                ? "Meal added back to subscription availability and restored in its plans"
                : "Meal successfully added to subscription availability",
            restored_plan_items: restored.affectedRows
        });
    } catch (error) {
        console.error("Error adding meal to subscription:", error);
        res.status(500).json({
            success: false,
            message: "Server error while adding meal to subscription",
            error: error.message
        });
    }
};
