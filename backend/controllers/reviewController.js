import db from "../config/db.js";

export const addReview = async (req, res) => {
    const connection = await db.promise().getConnection();

    try {
        const customerId = req.user.id;
        const { order_id, cook_id, rating, comment } = req.body;

        if (!order_id || !cook_id || !rating) {
            return res.status(400).json({
                success: false,
                message: "order_id, cook_id and rating are required."
            });
        }

        if (rating < 1 || rating > 5) {
            return res.status(400).json({
                success: false,
                message: "Rating must be between 1 and 5."
            });
        }

        const [orders] = await connection.query(
            `SELECT * FROM orders
             WHERE id = ? AND customer_id = ? AND status = 'delivered'`,
            [order_id, customerId]
        );

        if (orders.length === 0) {
            return res.status(400).json({
                success: false,
                message: "Order not found or not delivered yet."
            });
        }

        const [existing] = await connection.query(
            "SELECT id FROM reviews WHERE order_id = ? AND customer_id = ?",
            [order_id, customerId]
        );

        if (existing.length > 0) {
            return res.status(400).json({
                success: false,
                message: "You have already reviewed this order."
            });
        }

        await connection.beginTransaction();

        await connection.query(
            `INSERT INTO reviews (order_id, customer_id, cook_id, rating, comment)
             VALUES (?, ?, ?, ?, ?)`,
            [order_id, customerId, cook_id, rating, comment || null]
        );

        const [ratingResult] = await connection.query(
            `SELECT AVG(rating) as avg_rating
             FROM reviews WHERE cook_id = ?`,
            [cook_id]
        );

        // cook_profiles has no total_reviews column — review counts are computed
        // live via COUNT(*) subqueries elsewhere (see getNearbyCooks/getAllCooks).
        await connection.query(
            `UPDATE cook_profiles
             SET rating = ?
             WHERE user_id = ?`,
            [
                // mysql2 returns AVG() as a string, not a number — .toFixed() crashed here.
                parseFloat(ratingResult[0].avg_rating).toFixed(2),
                cook_id
            ]
        );

        await connection.commit();

        return res.status(201).json({
            success: true,
            message: "Review submitted successfully."
        });

    } catch (error) {
        await connection.rollback();
        console.error("addReview error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    } finally {
        connection.release();
    }
};

// PUT /api/reviews/:reviewId — customer edits their own review
export const updateReview = async (req, res) => {
    const connection = await db.promise().getConnection();

    try {
        const customerId = req.user.id;
        const { reviewId } = req.params;
        const { rating, comment } = req.body;

        if (!rating || rating < 1 || rating > 5) {
            return res.status(400).json({
                success: false,
                message: "Rating must be between 1 and 5."
            });
        }

        const [reviews] = await connection.query(
            "SELECT id, cook_id FROM reviews WHERE id = ? AND customer_id = ?",
            [reviewId, customerId]
        );

        if (reviews.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Review not found."
            });
        }

        const cookId = reviews[0].cook_id;

        await connection.beginTransaction();

        await connection.query(
            "UPDATE reviews SET rating = ?, comment = ?, updated_at = NOW() WHERE id = ?",
            [rating, comment || null, reviewId]
        );

        const [ratingResult] = await connection.query(
            "SELECT AVG(rating) as avg_rating FROM reviews WHERE cook_id = ?",
            [cookId]
        );

        await connection.query(
            "UPDATE cook_profiles SET rating = ? WHERE user_id = ?",
            [parseFloat(ratingResult[0].avg_rating).toFixed(2), cookId]
        );

        await connection.commit();

        return res.status(200).json({
            success: true,
            message: "Review updated successfully."
        });

    } catch (error) {
        await connection.rollback();
        console.error("updateReview error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    } finally {
        connection.release();
    }
};

