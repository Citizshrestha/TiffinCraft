import db from '../config/db.js';

// Get user's favorite cooks
export const getFavorites = async (req, res) => {
    try {
        const userId = req.user.id;

        const [favorites] = await db.promise().query(
            `SELECT 
                f.id as favorite_id,
                f.created_at as favorited_at,
                u.id as cook_id,
                u.full_name as cook_name,
                u.profile_image,
                u.address,
                cp.kitchen_name,
                cp.food_type,
                cp.description,
                cp.rating,
                cp.total_orders,
                cp.is_verified
             FROM favorites f
             JOIN users u ON f.cook_id = u.id
             JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE f.customer_id = ? AND u.is_active = TRUE AND cp.is_approved = TRUE
             ORDER BY f.created_at DESC`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            count: favorites.length,
            favorites: favorites.map(fav => ({
                favoriteId: fav.favorite_id,
                cookId: fav.cook_id,
                cookName: fav.cook_name,
                kitchenName: fav.kitchen_name || fav.cook_name,
                profileImage: fav.profile_image,
                address: fav.address,
                foodType: fav.food_type,
                description: fav.description,
                rating: parseFloat(fav.rating) || 0,
                totalOrders: fav.total_orders || 0,
                isVerified: Boolean(fav.is_verified),
                favoritedAt: fav.favorited_at
            }))
        });

    } catch (error) {
        console.error('getFavorites error:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Add cook to favorites
export const addFavorite = async (req, res) => {
    try {
        const userId = req.user.id;
        const { cook_id } = req.body;

        if (!cook_id) {
            return res.status(400).json({
                success: false,
                message: 'Cook ID is required'
            });
        }

        // Check if cook exists and is active
        const [cook] = await db.promise().query(
            `SELECT u.id, u.role, cp.is_approved 
             FROM users u
             JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE u.id = ? AND u.role = 'cook' AND u.is_active = TRUE`,
            [cook_id]
        );

        if (cook.length === 0) {
            return res.status(404).json({
                success: false,
                message: 'Cook not found or not available'
            });
        }

        // Check if already favorited
        const [existing] = await db.promise().query(
            'SELECT id FROM favorites WHERE customer_id = ? AND cook_id = ?',
            [userId, cook_id]
        );

        if (existing.length > 0) {
            return res.status(400).json({
                success: false,
                message: 'Cook already in favorites'
            });
        }

        // Add to favorites
        await db.promise().query(
            'INSERT INTO favorites (customer_id, cook_id) VALUES (?, ?)',
            [userId, cook_id]
        );

        return res.status(201).json({
            success: true,
            message: 'Cook added to favorites'
        });

    } catch (error) {
        console.error('addFavorite error:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Remove cook from favorites
export const removeFavorite = async (req, res) => {
    try {
        const userId = req.user.id;
        const { cook_id } = req.params;

        const [result] = await db.promise().query(
            'DELETE FROM favorites WHERE customer_id = ? AND cook_id = ?',
            [userId, cook_id]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({
                success: false,
                message: 'Favorite not found'
            });
        }

        return res.status(200).json({
            success: true,
            message: 'Cook removed from favorites'
        });

    } catch (error) {
        console.error('removeFavorite error:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Check if cook is favorited
export const checkFavorite = async (req, res) => {
    try {
        const userId = req.user.id;
        const { cook_id } = req.params;

        const [favorite] = await db.promise().query(
            'SELECT id FROM favorites WHERE customer_id = ? AND cook_id = ?',
            [userId, cook_id]
        );

        return res.status(200).json({
            success: true,
            isFavorite: favorite.length > 0
        });

    } catch (error) {
        console.error('checkFavorite error:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};
