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

        // Convert DECIMAL rating to real number
        const formattedProfile = {
            ...profiles[0],
            rating: parseFloat(profiles[0].rating)
        };

        return res.status(200).json({
            success: true,
            profile: formattedProfile
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

        // Convert DECIMAL rating to real number
        const formattedCooks = cooks.map(cook => ({
            ...cook,
            rating: parseFloat(cook.rating)
        }));

        return res.status(200).json({
            success: true,
            cooks: formattedCooks
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

        // Convert DECIMAL rating to real number
        const formattedCook = {
            ...cooks[0],
            rating: parseFloat(cooks[0].rating)
        };

        return res.status(200).json({
            success: true,
            cook: formattedCook
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

        // Convert DECIMAL rating to real number
        const formattedProfile = {
            ...profiles[0],
            rating: parseFloat(profiles[0].rating)
        };

        return res.status(200).json({
            success: true,
            message: "Profile updated successfully",
            profile: formattedProfile
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

// PUT /api/cook/profile/holiday-mode
// Toggle holiday mode for cook
export const updateHolidayMode = async (req, res) => {
    try {
        const userId = req.user.id;
        const { is_holiday_mode } = req.body;

        if (is_holiday_mode === undefined) {
            return res.status(400).json({
                success: false,
                message: "is_holiday_mode field is required"
            });
        }

        await db.promise().query(
            'UPDATE cook_profiles SET is_holiday_mode = ? WHERE user_id = ?',
            [is_holiday_mode, userId]
        );

        return res.status(200).json({
            success: true,
            message: `Holiday mode ${is_holiday_mode ? 'enabled' : 'disabled'} successfully`,
            is_holiday_mode
        });

    } catch (error) {
        console.error("updateHolidayMode error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};

// PUT /api/cook/profile/operating-hours
// Update cook's operating hours
export const updateOperatingHours = async (req, res) => {
    try {
        const userId = req.user.id;
        const { operating_hours } = req.body;

        if (!operating_hours) {
            return res.status(400).json({
                success: false,
                message: "operating_hours is required"
            });
        }

        // Validate JSON structure (should have days of week)
        const validDays = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
        const isValid = Object.keys(operating_hours).every(day => validDays.includes(day.toLowerCase()));

        if (!isValid) {
            return res.status(400).json({
                success: false,
                message: "Invalid operating_hours format. Must contain valid days of week."
            });
        }

        await db.promise().query(
            'UPDATE cook_profiles SET operating_hours = ? WHERE user_id = ?',
            [JSON.stringify(operating_hours), userId]
        );

        return res.status(200).json({
            success: true,
            message: "Operating hours updated successfully",
            operating_hours
        });

    } catch (error) {
        console.error("updateOperatingHours error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};

// PUT /api/cook/profile/bank-details
// Update cook's bank details for payouts
export const updateBankDetails = async (req, res) => {
    try {
        const userId = req.user.id;
        const { bank_details } = req.body;

        if (!bank_details) {
            return res.status(400).json({
                success: false,
                message: "bank_details is required"
            });
        }

        // Validate required fields
        const requiredFields = ['accountHolder', 'accountNumber', 'ifsc', 'bankName'];
        const missingFields = requiredFields.filter(field => !bank_details[field]);

        if (missingFields.length > 0) {
            return res.status(400).json({
                success: false,
                message: `Missing required fields: ${missingFields.join(', ')}`
            });
        }

        await db.promise().query(
            'UPDATE cook_profiles SET bank_details = ? WHERE user_id = ?',
            [JSON.stringify(bank_details), userId]
        );

        return res.status(200).json({
            success: true,
            message: "Bank details updated successfully"
        });

    } catch (error) {
        console.error("updateBankDetails error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};
