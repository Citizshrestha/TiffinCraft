import db from "../config/db.js";
import { getNptToday } from "../utils/nptTime.js";

/**
 * Admin oversight for subscriptions.
 *
 * Kept in its own file rather than appended to adminController.js (already 1500+
 * lines) because these two handlers are read-only and share a lot of SQL with
 * each other and nothing with the rest of the admin surface.
 *
 * ── Why a dispute view exists at all ─────────────────────────────────────────
 * Payment verification in this app is MANUAL and TRUST-BASED: the cook looks at
 * a screenshot the customer uploaded and taps Verify or Reject. Nothing proves
 * money moved. That means two failure modes land on a human:
 *
 *   1. A cook rejects a genuine screenshot (customer is out of pocket and blocked).
 *   2. A cook verifies a forged or reused one (cook is out of pocket).
 *
 * getPaymentDisputes() gives the admin everything needed to judge (1) without
 * asking either party for a re-upload: the screenshot URL is deliberately KEPT
 * on the row when a cook rejects it (only the uniqueness hash is cleared), the
 * cook's stated reason, how many times the customer has tried, and the full
 * subscription_payment_events trail with timestamps.
 *
 * Case (2) is partly handled mechanically instead: payment_screenshot_hash is
 * UNIQUE across the whole table, so one image can never pay for two
 * subscriptions.
 *
 * Both handlers are read-only on purpose. An admin flipping a subscription's
 * status by hand would bypass the guarded transitions (and the notifications)
 * that every other path goes through, so remediation stays a conversation plus
 * the existing refund tooling rather than a write endpoint here.
 */

// Mirrors the ENUM on subscriptions.status, in lifecycle order so the admin UI
// can render filter chips without hardcoding the list client-side.
const SUBSCRIPTION_STATUSES = [
    "requested", "accepted", "rejected",
    "pending_payment", "pending_verification", "verified", "scheduled",
    "active", "paused", "completed", "cancelled"
];

/**
 * GET /api/admin/subscriptions — admin only.
 * Query: page, limit, status (one of SUBSCRIPTION_STATUSES or 'all'), search
 *
 * Platform-wide list with the current lifecycle status of every subscription.
 */
