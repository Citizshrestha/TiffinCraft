import express from 'express';
import { 
    getUserNotifications, 
    markAsRead, 
    getUnreadCount 
} from '../controllers/notificationController.js';
import { protect } from '../middleware/authMiddleware.js';

const router = express.Router();

// All notification routes require authentication
router.use(protect);

router.get('/', getUserNotifications);
router.get('/unread-count', getUnreadCount);
router.put('/:id/read', markAsRead);

export default router;
