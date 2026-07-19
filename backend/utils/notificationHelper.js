import db from '../config/db.js';
import { sendPush } from '../config/firebaseAdmin.js';

/**
 * Notification Helper
 * Centralized utility for creating notifications across the app
 */

/**
 * Fetch a user's FCM token so push notifications can be sent alongside in-app ones.
 * @param {number} userId
 * @returns {Promise<string|null>}
 */
const getFcmToken = async (userId) => {
    try {
        const [rows] = await db.promise().query(
            "SELECT fcm_token FROM users WHERE id = ?",
            [userId]
        );
        return rows.length > 0 ? rows[0].fcm_token : null;
    } catch {
        return null;
    }
};

/**
 * Create a notification for a user
 * @param {number} userId - User ID to send notification to
 * @param {string} title - Notification title
 * @param {string} message - Notification message
 * @param {string} type - Notification type (new_order, order_status, review, system)
 * @param {number} referenceId - Optional reference ID (order_id, review_id, etc)
 * @param {string} referenceType - Optional reference type (order, review, meal, etc)
 * @param {object} extra - Optional extra payload { pushData, orderId }
 */
export const createNotification = async (userId, title, message, type, referenceId = null, referenceType = null, extra = {}) => {
    try {
        await db.promise().query(
            `INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [userId, title, message, type, referenceId, referenceType]
        );
        console.log(`✅ Notification created for user ${userId}: ${title}`);

        // Also send an FCM push if the user has a device token
        const pushData = extra.pushData || {};
        const fcmToken = await getFcmToken(userId);
        if (fcmToken) {
            // Don't await — fire and forget so in-app notification creation is never blocked
            sendPush(fcmToken, title, message, pushData).then(result => {
                if (result.success) {
                    console.log(`📲 Push sent to user ${userId}: ${title}`);
                }
            }).catch(err => {
                console.error(`❌ Push failed for user ${userId}:`, err.message);
            });
        }

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
        'order',
        {
            pushData: {
                type: 'new_order',
                orderId: String(orderId),
                customerName: customerName || '',
                totalAmount: String(totalAmount)
            }
        }
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
        'order',
        {
            pushData: {
                type: 'order_status',
                orderId: String(orderId),
                status: status || ''
            }
        }
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
        'order',
        {
            pushData: {
                type: 'order_cancelled',
                orderId: String(orderId),
                customerName: customerName || ''
            }
        }
    );
};

/**
 * Create notification when a cook cancels an order (sent to customer)
 */
export const notifyOrderCancelledToCustomer = async (customerId, orderId, reason, cookName) => {
    const message = reason
        ? `${cookName} cancelled order #${orderId}. Reason: ${reason}`
        : `${cookName} cancelled order #${orderId}`;
    return createNotification(
        customerId,
        'Order Cancelled by Cook',
        message,
        'order_status',
        orderId,
        'order',
        {
            pushData: {
                type: 'order_cancelled_by_cook',
                orderId: String(orderId)
            }
        }
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
        'review',
        {
            pushData: {
                type: 'new_review',
                reviewId: String(reviewId)
            }
        }
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
        'review',
        {
            pushData: {
                type: 'review_reply',
                reviewId: String(reviewId)
            }
        }
    );
};

/**
 * Create a notification for cook approval/rejection by admin (sent to cook).
 * Reuses createNotification which now also sends FCM push.
 */
export const notifyCookApprovalUpdate = async (cookId, approved) => {
    const title = approved ? 'Kitchen Approved 🎉' : 'Application Update';
    const message = approved
        ? 'Congratulations! Your kitchen has been approved. You can now start receiving orders. 🚀'
        : 'Your TiffinCraft kitchen application needs changes. Please check your profile for details.';

    return createNotification(
        cookId,
        title,
        message,
        approved ? 'cook_approved' : 'cook_rejected',
        cookId,
        'cook_profile',
        {
            pushData: {
                type: 'cook_approval',
                approved: approved ? 'true' : 'false'
            }
        }
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
    notifyCookApprovalUpdate,
    markAllAsRead,
    cleanupOldNotifications
};
