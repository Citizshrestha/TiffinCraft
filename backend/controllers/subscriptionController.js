import db from "../config/db.js";
import { fetchPlanWithItems } from "./subscriptionPlanController.js";
import {
    notifySubscriptionPaymentSubmitted,
    notifySubscriptionVerified,
    notifySubscriptionRejected
} from "../utils/notificationHelper.js";

export function getDurationDays(duration) {
    if (duration === "2_weeks" || duration === "biweekly") return 14;
    if (duration === "monthly" || duration === "1_month") return 30;
    return 7;
}

/**
 * POST /api/subscriptions
 * MANUAL-QR path (customer pays the cook directly and uploads a screenshot).
 * The gateway path is POST /api/subscriptions/initiate — see
 * subscriptionPaymentController.js — which is what the app uses by default.
 *
 * Starts in 'pending_payment'; the subscription does NOT activate here. The
 * customer still has to pay the cook, upload a screenshot via
 * uploadSubscriptionScreenshot, and the cook has to verify it via
 * verifySubscriptionPayment before this ever generates a real delivery.
 * start_date/next_delivery_date are placeholders here — they're recomputed
 * from the actual verification date once payment is confirmed, since the
 * cook may take a day or two to verify and a stale requested date would
 * otherwise leave next_delivery_date sitting in the past.
 * Body: { plan_id, delivery_address, start_date }
 */