export const getAdminSubscriptions = async (req, res) => {
    try {
        const page = Math.max(1, parseInt(req.query.page) || 1);
        const limit = Math.min(50, parseInt(req.query.limit) || 15);
        const offset = (page - 1) * limit;
        const search = req.query.search ? `%${req.query.search.trim()}%` : null;
        const status = req.query.status && req.query.status !== "all"
            ? String(req.query.status).trim()
            : null;

        // Whitelisted against the ENUM rather than interpolated: `status` reaches
        // a WHERE clause, and an unknown value should be an explicit 400 instead
        // of a silently empty list.
        if (status && !SUBSCRIPTION_STATUSES.includes(status)) {
            return res.status(400).json({
                success: false,
                message: `Unknown status "${status}". Valid values: ${SUBSCRIPTION_STATUSES.join(", ")}, all.`
            });
        }

        const conditions = [];
        const params = [];

        if (status) {
            conditions.push("s.status = ?");
            params.push(status);
        }
        if (search) {
            conditions.push(`(
                s.id LIKE ?
                OR COALESCE(cu.full_name, '') LIKE ?
                OR COALESCE(ck.full_name, '') LIKE ?
                OR COALESCE(cp.kitchen_name, '') LIKE ?
                OR COALESCE(p.name, '') LIKE ?
            )`);
            params.push(search, search, search, search, search);
        }
        const where = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";

        // Platform-wide counts, deliberately UNFILTERED — these drive the filter
        // chips, so they have to describe the whole population, not the page.
        const [statusRows] = await db.promise().query(
            `SELECT status, COUNT(*) AS n FROM subscriptions GROUP BY status`
        );
        const counts = SUBSCRIPTION_STATUSES.reduce((acc, s) => ({ ...acc, [s]: 0 }), {});
        let totalAll = 0;
        statusRows.forEach(r => {
            counts[r.status] = Number(r.n);
            totalAll += Number(r.n);
        });

        const [[countResult]] = await db.promise().query(
            `SELECT COUNT(*) AS total
             FROM subscriptions s
             JOIN subscription_plans p ON p.id = s.plan_id
             JOIN users cu ON cu.id = s.customer_id
             JOIN users ck ON ck.id = s.cook_id
             LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
             ${where}`,
            params
        );
        const total = Number(countResult?.total || 0);

        const [rows] = await db.promise().query(
            `SELECT s.id, s.status, s.payment_status,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')   AS end_date,
                    s.payment_screenshot_url,
                    s.payment_proof_attempts, s.payment_rejection_reason,
                    s.requested_at, s.responded_at, s.payment_submitted_at,
                    s.verified_at, s.created_at,
                    p.name AS plan_name, p.duration, p.price_per_delivery AS amount,
                    cu.id AS customer_id, cu.full_name AS customer_name, cu.phone AS customer_phone,
                    ck.id AS cook_id, ck.full_name AS cook_name,
                    cp.kitchen_name
             FROM subscriptions s
             JOIN subscription_plans p ON p.id = s.plan_id
             JOIN users cu ON cu.id = s.customer_id
             JOIN users ck ON ck.id = s.cook_id
             LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
             ${where}
             ORDER BY s.id DESC
             LIMIT ? OFFSET ?`,
            [...params, limit, offset]
        );

        const today = getNptToday();

        return res.status(200).json({
            success: true,
            today,
            statuses: SUBSCRIPTION_STATUSES,
            counts,
            summary: {
                total: totalAll,
                live: counts.active + counts.scheduled + counts.verified,
                awaiting_cook: counts.requested,
                awaiting_payment: counts.accepted + counts.pending_payment,
                awaiting_verification: counts.pending_verification
            },
            subscriptions: rows.map(r => ({
                id: r.id,
                status: r.status,
                payment_status: r.payment_status,
                plan_name: r.plan_name,
                duration: r.duration,
                customer: { id: r.customer_id, name: r.customer_name, phone: r.customer_phone },
                cook: { id: r.cook_id, name: r.kitchen_name || r.cook_name },
                start_date: r.start_date,
                end_date: r.end_date,
                amount_per_day: r.amount === null ? null : Number(r.amount),
                // Present only so the admin list can show a paperclip; the full
                // image and the trail live in the dispute view.
                has_payment_proof: !!r.payment_screenshot_url,
                payment_proof_attempts: r.payment_proof_attempts,
                created_at: r.created_at
            })),
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.max(1, Math.ceil(total / limit))
            }
        });
    } catch (error) {
        console.error("getAdminSubscriptions error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * GET /api/admin/subscriptions/disputes — admin only.
 * Query: page, limit
 *
 * Every subscription where a payment proof was rejected or has been resubmitted,
 * newest first. This is the screen an admin opens when a customer says "I paid
 * and the cook rejected my screenshot".
 */
export const getPaymentDisputes = async (req, res) => {
    try {
        const page = Math.max(1, parseInt(req.query.page) || 1);
        const limit = Math.min(50, parseInt(req.query.limit) || 15);
        const offset = (page - 1) * limit;

        // Two signals of a contested payment, either one qualifies:
        //   • payment_status = 'rejected'  — a cook actively said no.
        //   • payment_proof_attempts > 1   — the customer had to upload more than
        //     once, which only happens after a rejection, and stays visible even
        //     after a later attempt was accepted (the argument may be about the
        //     rejected one).
        const where = `WHERE s.payment_status = 'rejected' OR s.payment_proof_attempts > 1`;

        const [[countResult]] = await db.promise().query(
            `SELECT COUNT(*) AS total FROM subscriptions s ${where}`
        );
        const total = Number(countResult?.total || 0);

        const [rows] = await db.promise().query(
            `SELECT s.id, s.status, s.payment_status,
                    s.payment_screenshot_url, s.payment_screenshot_hash,
                    s.payment_proof_attempts, s.payment_rejection_reason,
                    s.payment_submitted_at, s.verified_at, s.verified_by,
                    s.response_note,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')   AS end_date,
                    p.name AS plan_name, p.duration, p.price_per_delivery AS amount,
                    cu.id AS customer_id, cu.full_name AS customer_name,
                    cu.phone AS customer_phone, cu.email AS customer_email,
                    ck.id AS cook_id, ck.full_name AS cook_name, ck.phone AS cook_phone,
                    cp.kitchen_name
             FROM subscriptions s
             JOIN subscription_plans p ON p.id = s.plan_id
             JOIN users cu ON cu.id = s.customer_id
             JOIN users ck ON ck.id = s.cook_id
             LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
             ${where}
             ORDER BY s.payment_submitted_at DESC, s.id DESC
             LIMIT ? OFFSET ?`,
            [limit, offset]
        );

        // One batched query for the audit trails instead of one per row. Empty
        // guard because `IN (?)` with an empty array is a SQL syntax error.
        let eventsBySub = new Map();
        if (rows.length) {
            const ids = rows.map(r => r.id);
            const [events] = await db.promise().query(
                `SELECT subscription_id, event, amount, detail, created_at
                 FROM subscription_payment_events
                 WHERE subscription_id IN (?)
                 ORDER BY id ASC`,
                [ids]
            );
            eventsBySub = events.reduce((map, e) => {
                if (!map.has(e.subscription_id)) map.set(e.subscription_id, []);
                map.get(e.subscription_id).push({
                    event: e.event,
                    amount: e.amount === null ? null : Number(e.amount),
                    detail: e.detail,
                    created_at: e.created_at
                });
                return map;
            }, new Map());
        }

        return res.status(200).json({
            success: true,
            // Restated in the payload so the admin screen can show it verbatim
            // rather than the frontend inventing its own wording.
            verification_note:
                "Payment verification is manual and trust-based: a cook approves or rejects a screenshot by eye. "
                + "The only mechanical check is that one image file can never be submitted for two subscriptions. "
                + "Judge these from the screenshot and the event trail, not from the status alone.",
            disputes: rows.map(r => ({
                subscription_id: r.id,
                status: r.status,
                payment_status: r.payment_status,
                plan_name: r.plan_name,
                duration: r.duration,
                amount_per_day: r.amount === null ? null : Number(r.amount),
                start_date: r.start_date,
                end_date: r.end_date,
                customer: {
                    id: r.customer_id,
                    name: r.customer_name,
                    phone: r.customer_phone,
                    email: r.customer_email
                },
                cook: {
                    id: r.cook_id,
                    name: r.kitchen_name || r.cook_name,
                    phone: r.cook_phone
                },
                // The image the cook rejected is still here on purpose — this is
                // the whole point of keeping the URL when the hash is cleared.
                payment_screenshot_url: r.payment_screenshot_url,
                screenshot_sha256: r.payment_screenshot_hash,
                attempts: r.payment_proof_attempts,
                cook_rejection_reason: r.payment_rejection_reason,
                cook_response_note: r.response_note,
                submitted_at: r.payment_submitted_at,
                verified_at: r.verified_at,
                verified_by: r.verified_by,
                // Customer is still blocked right now, so this one needs an
                // answer before the others.
                is_open: r.payment_status === "rejected",
                events: eventsBySub.get(r.id) || []
            })),
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.max(1, Math.ceil(total / limit))
            }
        });
    } catch (error) {
        console.error("getPaymentDisputes error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

export default { getAdminSubscriptions, getPaymentDisputes };
