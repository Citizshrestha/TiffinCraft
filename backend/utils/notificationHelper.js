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
        console.log(`🔍 Fetching FCM token for user ${userId}...`);
        const fcmToken = await getFcmToken(userId);
        if (fcmToken) {
            console.log(`✅ FCM token found for user ${userId}: ${fcmToken.substring(0, 20)}...`);
            // Don't await — fire and forget so in-app notification creation is never blocked
            sendPush(fcmToken, title, message, pushData).then(result => {
                if (result.success) {
                    console.log(`📲 Push sent to user ${userId}: ${title}`);
                } else {
                    console.log(`❌ Push failed for user ${userId}: ${result.error || 'Unknown error'}`);
                }
            }).catch(err => {
                console.error(`❌ Push failed for user ${userId}:`, err.message);
            });
        } else {
            console.log(`⚠️ No FCM token found for user ${userId} - push notification skipped`);
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
 * Create notification for a new combo order (sent to cook).
 * Uses type 'new_order' so the Android NotificationAdapter detects the word
 * "combo" in the message and renders the purple ic_offers badge instead of the
 * standard orange order icon.
 */
export const notifyNewComboOrder = async (cookId, orderId, customerName, comboName, totalAmount, mealSummary) => {
    const message = `${customerName} requested combo "${comboName}" (${mealSummary}) — ₹${Number(totalAmount).toFixed(0)}`;
    return createNotification(
        cookId,
        'New Combo Order Received! 🍱',
        message,
        'new_order',
        orderId,
        'order',
        {
            pushData: {
                type: 'new_order',
                orderId: String(orderId),
                customerName: customerName || '',
                totalAmount: String(totalAmount),
                meals: mealSummary || '',
                isCombo: 'true'
            }
        }
    );
};

/**
 * Create notification for order status update (sent to customer)
 * Enhanced with detailed, concise messages for each status
 */
export const notifyOrderStatusUpdate = async (customerId, orderId, status, cookName) => {
    const statusConfig = {
        pending: {
            title: 'Order Placed Successfully 📋',
            message: `Your order #${orderId} has been placed. Waiting for ${cookName} to confirm.`
        },
        confirmed: {
            title: 'Order Confirmed ✅',
            message: `Great news! ${cookName} confirmed your order #${orderId}. They'll start preparing it soon.`
        },
        preparing: {
            title: 'Order Being Prepared 👨‍🍳',
            message: `${cookName} is now preparing your delicious meal. It'll be ready soon!`
        },
        ready: {
            title: 'Order Ready for Pickup! 🎉',
            message: `Your order #${orderId} is ready! You can pick it up from ${cookName} now.`
        },
        delivered: {
            title: 'Order Delivered 🚚',
            message: `Your order #${orderId} has been delivered. Enjoy your meal! Don't forget to leave a review.`
        },
        cancelled: {
            title: 'Order Cancelled ❌',
            message: `Your order #${orderId} has been cancelled by ${cookName}.`
        }
    };

    const config = statusConfig[status] || {
        title: 'Order Update',
        message: `Your order #${orderId} status: ${status}`
    };

    return createNotification(
        customerId,
        config.title,
        config.message,
        'order_status',
        orderId,
        'order',
        {
            pushData: {
                type: 'order_status',
                orderId: String(orderId),
                status: status || '',
                cookName: cookName || '',
                title: config.title,
                message: config.message
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
 * Create notification for a confirmed eSewa payment (sent to cook — the
 * customer already sees it live via the payment-result screen).
 */
export const notifyPaymentConfirmed = async (cookId, orderId, customerName, amount) => {
    return createNotification(
        cookId,
        'Payment Received 💳',
        `${customerName} paid ₹${Number(amount).toFixed(0)} via eSewa for Order #${orderId}`,
        'payment_success',
        orderId,
        'order',
        {
            pushData: {
                type: 'payment_success',
                orderId: String(orderId)
            }
        }
    );
};

/**
 * Create notification for a new refund request (sent to admin).
 */
export const notifyRefundRequested = async (adminId, refundRequestId, orderId, requesterName) => {
    return createNotification(
        adminId,
        'Refund Requested',
        `${requesterName} requested a refund for Order #${orderId}`,
        'refund_requested',
        refundRequestId,
        'refund_request',
        {}
    );
};

/** Human-readable labels for refund_requests.reason — used in the cook-facing notification below. */
const REFUND_REASON_LABELS = {
    failed_delivery: 'Failed delivery',
    cook_mistake: 'Order mistake',
    customer_cancelled: 'Customer cancelled',
    other: 'Other issue'
};

/**
 * Create notification for a new refund request (sent to the COOK) so they
 * see what went wrong and can correct it — separate from
 * notifyRefundRequested (admin) since the cook doesn't process refunds,
 * they just need the feedback. Only call this when a CUSTOMER filed the
 * request; a cook filing their own refund shouldn't be told about it.
 */
export const notifyRefundFeedbackToCook = async (cookId, orderId, reason, reasonNotes) => {
    const reasonLabel = REFUND_REASON_LABELS[reason] || reason;
    const message = `A customer requested a refund for Order #${orderId}: "${reasonLabel}"`
        + (reasonNotes ? ` — "${reasonNotes}"` : '')
        + '. Please review to help avoid this next time.';
    return createNotification(
        cookId,
        'Customer Reported an Issue',
        message,
        'refund_feedback',
        orderId,
        'order',
        {
            pushData: {
                type: 'refund_feedback',
                orderId: String(orderId)
            }
        }
    );
};

/**
 * Create notification when an admin processes a refund (sent to customer).
 */
export const notifyRefundProcessed = async (customerId, orderId, status, adminNotes) => {
    const statusMessages = {
        approved: 'Your refund request has been approved and is being processed.',
        rejected: 'Your refund request was reviewed and could not be approved.',
        processed: 'Your refund has been processed via eSewa.'
    };
    return createNotification(
        customerId,
        'Refund Update',
        (statusMessages[status] || `Your refund status: ${status}`) + (adminNotes ? ` (${adminNotes})` : ''),
        'refund_status',
        orderId,
        'order',
        {
            pushData: {
                type: 'refund_status',
                orderId: String(orderId),
                status: status || ''
            }
        }
    );
};

/**
 * Create notification when a monthly commission settlement is generated
 * for a cook (sent to the cook).
 */
export const notifyCommissionDue = async (cookId, amountDue, month, year, settlementId = null) => {
    const monthLabel = new Date(year, month - 1, 1).toLocaleString('en-US', { month: 'long' });
    return createNotification(
        cookId,
        'Commission Due',
        `Your platform commission for ${monthLabel} ${year} is ₹${Number(amountDue).toFixed(2)}. Pay via the platform QR in Earnings and upload your payment screenshot.`,
        'commission_due',
        settlementId,
        'commission_settlement',
        {
            pushData: {
                type: 'commission_due',
                settlementId: settlementId == null ? '' : String(settlementId),
                month: String(month),
                year: String(year)
            }
        }
    );
};

/**
 * Due-date reminder for a settlement that is still unpaid.
 *
 * Separate from notifyCommissionDue because the copy has to differ: "here is
 * your bill" and "your bill is late" read the same to a cook otherwise, and a
 * cook who cannot tell a reminder from a new charge assumes they have been
 * double-billed. Amount is what is STILL OWED, not the original bill — a cook
 * who has already part-paid must never be chased for the full figure.
 */
export const notifyCommissionDueReminder = async (
    cookId, amountRemaining, month, year, settlementId, dueDateLabel, isOverdue
) => {
    const monthLabel = new Date(year, month - 1, 1).toLocaleString('en-US', { month: 'long' });
    const amount = `₹${Number(amountRemaining).toFixed(2)}`;
    const title = isOverdue ? 'Commission Payment Overdue' : 'Commission Due Soon';
    const message = isOverdue
        ? `${amount} commission for ${monthLabel} ${year} was due${dueDateLabel ? ` on ${dueDateLabel}` : ''}. Please pay it and upload your screenshot to keep your kitchen in good standing.`
        : `${amount} commission for ${monthLabel} ${year} is due${dueDateLabel ? ` on ${dueDateLabel}` : ' soon'}. Pay via the platform QR and upload your screenshot.`;

    return createNotification(
        cookId,
        title,
        message,
        'commission_due',
        settlementId,
        'commission_settlement',
        {
            pushData: {
                type: 'commission_due',
                settlementId: String(settlementId),
                month: String(month),
                year: String(year),
                overdue: isOverdue ? '1' : '0'
            }
        }
    );
};

/**
 * Create notification when a cook submits proof of commission payment
 * (sent to admin).
 */
export const notifyCommissionSettlementSubmitted = async (adminId, settlementId, cookName, amount) => {
    return createNotification(
        adminId,
        'Commission Payment Submitted',
        `${cookName} submitted proof of payment for ₹${Number(amount).toFixed(2)} commission.`,
        'commission_submitted',
        settlementId,
        'commission_settlement',
        { pushData: { type: 'commission_submitted', settlementId: String(settlementId) } }
    );
};

/**
 * Create notification when an admin verifies or rejects a cook's commission
 * payment (sent to the cook).
 */
export const notifyCommissionSettlementVerified = async (cookId, amount, month, year, settlementId = null) => {
    const monthLabel = new Date(year, month - 1, 1).toLocaleString('en-US', { month: 'long' });
    return createNotification(
        cookId,
        'Commission Payment Verified ✅',
        `Your ₹${Number(amount).toFixed(2)} commission payment for ${monthLabel} ${year} has been verified. Thank you!`,
        'commission_verified',
        settlementId,
        'commission_settlement',
        { pushData: { type: 'commission_verified', settlementId: settlementId == null ? '' : String(settlementId) } }
    );
};

export const notifyCommissionSettlementRejected = async (cookId, amount, adminNotes, settlementId = null) => {
    const message = `Your ₹${Number(amount).toFixed(2)} commission payment screenshot could not be verified`
        + (adminNotes ? ` — "${adminNotes}"` : '')
        + '. Please re-upload proof of payment.';
    return createNotification(
        cookId,
        'Commission Payment Rejected',
        message,
        'commission_rejected',
        settlementId,
        'commission_settlement',
        { pushData: { type: 'commission_rejected', settlementId: settlementId == null ? '' : String(settlementId) } }
    );
};

/**
 * Create notification when admin changes the platform commission rate
 * (sent to all active cooks).
 */
export const notifyCommissionRateChange = async (cookId, oldRate, newRate) => {
    const direction = newRate > oldRate ? 'increased' : 'decreased';
    const message = `Platform commission rate has been ${direction} from ${oldRate}% to ${newRate}%. This applies to all future orders. Your pending settlements remain unchanged.`;
    
    return createNotification(
        cookId,
        'Commission Rate Updated',
        message,
        'commission_rate_change',
        null,
        'platform_update',
        {
            pushData: {
                type: 'commission_rate_change',
                old_rate: String(oldRate),
                new_rate: String(newRate)
            }
        }
    );
};

/**
 * Create notification when a subscription's recurring delivery is skipped
 * because one of its meals is currently unavailable (sent to the customer,
 * who paid for a real, fulfillable delivery every cycle — not silence).
 */
export const notifySubscriptionDeliverySkipped = async (customerId, subscriptionId, planName) => {
    return createNotification(
        customerId,
        'Delivery Skipped',
        `Today's delivery for "${planName}" was skipped — the cook has an item from this plan marked unavailable right now. You have not been charged for it.`,
        'subscription_delivery_skipped',
        subscriptionId,
        'subscription',
        { pushData: { type: 'subscription_delivery_skipped', subscriptionId: String(subscriptionId) } }
    );
};

/** Same event, cook-facing — they're leaving a paying subscriber unfulfilled. */
export const notifySubscriptionDeliverySkippedToCook = async (cookId, subscriptionId, planName, customerName) => {
    return createNotification(
        cookId,
        'Subscription Delivery Skipped',
        `Today's delivery of "${planName}" for ${customerName} was skipped because one of the plan's meals is marked unavailable. Restock it so their next cycle isn't missed too.`,
        'subscription_delivery_skipped',
        subscriptionId,
        'subscription',
        {}
    );
};

/**
 * Create notification when a customer submits proof of payment for a new
 * subscription (sent to the cook, who must verify it before it activates).
 */
export const notifySubscriptionPaymentSubmitted = async (cookId, subscriptionId, customerName, planName) => {
    return createNotification(
        cookId,
        'Subscription Payment Submitted',
        `${customerName} submitted payment proof for "${planName}". Review and verify to activate their subscription.`,
        'subscription_payment_submitted',
        subscriptionId,
        'subscription',
        { pushData: { type: 'subscription_payment_submitted', subscriptionId: String(subscriptionId) } }
    );
};

/**
 * Create notification when a subscription is paid through eSewa and activated
 * automatically (sent to the cook). Distinct from
 * notifySubscriptionPaymentSubmitted: there is nothing for the cook to verify
 * here — the gateway payment was already confirmed server-side, so this is an
 * FYI that they have a new paying subscriber, not a task.
 */
export const notifySubscriptionPaidToCook = async (cookId, subscriptionId, customerName, planName, amount) => {
    return createNotification(
        cookId,
        'New Paid Subscriber 🎉',
        `${customerName} paid ₹${Number(amount).toFixed(0)} for "${planName}" via eSewa. Their subscription is active — no verification needed.`,
        'subscription_paid',
        subscriptionId,
        'subscription',
        { pushData: { type: 'subscription_paid', subscriptionId: String(subscriptionId) } }
    );
};

/**
 * Create notification when a cook verifies a subscription payment (sent to
 * the customer — their subscription is now active).
 */
export const notifySubscriptionVerified = async (customerId, subscriptionId, planName) => {
    return createNotification(
        customerId,
        'Subscription Activated 🎉',
        `Your payment was verified — "${planName}" is now active. Your first delivery is on the way per its schedule.`,
        'subscription_verified',
        subscriptionId,
        'subscription',
        { pushData: { type: 'subscription_verified', subscriptionId: String(subscriptionId) } }
    );
};

/**
 * Create notification when a cook rejects a subscription payment (sent to
 * the customer, prompting them to re-upload proof).
 */
export const notifySubscriptionRejected = async (customerId, subscriptionId, planName, notes) => {
    const message = `Your payment proof for "${planName}" could not be verified`
        + (notes ? ` — "${notes}"` : '')
        + '. Please re-upload a clearer screenshot.';
    return createNotification(
        customerId,
        'Subscription Payment Rejected',
        message,
        'subscription_rejected',
        subscriptionId,
        'subscription',
        { pushData: { type: 'subscription_rejected', subscriptionId: String(subscriptionId) } }
    );
};

/**
 * "Sep 3, 2026" from a 'YYYY-MM-DD' string. Built from the string's own parts
 * rather than new Date(str) so the message never shows a day-shifted date on a
 * UTC server (Nepal is UTC+05:45 — see utils/nptTime.js).
 */
const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
export const formatDeliveryDate = (dateStr) => {
    const match = String(dateStr || '').match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (!match) return String(dateStr || 'the scheduled date');
    const [, y, m, d] = match;
    return `${MONTH_NAMES[Number(m) - 1]} ${Number(d)}, ${y}`;
};

/**
 * Customer skipped one day of their subscription (sent to the COOK, who is the
 * one that must not cook that meal). Fires before the cutoff by definition —
 * the endpoint rejects late skips — so the cook always has time to act on it.
 */
export const notifySkipDay = async (cookId, subscriptionId, customerName, planName, deliveryDate, reason) => {
    const message = `${customerName} skipped their "${planName}" delivery on ${formatDeliveryDate(deliveryDate)}`
        + (reason ? ` — "${reason}"` : '')
        + '. Don\'t prepare this one; their meal credit was not used.';
    return createNotification(
        cookId,
        'Delivery Skipped by Customer',
        message,
        'subscription_day_skipped',
        subscriptionId,
        'subscription',
        {
            pushData: {
                type: 'subscription_day_skipped',
                subscriptionId: String(subscriptionId),
                deliveryDate: String(deliveryDate)
            }
        }
    );
};

/**
 * Cook marked a whole date unavailable (sent to EVERY affected customer — this
 * is the fan-out arm of the bulk toggle). Says explicitly that no credit was
 * consumed, because the alternative reading — "I paid for a meal I never got" —
 * is what turns a closed kitchen into a refund request.
 */
export const notifyCookUnavailable = async (customerId, subscriptionId, cookName, deliveryDate, reason) => {
    const message = `${cookName} is not delivering on ${formatDeliveryDate(deliveryDate)}`
        + (reason ? ` — "${reason}"` : '')
        + '. You have not been charged a meal credit for that day.';
    return createNotification(
        customerId,
        'Kitchen Closed That Day',
        message,
        'cook_unavailable',
        subscriptionId,
        'subscription',
        {
            pushData: {
                type: 'cook_unavailable',
                subscriptionId: String(subscriptionId),
                deliveryDate: String(deliveryDate)
            }
        }
    );
};

/**
 * Payment verified but the customer's chosen start date is still in the future
 * (sent to the customer). Deliberately distinct from
 * notifySubscriptionVerified, whose "your first delivery is on the way" is a
 * lie for a scheduled subscription — this one names the date so nobody waits
 * by the door a week early.
 */
export const notifySubscriptionScheduled = async (customerId, subscriptionId, planName, startDate) => {
    return createNotification(
        customerId,
        'Subscription Confirmed ✅',
        `Your payment for "${planName}" was verified. Deliveries begin on ${formatDeliveryDate(startDate)} — nothing more to do until then.`,
        'subscription_scheduled',
        subscriptionId,
        'subscription',
        {
            pushData: {
                type: 'subscription_scheduled',
                subscriptionId: String(subscriptionId),
                startDate: String(startDate)
            }
        }
    );
};

/**
 * Every paid meal has been used (sent to the customer). Sent by the cron at the
 * moment credits hit zero, not when end_date passes — a customer who skipped
 * days finishes later than their end_date, and one who never skipped finishes
 * on it.
 */
export const notifySubscriptionCompleted = async (customerId, subscriptionId, planName, mealsTotal) => {
    const count = Number(mealsTotal) > 0 ? `all ${mealsTotal} ` : 'all ';
    return createNotification(
        customerId,
        'Subscription Complete',
        `You've received ${count}deliveries from "${planName}". Subscribe again to keep them coming.`,
        'subscription_completed',
        subscriptionId,
        'subscription',
        {
            pushData: {
                type: 'subscription_completed',
                subscriptionId: String(subscriptionId)
            }
        }
    );
};

/** Same completion event, cook-facing — a paying subscriber just lapsed. */
export const notifySubscriptionCompletedToCook = async (cookId, subscriptionId, customerName, planName) => {
    return createNotification(
        cookId,
        'Subscription Ended',
        `${customerName}'s "${planName}" subscription has used all its paid deliveries. No more meals are scheduled for them.`,
        'subscription_completed',
        subscriptionId,
        'subscription',
        {}
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
    notifyNewComboOrder,
    notifyOrderStatusUpdate,
    notifyOrderCancelled,
    notifyNewReview,
    notifyReviewReply,
    notifyCookApprovalUpdate,
    notifyPaymentConfirmed,
    notifyRefundRequested,
    notifyRefundFeedbackToCook,
    notifyRefundProcessed,
    notifyCommissionDue,
    notifyCommissionDueReminder,
    notifyCommissionSettlementSubmitted,
    notifyCommissionSettlementVerified,
    notifyCommissionSettlementRejected,
    notifyCommissionRateChange,
    notifySubscriptionDeliverySkipped,
    notifySubscriptionDeliverySkippedToCook,
    notifySubscriptionPaymentSubmitted,
    notifySubscriptionVerified,
    notifySubscriptionRejected,
    notifySkipDay,
    notifyCookUnavailable,
    notifySubscriptionScheduled,
    notifySubscriptionCompleted,
    notifySubscriptionCompletedToCook,
    markAllAsRead,
    cleanupOldNotifications
};
