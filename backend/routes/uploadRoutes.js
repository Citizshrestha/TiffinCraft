import express from 'express';
import { uploadMealImage, uploadProfileImage, uploadDocument, deleteImage } from '../controllers/uploadController.js';
import { uploadSingle } from '../middleware/uploadMiddleware.js';
import { protect } from '../middleware/authMiddleware.js';

const router = express.Router();

// Upload meal image (requires authentication)
router.post(
  '/meal-image',
  protect,
  uploadSingle('image'),
  uploadMealImage
);

// Upload profile image (requires authentication)
router.post(
  '/profile-image',
  protect,
  uploadSingle('image'),
  uploadProfileImage
);

// Upload document (requires authentication)
router.post(
  '/document',
  protect,
  uploadSingle('document'),
  uploadDocument
);

// Delete image (requires authentication)
router.delete(
  '/image',
  protect,
  deleteImage
);

// Error handling middleware for multer errors
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
