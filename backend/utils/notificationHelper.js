import db from '../config/db.js';

/**
 * Notification Helper
 * Centralized utility for creating notifications across the app
 */

/**
 * Create a notification for a user
 * @param {number} userId - User ID to send notification to
 * @param {string} title - Notification title
 * @param {string} message - Notification message
 * @param {string} type - Notification type (new_order, order_status, review, system)
 * @param {number} referenceId - Optional reference ID (order_id, review_id, etc)
 * @param {string} referenceType - Optional reference type (order, review, meal, etc)
 */
export const createNotification = async (userId, title, message, type, referenceId = null, referenceType = null) => {
    try {
        await db.promise().query(
            `INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [userId, title, message, type, referenceId, referenceType]
        );
        console.log(`✅ Notification created for user ${userId}: ${title}`);
        return { success: true };
    } catch (error) {
        console.error('❌ Error creating notification:', error);
        return { success: false, error: error.message };
    }
};

/**
 * Create notification for a new order (sent to cook)
 */
export const notifyNewOrder = async (cookId, orderId, customerName, totalAmount) => {
    return createNotification(
        cookId,
        'New Order Received! 🎉',
        `${customerName} placed an order worth ₹${totalAmount}`,
        'new_order',
        orderId,
        'order'
    );
};

/**
 * Create notification for order status update (sent to customer)
 */
export const notifyOrderStatusUpdate = async (customerId, orderId, status, cookName) => {
    const statusMessages = {
        confirmed: `${cookName} confirmed your order`,
        preparing: `${cookName} is preparing your order`,
        ready: 'Your order is ready for pickup/delivery',
        delivered: 'Your order has been delivered. Enjoy!',
        cancelled: 'Your order has been cancelled'
    };

    return createNotification(
        customerId,
        'Order Status Updated',
        statusMessages[status] || `Your order status: ${status}`,
        'order_status',
        orderId,
        'order'
    );
};

/**
 * Create notification for order cancellation (sent to cook)
 */
export const notifyOrderCancelled = async (cookId, orderId, customerName) => {
    return createNotification(
        cookId,
        'Order Cancelled',
        `${customerName} cancelled order #${orderId}`,
        'order_status',
        orderId,
        'order'
    );
};

/**
 * Create notification for new review (sent to cook)
 */
export const notifyNewReview = async (cookId, reviewId, customerName, rating) => {
    const stars = '⭐'.repeat(rating);
    return createNotification(
        cookId,
        'New Review Received!',
        `${customerName} gave you ${rating} stars ${stars}`,
        'review',
        reviewId,
        'review'
    );
};

/**
 * Create notification for cook reply to review (sent to customer)
 */
export const notifyReviewReply = async (customerId, reviewId, cookName) => {
    return createNotification(
        customerId,
        'Cook Replied to Your Review',
        `${cookName} replied to your review`,
        'review',
        reviewId,
        'review'
    );
};

/**
 * Mark all notifications as read for a user
 */
export const markAllAsRead = async (userId) => {
    try {
        await db.promise().query(
            'UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE',
            [userId]
        );
        return { success: true };
    } catch (error) {
        console.error('Error marking all as read:', error);
        return { success: false, error: error.message };
    }
};

/**
 * Delete old read notifications (cleanup utility)
 * @param {number} daysOld - Delete notifications older than this many days
 */
export const cleanupOldNotifications = async (daysOld = 30) => {
    try {
        const [result] = await db.promise().query(
            `DELETE FROM notifications
             WHERE is_read = TRUE
             AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)`,
            [daysOld]
        );
        console.log(`🧹 Cleaned up ${result.affectedRows} old notifications`);
        return { success: true, deletedCount: result.affectedRows };
    } catch (error) {
        console.error('Error cleaning up notifications:', error);
        return { success: false, error: error.message };
    }
};

export default {
    createNotification,
    notifyNewOrder,
    notifyOrderStatusUpdate,
    notifyOrderCancelled,
    notifyNewReview,
    notifyReviewReply,
    markAllAsRead,
    cleanupOldNotifications
};
