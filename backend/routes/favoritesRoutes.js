import express from 'express';
import { authenticate } from '../middleware/authMiddleware.js';
import * as favoritesController from '../controllers/favoritesController.js';

const router = express.Router();

// All routes require authentication
router.use(authenticate);

// Get all favorites for logged-in customer
router.get('/', favoritesController.getFavorites);

// Add cook to favorites
router.post('/', favoritesController.addToFavorites);

// Remove cook from favorites
router.delete('/:cookId', favoritesController.removeFromFavorites);

// Check if cook is in favorites
router.get('/check/:cookId', favoritesController.checkFavoriteStatus);

export default router;