export const createSubscription = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { plan_id, delivery_address, start_date } = req.body;

        if (!plan_id || !delivery_address || !start_date) {
            return res.status(400).json({
                success: false,
                message: "plan_id, delivery_address, and start_date are required."
            });
        }

        const [plans] = await db.promise().query(
            "SELECT id, cook_id, duration, is_active FROM subscription_plans WHERE id = ?",
            [plan_id]
        );
        if (plans.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription plan not found." });
        }
        const plan = plans[0];
        if (!plan.is_active) {
            return res.status(400).json({ success: false, message: "This plan is no longer available." });
        }
        if (plan.cook_id === customerId) {
            return res.status(400).json({ success: false, message: "You can't subscribe to your own plan." });
        }

        // One or more of this plan's meals is currently marked unavailable by
        // the cook — block signup rather than accept payment for something
        // that can't actually be delivered (same check the recurring-order
        // job re-runs on every cycle — see subscriptionOrderJob.js).
        const fullPlan = await fetchPlanWithItems(plan_id);
        if (!fullPlan || !fullPlan.is_available) {
            return res.status(400).json({
                success: false,
                message: "This plan includes an item that's currently unavailable. Please try again once the cook restocks it."
            });
        }

        // Only a subscription that's actually RUNNING blocks a new one. The
        // previous check was `status != 'cancelled'`, which also matched
        // abandoned 'pending_payment' rows — so one tap that never got paid
        // for permanently locked the customer out of that plan with a flat
        // "You already have a subscription to this plan."
        const [live] = await db.promise().query(
            `SELECT id, status FROM subscriptions
             WHERE customer_id = ? AND plan_id = ? AND status IN ('active', 'paused')
               AND (end_date IS NULL OR end_date >= CURDATE())
             LIMIT 1`,
            [customerId, plan_id]
        );
        if (live.length > 0) {
            return res.status(409).json({
                success: false,
                message: `You already have a ${live[0].status} subscription to this plan. Cancel it first, or wait for it to finish.`
            });
        }

        const interval = getDurationDays(plan.duration);
        const startDate = new Date(start_date);
        if (Number.isNaN(startDate.getTime())) {
            return res.status(400).json({ success: false, message: "start_date is invalid." });
        }
        const nextDelivery = new Date(startDate);
        nextDelivery.setDate(nextDelivery.getDate() + interval);

        // Reuse an unpaid attempt for this plan instead of stacking dead rows.
        const [pending] = await db.promise().query(
            `SELECT id FROM subscriptions
             WHERE customer_id = ? AND plan_id = ? AND status = 'pending_payment'
             ORDER BY id DESC LIMIT 1`,
            [customerId, plan_id]
        );
        if (pending.length > 0) {
            await db.promise().query(
                `UPDATE subscriptions
                 SET delivery_address = ?, start_date = ?, next_delivery_date = ?,
                     payment_status = 'pending', payment_method = 'manual_qr',
                     payment_screenshot_url = NULL
                 WHERE id = ? AND status = 'pending_payment'`,
                [delivery_address, start_date, nextDelivery.toISOString().split('T')[0], pending[0].id]
            );
            return res.status(200).json({
                success: true,
                message: "Continuing your unpaid subscription — pay the cook and upload proof to activate it.",
                subscriptionId: pending[0].id
            });
        }

        const [result] = await db.promise().query(
            `INSERT INTO subscriptions (customer_id, cook_id, plan_id, delivery_address, start_date, next_delivery_date, status, payment_status, payment_method)
             VALUES (?, ?, ?, ?, ?, ?, 'pending_payment', 'pending', 'manual_qr')`,
            [customerId, plan.cook_id, plan_id, delivery_address, start_date, nextDelivery.toISOString().split('T')[0]]
        );

        return res.status(201).json({
            success: true,
            message: "Subscription created — pay the cook and upload proof to activate it.",
            subscriptionId: result.insertId
        });

    } catch (error) {
        console.error("createSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/screenshot — customer only.
 * Uploads to /api/upload/document first (same two-step pattern as orders and
 * commission settlements), then posts the resulting URL here.
 */
export const uploadSubscriptionScreenshot = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { payment_screenshot_url } = req.body;

        if (!payment_screenshot_url) {
            return res.status(400).json({ success: false, message: "payment_screenshot_url is required." });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];

        if (sub.customer_id !== customerId) {
            return res.status(403).json({ success: false, message: "This subscription does not belong to you." });
        }
        if (!["pending", "rejected", "failed"].includes(sub.payment_status)) {
            return res.status(400).json({ success: false, message: `Cannot submit payment proof — this subscription's payment is already "${sub.payment_status}".` });
        }

        await db.promise().query(
            `UPDATE subscriptions SET payment_screenshot_url = ?, payment_status = 'submitted' WHERE id = ?`,
            [payment_screenshot_url, id]
        );

        const [[customer]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [customerId]);
        await notifySubscriptionPaymentSubmitted(sub.cook_id, id, customer?.full_name || "A customer", sub.plan_name);

        return res.status(200).json({ success: true, message: "Payment proof submitted. The cook will verify it shortly." });
    } catch (error) {
        console.error("uploadSubscriptionScreenshot error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * PUT /api/subscriptions/:id/verify-payment — cook only.
 * Body: { status: 'verified'|'rejected', notes }
 * On verify: activates the subscription and (re)computes start_date/
 * next_delivery_date from TODAY, not the customer's originally requested
 * date — see createSubscription's comment for why.
 */
export const verifySubscriptionPayment = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const { status, notes } = req.body;

        const ALLOWED = ["verified", "rejected"];
        if (!status || !ALLOWED.includes(status)) {
            return res.status(400).json({ success: false, message: `status must be one of: ${ALLOWED.join(", ")}` });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name, p.duration FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];

        if (sub.cook_id !== cookId) {
            return res.status(403).json({ success: false, message: "This subscription does not belong to your kitchen." });
        }
        if (sub.payment_status !== "submitted") {
            return res.status(400).json({ success: false, message: `Cannot ${status === "verified" ? "verify" : "reject"} — payment proof hasn't been submitted (current status: ${sub.payment_status}).` });
        }

        if (status === "verified") {
            const interval = getDurationDays(sub.duration);
            const today = new Date();
            const nextDelivery = new Date(today);
            nextDelivery.setDate(nextDelivery.getDate() + interval);
            // A subscription is a single up-front payment, so the delivery
            // window it buys is bounded — same rule the gateway path applies
            // in subscriptionPaymentState.js, enforced in subscriptionOrderJob.js.
            const endDate = new Date(today);
            endDate.setDate(endDate.getDate() + interval);

            await db.promise().query(
                `UPDATE subscriptions
                 SET status = 'active', payment_status = 'verified', payment_verified_at = NOW(),
                     verification_notes = ?, start_date = ?, end_date = ?, next_delivery_date = ?
                 WHERE id = ? AND status = 'pending_payment'`,
                [notes || null, today.toISOString().split('T')[0], endDate.toISOString().split('T')[0],
                 nextDelivery.toISOString().split('T')[0], id]
            );
            await notifySubscriptionVerified(sub.customer_id, sub.id, sub.plan_name);
        } else {
            await db.promise().query(
                `UPDATE subscriptions SET payment_status = 'rejected', verification_notes = ? WHERE id = ?`,
                [notes || null, id]
            );
            await notifySubscriptionRejected(sub.customer_id, sub.id, sub.plan_name, notes);
        }

        return res.status(200).json({ success: true, message: `Subscription payment marked ${status}.` });
    } catch (error) {
        console.error("verifySubscriptionPayment error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

const formatSubscriptionRow = async (row) => {
    const plan = await fetchPlanWithItems(row.plan_id);

    // Cook's eSewa QR for the manual pay flow — the customer scans it in their
    // own eSewa app. Same field name and shape the order detail endpoint
    // already exposes (see orderController.js), so the two manual-pay screens
    // consume one contract. bank_details is a JSON blob (esewa_qr_url /
    // khalti_qr_url / bank_qr_url); pull just eSewa's, tolerating rows that
    // are null (cook never set one up) or malformed.
    let cookEsewaQrUrl = null;
    try {
        if (row.cook_bank_details) cookEsewaQrUrl = JSON.parse(row.cook_bank_details).esewa_qr_url || null;
    } catch (_) { /* legacy/malformed JSON — no QR */ }

    const { cook_bank_details, ...rest } = row;
    return { ...rest, plan, cook_esewa_qr_url: cookEsewaQrUrl };
};

/**
 * GET /api/subscriptions/customer/my
 * Customer views their subscriptions (active, paused, cancelled)
 */
export const getMySubscriptions = async (req, res) => {
    try {
        const customerId = req.user.id;

        const [subscriptions] = await db.promise().query(
            `SELECT s.*, cp.bank_details AS cook_bank_details
             FROM subscriptions s
             LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
             WHERE s.customer_id = ? ORDER BY s.created_at DESC`,
            [customerId]
        );

        const formatted = await Promise.all(subscriptions.map(formatSubscriptionRow));

        return res.status(200).json({
            success: true,
            subscriptions: formatted
        });

    } catch (error) {
        console.error("getMySubscriptions error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /api/subscriptions/cook/my
 * Cook views their subscribers across all plans
 */
export const getCookSubscribers = async (req, res) => {
    try {
        const cookId = req.user.id;

        const [subscriptions] = await db.promise().query(
            `SELECT s.*, p.name as plan_name, p.duration, u.full_name as customer_name,
                    u.phone as customer_phone
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             JOIN users u ON s.customer_id = u.id
             WHERE s.cook_id = ?
             ORDER BY s.created_at DESC`,
            [cookId]
        );

        const [[{ count }]] = await db.promise().query(
            "SELECT COUNT(*) as count FROM subscriptions WHERE cook_id = ? AND status = 'active'",
            [cookId]
        );

        return res.status(200).json({
            success: true,
            activeSubscriberCount: count,
            subscriptions
        });

    } catch (error) {
        console.error("getCookSubscribers error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/pause
 * Pause a subscription
 */
export const pauseSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        // Verify ownership
        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        await db.promise().query(
            "UPDATE subscriptions SET status = 'paused' WHERE id = ?",
            [id]
        );

        return res.status(200).json({
            success: true,
            message: "Subscription paused successfully."
        });

    } catch (error) {
        console.error("pauseSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/resume
 * Resume a paused subscription
 */
export const resumeSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        const sub = subs[0];
        const [[plan]] = await db.promise().query(
            "SELECT duration FROM subscription_plans WHERE id = ?",
            [sub.plan_id]
        );
        const interval = getDurationDays(plan?.duration);
        const nextDelivery = new Date();
        nextDelivery.setDate(nextDelivery.getDate() + interval);

        await db.promise().query(
            "UPDATE subscriptions SET status = 'active', next_delivery_date = ? WHERE id = ?",
            [nextDelivery.toISOString().split('T')[0], id]
        );

        return res.status(200).json({
            success: true,
            message: "Subscription resumed successfully."
        });

    } catch (error) {
        console.error("resumeSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /api/subscriptions/:id/skip
 * Customer skips a specific delivery date
 * Body: { date }
 */
export const skipDay = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { date } = req.body;

        if (!date) {
            return res.status(400).json({
                success: false,
                message: "date is required."
            });
        }

        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND customer_id = ?",
            [id, customerId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        // Append date to skipped_dates JSON array
        const sub = subs[0];
        const skipped = sub.skipped_dates ? JSON.parse(sub.skipped_dates) : [];
        skipped.push(date);
        const updatedSkipped = JSON.stringify(skipped);

        await db.promise().query(
            "UPDATE subscriptions SET skipped_dates = ? WHERE id = ?",
            [updatedSkipped, id]
        );

        return res.status(200).json({
            success: true,
            message: "Delivery date skipped successfully."
        });

    } catch (error) {
        console.error("skipDay error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * DELETE /api/subscriptions/:id
 * Cancel a subscription
 */
export const cancelSubscription = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [subs] = await db.promise().query(
            "SELECT * FROM subscriptions WHERE id = ? AND (customer_id = ? OR cook_id = ?)",
            [id, userId, userId]
        );

        if (subs.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Subscription not found."
            });
        }

        await db.promise().query(
            "UPDATE subscriptions SET status = 'cancelled' WHERE id = ?",
            [id]
        );

        return res.status(200).json({
            success: true,
            message: "Subscription cancelled successfully."
        });

    } catch (error) {
        console.error("cancelSubscription error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
