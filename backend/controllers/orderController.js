import db from "../config/db.js";
import { createNotification, notifyNewOrder, notifyOrderStatusUpdate, notifyOrderCancelled, notifyOrderCancelledToCustomer } from '../utils/notificationHelper.js';
import { applyDeliveryCommission } from "./commissionController.js";

export const placeOrder = async (req, res) => {
    const connection = await db.promise().getConnection();

    try {
        const customerId = req.user.id;
        const {
            cook_id,
            meal_id,
            quantity,
            delivery_address,
            delivery_latitude,
            delivery_longitude,
            subscription_type,
            payment_method,
            special_instructions
        } = req.body;

        if (!cook_id || !meal_id || !quantity || !delivery_address) {
            return res.status(400).json({
                success: false,
                message: "cook_id, meal_id, quantity and delivery_address are required."
            });
        }

        // Validate payment_method
        const validPaymentMethods = ['cod', 'online'];
        const selectedPaymentMethod = payment_method || 'cod';
        if (!validPaymentMethods.includes(selectedPaymentMethod)) {
            return res.status(400).json({
                success: false,
                message: "payment_method must be 'cod' or 'online'"
            });
        }

        const [meals] = await connection.query(
            "SELECT * FROM meals WHERE id = ? AND is_available = TRUE",
            [meal_id]
        );

        if (meals.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Meal not found or not available."
            });
        }

        const meal = meals[0];
        const total_amount = meal.price * quantity;

        await connection.beginTransaction();

        // Set initial payment_status based on payment_method
        const initialPaymentStatus = selectedPaymentMethod === 'cod' ? 'pending' : 'pending';

        const [result] = await connection.query(
            `INSERT INTO orders
             (customer_id, cook_id, total_amount, delivery_address,
              delivery_latitude, delivery_longitude,
              status, payment_method, payment_status, special_instructions)
             VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?)`,
            [
                customerId,
                cook_id,
                total_amount,
                delivery_address,
                delivery_latitude || null,
                delivery_longitude || null,
                selectedPaymentMethod,
                initialPaymentStatus,
                special_instructions || null
            ]
        );

        const orderId = result.insertId;

        await connection.query(
            `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time)
             VALUES (?, ?, ?, ?)`,
            [orderId, meal_id, quantity, meal.price]
        );

        await connection.commit();

        // Get customer name for notification
        const [customer] = await connection.query(
            'SELECT full_name FROM users WHERE id = ?',
            [customerId]
        );

        // In-app + FCM to this cook only (meal-specific message)
        const customerName = customer[0]?.full_name || "A customer";
        const mealSummary = `${quantity}x ${meal.name}`;
        await createNotification(
            cook_id,
            "New Order Received! 🎉",
            `${customerName} ordered ${mealSummary} — ₹${Number(total_amount).toFixed(0)}`,
            "new_order",
            orderId,
            "order",
            {
                pushData: {
                    type: "new_order",
                    orderId: String(orderId),
                    customerName,
                    totalAmount: String(total_amount),
                    meals: mealSummary,
                },
            }
        );

        // Emit Socket.IO event to this cook's room only
        const io = req.app.get("io");
        if (io) {
            io.to(`cook_${cook_id}`).emit("newOrder", {
                orderId,
                customerId,
                customerName,
                mealName: meal.name,
                mealSummary,
                quantity,
                total_amount,
                delivery_address,
                payment_method: selectedPaymentMethod
            });
        }

        return res.status(201).json({
            success: true,
            message: "Order placed successfully.",
            orderId,
            payment_method: selectedPaymentMethod
        });

    } catch (error) {
        await connection.rollback();
        console.error("placeOrder error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    } finally {
        connection.release();
    }
};

