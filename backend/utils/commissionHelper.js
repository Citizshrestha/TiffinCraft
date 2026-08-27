import db from "../config/db.js";
import { notifyCommissionRateChange } from "./notificationHelper.js";

/**
 * Handles commission rate change notifications and automated chat messages
 * to all active cooks. This is called when admin updates the commission rate.
 * 
 * @param {number} oldRate - Previous commission rate (e.g., 5.00)
 * @param {number} newRate - New commission rate (e.g., 7.00)
 * @param {number} adminId - ID of admin who made the change
 * @param {string} changeReason - Optional reason for the change
 * @param {object} io - Socket.io instance for real-time updates
 * @returns {Promise<{notifiedCount: number, chatsSent: number}>}
 */
export const notifyAllCooksOfRateChange = async (oldRate, newRate, adminId, changeReason = null, io = null) => {
    try {
        // Get all active cooks
        const [cooks] = await db.promise().query(
            `SELECT u.id, u.full_name, COALESCE(cp.kitchen_name, u.full_name) as kitchen_name
             FROM users u
             LEFT JOIN cook_profiles cp ON cp.user_id = u.id
             WHERE u.role = 'cook' AND u.is_active = TRUE`
        );

        if (cooks.length === 0) {
            return { notifiedCount: 0, chatsSent: 0 };
        }

        let notifiedCount = 0;
        let chatsSent = 0;

        const direction = newRate > oldRate ? 'increased' : 'decreased';
        const change = Math.abs(newRate - oldRate).toFixed(2);
        
        // Create chat message content
        const chatMessage = `📢 *Platform Commission Rate Update*\n\n` +
            `The platform commission rate has been ${direction} from *${oldRate}%* to *${newRate}%* ` +
            `(${direction === 'increased' ? '+' : '-'}${change}%).\n\n` +
            `✅ *What this means for you:*\n` +
            `• This new rate applies to all *future orders* starting now\n` +
            `• Your existing pending settlements remain at their original rates\n` +
            `• Past orders are not affected\n\n` +
            (changeReason ? `📝 *Admin Note:* ${changeReason}\n\n` : '') +
            `For questions, please contact support. Thank you for being part of TiffinCraft! 🍛`;

        // Process each cook
        for (const cook of cooks) {
            try {
                // Send in-app notification
                await notifyCommissionRateChange(cook.id, oldRate, newRate);
                notifiedCount++;

                // Send automated chat message from admin
                await sendAutomatedCommissionChatMessage(
                    cook.id,
                    adminId,
                    chatMessage,
                    oldRate,
                    newRate
                );
                chatsSent++;

                // Send real-time socket event if available
                if (io) {
                    io.to(`user_${cook.id}`).emit('commissionRateChanged', {
                        oldRate,
                        newRate,
                        message: `Commission rate ${direction} to ${newRate}%`
                    });
                }
            } catch (err) {
                console.error(`Failed to notify cook ${cook.id}:`, err.message);
                // Continue with other cooks even if one fails
            }
        }

        // Record the change in history
        await db.promise().query(
            `INSERT INTO commission_rate_history 
             (old_rate, new_rate, changed_by, change_reason, affected_cooks_count)
             VALUES (?, ?, ?, ?, ?)`,
            [oldRate, newRate, adminId, changeReason, notifiedCount]
        );

        // Update last notification timestamp
        await db.promise().query(
            `UPDATE platform_settings 
             SET last_rate_notification_at = NOW() 
             WHERE id = 1`
        );

        console.log(`✅ Commission rate change: notified ${notifiedCount} cooks, sent ${chatsSent} chat messages`);

        return { notifiedCount, chatsSent };

    } catch (error) {
        console.error('Error in notifyAllCooksOfRateChange:', error);
        throw error;
    }
};

