import db from '../config/db.js';

export const getUserNotifications = (req, res) => {
    // Assuming auth middleware puts user data in req.user
    const userId = req.user.id; 

    const sql = 'SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC';
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
