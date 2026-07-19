import express from "express";
import { protect, roleOnly } from "../middleware/authMiddleware.js";
import {
    getCart,
    addToCart,
    updateCartItem,
    removeCartItem,
    clearCart,
    checkoutCart,
} from "../controllers/cartController.js";

const router = express.Router();

// Customer cart — multi-cook cart (items from Cook A + Cook B allowed)
router.get("/", protect, roleOnly("customer"), getCart);

// Prefer /add (matches Android client); also accept POST / for older clients
router.post("/add", protect, roleOnly("customer"), addToCart);
router.post("/", protect, roleOnly("customer"), addToCart);

// Prefer /item/:id (matches Android client); also accept bare /:id
router.put("/item/:cartItemId", protect, roleOnly("customer"), updateCartItem);
router.put("/:cartItemId", protect, roleOnly("customer"), updateCartItem);

router.delete("/item/:cartItemId", protect, roleOnly("customer"), removeCartItem);
router.delete("/:cartItemId", protect, roleOnly("customer"), removeCartItem);

router.delete("/", protect, roleOnly("customer"), clearCart);

// Checkout splits cart into one order per cook + notifies each cook (in-app + FCM)
router.post("/checkout", protect, roleOnly("customer"), checkoutCart);

export default router;