/**
 * Creates an automated chat message about commission rate change.
 * Ensures a conversation exists between admin and cook, then sends the message.
 * 
 * @param {number} cookId - Cook's user ID
 * @param {number} adminId - Admin's user ID
 * @param {string} message - Chat message content
 * @param {number} oldRate - Old commission rate
 * @param {number} newRate - New commission rate
 */
async function sendAutomatedCommissionChatMessage(cookId, adminId, message, oldRate, newRate) {
    try {
        // Check if conversation exists between admin and cook
        const [existing] = await db.promise().query(
            `SELECT id FROM chat_conversations 
             WHERE (participant1_id = ? AND participant2_id = ?)
                OR (participant1_id = ? AND participant2_id = ?)`,
            [adminId, cookId, cookId, adminId]
        );

        let conversationId;

        if (existing.length > 0) {
            conversationId = existing[0].id;
        } else {
            // Create new conversation
            const [result] = await db.promise().query(
                `INSERT INTO chat_conversations 
                 (participant1_id, participant2_id, last_message_at)
                 VALUES (?, ?, NOW())`,
                [adminId, cookId]
            );
            conversationId = result.insertId;
        }

        // Insert the automated system message
        await db.promise().query(
            `INSERT INTO chat_messages 
             (conversation_id, sender_id, message_text, message_type, metadata, is_read)
             VALUES (?, ?, ?, 'system', ?, FALSE)`,
            [
                conversationId,
                adminId,
                message,
                JSON.stringify({
                    type: 'commission_rate_change',
                    old_rate: oldRate,
                    new_rate: newRate,
                    timestamp: new Date().toISOString()
                })
            ]
        );

        // Update conversation's last message timestamp
        await db.promise().query(
            `UPDATE chat_conversations 
             SET last_message_at = NOW() 
             WHERE id = ?`,
            [conversationId]
        );

        return conversationId;

    } catch (error) {
        console.error(`Failed to send chat message to cook ${cookId}:`, error);
        throw error;
    }
}

/**
 * Calculate pending commission with current rate for display purposes
 * (not snapshotted - just for preview)
 */
export const calculatePendingCommissionForCook = async (cookId) => {
    try {
        // Get current rate
        const [[settings]] = await db.promise().query(
            "SELECT commission_pct FROM platform_settings WHERE id = 1"
        );
        const currentRate = settings ? parseFloat(settings.commission_pct) : 5.00;

        // Get pending orders (not yet delivered)
        const [[pending]] = await db.promise().query(
            `SELECT 
                COUNT(*) as order_count,
                COALESCE(SUM(total_amount), 0) as total_amount,
                COALESCE(SUM(ROUND(total_amount * ? / 100, 2)), 0) as estimated_commission
             FROM orders
             WHERE cook_id = ? 
               AND status NOT IN ('delivered', 'cancelled', 'completed')
               AND (refund_status IS NULL OR refund_status != 'refunded')`,
            [currentRate, cookId]
        );

        return {
            order_count: pending.order_count || 0,
            total_amount: parseFloat(pending.total_amount || 0),
            estimated_commission: parseFloat(pending.estimated_commission || 0),
            current_rate: currentRate
        };

    } catch (error) {
        console.error('Error calculating pending commission:', error);
        return {
            order_count: 0,
            total_amount: 0,
            estimated_commission: 0,
            current_rate: 5.00
        };
    }
};

/**
 * Get commission rate history for display in admin panel
 */
export const getCommissionRateHistory = async (limit = 20) => {
    try {
        const [history] = await db.promise().query(
            `SELECT 
                crh.*,
                u.full_name as admin_name
             FROM commission_rate_history crh
             LEFT JOIN users u ON u.id = crh.changed_by
             ORDER BY crh.created_at DESC
             LIMIT ?`,
            [limit]
        );

        return history;
    } catch (error) {
        console.error('Error fetching commission rate history:', error);
        return [];
    }
};

export default {
    notifyAllCooksOfRateChange,
    calculatePendingCommissionForCook,
    getCommissionRateHistory
};
