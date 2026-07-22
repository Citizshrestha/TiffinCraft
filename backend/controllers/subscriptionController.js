import db from "../config/db.js";

/**
 * POST /api/subscriptions
 * Customer creates a subscription to a meal
 * Body: { cook_id, meal_id, frequency, delivery_address, start_date }
 */
export const createSubscription = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { cook_id, meal_id, frequency, delivery_address, start_date } = req.body;

        if (!cook_id || !meal_id || !frequency || !delivery_address || !start_date) {
            return res.status(400).json({
                success: false,
                message: "cook_id, meal_id, frequency, delivery_address, and start_date are required."
            });
        }

        if (!['weekly', 'monthly'].includes(frequency)) {
            return res.status(400).json({
                success: false,
                message: "frequency must be 'weekly' or 'monthly'."
            });
        }

        // Calculate next_delivery_date based on frequency
        const interval = frequency === 'weekly' ? 7 : 30;
        const startDate = new Date(start_date);
        const nextDelivery = new Date(startDate);
        nextDelivery.setDate(nextDelivery.getDate() + interval);

        const [result] = await db.promise().query(
            `INSERT INTO subscriptions (customer_id, cook_id, meal_id, frequency, delivery_address, start_date, next_delivery_date)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [customerId, cook_id, meal_id, frequency, delivery_address, start_date, nextDelivery.toISOString().split('T')[0]]
        );

        return res.status(201).json({
            success: true,
            message: "Subscription created successfully.",
            subscriptionId: result.insertId
        });

    } catch (error) {
        console.error("createSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /api/subscriptions/customer/my
 * Customer views their active subscriptions
 */
export const getMySubscriptions = async (req, res) => {
    try {
        const customerId = req.user.id;

        const [subscriptions] = await db.promise().query(
            `SELECT s.*, m.name as meal_name, m.price, m.image_url as meal_image,
                    u.full_name as cook_name, cp.kitchen_name
             FROM subscriptions s
             JOIN meals m ON s.meal_id = m.id
             JOIN users u ON s.cook_id = u.id
             JOIN cook_profiles cp ON s.cook_id = cp.user_id
             WHERE s.customer_id = ?
             ORDER BY s.created_at DESC`,
            [customerId]
        );

        return res.status(200).json({
            success: true,
            subscriptions
        });

    } catch (error) {
        console.error("getMySubscriptions error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /api/subscriptions/cook/my
 * Cook views their subscribers
 */
export const getCookSubscribers = async (req, res) => {
    try {
        const cookId = req.user.id;

        const [subscriptions] = await db.promise().query(
            `SELECT s.*, m.name as meal_name, m.price, u.full_name as customer_name,
                    u.phone as customer_phone
             FROM subscriptions s
             JOIN meals m ON s.meal_id = m.id
             JOIN users u ON s.customer_id = u.id
             WHERE s.cook_id = ?
             ORDER BY s.created_at DESC`,
            [cookId]
        );

        const [[{ count }]] = await db.promise().query(
            "SELECT COUNT(*) as count FROM subscriptions WHERE cook_id = ? AND status = 'active'",
            [cookId]
        );

        return res.status(200).json({
            success: true,
            activeSubscriberCount: count,
            subscriptions
        });

    } catch (error) {
        console.error("getCookSubscribers error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/pause
 * Pause a subscription
 */
export const pauseSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        // Verify ownership
        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        await db.promise().query(
            "UPDATE subscriptions SET status = 'paused' WHERE id = ?",
            [id]
        );

        return res.status(200).json({
            success: true,
            message: "Subscription paused successfully."
        });

    } catch (error) {
        console.error("pauseSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/resume
 * Resume a paused subscription
 */
export const resumeSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        // Recalculate next_delivery_date from today
        const sub = subs[0];
        const interval = sub.frequency === 'weekly' ? 7 : 30;
        const nextDelivery = new Date();
        nextDelivery.setDate(nextDelivery.getDate() + interval);

        await db.promise().query(
            "UPDATE subscriptions SET status = 'active', next_delivery_date = ? WHERE id = ?",
            [nextDelivery.toISOString().split('T')[0], id]
        );

        return res.status(200).json({
            success: true,
            message: "Subscription resumed successfully."
        });

    } catch (error) {
        console.error("resumeSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/skip
 * Customer skips a specific delivery date
 * Body: { date }
 */
export const skipDay = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { date } = req.body;

        if (!date) {
            return res.status(400).json({
                success: false,
                message: "date is required."
            });
        }

        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND customer_id = ?",
            [id, customerId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        // Append date to skipped_dates JSON array
        const sub = subs[0];
        const skipped = sub.skipped_dates ? JSON.parse(sub.skipped_dates) : [];
        skipped.push(date);
        const updatedSkipped = JSON.stringify(skipped);

        await db.promise().query(
            "UPDATE subscriptions SET skipped_dates = ? WHERE id = ?",
            [updatedSkipped, id]
        );

        return res.status(200).json({
            success: true,
            message: "Delivery date skipped successfully."
        });

    } catch (error) {
        console.error("skipDay error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * DELETE /api/subscriptions/:id
 * Cancel a subscription
 */
export const cancelSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        await db.promise().query(
            "UPDATE subscriptions SET status = 'cancelled' WHERE id = ?",
            [id]
        );

        return res.status(200).json({
            success: true,
            message: "Subscription cancelled successfully."
        });

    } catch (error) {
        console.error("cancelSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
