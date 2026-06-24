import db from "../config/db.js";
import { deleteFile, buildFileUrl } from "../utils/fileHelper.js";


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

export const uploadCookProfileImage = async (req, res) => {
    try {
        const userId = req.user.id;

        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: "No image file provided."
            });
        }

        // Get current image to delete old one
        const [users] = await db.promise().query(
            "SELECT profile_image FROM users WHERE id = ?",
            [userId]
        );

        // Delete old image if exists
        if (users[0]?.profile_image) {
            deleteFile(users[0].profile_image);
        }

        // Build public URL
        const imageUrl = buildFileUrl(req, "profiles", req.file.filename);

        // Update user profile image
        await db.promise().query(
            "UPDATE users SET profile_image = ? WHERE id = ?",
            [imageUrl, userId]
        );

        return res.status(200).json({
            success: true,
            message: "Profile image uploaded successfully.",
            image_url: imageUrl
        });

    } catch (error) {
        console.error("uploadCookProfileImage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
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

export const updateCookProfile = async (req, res) => {
    try {
        const userId = req.user.id;
        const { kitchen_name, food_type, description, capacity_per_day, bio, specialties } = req.body;

        const [profile] = await db.promise().query(
            "SELECT user_id FROM cook_profiles WHERE user_id = ?",
            [userId]
        );

        if (profile.length === 0) {
            return res.status(403).json({
                success: false,
                message: "You don't have permission to update this profile."
            });
        }

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
