import db from "../config/db.js";


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
                SUM(CASE WHEN order_status = 'delivered' 
                    THEN total_amount ELSE 0 END) as total_revenue,
                SUM(CASE WHEN order_status = 'placed' 
                    THEN 1 ELSE 0 END) as pending_orders,
                SUM(CASE WHEN DATE(created_at) = CURDATE() 
                    THEN 1 ELSE 0 END) as today_orders
             FROM orders`
        );

        const [pendingCooks] = await db.promise().query(
            `SELECT COUNT(*) as pending_approvals
             FROM cook_profiles WHERE is_approved = FALSE`
        );

        return res.status(200).json({
            success: true,
            dashboard: {
                ...users[0],
                ...orders[0],
                ...pendingCooks[0]
            }
        });

    } catch (error) {
        console.error("getDashboard error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

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
            message: "Server error.",
            error: error.message
        });
    }
};


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

        // Log admin action
        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "cook_approved", `Cook user_id ${cookId} approved`]
        );

        return res.status(200).json({
            success: true,
            message: "Cook approved successfully."
        });

    } catch (error) {
        console.error("approveCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// PUT /api/admin/cooks/:cookId/reject
// Reject a cook
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

        return res.status(200).json({
            success: true,
            message: "Cook rejected."
        });

    } catch (error) {
        console.error("rejectCook error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// GET /api/admin/users
// Get all users
export const getAllUsers = async (req, res) => {
    try {
        const { role } = req.query;

        let query = `SELECT id, full_name, email, phone, role, 
                            is_active, is_verified, created_at 
                     FROM users WHERE role != 'admin'`;
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
            message: "Server error.",
            error: error.message
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
            message: "Server error.",
            error: error.message
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
                   cp.kitchen_name
            FROM orders o
            JOIN users u ON o.customer_id = u.id
            JOIN users cu ON o.cook_id = cu.id
            JOIN cook_profiles cp ON cu.id = cp.user_id
            WHERE 1=1`;

        const params = [];

        if (status) {
            query += ` AND o.order_status = ?`;
            params.push(status);
        }

        query += ` ORDER BY o.created_at DESC`;

        const [orders] = await db.promise().query(query, params);

        return res.status(200).json({
            success: true,
            orders
        });

    } catch (error) {
        console.error("getAllOrders error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};