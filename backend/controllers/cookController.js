import db from "../config/db.js";

// POST /api/cook/profile
// Cook sets up their profile after registering
export const setupCookProfile = async (req, res) => {
    try {
        const userId = req.user.id;
        const { kitchen_name, food_type, description, capacity_per_day } = req.body;

        if (!kitchen_name || !food_type || !description) {
            return res.status(400).json({
                success: false,
                message: "Kitchen name, food type, and description are required."
            });
        }

        // Check if cook profile exists (created at register time)
        const [existing] = await db.promise().query(
            "SELECT id FROM cook_profiles WHERE user_id = ?",
            [userId]
        );

        if (existing.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Cook profile not found. Please register again."
            });
        }

        // Update the cook profile with additional details
        await db.promise().query(
            `UPDATE cook_profiles
             SET kitchen_name = ?, food_type = ?, description = ?, capacity_per_day = ?
             WHERE user_id = ?`,
            [kitchen_name, food_type, description, capacity_per_day || 0, userId]
        );

        return res.status(200).json({
            success: true,
            message: "Cook profile set up successfully!"
        });

    } catch (error) {
        console.error("setupCookProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/cook/profile
// Cook views their own profile
export const getMyCookProfile = async (req, res) => {
    try {
        const userId = req.user.id;

        const [profiles] = await db.promise().query(
            `SELECT cp.*, u.full_name, u.email, u.phone, u.profile_image, u.address
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE cp.user_id = ?`,
            [userId]
        );

        if (profiles.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Cook profile not found."
            });
        }

        return res.status(200).json({
            success: true,
            profile: profiles[0]
        });

    } catch (error) {
        console.error("getMyCookProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/cook/profile
// Cook updates their profile
export const updateCookProfile = async (req, res) => {
    try {
        const userId = req.user.id;
        const { kitchen_name, food_type, description, capacity_per_day, bio, specialties } = req.body;

        // Build dynamic update query
        const updates = [];
        const values = [];

        if (kitchen_name !== undefined) {
            updates.push("kitchen_name = ?");
            values.push(kitchen_name);
        }
        if (food_type !== undefined) {
            updates.push("food_type = ?");
            values.push(food_type);
        }
        if (description !== undefined) {
            updates.push("description = ?");
            values.push(description);
        }
        if (capacity_per_day !== undefined) {
            updates.push("capacity_per_day = ?");
            values.push(capacity_per_day);
        }
        if (bio !== undefined) {
            updates.push("bio = ?");
            values.push(bio);
        }
        if (specialties !== undefined) {
            updates.push("specialties = ?");
            values.push(specialties);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                message: "No fields to update."
            });
        }

        values.push(userId);

        await db.promise().query(
            `UPDATE cook_profiles SET ${updates.join(", ")} WHERE user_id = ?`,
            values
        );

        return res.status(200).json({
            success: true,
            message: "Profile updated successfully."
        });

    } catch (error) {
        console.error("updateCookProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/cooks
// Public - get all approved cooks for customers to browse
export const getAllCooks = async (req, res) => {
    try {
        const [cooks] = await db.promise().query(
            `SELECT cp.*, u.full_name, u.profile_image, u.address
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE u.is_active = TRUE AND cp.is_approved = TRUE
             ORDER BY cp.rating DESC, cp.total_orders DESC`
        );

        return res.status(200).json({
            success: true,
            cooks: cooks
        });

    } catch (error) {
        console.error("getAllCooks error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/cooks/:cookId
// Public - get one cook's full profile by their user ID
export const getCookById = async (req, res) => {
    try {
        const { cookId } = req.params;

        const [cooks] = await db.promise().query(
            `SELECT cp.*, u.full_name, u.profile_image, u.address, u.email, u.phone
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE u.id = ? AND u.is_active = TRUE`,
            [cookId]
        );

        if (cooks.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Cook not found."
            });
        }

        return res.status(200).json({
            success: true,
            cook: cooks[0]
        });

    } catch (error) {
        console.error("getCookById error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
