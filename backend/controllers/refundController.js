import db from "../config/db.js";
import { notifyRefundRequested, notifyRefundFeedbackToCook, notifyRefundProcessed } from "../utils/notificationHelper.js";

const VALID_REASONS = ["failed_delivery", "cook_mistake", "customer_cancelled", "other"];
// Payment states that mean money actually changed hands — a refund only
// makes sense against one of these (mirrors cancelOrder's own
// ['paid','verified'] check for the same reason).
const REFUNDABLE_PAYMENT_STATUSES = ["paid", "verified"];

// POST /api/refunds/request — cook or customer, protected
export const requestRefund = async (req, res) => {
    try {
        const userId = req.user.id;
        const userRole = req.user.role;
        const { order_id, reason, reason_notes } = req.body;

        if (!order_id || !reason) {
            return res.status(400).json({ success: false, message: "order_id and reason are required." });
        }
        if (!VALID_REASONS.includes(reason)) {
            return res.status(400).json({ success: false, message: `reason must be one of: ${VALID_REASONS.join(", ")}` });
        }

        const [orders] = await db.promise().query("SELECT * FROM orders WHERE id = ?", [order_id]);
        if (orders.length === 0) {
            return res.status(404).json({ success: false, message: "Order not found." });
        }
        const order = orders[0];

        const belongsToRequester = (userRole === "customer" && order.customer_id === userId)
            || (userRole === "cook" && order.cook_id === userId);
        if (!belongsToRequester) {
            return res.status(403).json({ success: false, message: "This order does not belong to you." });
        }

        if (!REFUNDABLE_PAYMENT_STATUSES.includes(order.payment_status)) {
            return res.status(400).json({ success: false, message: "Cannot request a refund for an order that hasn't been paid." });
        }

        const [existing] = await db.promise().query(
            `SELECT id, status FROM refund_requests WHERE order_id = ? AND status IN ('requested', 'under_review', 'approved') LIMIT 1`,
            [order_id]
        );
        if (existing.length > 0) {
            return res.status(400).json({ success: false, message: `A refund request for this order is already "${existing[0].status}".` });
        }

        const [[latestPayment]] = await db.promise().query(
            `SELECT id FROM payments WHERE order_id = ? AND status = 'SUCCESS' ORDER BY id DESC LIMIT 1`,
            [order_id]
        );

        const [result] = await db.promise().query(
            `INSERT INTO refund_requests (order_id, payment_id, requested_by, reason, reason_notes, refund_amount)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [order_id, latestPayment ? latestPayment.id : null, userId, reason, reason_notes || null, order.total_amount]
        );

        const [[requester]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [userId]);
        const [admins] = await db.promise().query("SELECT id FROM users WHERE role = 'admin' AND is_active = TRUE");
        for (const admin of admins) {
            await notifyRefundRequested(admin.id, result.insertId, order_id, requester?.full_name || "A user");
        }

        // Also tell the cook what went wrong so they can correct it — only
        // when the CUSTOMER filed this, not when the cook filed it themselves.
        if (userRole === "customer") {
            await notifyRefundFeedbackToCook(order.cook_id, order_id, reason, reason_notes);
        }

        return res.status(201).json({
            success: true,
            message: "Refund request submitted. An admin will review it shortly.",
            refund_request_id: result.insertId
        });
    } catch (error) {
        console.error("requestRefund error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/refunds — admin only, optional ?status= filter
export const listRefunds = async (req, res) => {
    try {
        const { status } = req.query;

        let query = `
            SELECT r.*,
                   o.total_amount AS order_total_amount, o.status AS order_status,
                   cust.full_name AS customer_name, cust.phone AS customer_phone,
                   cook.full_name AS cook_name, cp.kitchen_name,
                   requester.full_name AS requested_by_name, requester.role AS requested_by_role,
                   processor.full_name AS processed_by_name
            FROM refund_requests r
            JOIN orders o ON o.id = r.order_id
            JOIN users cust ON cust.id = o.customer_id
            JOIN users cook ON cook.id = o.cook_id
            LEFT JOIN cook_profiles cp ON cp.user_id = cook.id
            JOIN users requester ON requester.id = r.requested_by
            LEFT JOIN users processor ON processor.id = r.processed_by
            WHERE 1=1`;
        const params = [];

        if (status) {
            query += " AND r.status = ?";
            params.push(status);
        }

        query += " ORDER BY r.created_at DESC";

        const [refunds] = await db.promise().query(query, params);

        return res.status(200).json({ success: true, refunds });
    } catch (error) {
        console.error("listRefunds error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// PUT /api/refunds/:id/process — admin only
// Admin has already issued the refund via the eSewa merchant dashboard (or
// decided not to) BEFORE calling this — this endpoint only records that
// decision, it never moves money itself (see migration file header).
export const processRefund = async (req, res) => {
    try {
        const adminId = req.user.id;
        const { id } = req.params;
        const { status, admin_notes } = req.body;

        // 'under_review' included so admins can flag a request as being looked
        // at without needing a separate endpoint for that lighter transition.
        const ALLOWED_STATUSES = ["under_review", "approved", "rejected", "processed"];
        if (!status || !ALLOWED_STATUSES.includes(status)) {
            return res.status(400).json({ success: false, message: `status must be one of: ${ALLOWED_STATUSES.join(", ")}` });
        }

        const [refunds] = await db.promise().query("SELECT * FROM refund_requests WHERE id = ?", [id]);
        if (refunds.length === 0) {
            return res.status(404).json({ success: false, message: "Refund request not found." });
        }
        const refund = refunds[0];

        const isFinalDecision = status === "approved" || status === "rejected" || status === "processed";

        await db.promise().query(
            `UPDATE refund_requests
             SET status = ?, admin_notes = ?, processed_by = ?, processed_at = ${isFinalDecision ? "NOW()" : "processed_at"}
             WHERE id = ?`,
            [status, admin_notes || null, isFinalDecision ? adminId : refund.processed_by, id]
        );

        if (status === "processed") {
            await db.promise().query(
                `UPDATE orders SET payment_status = 'refunded', refund_status = 'refunded', updated_at = NOW() WHERE id = ?`,
                [refund.order_id]
            );
        }

        if (isFinalDecision) {
            const [orders] = await db.promise().query("SELECT customer_id FROM orders WHERE id = ?", [refund.order_id]);
            if (orders[0]) {
                await notifyRefundProcessed(orders[0].customer_id, refund.order_id, status, admin_notes);
            }
        }

        return res.status(200).json({ success: true, message: "Refund request updated." });
    } catch (error) {
        console.error("processRefund error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};
