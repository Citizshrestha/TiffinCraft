import db from "../config/db.js";
import bcrypt from "bcryptjs";
import { sendEmail } from "../utils/emailService.js";
import { notifyCookApprovalUpdate } from "../utils/notificationHelper.js";
import { applyDeliveryCommission } from "./commissionController.js";

const ALLOWED_ROLES = ["customer", "cook", "admin"];

// GET /api/admin/dashboard
export const getDashboard = async (req, res) => {
    try {
        const [users] = await db.promise().query(
            `SELECT 
                COUNT(*) as total_users,
                SUM(CASE WHEN role = 'customer' THEN 1 ELSE 0 END) as total_customers,
                SUM(CASE WHEN role = 'cook' THEN 1 ELSE 0 END) as total_cooks
             FROM users WHERE role != 'admin'`
        );

        const [orders] = await db.promise().query(
            `SELECT
                COUNT(*) as total_orders,
                SUM(CASE WHEN status IN ('delivered', 'completed')
                    THEN total_amount ELSE 0 END) as total_revenue,
                SUM(CASE WHEN status = 'pending'
                    THEN 1 ELSE 0 END) as pending_orders,
                SUM(CASE WHEN DATE(created_at) = CURDATE()
                    THEN 1 ELSE 0 END) as today_orders
             FROM orders`
        );

        const [pendingCooks] = await db.promise().query(
            `SELECT COUNT(*) as pending_approvals
             FROM cook_profiles WHERE is_approved = FALSE`
        );

        const [recentOrders] = await db.promise().query(
            `SELECT o.id, o.total_amount, o.status, o.created_at,
                    u.full_name as customer_name,
                    cp.kitchen_name
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             JOIN cook_profiles cp ON o.cook_id = cp.user_id
             ORDER BY o.created_at DESC
             LIMIT 5`
        );

        const [topCooks] = await db.promise().query(
            `SELECT cp.user_id, cp.kitchen_name, cp.rating, cp.total_orders
             FROM cook_profiles cp
             ORDER BY cp.rating DESC, cp.total_orders DESC
             LIMIT 5`
        );

        const [last7Days] = await db.promise().query(
            `SELECT DATE(created_at) as date,
                    COUNT(*) as orders,
                    SUM(CASE WHEN status IN ('delivered', 'completed')
                        THEN total_amount ELSE 0 END) as revenue
             FROM orders
             WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
             GROUP BY DATE(created_at)
             ORDER BY date ASC`
        );

        return res.status(200).json({
            success: true,
            dashboard: {
                ...users[0],
                ...orders[0],
                ...pendingCooks[0]
            },
            recentOrders,
            topCooks,
            last7Days
        });

    } catch (error) {
        console.error("getDashboard error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// GET /api/admin/cooks/pending
export const getPendingCooks = async (req, res) => {
    try {
        const [cooks] = await db.promise().query(
            `SELECT cp.*, u.full_name, u.email, u.phone, u.created_at as registered_at
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE cp.is_approved = FALSE
             ORDER BY u.created_at DESC`
        );

        return res.status(200).json({
            success: true,
            cooks
        });

    } catch (error) {
        console.error("getPendingCooks error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// PUT /api/admin/cooks/:cookId/approve
export const approveCook = async (req, res) => {
    try {
        const { cookId } = req.params;

        const [cooks] = await db.promise().query(
            "SELECT id FROM cook_profiles WHERE user_id = ?",
            [cookId]
        );

        if (cooks.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Cook not found."
            });
        }

        await db.promise().query(
            "UPDATE cook_profiles SET is_approved = TRUE WHERE user_id = ?",
            [cookId]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "cook_approved", `Cook user_id ${cookId} approved`]
        );

        // ── Notify the cook ──────────────────────────────────────────────
        // 1. Get cook details for email
        const [userRows] = await db.promise().query(
            "SELECT email, full_name FROM users WHERE id = ?",
            [cookId]
        );

        if (userRows.length > 0) {
            const { email, full_name } = userRows[0];

            // 2. Send email
            const mailOptions = {
                from: `"${process.env.SMTP_FROM_NAME}" <${process.env.SMTP_FROM_EMAIL}>`,
                to: email,
                subject: "Your TiffinCraft kitchen is approved! 🎉",
                html: `
                <div style="font-family: Arial, sans-serif; max-width: 480px;
                            margin: auto; padding: 32px; border-radius: 12px;
                            border: 1px solid #eee; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #2E7D32; font-size: 24px; margin: 0;">TiffinCraft</h2>
                        <p style="color: #888; font-size: 14px; margin-top: 4px;">
                            Homemade meals, crafted with love
                        </p>
                    </div>
                    <p style="color: #333; font-size: 16px;">
                        Hi <strong>${full_name}</strong>,
                    </p>
                    <div style="text-align: center; margin: 24px 0;">
                        <div style="font-size: 48px;">🎉</div>
                    </div>
                    <p style="color: #555; font-size: 15px; line-height: 1.6;">
                        Great news! Your kitchen has been approved on TiffinCraft. 🚀<br><br>
                        You can now start receiving orders and selling your delicious homemade meals.
                        Log in to your account to manage your menu and start cooking!
                    </p>
                    <div style="text-align: center; margin: 24px 0;">
                        <a href="${process.env.CLIENT_URL || 'http://localhost:3000'}"
                           style="background: #4CAF50; color: white; padding: 12px 32px;
                                  border-radius: 8px; text-decoration: none; font-weight: bold;">
                            Get Started
                        </a>
                    </div>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #bbb; font-size: 12px; text-align: center;">
                        © 2025 TiffinCraft. All rights reserved.
                    </p>
                </div>`
            };
            sendEmail(mailOptions).catch(err => {
                console.error(`Failed to send approval email to ${email}:`, err.message);
            });
        }

        // 3. In-app notification + FCM push
        await notifyCookApprovalUpdate(Number(cookId), true);

        // 4. Socket.IO real-time event to the cook's personal room
        const io = req.app.get("io");
        if (io) {
            io.to(`user_${cookId}`).emit("cookApprovalUpdate", { approved: true });
        }

        return res.status(200).json({
            success: true,
            message: "Cook approved successfully.",
            notified: true
        });

    } catch (error) {
        console.error("approveCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// PUT /api/admin/cooks/:cookId/reject
export const rejectCook = async (req, res) => {
    try {
        const { cookId } = req.params;
        const { reason } = req.body;

        await db.promise().query(
            "UPDATE cook_profiles SET is_approved = FALSE WHERE user_id = ?",
            [cookId]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "cook_rejected",
             `Cook user_id ${cookId} rejected. Reason: ${reason || "Not specified"}`]
        );

        // ── Notify the cook ──────────────────────────────────────────────
        const [userRows] = await db.promise().query(
            "SELECT email, full_name FROM users WHERE id = ?",
            [cookId]
        );

        if (userRows.length > 0) {
            const { email, full_name } = userRows[0];
            const rejectionReason = reason || "Some information needs to be updated";

            // Send email
            const mailOptions = {
                from: `"${process.env.SMTP_FROM_NAME}" <${process.env.SMTP_FROM_EMAIL}>`,
                to: email,
                subject: "Your TiffinCraft application needs changes",
                html: `
                <div style="font-family: Arial, sans-serif; max-width: 480px;
                            margin: auto; padding: 32px; border-radius: 12px;
                            border: 1px solid #eee; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #2E7D32; font-size: 24px; margin: 0;">TiffinCraft</h2>
                        <p style="color: #888; font-size: 14px; margin-top: 4px;">
                            Homemade meals, crafted with love
                        </p>
                    </div>
                    <p style="color: #333; font-size: 16px;">
                        Hi <strong>${full_name}</strong>,
                    </p>
                    <p style="color: #555; font-size: 15px; line-height: 1.6;">
                        Thank you for applying to TiffinCraft. Unfortunately, we need some
                        changes before your kitchen can be approved:
                    </p>
                    <div style="background: #FFF3E0; border-radius: 8px; padding: 16px; margin: 16px 0;">
                        <p style="color: #E65100; font-size: 14px; margin: 0;">
                            ${rejectionReason}
                        </p>
                    </div>
                    <p style="color: #555; font-size: 15px; line-height: 1.6;">
                        Please update your profile and re-submit for approval.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #bbb; font-size: 12px; text-align: center;">
                        © 2025 TiffinCraft. All rights reserved.
                    </p>
                </div>`
            };
            sendEmail(mailOptions).catch(err => {
                console.error(`Failed to send rejection email to ${email}:`, err.message);
            });
        }

        // In-app notification + FCM push
        await notifyCookApprovalUpdate(Number(cookId), false);

        // Socket.IO real-time event to the cook's personal room
        const io = req.app.get("io");
        if (io) {
            io.to(`user_${cookId}`).emit("cookApprovalUpdate", { approved: false });
        }

        return res.status(200).json({
            success: true,
            message: "Cook rejected.",
            notified: true
        });

    } catch (error) {
        console.error("rejectCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// GET /api/admin/users
// Returns ALL users (customers, cooks, admins). Supports optional ?role= filter.
export const getAllUsers = async (req, res) => {
    try {
        const { role } = req.query;

        let query = `SELECT id, full_name, email, phone, role,
                            is_active, is_verified, created_at
                     FROM users WHERE 1=1`;
        const params = [];

        if (role) {
            query += ` AND role = ?`;
            params.push(role);
        }

        query += ` ORDER BY created_at DESC`;

        const [users] = await db.promise().query(query, params);

        return res.status(200).json({
            success: true,
            users
        });

    } catch (error) {
        console.error("getAllUsers error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// POST /api/admin/users
// Create a new user (customer, cook or admin)
export const createUser = async (req, res) => {
    try {
        const { full_name, email, phone, role, password, is_active } = req.body;

        if (!full_name || !email || !phone || !role || !password) {
            return res.status(400).json({
                success: false,
                message: "full_name, email, phone, role and password are required."
            });
        }

        if (!ALLOWED_ROLES.includes(role)) {
            return res.status(400).json({
                success: false,
                message: "Role must be one of: customer, cook, admin."
            });
        }

        if (password.length < 6) {
            return res.status(400).json({
                success: false,
                message: "Password must be at least 6 characters."
            });
        }

        const [existingEmail] = await db.promise().query(
            "SELECT id FROM users WHERE email = ?", [email]
        );
        if (existingEmail.length > 0) {
            return res.status(400).json({ success: false, message: "Email is already registered." });
        }

        const [existingPhone] = await db.promise().query(
            "SELECT id FROM users WHERE phone = ?", [phone]
        );
        if (existingPhone.length > 0) {
            return res.status(400).json({ success: false, message: "Phone number is already registered." });
        }

        const password_hash = await bcrypt.hash(password, 10);

        const [result] = await db.promise().query(
            `INSERT INTO users (full_name, email, phone, password_hash, role, is_active, is_verified, auth_provider)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [full_name, email, phone, password_hash, role, is_active !== false, true, "local"]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "user_created", `User id ${result.insertId} (${email}) created`]
        );

        const [rows] = await db.promise().query(
            `SELECT id, full_name, email, phone, role, is_active, is_verified, created_at
             FROM users WHERE id = ?`,
            [result.insertId]
        );

        return res.status(201).json({
            success: true,
            message: "User created successfully.",
            user: rows[0]
        });

    } catch (error) {
        console.error("createUser error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// PUT /api/admin/users/:userId
// Update a user's profile, role and active status
export const updateUser = async (req, res) => {
    try {
        const { userId } = req.params;
        const { full_name, email, phone, role, is_active } = req.body;

        const [existing] = await db.promise().query(
            "SELECT id FROM users WHERE id = ?", [userId]
        );
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "User not found." });
        }

        if (role !== undefined && !ALLOWED_ROLES.includes(role)) {
            return res.status(400).json({
                success: false,
                message: "Role must be one of: customer, cook, admin."
            });
        }

        if (email !== undefined) {
            const [dupEmail] = await db.promise().query(
                "SELECT id FROM users WHERE email = ? AND id != ?", [email, userId]
            );
            if (dupEmail.length > 0) {
                return res.status(400).json({ success: false, message: "Email is already in use by another account." });
            }
        }

        if (phone !== undefined) {
            const [dupPhone] = await db.promise().query(
                "SELECT id FROM users WHERE phone = ? AND id != ?", [phone, userId]
            );
            if (dupPhone.length > 0) {
                return res.status(400).json({ success: false, message: "Phone number is already in use by another account." });
            }
        }

        const updates = [];
        const values = [];
        if (full_name !== undefined) { updates.push("full_name = ?"); values.push(full_name); }
        if (email !== undefined) { updates.push("email = ?"); values.push(email); }
        if (phone !== undefined) { updates.push("phone = ?"); values.push(phone); }
        if (role !== undefined) { updates.push("role = ?"); values.push(role); }
        if (is_active !== undefined) { updates.push("is_active = ?"); values.push(!!is_active); }

        if (updates.length === 0) {
            return res.status(400).json({ success: false, message: "No fields provided to update." });
        }

        values.push(userId);
        await db.promise().query(`UPDATE users SET ${updates.join(", ")} WHERE id = ?`, values);

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "user_updated", `User id ${userId} updated`]
        );

        const [rows] = await db.promise().query(
            `SELECT id, full_name, email, phone, role, is_active, is_verified, created_at
             FROM users WHERE id = ?`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            message: "User updated successfully.",
            user: rows[0]
        });

    } catch (error) {
        console.error("updateUser error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// PUT /api/admin/users/:userId/deactivate
// Deactivate a user account
export const deactivateUser = async (req, res) => {
    try {
        const { userId } = req.params;

        await db.promise().query(
            "UPDATE users SET is_active = FALSE WHERE id = ?",
            [userId]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "user_deactivated", `User id ${userId} deactivated`]
        );

        return res.status(200).json({
            success: true,
            message: "User deactivated."
        });

    } catch (error) {
        console.error("deactivateUser error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// DELETE /api/admin/users/:userId
// Permanently delete a user account
export const deleteUser = async (req, res) => {
    try {
        const { userId } = req.params;

        if (Number(userId) === req.user.id) {
            return res.status(400).json({
                success: false,
                message: "You cannot delete your own admin account."
            });
        }

        const [existing] = await db.promise().query(
            "SELECT id FROM users WHERE id = ?", [userId]
        );
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "User not found." });
        }

        await db.promise().query("DELETE FROM users WHERE id = ?", [userId]);

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "user_deleted", `User id ${userId} deleted`]
        );

        return res.status(200).json({
            success: true,
            message: "User deleted successfully."
        });

    } catch (error) {
        console.error("deleteUser error:", error);
        if (error.code === "ER_ROW_IS_REFERENCED_2" || error.code === "ER_ROW_IS_REFERENCED") {
            return res.status(409).json({
                success: false,
                message: "This user has related orders or records and cannot be deleted. Deactivate the account instead."
            });
        }
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// GET /api/admin/orders
// Get all orders with filters
export const getAllOrders = async (req, res) => {
    try {
        const { status } = req.query;

        let query = `
            SELECT o.*,
                   u.full_name as customer_name,
                   cu.full_name as cook_name,
                   cp.kitchen_name,
                   GROUP_CONCAT(m.name SEPARATOR ', ') as items_summary
            FROM orders o
            JOIN users u ON o.customer_id = u.id
            JOIN users cu ON o.cook_id = cu.id
            LEFT JOIN cook_profiles cp ON cu.id = cp.user_id
            LEFT JOIN order_items oi ON oi.order_id = o.id
            LEFT JOIN meals m ON oi.meal_id = m.id
            WHERE 1=1`;

        const params = [];

        if (status) {
            query += ` AND o.status = ?`;
            params.push(status);
        }

        query += ` GROUP BY o.id ORDER BY o.created_at DESC`;

        const [orders] = await db.promise().query(query, params);

        return res.status(200).json({
            success: true,
            orders
        });

    } catch (error) {
        console.error("getAllOrders error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// Must match orders.status's actual DB enum exactly — "completed" was
// previously listed here but isn't a valid enum value, so selecting it
// passed this check yet still failed at the UPDATE with a DB-level error
// ("Data truncated for column 'status'") under strict SQL mode.
const ORDER_STATUSES = ["pending", "confirmed", "preparing", "ready", "delivered", "cancelled"];

// PUT /api/admin/orders/:orderId/status
// Admin updates an order's status (bypasses the cook-ownership check used by the cook app)
export const updateOrderStatus = async (req, res) => {
    try {
        const { orderId } = req.params;
        const { status } = req.body;

        if (!status || !ORDER_STATUSES.includes(status)) {
            return res.status(400).json({
                success: false,
                message: `Status must be one of: ${ORDER_STATUSES.join(", ")}.`
            });
        }

        const [existing] = await db.promise().query("SELECT id FROM orders WHERE id = ?", [orderId]);
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "Order not found." });
        }

        await db.promise().query("UPDATE orders SET status = ? WHERE id = ?", [status, orderId]);

        if (status === "delivered") {
            await applyDeliveryCommission(orderId);
        }

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "order_status_updated", `Order id ${orderId} status set to ${status} by admin`]
        );

        const [rows] = await db.promise().query(
            `SELECT o.*,
                    u.full_name as customer_name,
                    cu.full_name as cook_name,
                    cp.kitchen_name,
                    GROUP_CONCAT(m.name SEPARATOR ', ') as items_summary
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             JOIN users cu ON o.cook_id = cu.id
             LEFT JOIN cook_profiles cp ON cu.id = cp.user_id
             LEFT JOIN order_items oi ON oi.order_id = o.id
             LEFT JOIN meals m ON oi.meal_id = m.id
             WHERE o.id = ?
             GROUP BY o.id`,
            [orderId]
        );

        return res.status(200).json({
            success: true,
            message: "Order status updated.",
            order: rows[0]
        });

    } catch (error) {
        console.error("updateOrderStatus error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// DELETE /api/admin/orders/:orderId
// Permanently delete an order (cascades to order_items via FK)
export const deleteOrder = async (req, res) => {
    try {
        const { orderId } = req.params;

        const [existing] = await db.promise().query("SELECT id FROM orders WHERE id = ?", [orderId]);
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "Order not found." });
        }

        await db.promise().query("DELETE FROM orders WHERE id = ?", [orderId]);

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "order_deleted", `Order id ${orderId} deleted by admin`]
        );

        return res.status(200).json({
            success: true,
            message: "Order deleted successfully."
        });

    } catch (error) {
        console.error("deleteOrder error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// GET /api/admin/meals
// Returns ALL meals across every cook
export const getAllMeals = async (req, res) => {
    try {
        const [meals] = await db.promise().query(
            `SELECT m.*, u.full_name as cook_name, cp.kitchen_name
             FROM meals m
             JOIN users u ON m.cook_id = u.id
             LEFT JOIN cook_profiles cp ON u.id = cp.user_id
             ORDER BY m.name ASC`
        );

        const formatted = meals.map(m => ({ ...m, price: parseFloat(m.price) }));

        return res.status(200).json({
            success: true,
            meals: formatted
        });

    } catch (error) {
        console.error("getAllMeals error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// POST /api/admin/meals
// Admin creates a meal on behalf of a specific cook
export const createMeal = async (req, res) => {
    try {
        const {
            cook_id, name, description, price, category, cuisine_type,
            is_available, is_vegetarian, is_vegan, image_url
        } = req.body;

        if (!cook_id || !name || !price) {
            return res.status(400).json({
                success: false,
                message: "cook_id, name and price are required."
            });
        }

        if (price <= 0) {
            return res.status(400).json({ success: false, message: "Price must be greater than zero." });
        }

        const [cookCheck] = await db.promise().query(
            "SELECT id FROM users WHERE id = ? AND role = 'cook'", [cook_id]
        );
        if (cookCheck.length === 0) {
            return res.status(400).json({ success: false, message: "Selected cook does not exist." });
        }

        const [result] = await db.promise().query(
            `INSERT INTO meals (cook_id, name, description, price, category, cuisine_type, is_available, is_vegetarian, is_vegan, image_url)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [cook_id, name, description || null, price, category || null, cuisine_type || null,
             is_available !== false, !!is_vegetarian, !!is_vegan, image_url || null]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "meal_created", `Meal id ${result.insertId} (${name}) created by admin`]
        );

        const [rows] = await db.promise().query(
            `SELECT m.*, u.full_name as cook_name, cp.kitchen_name
             FROM meals m
             JOIN users u ON m.cook_id = u.id
             LEFT JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE m.id = ?`,
            [result.insertId]
        );
        const meal = { ...rows[0], price: parseFloat(rows[0].price) };

        return res.status(201).json({
            success: true,
            message: "Meal created successfully.",
            meal
        });

    } catch (error) {
        console.error("createMeal error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// PUT /api/admin/meals/:mealId
// Admin updates any meal, regardless of which cook owns it
export const updateMeal = async (req, res) => {
    try {
        const { mealId } = req.params;
        const {
            name, description, price, category, cuisine_type,
            is_available, is_vegetarian, is_vegan, image_url, cook_id
        } = req.body;

        const [existing] = await db.promise().query("SELECT id FROM meals WHERE id = ?", [mealId]);
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "Meal not found." });
        }

        if (price !== undefined && price <= 0) {
            return res.status(400).json({ success: false, message: "Price must be greater than zero." });
        }

        if (cook_id !== undefined) {
            const [cookCheck] = await db.promise().query(
                "SELECT id FROM users WHERE id = ? AND role = 'cook'", [cook_id]
            );
            if (cookCheck.length === 0) {
                return res.status(400).json({ success: false, message: "Selected cook does not exist." });
            }
        }

        const updates = [];
        const values = [];
        if (name !== undefined) { updates.push("name = ?"); values.push(name); }
        if (description !== undefined) { updates.push("description = ?"); values.push(description); }
        if (price !== undefined) { updates.push("price = ?"); values.push(price); }
        if (category !== undefined) { updates.push("category = ?"); values.push(category); }
        if (cuisine_type !== undefined) { updates.push("cuisine_type = ?"); values.push(cuisine_type); }
        if (is_available !== undefined) { updates.push("is_available = ?"); values.push(!!is_available); }
        if (is_vegetarian !== undefined) { updates.push("is_vegetarian = ?"); values.push(!!is_vegetarian); }
        if (is_vegan !== undefined) { updates.push("is_vegan = ?"); values.push(!!is_vegan); }
        if (image_url !== undefined) { updates.push("image_url = ?"); values.push(image_url); }
        if (cook_id !== undefined) { updates.push("cook_id = ?"); values.push(cook_id); }

        if (updates.length === 0) {
            return res.status(400).json({ success: false, message: "No fields provided to update." });
        }

        values.push(mealId);
        await db.promise().query(`UPDATE meals SET ${updates.join(", ")} WHERE id = ?`, values);

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "meal_updated", `Meal id ${mealId} updated by admin`]
        );

        const [rows] = await db.promise().query(
            `SELECT m.*, u.full_name as cook_name, cp.kitchen_name
             FROM meals m
             JOIN users u ON m.cook_id = u.id
             LEFT JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE m.id = ?`,
            [mealId]
        );
        const meal = { ...rows[0], price: parseFloat(rows[0].price) };

        return res.status(200).json({
            success: true,
            message: "Meal updated successfully.",
            meal
        });

    } catch (error) {
        console.error("updateMeal error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// DELETE /api/admin/meals/:mealId
// Admin deletes any meal, regardless of which cook owns it
export const deleteMeal = async (req, res) => {
    try {
        const { mealId } = req.params;

        const [existing] = await db.promise().query("SELECT id, name FROM meals WHERE id = ?", [mealId]);
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "Meal not found." });
        }

        await db.promise().query("DELETE FROM meals WHERE id = ?", [mealId]);

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "meal_deleted", `Meal id ${mealId} (${existing[0].name}) deleted by admin`]
        );

        return res.status(200).json({
            success: true,
            message: "Meal deleted successfully."
        });

    } catch (error) {
        console.error("deleteMeal error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// GET /api/admin/cooks
// Returns ALL cooks (kitchen profile + owner details)
export const getAllCooks = async (req, res) => {
    try {
        const [cooks] = await db.promise().query(
            `SELECT cp.user_id, cp.kitchen_name, cp.rating, cp.total_orders,
                    cp.is_verified, cp.is_approved,
                    u.full_name, u.email, u.phone, u.is_active, u.created_at
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             ORDER BY u.created_at DESC`
        );

        return res.status(200).json({
            success: true,
            cooks
        });

    } catch (error) {
        console.error("getAllCooks error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// POST /api/admin/cooks
// Create a new cook account with its kitchen profile
export const createCook = async (req, res) => {
    try {
        const { full_name, email, phone, password, kitchen_name, rating, is_verified } = req.body;

        if (!full_name || !email || !phone || !password || !kitchen_name) {
            return res.status(400).json({
                success: false,
                message: "full_name, email, phone, password and kitchen_name are required."
            });
        }

        if (password.length < 6) {
            return res.status(400).json({
                success: false,
                message: "Password must be at least 6 characters."
            });
        }

        if (rating !== undefined && (rating < 0 || rating > 5)) {
            return res.status(400).json({ success: false, message: "Rating must be between 0 and 5." });
        }

        const [existingEmail] = await db.promise().query(
            "SELECT id FROM users WHERE email = ?", [email]
        );
        if (existingEmail.length > 0) {
            return res.status(400).json({ success: false, message: "Email is already registered." });
        }

        const [existingPhone] = await db.promise().query(
            "SELECT id FROM users WHERE phone = ?", [phone]
        );
        if (existingPhone.length > 0) {
            return res.status(400).json({ success: false, message: "Phone number is already registered." });
        }

        const password_hash = await bcrypt.hash(password, 10);

        const [result] = await db.promise().query(
            `INSERT INTO users (full_name, email, phone, password_hash, role, is_active, is_verified, auth_provider)
             VALUES (?, ?, ?, ?, 'cook', TRUE, TRUE, 'local')`,
            [full_name, email, phone, password_hash]
        );

        await db.promise().query(
            `INSERT INTO cook_profiles (user_id, kitchen_name, rating, is_verified, is_approved)
             VALUES (?, ?, ?, ?, TRUE)`,
            [result.insertId, kitchen_name, rating || 0, !!is_verified]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "cook_created", `Cook user_id ${result.insertId} (${email}) created`]
        );

        const [rows] = await db.promise().query(
            `SELECT cp.user_id, cp.kitchen_name, cp.rating, cp.total_orders,
                    cp.is_verified, cp.is_approved,
                    u.full_name, u.email, u.phone, u.is_active, u.created_at
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE cp.user_id = ?`,
            [result.insertId]
        );

        return res.status(201).json({
            success: true,
            message: "Cook created successfully.",
            cook: rows[0]
        });

    } catch (error) {
        console.error("createCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};

// PUT /api/admin/cooks/:userId
// Update a cook's kitchen profile and owner name
export const updateCook = async (req, res) => {
    try {
        const { userId } = req.params;
        const { full_name, kitchen_name, rating, is_verified } = req.body;

        const [existing] = await db.promise().query(
            "SELECT id FROM cook_profiles WHERE user_id = ?", [userId]
        );
        if (existing.length === 0) {
            return res.status(404).json({ success: false, message: "Cook not found." });
        }

        if (rating !== undefined && (rating < 0 || rating > 5)) {
            return res.status(400).json({ success: false, message: "Rating must be between 0 and 5." });
        }

        if (full_name !== undefined) {
            await db.promise().query("UPDATE users SET full_name = ? WHERE id = ?", [full_name, userId]);
        }

        const updates = [];
        const values = [];
        if (kitchen_name !== undefined) { updates.push("kitchen_name = ?"); values.push(kitchen_name); }
        if (rating !== undefined) { updates.push("rating = ?"); values.push(rating); }
        if (is_verified !== undefined) { updates.push("is_verified = ?"); values.push(!!is_verified); }

        if (updates.length > 0) {
            values.push(userId);
            await db.promise().query(`UPDATE cook_profiles SET ${updates.join(", ")} WHERE user_id = ?`, values);
        }

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "cook_updated", `Cook user_id ${userId} updated`]
        );

        const [rows] = await db.promise().query(
            `SELECT cp.user_id, cp.kitchen_name, cp.rating, cp.total_orders,
                    cp.is_verified, cp.is_approved,
                    u.full_name, u.email, u.phone, u.is_active, u.created_at
             FROM cook_profiles cp
             JOIN users u ON cp.user_id = u.id
             WHERE cp.user_id = ?`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            message: "Cook updated successfully.",
            cook: rows[0]
        });

    } catch (error) {
        console.error("updateCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error."
        });
    }
};
