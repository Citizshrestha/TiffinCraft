import express from 'express';
import { uploadMealImage, uploadProfileImage, uploadDocument, uploadChatMedia, uploadBankQr } from '../controllers/uploadController.js';
import { uploadSingle, uploadChatMediaSingle } from '../middleware/uploadMiddleware.js';
import { protect, roleOnly } from '../middleware/authMiddleware.js';
import { uploadLimiter } from '../middleware/rateLimiters.js';

const router = express.Router();


router.post(
  '/meal-image',
  protect,
  roleOnly('cook'),
  uploadLimiter,
  uploadSingle('image'),
  uploadMealImage
);


router.post(
  '/profile-image',
  protect,
  uploadLimiter,
  uploadSingle('image'),
  uploadProfileImage
);


router.post(
  '/document',
  protect,
  roleOnly('customer', 'cook'),
  uploadLimiter,
  uploadSingle('document'),
  uploadDocument
);


router.post(
  '/bank-qr',
  protect,
  roleOnly('cook', 'admin'),
  uploadLimiter,
  uploadSingle('document'),
  uploadBankQr
);


router.post(
  '/chat-media',
  protect,
  uploadLimiter,
  uploadChatMediaSingle('media'),
  uploadChatMedia
);


router.use((error, req, res, next) => {
  if (error) {
    return res.status(400).json({
      success: false,
      message: error.message || 'Upload error occurred'
    });
  }
  next();
});

export default router;