// DELETE /api/reviews/:reviewId — customer deletes their own review
export const deleteReview = async (req, res) => {
    const connection = await db.promise().getConnection();

    try {
        const customerId = req.user.id;
        const { reviewId } = req.params;

        const [reviews] = await connection.query(
            "SELECT id, cook_id FROM reviews WHERE id = ? AND customer_id = ?",
            [reviewId, customerId]
        );

        if (reviews.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Review not found."
            });
        }

        const cookId = reviews[0].cook_id;

        await connection.beginTransaction();

        await connection.query("DELETE FROM reviews WHERE id = ?", [reviewId]);

        const [ratingResult] = await connection.query(
            "SELECT AVG(rating) as avg_rating FROM reviews WHERE cook_id = ?",
            [cookId]
        );
        // No reviews left for this cook — AVG() returns NULL, reset to 0 instead of "NaN".
        const newRating = ratingResult[0].avg_rating !== null
            ? parseFloat(ratingResult[0].avg_rating).toFixed(2) : 0;

        await connection.query(
            "UPDATE cook_profiles SET rating = ? WHERE user_id = ?",
            [newRating, cookId]
        );

        await connection.commit();

        return res.status(200).json({
            success: true,
            message: "Review deleted successfully."
        });

    } catch (error) {
        await connection.rollback();
        console.error("deleteReview error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    } finally {
        connection.release();
    }
};

export const getCookReviews = async (req, res) => {
    try {
        const { cookId } = req.params;

        const [reviews] = await db.promise().query(
            `SELECT r.*, u.full_name as customer_name,
                    u.profile_image as customer_image
             FROM reviews r
             JOIN users u ON r.customer_id = u.id
             WHERE r.cook_id = ?
             ORDER BY r.created_at DESC`,
            [cookId]
        );

        // Get rating summary
        const [summary] = await db.promise().query(
            `SELECT 
                AVG(rating) as average_rating,
                COUNT(*) as total_reviews,
                SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END) as five_star,
                SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END) as four_star,
                SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END) as three_star,
                SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END) as two_star,
                SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END) as one_star
             FROM reviews WHERE cook_id = ?`,
            [cookId]
        );

        return res.status(200).json({
            success: true,
            summary: summary[0],
            reviews
        });

    } catch (error) {
        console.error("getCookReviews error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const getMyReviews = async (req, res) => {
    try {
        const customerId = req.user.id;

        const [reviews] = await db.promise().query(
            `SELECT r.*, u.full_name as cook_name,
                    cp.kitchen_name
             FROM reviews r
             JOIN users u ON r.cook_id = u.id
             JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE r.customer_id = ?
             ORDER BY r.created_at DESC`,
            [customerId]
        );

        return res.status(200).json({
            success: true,
            reviews
        });

    } catch (error) {
        console.error("getMyReviews error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/reviews/cook/my
// Cook views all reviews they received
export const getMyCookReviews = async (req, res) => {
    try {
        const cookId = req.user.id;

        // ponytail: no meal_id column on the live reviews table (schema drift
        // from complete_schema.sql) — dropped the meals join rather than
        // migrating; ReviewAdapter already null-guards getMealName().
        const [reviews] = await db.promise().query(
            `SELECT r.*,
                    u.full_name as customer_name,
                    u.profile_image as customer_image
             FROM reviews r
             JOIN users u ON r.customer_id = u.id
             WHERE r.cook_id = ?
             ORDER BY r.created_at DESC`,
            [cookId]
        );

        // Get rating summary
        const [summary] = await db.promise().query(
            `SELECT
                AVG(rating) as average_rating,
                COUNT(*) as total_reviews,
                SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END) as five_star,
                SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END) as four_star,
                SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END) as three_star,
                SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END) as two_star,
                SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END) as one_star
             FROM reviews WHERE cook_id = ?`,
            [cookId]
        );

        return res.status(200).json({
            success: true,
            summary: summary[0],
            reviews
        });

    } catch (error) {
        console.error("getMyCookReviews error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/reviews/:reviewId/reply
// Cook replies to a review
export const replyToReview = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { reviewId } = req.params;
        const { cook_reply } = req.body;

        if (!cook_reply || cook_reply.trim() === '') {
            return res.status(400).json({
                success: false,
                message: "Reply text is required"
            });
        }

        // Check if review exists and belongs to this cook
        const [reviews] = await db.promise().query(
            'SELECT id, cook_id FROM reviews WHERE id = ?',
            [reviewId]
        );

        if (reviews.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Review not found"
            });
        }

        if (reviews[0].cook_id !== cookId) {
            return res.status(403).json({
                success: false,
                message: "You can only reply to your own reviews"
            });
        }

        await db.promise().query(
            'UPDATE reviews SET cook_reply = ?, cook_reply_at = NOW() WHERE id = ?',
            [cook_reply.trim(), reviewId]
        );

        return res.status(200).json({
            success: true,
            message: "Reply posted successfully"
        });

    } catch (error) {
        console.error("replyToReview error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};
