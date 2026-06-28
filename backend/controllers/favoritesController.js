import db from '../config/db.js';

// Get all favorites for a customer
export const getFavorites = async (req, res) => {
    try {
        const customerId = req.user.id;

        const [favorites] = await db.query(`
            SELECT 
                f.id as favorite_id,
                u.id as cook_id,
                u.name as cook_name,
                u.email as cook_email,
                u.phone as cook_phone,
                c.profile_image,
                c.address,
                c.description,
                c.specialties,
                c.experience_years,
                c.certifications,
                COALESCE(AVG(r.rating), 0) as average_rating,
                COUNT(DISTINCT r.id) as total_reviews,
                COUNT(DISTINCT m.id) as total_meals,
                f.created_at
            FROM favorites f
            INNER JOIN users u ON f.cook_id = u.id
            LEFT JOIN cook_profiles c ON u.id = c.user_id
            LEFT JOIN reviews r ON u.id = r.cook_id
            LEFT JOIN meals m ON u.id = m.cook_id AND m.is_available = TRUE
            WHERE f.customer_id = ?
            GROUP BY f.id, u.id, c.profile_image, c.address, c.description, 
                     c.specialties, c.experience_years, c.certifications, f.created_at
            ORDER BY f.created_at DESC
        `, [customerId]);

        res.json({
            success: true,
            favorites: favorites.map(fav => ({
                favoriteId: fav.favorite_id,
                cookId: fav.cook_id,
                cookName: fav.cook_name,
                profileImage: fav.profile_image || null,
                address: fav.address,
                description: fav.description,
                specialties: fav.specialties ? fav.specialties.split(',') : [],
                experienceYears: fav.experience_years,
                certifications: fav.certifications ? fav.certifications.split(',') : [],
                averageRating: parseFloat(fav.average_rating.toFixed(1)),
                totalReviews: fav.total_reviews,
                totalMeals: fav.total_meals,
                addedAt: fav.created_at
            }))
        });

    } catch (error) {
        console.error('Get favorites error:', error);
        res.status(500).json({
            success: false,
            message: 'Error fetching favorites',
            error: error.message
        });
    }
};

// Add a cook to favorites
export const addToFavorites = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { cookId } = req.body;

        if (!cookId) {
            return res.status(400).json({
                success: false,
                message: 'Cook ID is required'
            });
        }

        // Check if cook exists and is a cook
        const [cook] = await db.query(
            'SELECT id, role FROM users WHERE id = ? AND role = "cook"',
            [cookId]
        );

        if (cook.length === 0) {
            return res.status(404).json({
                success: false,
                message: 'Cook not found'
            });
        }

        // Check if already in favorites
        const [existing] = await db.query(
            'SELECT id FROM favorites WHERE customer_id = ? AND cook_id = ?',
            [customerId, cookId]
        );

        if (existing.length > 0) {
            return res.status(400).json({
                success: false,
                message: 'Cook is already in your favorites'
            });
        }

        // Add to favorites
        await db.query(
            'INSERT INTO favorites (customer_id, cook_id) VALUES (?, ?)',
            [customerId, cookId]
        );

        res.json({
            success: true,
            message: 'Cook added to favorites successfully'
        });

    } catch (error) {
        console.error('Add to favorites error:', error);
        res.status(500).json({
            success: false,
            message: 'Error adding to favorites',
            error: error.message
        });
    }
};

// Remove a cook from favorites
export const removeFromFavorites = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { cookId } = req.params;

        const [result] = await db.query(
            'DELETE FROM favorites WHERE customer_id = ? AND cook_id = ?',
            [customerId, cookId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({
                success: false,
                message: 'Favorite not found'
            });
        }

        res.json({
            success: true,
            message: 'Cook removed from favorites successfully'
        });

    } catch (error) {
        console.error('Remove from favorites error:', error);
        res.status(500).json({
            success: false,
            message: 'Error removing from favorites',
            error: error.message
        });
    }
};

// Check if a cook is in favorites
export const checkFavoriteStatus = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { cookId } = req.params;

        const [favorite] = await db.query(
            'SELECT id FROM favorites WHERE customer_id = ? AND cook_id = ?',
            [customerId, cookId]
        );

        res.json({
            success: true,
            isFavorite: favorite.length > 0
        });

    } catch (error) {
        console.error('Check favorite status error:', error);
        res.status(500).json({
            success: false,
            message: 'Error checking favorite status',
            error: error.message
        });
    }
};
