import db from '../config/db.js';
import { markAllAsRead as markAllAsReadHelper } from '../utils/notificationHelper.js';

export const getUserNotifications = (req, res) => {
    // Assuming auth middleware puts user data in req.user
    const userId = req.user.id;

    // Custom meal notifications point at the request row. Resolve the
    // customer/cook conversation here so an inbox tap can open the exact chat
    // card, including Accept/Decline, instead of the generic subscribers list.
    const sql = `
        SELECT n.*,
               CASE WHEN n.type = 'custom_meal_request' THEN c.id ELSE NULL END AS conversation_id
        FROM notifications n
        LEFT JOIN custom_meal_requests r
               ON n.type = 'custom_meal_request'
              AND n.reference_type = 'custom_meal_request'
              AND n.reference_id = r.id
        LEFT JOIN conversations c
               ON c.customer_id = r.customer_id AND c.cook_id = r.cook_id
        WHERE n.user_id = ?
        ORDER BY n.created_at DESC`;
    db.query(sql, [userId], (err, results) => {
        if (err) {
            console.error('Error fetching notifications:', err);
            return res.status(500).json({ success: false, message: 'Database error' });
        }
        res.json({ success: true, notifications: results });
    });
};

export const markAsRead = (req, res) => {
    const userId = req.user.id;
    const notificationId = req.params.id;

    const sql = 'UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?';
    db.query(sql, [notificationId, userId], (err, results) => {
        if (err) {
            console.error('Error updating notification:', err);
            return res.status(500).json({ success: false, message: 'Database error' });
        }

        if (results.affectedRows === 0) {
            return res.status(404).json({ success: false, message: 'Notification not found or unauthorized' });
        }

        res.json({ success: true, message: 'Notification marked as read' });
    });
};

export const getUnreadCount = (req, res) => {
    const userId = req.user.id;

    const sql = 'SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND is_read = FALSE';
    db.query(sql, [userId], (err, results) => {
        if (err) {
            console.error('Error counting notifications:', err);
            return res.status(500).json({ success: false, message: 'Database error' });
        }
        res.json({ success: true, unread_count: results[0].unread_count });
    });
};

export const markAllNotificationsAsRead = async (req, res) => {
    try {
        const userId = req.user.id;
        const result = await markAllAsReadHelper(userId);

        if (result.success) {
            return res.json({
                success: true,
                message: 'All notifications marked as read'
            });
        } else {
            return res.status(500).json({
                success: false,
                message: 'Failed to mark notifications as read',
                error: result.error
            });
        }
    } catch (error) {
        console.error('Error in markAllNotificationsAsRead:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};
