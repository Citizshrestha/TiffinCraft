import db from "../config/db.js";
import { notifyNewOrder, notifyOrderStatusUpdate, notifyOrderCancelled } from '../utils/notificationHelper.js';

export const placeOrder = async (req, res) => {
    const connection = await db.promise().getConnection();

    try {
        const customerId = req.user.id;
        const {
            cook_id,
            meal_id,
            quantity,
            delivery_address,
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
              status, payment_method, payment_status, special_instructions)
             VALUES (?, ?, ?, ?, 'pending', ?, ?, ?)`,
            [
                customerId,
                cook_id,
                total_amount,
                delivery_address,
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

        // Send notification to cook
        await notifyNewOrder(cook_id, orderId, customer[0].full_name, total_amount);

        // Emit Socket.IO event
        const io = req.app.get("io");
        if (io) {
            io.to(`cook_${cook_id}`).emit("newOrder", {
                orderId,
                customerId,
                mealName: meal.name,
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
                    cu.full_name as cook_name,
                    cu.phone as cook_phone
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             JOIN users cu ON o.cook_id = cu.id
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

        return res.status(200).json({
            success: true,
            order: { ...order, items }
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

        const [orders] = await db.promise().query(
            `SELECT o.*, 
                    cu.full_name as cook_name,
                    cp.kitchen_name,
                    m.name as meal_name,
                    m.image_url as meal_image
             FROM orders o
             JOIN users cu ON o.cook_id = cu.id
             JOIN cook_profiles cp ON cu.id = cp.user_id
             JOIN order_items oi ON o.id = oi.order_id
             JOIN meals m ON oi.meal_id = m.id
             WHERE o.customer_id = ?
             ORDER BY o.created_at DESC`,
            [customerId]
        );

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

        let query = `
            SELECT o.*,
                   u.full_name as customer_name,
                   u.phone as customer_phone,
                   m.name as meal_name,
                   oi.quantity,
                   oi.price_at_time as meal_price
            FROM orders o
            JOIN users u ON o.customer_id = u.id
            JOIN order_items oi ON o.id = oi.order_id
            JOIN meals m ON oi.meal_id = m.id
            WHERE o.cook_id = ?`;

        const params = [cookId];

        if (status) {
            query += ` AND o.status = ?`;
            params.push(status);
        }

        query += ` ORDER BY o.created_at DESC`;

        const [orders] = await db.promise().query(query, params);

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

        await db.promise().query(
            "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?",
            [status, orderId]
        );

        const order = orders[0];

        // Get cook name for notification
        const [cook] = await db.promise().query(
            'SELECT full_name FROM users WHERE id = ?',
            [cookId]
        );

        // Send notification to customer
        await notifyOrderStatusUpdate(order.customer_id, orderId, status, cook[0].full_name);

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
// Customer cancels their order (only if placed or accepted)
export const cancelOrder = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { orderId } = req.params;

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

        // Can only cancel if pending or confirmed
        if (!["pending", "confirmed"].includes(order.status)) {
            return res.status(400).json({
                success: false,
                message: `Cannot cancel order with status "${order.status}".`
            });
        }

        await db.promise().query(
            "UPDATE orders SET status = 'cancelled', updated_at = NOW() WHERE id = ?",
            [orderId]
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
            io.to(`cook_${order.cook_id}`).emit("orderCancelled", {
                orderId,
                message: "Customer cancelled the order."
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

        // Update payment screenshot and mark as paid (waiting for verification)
        await db.promise().query(
            `UPDATE orders 
             SET payment_screenshot_url = ?, 
                 payment_status = 'paid',
                 updated_at = NOW() 
             WHERE id = ?`,
            [payment_screenshot_url, orderId]
        );

        // Notify cook about payment submission
        const [customer] = await db.promise().query(
            'SELECT full_name FROM users WHERE id = ?',
            [customerId]
        );

        await db.promise().query(
            `INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [
                order.cook_id,
                'Payment Submitted',
                `${customer[0].full_name} submitted payment proof for Order #${orderId}. Please verify.`,
                'payment_verification',
                orderId,
                'order'
            ]
        );

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
        const { verified } = req.body; // true or false

        if (verified === undefined) {
            return res.status(400).json({
                success: false,
                message: "verified field is required (true/false)"
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

        if (verified) {
            // Payment verified - update status and confirm order
            await db.promise().query(
                `UPDATE orders 
                 SET payment_status = 'verified',
                     payment_verified_at = NOW(),
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

            await db.promise().query(
                `INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
                 VALUES (?, ?, ?, ?, ?, ?)`,
                [
                    order.customer_id,
                    'Payment Verified',
                    `${cook[0].full_name} verified your payment for Order #${orderId}. Your order is confirmed!`,
                    'payment_verified',
                    orderId,
                    'order'
                ]
            );

            return res.status(200).json({
                success: true,
                message: "Payment verified successfully. Order confirmed."
            });
        } else {
            // Payment rejected - notify customer to resubmit
            await db.promise().query(
                `UPDATE orders 
                 SET payment_status = 'pending',
                     payment_screenshot_url = NULL,
                     updated_at = NOW()
                 WHERE id = ?`,
                [orderId]
            );

            // Notify customer about rejected payment
            const [cook] = await db.promise().query(
                'SELECT full_name FROM users WHERE id = ?',
                [cookId]
            );

            await db.promise().query(
                `INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
                 VALUES (?, ?, ?, ?, ?, ?)`,
                [
                    order.customer_id,
                    'Payment Verification Failed',
                    `${cook[0].full_name} could not verify your payment for Order #${orderId}. Please resubmit payment proof.`,
                    'payment_rejected',
                    orderId,
                    'order'
                ]
            );

            return res.status(200).json({
                success: true,
                message: "Payment rejected. Customer notified to resubmit."
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