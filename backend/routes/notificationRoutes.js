import express from 'express';
import {
    getUserNotifications,
    markAsRead,
    getUnreadCount,
    markAllNotificationsAsRead
} from '../controllers/notificationController.js';
import { protect } from '../middleware/authMiddleware.js';

const router = express.Router();

router.use(protect);

router.get('/', getUserNotifications);
router.get('/unread-count', getUnreadCount);
router.put('/read-all', markAllNotificationsAsRead);
router.put('/:id/read', markAsRead);

export default router;
