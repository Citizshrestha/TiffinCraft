import db from "../config/db.js";
import { deleteFile, buildFileUrl } from "../utils/fileHelper.js";

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
            image_url
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

        const [result] = await db.promise().query(
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
                category || null,
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

        console.log("Meal inserted with ID:", result.insertId);

        return res.status(201).json({
            success: true,
            message: "Meal added successfully!",
            mealId: result.insertId
        });

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

        const [meals] = await db.promise().query(
            `SELECT * FROM meals
             WHERE cook_id = ?
             ORDER BY created_at DESC`,
            [cookId]
        );

        console.log("Meals found:", meals.length);
        console.log("Meals data:", JSON.stringify(meals, null, 2));

        return res.status(200).json({
            success: true,
            meals: meals
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
            `SELECT m.*, u.full_name as cook_name
             FROM meals m
             JOIN users u ON m.cook_id = u.id
             WHERE m.cook_id = ? AND m.is_available = TRUE
             ORDER BY m.created_at DESC`,
            [cookId]
        );

        return res.status(200).json({
            success: true,
            meals: meals
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

        console.log("=== uploadMealImage called ===");
        console.log("Cook ID:", cookId);
        console.log("Meal ID:", mealId);
        console.log("File:", req.file ? "Present" : "Missing");
        if (req.file) {
            console.log("File details:", {
                originalname: req.file.originalname,
                filename: req.file.filename,
                path: req.file.path,
                size: req.file.size,
                mimetype: req.file.mimetype
            });
        }

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
            console.log("❌ Meal not found or access denied");
            return res.status(403).json({
                success: false,
                message: "Meal not found or access denied."
            });
        }

        // Delete old image if exists
        if (meals[0].image_url) {
            deleteFile(meals[0].image_url);
            console.log("Deleted old image:", meals[0].image_url);
        }

        // Build public URL
        const imageUrl = buildFileUrl(req, "meals", req.file.filename);
        console.log("New image URL:", imageUrl);

        // Update meal image_url
        await db.promise().query(
            "UPDATE meals SET image_url = ? WHERE id = ?",
            [imageUrl, mealId]
        );

        console.log("✅ Meal image uploaded successfully");

        return res.status(200).json({
            success: true,
            message: "Meal image uploaded successfully.",
            image_url: imageUrl
        });

    } catch (error) {
        console.error("❌ uploadMealImage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const getAllMeals = async (req, res) => {
    try {
        const { category, cuisine_type, is_vegetarian, is_vegan, max_price } = req.query;

        let query = `SELECT m.*, u.full_name as cook_name, cp.rating as cook_rating
                     FROM meals m
                     JOIN users u ON m.cook_id = u.id
                     JOIN cook_profiles cp ON u.id = cp.user_id
                     WHERE m.is_available = TRUE AND u.is_active = TRUE`;

        const params = [];

        if (category) {
            query += ` AND m.category = ?`;
            params.push(category);
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

        query += ` ORDER BY cp.rating DESC, m.created_at DESC`;

        const [meals] = await db.promise().query(query, params);

        return res.status(200).json({
            success: true,
            meals: meals
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
            `SELECT m.*, u.full_name as cook_name, u.profile_image as cook_image,
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

        return res.status(200).json({
            success: true,
            meal: meals[0]
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
            image_url
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

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                message: "No fields to update."
            });
        }

        values.push(mealId);

        await db.promise().query(
            `UPDATE meals SET ${updates.join(", ")} WHERE id = ?`,
            values
        );

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
