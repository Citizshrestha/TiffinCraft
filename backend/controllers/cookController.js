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

export const getCookDashboard = async (req, res) => {
    try {
        const cookId = req.user.id;

        // Get today's orders count
        const [[todayOrders]] = await db.promise().query(
            `SELECT COUNT(*) as count
             FROM orders
             WHERE cook_id = ? AND DATE(created_at) = CURDATE()`,
            [cookId]
        );

        // Get yesterday's orders count for percentage calculation
        const [[yesterdayOrders]] = await db.promise().query(
            `SELECT COUNT(*) as count
             FROM orders
             WHERE cook_id = ? AND DATE(created_at) = CURDATE() - INTERVAL 1 DAY`,
            [cookId]
        );

        // Calculate today's orders percentage change
        const todayOrdersCount = todayOrders.count || 0;
        const yesterdayOrdersCount = yesterdayOrders.count || 0;
        let todayOrdersChange = 0;
        if (yesterdayOrdersCount > 0) {
            todayOrdersChange = ((todayOrdersCount - yesterdayOrdersCount) / yesterdayOrdersCount) * 100;
        } else if (todayOrdersCount > 0) {
            todayOrdersChange = 100;
        }

        // Get today's earnings (exclude cancelled orders)
        const [[todayEarnings]] = await db.promise().query(
            `SELECT COALESCE(SUM(total_amount), 0) as amount
             FROM orders
             WHERE cook_id = ?
             AND DATE(created_at) = CURDATE()
             AND status != 'cancelled'`,
            [cookId]
        );

        // Get yesterday's earnings for percentage calculation
        const [[yesterdayEarnings]] = await db.promise().query(
            `SELECT COALESCE(SUM(total_amount), 0) as amount
             FROM orders
             WHERE cook_id = ?
             AND DATE(created_at) = CURDATE() - INTERVAL 1 DAY
             AND status != 'cancelled'`,
            [cookId]
        );

        // Calculate earnings percentage change
        const todayEarningsAmount = parseFloat(todayEarnings.amount) || 0;
        const yesterdayEarningsAmount = parseFloat(yesterdayEarnings.amount) || 0;
        let todayEarningsChange = 0;
        if (yesterdayEarningsAmount > 0) {
            todayEarningsChange = ((todayEarningsAmount - yesterdayEarningsAmount) / yesterdayEarningsAmount) * 100;
        } else if (todayEarningsAmount > 0) {
            todayEarningsChange = 100;
        }

        // Get active orders (not delivered or cancelled) - substitute for subscriptions
        const [[activeOrders]] = await db.promise().query(
            `SELECT COUNT(*) as count
             FROM orders
             WHERE cook_id = ?
             AND status NOT IN ('delivered', 'cancelled')`,
            [cookId]
        );

        // Get last week's active orders for comparison
        const [[lastWeekActiveOrders]] = await db.promise().query(
            `SELECT COUNT(*) as count
             FROM orders
             WHERE cook_id = ?
             AND DATE(created_at) >= CURDATE() - INTERVAL 7 DAY
             AND DATE(created_at) < CURDATE()
             AND status NOT IN ('delivered', 'cancelled')`,
            [cookId]
        );

        // Calculate active orders percentage change
        const activeOrdersCount = activeOrders.count || 0;
        const lastWeekActiveOrdersCount = lastWeekActiveOrders.count || 0;
        let activeOrdersChange = 0;
        if (lastWeekActiveOrdersCount > 0) {
            activeOrdersChange = ((activeOrdersCount - lastWeekActiveOrdersCount) / lastWeekActiveOrdersCount) * 100;
        } else if (activeOrdersCount > 0) {
            activeOrdersChange = 100;
        }

        // Get average rating
        const [[ratingData]] = await db.promise().query(
            `SELECT
                COALESCE(AVG(rating), 0) as avg_rating,
                COUNT(*) as review_count
             FROM reviews
             WHERE cook_id = ?`,
            [cookId]
        );

        // Get last week's average rating for comparison
        const [[lastWeekRating]] = await db.promise().query(
            `SELECT COALESCE(AVG(rating), 0) as avg_rating
             FROM reviews
             WHERE cook_id = ?
             AND DATE(created_at) >= CURDATE() - INTERVAL 7 DAY
             AND DATE(created_at) < CURDATE()`,
            [cookId]
        );

        const avgRating = parseFloat(ratingData.avg_rating) || 0;
        const lastWeekAvgRating = parseFloat(lastWeekRating.avg_rating) || 0;
        let ratingChange = 0;
        if (lastWeekAvgRating > 0) {
            ratingChange = avgRating - lastWeekAvgRating;
        } else if (avgRating > 0) {
            ratingChange = avgRating;
        }

        return res.status(200).json({
            success: true,
            dashboard: {
                today_orders: {
                    count: todayOrdersCount,
                    change_percentage: Math.round(todayOrdersChange * 10) / 10,
                    vs_label: "vs yesterday"
                },
                today_earnings: {
                    amount: todayEarningsAmount,
                    change_percentage: Math.round(todayEarningsChange * 10) / 10,
                    vs_label: "vs yesterday"
                },
                active_orders: {
                    count: activeOrdersCount,
                    change_percentage: Math.round(activeOrdersChange * 10) / 10,
                    vs_label: "vs last week"
                },
                average_rating: {
                    rating: Math.round(avgRating * 10) / 10,
                    change_value: Math.round(ratingChange * 10) / 10,
                    review_count: ratingData.review_count || 0,
                    vs_label: "based on " + (ratingData.review_count || 0) + " reviews"
                }
            }
        });

    } catch (error) {
        console.error("getCookDashboard error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// Update cook's complete profile (user data + cook profile data)
export const updateCookCompleteProfile = async (req, res) => {
    try {
        const userId = req.user.id;
        const {
            full_name,
            phone,
            address,
            latitude,
            longitude,
            kitchen_name,
            food_type,
            description,
            capacity_per_day,
            bio,
            specialties
        } = req.body;

        // Update user table fields
        const userUpdates = [];
        const userValues = [];

        if (full_name !== undefined) {
            userUpdates.push("full_name = ?");
            userValues.push(full_name);
        }
        if (phone !== undefined) {
            // Check if phone is already used by another user
            const [existingPhone] = await db.promise().query(
                "SELECT id FROM users WHERE phone = ? AND id != ?",
                [phone, userId]
            );
            if (existingPhone.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: "Phone number already in use"
                });
            }
            userUpdates.push("phone = ?");
            userValues.push(phone);
        }
        if (address !== undefined) {
            userUpdates.push("address = ?");
            userValues.push(address);
        }
        if (latitude !== undefined) {
            userUpdates.push("latitude = ?");
            userValues.push(latitude);
        }
        if (longitude !== undefined) {
            userUpdates.push("longitude = ?");
            userValues.push(longitude);
        }

        if (userUpdates.length > 0) {
            userValues.push(userId);
            await db.promise().query(
                `UPDATE users SET ${userUpdates.join(", ")} WHERE id = ?`,
                userValues
            );
        }

        // Update cook_profiles table fields
        const cookUpdates = [];
        const cookValues = [];

        if (kitchen_name !== undefined) {
            cookUpdates.push("kitchen_name = ?");
            cookValues.push(kitchen_name);
        }
        if (food_type !== undefined) {
            cookUpdates.push("food_type = ?");
            cookValues.push(food_type);
        }
        if (description !== undefined) {
            cookUpdates.push("description = ?");
            cookValues.push(description);
        }
        if (capacity_per_day !== undefined) {
            cookUpdates.push("capacity_per_day = ?");
            cookValues.push(capacity_per_day);
        }
        if (bio !== undefined) {
            cookUpdates.push("bio = ?");
            cookValues.push(bio);
        }
        if (specialties !== undefined) {
            cookUpdates.push("specialties = ?");
            cookValues.push(specialties);
        }

        if (cookUpdates.length > 0) {
            cookValues.push(userId);
            await db.promise().query(
                `UPDATE cook_profiles SET ${cookUpdates.join(", ")} WHERE user_id = ?`,
                cookValues
            );
        }

        if (userUpdates.length === 0 && cookUpdates.length === 0) {
            return res.status(400).json({
                success: false,
                message: "No fields to update"
            });
        }

        // Get updated profile data
        const [profiles] = await db.promise().query(
            `SELECT cp.*, u.full_name, u.email, u.phone, u.profile_image, u.address, u.latitude, u.longitude
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE cp.user_id = ?`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            message: "Profile updated successfully",
            profile: profiles[0]
        });

    } catch (error) {
        console.error("updateCookCompleteProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};
