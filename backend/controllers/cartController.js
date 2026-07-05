import db from "../config/db.js";

// GET /api/cart — get current user's full cart, grouped by cook
export const getCart = async (req, res) => {
    try {
        const customerId = req.user.id;
        const [rows] = await db.promise().query(
            `SELECT 
                ci.id AS cart_item_id, ci.meal_id, ci.cook_id, ci.quantity,
                m.name AS meal_name, m.price, m.image_url, m.is_available,
                u.full_name AS cook_name, cp.kitchen_name
             FROM cart_items ci
             JOIN meals m ON ci.meal_id = m.id
             JOIN users u ON ci.cook_id = u.id
             LEFT JOIN cook_profiles cp ON cp.user_id = ci.cook_id
             WHERE ci.customer_id = ?
             ORDER BY ci.cook_id, ci.created_at`,
            [customerId]
        );

        // Group by cook
        const grouped = {};
        let grandTotal = 0;

        for (const row of rows) {
            const lineTotal = parseFloat(row.price) * row.quantity;
            grandTotal += lineTotal;

            if (!grouped[row.cook_id]) {
                grouped[row.cook_id] = {
                    cook_id: row.cook_id,
                    cook_name: row.cook_name,
                    kitchen_name: row.kitchen_name,
                    items: [],
                    subtotal: 0,
                };
            }
            grouped[row.cook_id].items.push({
                cart_item_id: row.cart_item_id,
                meal_id: row.meal_id,
                meal_name: row.meal_name,
                price: row.price,
                image_url: row.image_url,
                quantity: row.quantity,
                is_available: !!row.is_available,
                line_total: lineTotal,
            });
            grouped[row.cook_id].subtotal += lineTotal;
        }

        res.status(200).json({
            success: true,
            cart_groups: Object.values(grouped),
            grand_total: grandTotal,
            item_count: rows.reduce((sum, r) => sum + r.quantity, 0),
        });
    } catch (error) {
        console.error("getCart error:", error);
        res.status(500).json({ success: false, message: "Failed to fetch cart" });
    }
};

// POST /api/cart — add item to cart (or increment if exists)
// body: { meal_id, quantity (optional, default 1) }
export const addToCart = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { meal_id, quantity } = req.body;
        const qty = quantity && quantity > 0 ? quantity : 1;

        if (!meal_id) {
            return res.status(400).json({ success: false, message: "meal_id is required" });
        }

        const [meals] = await db.promise().query(
            "SELECT id, cook_id, is_available FROM meals WHERE id = ?",
            [meal_id]
        );
        if (meals.length === 0) {
            return res.status(404).json({ success: false, message: "Meal not found" });
        }
        if (!meals[0].is_available) {
            return res.status(400).json({ success: false, message: "Meal is currently unavailable" });
        }
        const cookId = meals[0].cook_id;

        await db.promise().query(
            `INSERT INTO cart_items (customer_id, meal_id, cook_id, quantity)
             VALUES (?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)`,
            [customerId, meal_id, cookId, qty]
        );

        res.status(200).json({ success: true, message: "Added to cart" });
    } catch (error) {
        console.error("addToCart error:", error);
        res.status(500).json({ success: false, message: "Failed to add to cart" });
    }
};

// PUT /api/cart/:cartItemId — set absolute quantity (used by +/- stepper)
// body: { quantity }
export const updateCartItem = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { cartItemId } = req.params;
        const { quantity } = req.body;

        if (quantity === undefined || quantity < 1) {
            return res.status(400).json({ success: false, message: "quantity must be at least 1" });
        }

        const [result] = await db.promise().query(
            "UPDATE cart_items SET quantity = ? WHERE id = ? AND customer_id = ?",
            [quantity, cartItemId, customerId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: "Cart item not found" });
        }

        res.status(200).json({ success: true, message: "Cart updated" });
    } catch (error) {
        console.error("updateCartItem error:", error);
        res.status(500).json({ success: false, message: "Failed to update cart" });
    }
};

