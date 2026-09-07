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


router.get("/", protect, roleOnly("customer"), getCart);


router.post("/add", protect, roleOnly("customer"), addToCart);
router.post("/", protect, roleOnly("customer"), addToCart);


router.put("/item/:cartItemId", protect, roleOnly("customer"), updateCartItem);
router.put("/:cartItemId", protect, roleOnly("customer"), updateCartItem);

router.delete("/item/:cartItemId", protect, roleOnly("customer"), removeCartItem);
router.delete("/:cartItemId", protect, roleOnly("customer"), removeCartItem);

router.delete("/", protect, roleOnly("customer"), clearCart);


router.post("/checkout", protect, roleOnly("customer"), checkoutCart);

export default router;
