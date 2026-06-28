import db from "../config/db.js";

export const getCustomerDashboard = async (req, res) => {
    try {
        const userId = req.user.id;

        // Get user info
        const [users] = await db.promise().query(
            `SELECT id, full_name, profile_image FROM users WHERE id = ?`,
            [userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                message: "User not found."
            });
        }

        // Get statistics
        const [orderStats] = await db.promise().query(
            `SELECT COUNT(*) as total_orders FROM orders WHERE customer_id = ?`,
            [userId]
        );

        const [activeOrdersCount] = await db.promise().query(
            `SELECT COUNT(*) as active_orders 
             FROM orders 
             WHERE customer_id = ? AND status IN ('pending', 'accepted', 'preparing', 'ready')`,
            [userId]
        );

        const [cartCount] = await db.promise().query(
            `SELECT COALESCE(SUM(quantity), 0) as cart_items 
             FROM cart 
             WHERE customer_id = ?`,
            [userId]
        );

        const [favoriteCount] = await db.promise().query(
            `SELECT COUNT(*) as favorite_count 
             FROM favorites 
             WHERE customer_id = ?`,
            [userId]
        );

        // Get active orders with details
        const [activeOrders] = await db.promise().query(
            `SELECT o.id as order_id, u.full_name as cook_name, o.status, 
                    o.total_amount, cp.kitchen_name
             FROM orders o
             JOIN users u ON o.cook_id = u.id
             LEFT JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE o.customer_id = ? AND o.status IN ('pending', 'accepted', 'preparing', 'ready')
             ORDER BY o.created_at DESC
             LIMIT 5`,
            [userId]
        );

        // Get popular cooks (highly rated, approved)
        const [popularCooks] = await db.promise().query(
            `SELECT u.id as cook_id, u.full_name as cook_name, 
                    cp.kitchen_name, u.profile_image, 
                    COALESCE(cp.rating, 0) as rating, 
                    cp.total_orders as review_count,
                    30 as avg_delivery_time,
                    2.5 as distance,
                    TRUE as is_open
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE cp.is_approved = TRUE AND u.is_active = TRUE
             ORDER BY cp.rating DESC, cp.total_orders DESC
             LIMIT 10`,
            []
        );

        // Get recent notifications
        const [notifications] = await db.promise().query(
            `SELECT id, title, message, type, is_read, order_id, created_at
             FROM notifications
             WHERE user_id = ?
             ORDER BY created_at DESC
             LIMIT 10`,
            [userId]
        );

        const [unreadCount] = await db.promise().query(
            `SELECT COUNT(*) as unread_count 
             FROM notifications 
             WHERE user_id = ? AND is_read = FALSE`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            data: {
                user: {
                    id: users[0].id,
                    fullName: users[0].full_name,
                    profileImage: users[0].profile_image
                },
                stats: {
                    activeOrders: activeOrdersCount[0]?.active_orders || 0,
                    cartItemsCount: parseInt(cartCount[0]?.cart_items || 0),
                    favoriteCount: favoriteCount[0]?.favorite_count || 0,
                    totalOrders: orderStats[0]?.total_orders || 0
                },
                activeOrders: activeOrders.map(order => ({
                    orderId: order.order_id,
                    cookName: order.kitchen_name || order.cook_name,
                    status: order.status,
                    estimatedTime: "30 mins",
                    totalAmount: order.total_amount
                })),
                popularCooks: popularCooks.map(cook => ({
                    cookId: cook.cook_id,
                    cookName: cook.cook_name,
                    kitchenName: cook.kitchen_name || cook.cook_name,
                    profileImage: cook.profile_image,
                    rating: parseFloat(cook.rating) || 0,
                    reviewCount: cook.review_count || 0,
                    avgDeliveryTime: cook.avg_delivery_time,
                    distance: cook.distance,
                    isOpen: Boolean(cook.is_open)
                })),
                notifications: notifications,
                unreadNotificationsCount: unreadCount[0]?.unread_count || 0
            }
        });

    } catch (error) {
        console.error("getCustomerDashboard error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const getNotifications = async (req, res) => {
    try {
        const userId = req.user.id;

        const [notifications] = await db.promise().query(
            `SELECT id, title, message, type, is_read, order_id, created_at
             FROM notifications
             WHERE user_id = ?
             ORDER BY created_at DESC`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            notifications: notifications
        });

    } catch (error) {
        console.error("getNotifications error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const markNotificationAsRead = async (req, res) => {
    try {
        const userId = req.user.id;
        const notificationId = req.params.id;

        await db.promise().query(
            `UPDATE notifications 
             SET is_read = TRUE 
             WHERE id = ? AND user_id = ?`,
            [notificationId, userId]
        );

        return res.status(200).json({
            success: true,
            message: "Notification marked as read."
        });

    } catch (error) {
        console.error("markNotificationAsRead error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

export const markAllNotificationsAsRead = async (req, res) => {
    try {
        const userId = req.user.id;

        await db.promise().query(
            `UPDATE notifications 
             SET is_read = TRUE 
             WHERE user_id = ? AND is_read = FALSE`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            message: "All notifications marked as read."
        });

    } catch (error) {
        console.error("markAllNotificationsAsRead error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