// GET /api/orders/:orderId
// Get single order details
export const getOrderById = async (req, res) => {
    try {
        const { orderId } = req.params;
        const userId = req.user.id;

        const [orders] = await db.promise().query(
            `SELECT o.*,
                    u.full_name as customer_name,
                    u.phone as customer_phone,
                    u.profile_image as customer_profile_image,
                    cu.full_name as cook_name,
                    cu.phone as cook_phone,
                    cu.address AS cook_address,
                    cp.kitchen_name,
                    cp.bank_details AS cook_bank_details,
                    cu.latitude  AS cook_latitude,
                    cu.longitude AS cook_longitude,
                    -- Orders placed before delivery coords were captured have NULL
                    -- delivery_latitude/longitude; fall back to the customer's saved
                    -- profile location so the tracking map still has two points.
                    COALESCE(o.delivery_latitude,  u.latitude)  AS map_customer_latitude,
                    COALESCE(o.delivery_longitude, u.longitude) AS map_customer_longitude
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             LEFT JOIN users cu ON o.cook_id = cu.id
             LEFT JOIN cook_profiles cp ON cp.user_id = o.cook_id
             WHERE o.id = ?`,
            [orderId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        // Only customer or cook of this order can view it
        if (order.customer_id !== userId && order.cook_id !== userId) {
            return res.status(403).json({
                success: false,
                message: "Access denied."
            });
        }

        // Get order items
        const [items] = await db.promise().query(
            `SELECT oi.*, m.name as meal_name, m.image_url
             FROM order_items oi
             JOIN meals m ON oi.meal_id = m.id
             WHERE oi.order_id = ?`,
            [orderId]
        );

        // payment_status = 'paid' is now reached by two different paths — an
        // eSewa auto-confirmation (nothing left to do) vs. the older manual
        // screenshot flow (still awaiting the cook's verification). The
        // client needs to tell them apart to show the right label.
        const [[esewaPayment]] = await db.promise().query(
            `SELECT id FROM payments WHERE order_id = ? AND status = 'SUCCESS' LIMIT 1`,
            [orderId]
        );

        // mysql2 returns DECIMAL columns as strings — coerce so Gson maps them
        // onto Double (same treatment getNearbyCooks applies to its coords).
        const num = (v) => (v === null || v === undefined ? null : parseFloat(v));

        // Cook's eSewa QR for the manual pay flow — the customer scans it in
        // their own eSewa app. bank_details is a JSON blob (esewa_qr_url /
        // khalti_qr_url / bank_qr_url); pull just eSewa's, tolerating legacy
        // rows that are null or malformed.
        let cookEsewaQrUrl = null;
        try {
            if (order.cook_bank_details) cookEsewaQrUrl = JSON.parse(order.cook_bank_details).esewa_qr_url || null;
        } catch (_) { /* legacy/malformed JSON — no QR */ }
        delete order.cook_bank_details;

        return res.status(200).json({
            success: true,
            order: {
                ...order,
                items,
                cook_latitude: num(order.cook_latitude),
                cook_longitude: num(order.cook_longitude),
                map_customer_latitude: num(order.map_customer_latitude),
                map_customer_longitude: num(order.map_customer_longitude),
                cook_esewa_qr_url: cookEsewaQrUrl,
                esewa_confirmed: !!esewaPayment
            }
        });

    } catch (error) {
        console.error("getOrderById error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/orders/customer/my
// Customer views all their orders
export const getCustomerOrders = async (req, res) => {
    try {
        const customerId = req.user.id;

        // One row per order (not per item) — item details are aggregated so the
        // Android order card can render its item chips/summary without needing
        // to stitch together duplicate order rows itself.
        const [orders] = await db.promise().query(
            `SELECT o.id, o.customer_id, o.cook_id, o.total_amount, o.status,
                    o.delivery_address, o.special_instructions,
                    o.payment_method, o.payment_status, o.payment_screenshot_url,
                    o.payment_verified_at, o.created_at, o.updated_at,
                    -- GROUP BY o.id makes every o.* column functionally dependent
                    -- and therefore legal under only_full_group_by, but columns
                    -- from JOINed tables are not covered by that dependency and
                    -- must be wrapped. The join is o.cook_id = cu.id, so there is
                    -- exactly one matching row per order and ANY_VALUE picks from
                    -- a set of one — it cannot return the wrong cook.
                    ANY_VALUE(cu.full_name) as cook_name,
                    ANY_VALUE(cp.kitchen_name) as kitchen_name,
                    COUNT(oi.id) as items_count,
                    GROUP_CONCAT(CONCAT(oi.quantity, '× ', m.name) SEPARATOR ', ') as items_summary,
                    MIN(m.image_url) as meal_image,
                    CONCAT('[', GROUP_CONCAT(JSON_OBJECT(
                        'id', oi.id,
                        'meal_id', oi.meal_id,
                        'meal_name', m.name,
                        'quantity', oi.quantity,
                        'price_at_time', oi.price_at_time,
                        'image_url', m.image_url
                    ) SEPARATOR ','), ']') as items_json
             FROM orders o
             LEFT JOIN users cu ON o.cook_id = cu.id
             LEFT JOIN cook_profiles cp ON cu.id = cp.user_id
             LEFT JOIN order_items oi ON o.id = oi.order_id
             LEFT JOIN meals m ON oi.meal_id = m.id
             WHERE o.customer_id = ?
             GROUP BY o.id
             ORDER BY o.created_at DESC`,
            [customerId]
        );

        // JSON_ARRAYAGG comes back as a JSON string (or already parsed, depending on
        // the mysql2 version) — normalize it into a real "items" array per order so
        // the Android app can render a real per-item image carousel.
        orders.forEach(order => {
            try {
                order.items = typeof order.items_json === 'string'
                    ? JSON.parse(order.items_json)
                    : (order.items_json || []);
            } catch (e) {
                order.items = [];
            }
            delete order.items_json;
        });

        return res.status(200).json({
            success: true,
            orders
        });

    } catch (error) {
        console.error("getCustomerOrders error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/orders/cook/my
// Cook views all their incoming orders
export const getCookOrders = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { status } = req.query;

        // One row per order (not per item), aggregated the same way as the
        // customer's order history so the Android cook-side card can render
        // the identical per-item image carousel + item chips.
        let query = `
            SELECT o.id, o.customer_id, o.total_amount, o.status,
                    o.delivery_address, o.special_instructions,
                    o.payment_method, o.payment_status, o.payment_screenshot_url,
                    o.payment_verified_at, o.created_at, o.updated_at,
                    -- Joined-table columns need ANY_VALUE under only_full_group_by
                    -- (see getCustomerOrders). Exactly one users row per order via
                    -- o.customer_id = u.id, so the value is unambiguous.
                    ANY_VALUE(u.full_name) as customer_name,
                    ANY_VALUE(u.phone) as customer_phone,
                    ANY_VALUE(u.profile_image) as customer_profile_image,
                    COUNT(oi.id) as items_count,
                    GROUP_CONCAT(CONCAT(oi.quantity, '× ', m.name) SEPARATOR ', ') as items_summary,
                    MIN(m.image_url) as meal_image,
                    CONCAT('[', GROUP_CONCAT(JSON_OBJECT(
                        'id', oi.id,
                        'meal_id', oi.meal_id,
                        'meal_name', m.name,
                        'quantity', oi.quantity,
                        'price_at_time', oi.price_at_time,
                        'image_url', m.image_url
                    ) SEPARATOR ','), ']') as items_json,
                    -- Distinguishes an eSewa auto-confirmation from the older
                    -- manual-screenshot flow — both reach payment_status='paid'
                    -- but only the latter still needs the cook to verify anything.
                    EXISTS(SELECT 1 FROM payments p WHERE p.order_id = o.id AND p.status = 'SUCCESS') AS esewa_confirmed
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             LEFT JOIN order_items oi ON o.id = oi.order_id
             LEFT JOIN meals m ON oi.meal_id = m.id
             WHERE o.cook_id = ?`;

        const params = [cookId];

        if (status) {
            query += ` AND o.status = ?`;
            params.push(status);
        }

        query += ` GROUP BY o.id
                   ORDER BY o.created_at DESC`;

        const [orders] = await db.promise().query(query, params);

        orders.forEach(o => {
            // MySQL returns EXISTS(...) as a 0/1 TINYINT — Gson won't map that
            // onto a Java boolean, so normalize it here (same fix as chat's normalizeMessage).
            o.esewa_confirmed = !!o.esewa_confirmed;

            // items_json comes back as a JSON string (or already parsed, depending on
            // the mysql2 version) — normalize it into a real "items" array per order
            // so the Android app can render a real per-item image carousel.
            try {
                o.items = typeof o.items_json === 'string'
                    ? JSON.parse(o.items_json)
                    : (o.items_json || []);
            } catch (e) {
                o.items = [];
            }
            delete o.items_json;
        });

        return res.status(200).json({
            success: true,
            orders
        });

    } catch (error) {
        console.error("getCookOrders error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/orders/:orderId/status
// Cook updates order status
export const updateOrderStatus = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { orderId } = req.params;
        const { status } = req.body;

        const validStatuses = [
            "pending",
            "confirmed",
            "preparing",
            "ready",
            "delivered",
            "cancelled"
        ];

        if (!validStatuses.includes(status)) {
            return res.status(400).json({
                success: false,
                message: `Status must be one of: ${validStatuses.join(", ")}`
            });
        }

        // Check order belongs to this cook
        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND cook_id = ?",
            [orderId, cookId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        // SECURITY CHECK: For online payment orders, block "delivered" status
        // until payment has been verified by the cook
        if (status === 'delivered' && order.payment_method === 'online' && order.payment_status !== 'verified') {
            return res.status(403).json({
                success: false,
                message: "Payment not verified yet — verify the customer's payment proof before marking this order delivered."
            });
        }

        await db.promise().query(
            "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?",
            [status, orderId]
        );

        if (status === "delivered") {
            await applyDeliveryCommission(orderId);
        }

        // Get cook name for notification
        const [cook] = await db.promise().query(
            'SELECT full_name FROM users WHERE id = ?',
            [cookId]
        );

        // Send notification to customer
        console.log(`🔔 Calling notifyOrderStatusUpdate for customer ${order.customer_id}, order ${orderId}, status: ${status}`);
        await notifyOrderStatusUpdate(order.customer_id, orderId, status, cook[0].full_name);
        console.log(`✅ notifyOrderStatusUpdate completed`);

        // Emit real-time update to customer
        const io = req.app.get("io");
        if (io) {
            io.to(`order_${orderId}`).emit("orderStatusUpdated", {
                orderId,
                status,
                updatedAt: new Date().toISOString()
            });
        }

        return res.status(200).json({
            success: true,
            message: `Order status updated to "${status}".`
        });

    } catch (error) {
        console.error("updateOrderStatus error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/orders/:orderId/cancel
// Customer cancels their order within the 10-minute grace window
export const cancelOrder = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { orderId } = req.params;
        const { reason } = req.body;

        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND customer_id = ?",
            [orderId, customerId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        // Enforce 10-minute grace window — only allowed while status is 'pending'
        if (order.status !== 'pending') {
            return res.status(400).json({
                success: false,
                message: `Cannot cancel order with status "${order.status}".`
            });
        }

        // Check time window
        const [[{ minutesSinceCreated }]] = await db.promise().query(
            "SELECT TIMESTAMPDIFF(MINUTE, created_at, NOW()) as minutesSinceCreated FROM orders WHERE id = ?",
            [orderId]
        );

        if (minutesSinceCreated > 10) {
            return res.status(400).json({
                success: false,
                message: "Orders can only be cancelled within 10 minutes of placing them."
            });
        }

        // Determine refund status
        const refundStatus = ['paid', 'verified'].includes(order.payment_status) ? 'pending' : 'not_applicable';

        await db.promise().query(
            `UPDATE orders SET status = 'cancelled', cancelled_by = 'customer', cancellation_reason = ?, refund_status = ?, updated_at = NOW() WHERE id = ?`,
            [reason || null, refundStatus, orderId]
        );

        // Get customer name for notification
        const [customer] = await db.promise().query(
            'SELECT full_name FROM users WHERE id = ?',
            [customerId]
        );

        // Send notification to cook
        await notifyOrderCancelled(order.cook_id, orderId, customer[0].full_name);

        // Notify cook via Socket.IO
        const io = req.app.get("io");
        if (io) {
            io.to(`order_${orderId}`).emit("orderCancelled", {
                orderId,
                cancelled_by: 'customer',
                reason: reason || null
            });
            io.to(`cook_${order.cook_id}`).emit("orderCancelled", {
                orderId,
                message: `${customer[0].full_name} cancelled the order.`,
                reason: reason || null
            });
        }

        return res.status(200).json({
            success: true,
            message: "Order cancelled successfully."
        });

    } catch (error) {
        console.error("cancelOrder error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/orders/:orderId/cook-cancel
// Cook cancels an order (wider window: pending, confirmed, or preparing)
export const cookCancelOrder = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { orderId } = req.params;
        const { reason } = req.body;

        if (!reason || !reason.trim()) {
            return res.status(400).json({
                success: false,
                message: "Reason is required when cancelling an order."
            });
        }

        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND cook_id = ?",
            [orderId, cookId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        // Cook can cancel if pending, confirmed, or preparing
        if (!['pending', 'confirmed', 'preparing'].includes(order.status)) {
            return res.status(400).json({
                success: false,
                message: `Cannot cancel order with status "${order.status}". Only pending, confirmed, or preparing orders can be cancelled by the cook.`
            });
        }

        // Determine refund status
        const refundStatus = ['paid', 'verified'].includes(order.payment_status) ? 'pending' : 'not_applicable';

        await db.promise().query(
            `UPDATE orders SET status = 'cancelled', cancelled_by = 'cook', cancellation_reason = ?, refund_status = ?, updated_at = NOW() WHERE id = ?`,
            [reason, refundStatus, orderId]
        );

        // Get cook name for notification
        const [cook] = await db.promise().query(
            'SELECT full_name FROM users WHERE id = ?',
            [cookId]
        );

        // Send notification to customer
        await notifyOrderCancelledToCustomer(order.customer_id, orderId, reason, cook[0].full_name);

        // Notify customer via Socket.IO
        const io = req.app.get("io");
        if (io) {
            io.to(`order_${orderId}`).emit("orderCancelled", {
                orderId,
                cancelled_by: 'cook',
                reason
            });
        }

        return res.status(200).json({
            success: true,
            message: "Order cancelled and customer has been notified."
        });

    } catch (error) {
        console.error("cookCancelOrder error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/orders/cook/earnings
// Cook views earnings summary
export const getCookEarnings = async (req, res) => {
    try {
        const cookId = req.user.id;

        const [summary] = await db.promise().query(
            `SELECT
                COUNT(*) as total_orders,
                SUM(CASE WHEN status = 'delivered'
                    THEN total_amount ELSE 0 END) as total_earned,
                SUM(CASE WHEN status = 'delivered'
                    THEN COALESCE(commission_amount, 0) ELSE 0 END) as total_commission,
                SUM(CASE WHEN DATE(created_at) = CURDATE()
                    THEN total_amount ELSE 0 END) as today_earned,
                SUM(CASE WHEN status = 'delivered'
                    AND MONTH(created_at) = MONTH(NOW())
                    THEN total_amount ELSE 0 END) as this_month_earned,
                COUNT(CASE WHEN status = 'pending'
                    THEN 1 END) as pending_orders
             FROM orders
             WHERE cook_id = ?`,
            [cookId]
        );

        return res.status(200).json({
            success: true,
            earnings: summary[0]
        });

    } catch (error) {
        console.error("getCookEarnings error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// POST /api/orders/:orderId/payment-screenshot
// Customer uploads payment screenshot for online payment
export const uploadPaymentScreenshot = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { orderId } = req.params;
        const { payment_screenshot_url } = req.body;

        if (!payment_screenshot_url) {
            return res.status(400).json({
                success: false,
                message: "payment_screenshot_url is required"
            });
        }

        // Check order belongs to this customer
        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND customer_id = ?",
            [orderId, customerId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        if (order.payment_method !== 'online') {
            return res.status(400).json({
                success: false,
                message: "This order is not using online payment method"
            });
        }

        // Update payment screenshot and mark as paid (waiting for verification).
        //
        // The attempt counter is bumped here rather than on rejection so the cook
        // knows, when they open the proof, that this is the Nth one — and the
        // previous rejection reason is cleared because it describes an image that
        // has just been replaced.
        await db.promise().query(
            `UPDATE orders
             SET payment_screenshot_url = ?,
                 payment_status = 'paid',
                 payment_proof_attempts = payment_proof_attempts + 1,
                 payment_rejection_reason = NULL,
                 payment_rejected_at = NULL,
                 updated_at = NOW()
             WHERE id = ?`,
            [payment_screenshot_url, orderId]
        );

        // Notify cook about payment submission
        const [customer] = await db.promise().query(
            'SELECT full_name FROM users WHERE id = ?',
            [customerId]
        );

        console.log(`🔔 Calling createNotification for cook ${order.cook_id} about payment proof for order ${orderId}`);
        await createNotification(
            order.cook_id,
            order.combo_id ? 'Combo Payment Proof Submitted' : 'Payment Proof Submitted',
            order.combo_id
                ? `${customer[0].full_name} submitted payment proof for a combo order. Review it to accept the order.`
                : `${customer[0].full_name} submitted payment proof for Order #${orderId}. Please verify it.`,
            'payment_verification',
            orderId,
            'order',
            { pushData: { type: 'payment_verification', orderId: String(orderId) } }
        );
        console.log(`✅ createNotification completed for payment proof`);

        return res.status(200).json({
            success: true,
            message: "Payment screenshot uploaded. Waiting for cook verification."
        });

    } catch (error) {
        console.error("uploadPaymentScreenshot error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/orders/:orderId/verify-payment
// Cook verifies customer's payment screenshot
export const verifyPayment = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { orderId } = req.params;
        const { verified, reason } = req.body; // verified: true or false

        if (verified === undefined) {
            return res.status(400).json({
                success: false,
                message: "verified field is required (true/false)"
            });
        }

        // A rejection the customer can't act on is worse than no rejection —
        // they'd be told "not verified" with nothing to fix. Same rule the
        // subscription proof flow enforces.
        const rejectionReason = typeof reason === "string" ? reason.trim() : "";
        if (!verified && !rejectionReason) {
            return res.status(400).json({
                success: false,
                message: "Give a reason for rejecting — it's what the customer has to fix."
            });
        }
        if (rejectionReason.length > 500) {
            return res.status(400).json({
                success: false,
                message: "Reason is too long (max 500 characters)."
            });
        }

        // Check order belongs to this cook
        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND cook_id = ?",
            [orderId, cookId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        if (order.payment_method !== 'online') {
            return res.status(400).json({
                success: false,
                message: "This order is not using online payment method"
            });
        }

        if (order.payment_status !== 'paid') {
            return res.status(400).json({
                success: false,
                message: "Payment proof must be submitted before it can be verified."
            });
        }

        if (verified) {
            // Payment verified - update status and confirm order
            await db.promise().query(
                `UPDATE orders
                 SET payment_status = 'verified',
                     payment_verified_at = NOW(),
                     payment_rejection_reason = NULL,
                     payment_rejected_at = NULL,
                     status = CASE WHEN status = 'pending' THEN 'confirmed' ELSE status END,
                     updated_at = NOW()
                 WHERE id = ?`,
                [orderId]
            );

            // Notify customer about verified payment
            const [cook] = await db.promise().query(
                'SELECT full_name FROM users WHERE id = ?',
                [cookId]
            );

            await createNotification(
                order.customer_id,
                order.combo_id ? 'Combo Payment Accepted' : 'Payment Verified',
                order.combo_id
                    ? `${cook[0].full_name} accepted your payment. Your combo order is confirmed and will be prepared shortly.`
                    : `${cook[0].full_name} verified your payment for Order #${orderId}. Your order is confirmed!`,
                'payment_verified',
                orderId,
                'order',
                { pushData: { type: 'payment_verified', orderId: String(orderId) } }
            );

            return res.status(200).json({
                success: true,
                message: "Payment verified successfully. Order confirmed."
            });
        } else {
            // Payment rejected — back to 'pending' so the customer's "Upload
            // payment" button re-enables, with the reason recorded.
            //
            // payment_screenshot_url is deliberately LEFT IN PLACE. Rejecting is
            // not deleting: the image is the only evidence either side has if the
            // payment turns out to be real and gets disputed. The next upload
            // overwrites it anyway.
            await db.promise().query(
                `UPDATE orders
                 SET payment_status = 'pending',
                     payment_rejection_reason = ?,
                     payment_rejected_at = NOW(),
                     payment_verified_at = NULL,
                     updated_at = NOW()
                 WHERE id = ?`,
                [rejectionReason, orderId]
            );

            // Notify customer about rejected payment
            const [cook] = await db.promise().query(
                'SELECT full_name FROM users WHERE id = ?',
                [cookId]
            );

            // Goes through createNotification (not a raw INSERT) so this actually
            // reaches the phone as a push — a customer who never opens the
            // in-app list would otherwise never learn they have to re-upload.
            await createNotification(
                order.customer_id,
                order.combo_id ? 'Combo Payment Not Accepted' : 'Payment Not Verified',
                `${cook[0].full_name} couldn't verify your payment for Order #${orderId}: `
                    + `"${rejectionReason}". Please upload a new payment screenshot.`,
                'payment_rejected',
                orderId,
                'order',
                { pushData: { type: 'payment_rejected', orderId: String(orderId) } }
            );

            return res.status(200).json({
                success: true,
                message: "Payment rejected. The customer has been told why and can upload a new screenshot."
            });
        }

    } catch (error) {
        console.error("verifyPayment error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};


/**
 * DELETE /api/orders/:orderId
 * Cook permanently deletes a completed/delivered order
 * Only delivered or completed orders can be deleted
 */
export const deleteOrder = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { orderId } = req.params;

        // Check order belongs to this cook
        const [orders] = await db.promise().query(
            "SELECT * FROM orders WHERE id = ? AND cook_id = ?",
            [orderId, cookId]
        );

        if (orders.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Order not found."
            });
        }

        const order = orders[0];

        // Only allow deletion of delivered or completed orders
        if (order.status !== 'delivered' && order.status !== 'completed') {
            return res.status(403).json({
                success: false,
                message: "Only delivered or completed orders can be deleted."
            });
        }

        // Delete order items first (foreign key constraint)
        await db.promise().query(
            "DELETE FROM order_items WHERE order_id = ?",
            [orderId]
        );

        // Delete related notifications
        await db.promise().query(
            "DELETE FROM notifications WHERE reference_id = ? AND reference_type = 'order'",
            [orderId]
        );

        // Delete the order
        await db.promise().query(
            "DELETE FROM orders WHERE id = ?",
            [orderId]
        );

        return res.status(200).json({
            success: true,
            message: "Order deleted successfully."
        });

    } catch (error) {
        console.error("deleteOrder error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
