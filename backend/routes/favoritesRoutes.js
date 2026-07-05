import express from 'express';
import { protect } from '../middleware/authMiddleware.js';
import {
    getFavorites,
    addFavorite,
    removeFavorite,
    checkFavorite
} from '../controllers/favoritesController.js';

const router = express.Router();

router.use(protect);

router.get('/', getFavorites);

router.post('/', addFavorite);

router.delete('/:cook_id', removeFavorite);

router.get('/check/:cook_id', checkFavorite);

export default router;