// DELETE /api/cart/:cartItemId — remove single item
export const removeCartItem = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { cartItemId } = req.params;

        const [result] = await db.promise().query(
            "DELETE FROM cart_items WHERE id = ? AND customer_id = ?",
            [cartItemId, customerId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: "Cart item not found" });
        }

        res.status(200).json({ success: true, message: "Item removed from cart" });
    } catch (error) {
        console.error("removeCartItem error:", error);
        res.status(500).json({ success: false, message: "Failed to remove item" });
    }
};

// DELETE /api/cart — clear entire cart
export const clearCart = async (req, res) => {
    try {
        const customerId = req.user.id;
        await db.promise().query("DELETE FROM cart_items WHERE customer_id = ?", [customerId]);
        res.status(200).json({ success: true, message: "Cart cleared" });
    } catch (error) {
        console.error("clearCart error:", error);
        res.status(500).json({ success: false, message: "Failed to clear cart" });
    }
};

// POST /api/cart/checkout — convert cart into one order per cook
// body: { delivery_address, delivery_latitude, delivery_longitude, special_instructions (optional) }
export const checkoutCart = async (req, res) => {
    const conn = await db.promise().getConnection();
    try {
        const customerId = req.user.id;
        const { delivery_address, delivery_latitude, delivery_longitude, special_instructions } = req.body;

        if (!delivery_address) {
            return res.status(400).json({ success: false, message: "delivery_address is required" });
        }

        const [cartRows] = await conn.query(
            `SELECT ci.id AS cart_item_id, ci.meal_id, ci.cook_id, ci.quantity,
                    m.price, m.is_available
             FROM cart_items ci
             JOIN meals m ON ci.meal_id = m.id
             WHERE ci.customer_id = ?`,
            [customerId]
        );

        if (cartRows.length === 0) {
            return res.status(400).json({ success: false, message: "Cart is empty" });
        }

        const unavailable = cartRows.filter(r => !r.is_available);
        if (unavailable.length > 0) {
            return res.status(400).json({
                success: false,
                message: "Some items in your cart are no longer available",
                unavailable_meal_ids: unavailable.map(r => r.meal_id),
            });
        }

        // Group by cook
        const byCook = {};
        for (const row of cartRows) {
            if (!byCook[row.cook_id]) byCook[row.cook_id] = [];
            byCook[row.cook_id].push(row);
        }

        await conn.beginTransaction();

        const createdOrders = [];

        for (const cookId of Object.keys(byCook)) {
            const items = byCook[cookId];
            const totalAmount = items.reduce((sum, i) => sum + parseFloat(i.price) * i.quantity, 0);

            const [orderResult] = await conn.query(
                `INSERT INTO orders 
                    (customer_id, cook_id, total_amount, status, delivery_address, 
                     delivery_latitude, delivery_longitude, special_instructions)
                 VALUES (?, ?, ?, 'pending', ?, ?, ?, ?)`,
                [customerId, cookId, totalAmount, delivery_address,
                 delivery_latitude || null, delivery_longitude || null,
                 special_instructions || null]
            );
            const orderId = orderResult.insertId;

            for (const item of items) {
                await conn.query(
                    `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time)
                     VALUES (?, ?, ?, ?)`,
                    [orderId, item.meal_id, item.quantity, item.price]
                );
            }

            createdOrders.push({ order_id: orderId, cook_id: parseInt(cookId), total_amount: totalAmount });

            // Real-time notify cook of new order (if Socket.IO is attached)
            const io = req.app.get("io");
            if (io) {
                io.to(`cook_${cookId}`).emit("newOrder", { orderId, customerId });
            }
        }

        // Clear the cart now that orders are created
        await conn.query("DELETE FROM cart_items WHERE customer_id = ?", [customerId]);

        await conn.commit();

        res.status(201).json({
            success: true,
            message: "Order(s) placed successfully",
            orders: createdOrders,
        });
    } catch (error) {
        await conn.rollback();
        console.error("checkoutCart error:", error);
        res.status(500).json({ success: false, message: "Checkout failed" });
    } finally {
        conn.release();
    }
};