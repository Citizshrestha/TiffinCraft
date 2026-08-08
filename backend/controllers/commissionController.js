import db from "../config/db.js";
import {
    notifyCommissionDue,
    notifyCommissionSettlementSubmitted,
    notifyCommissionSettlementVerified,
    notifyCommissionSettlementRejected
} from "../utils/notificationHelper.js";

/**
 * Platform commission — one global rate admins can change, snapshotted onto
 * each order at delivery time (see applyDeliveryCommission) so a later rate
 * change never rewrites what past orders actually owed.
 */

// GET /api/admin/commission/settings
export const getCommissionSettings = async (req, res) => {
    try {
        const [[settings]] = await db.promise().query(
            "SELECT commission_pct, updated_at FROM platform_settings WHERE id = 1"
        );
        return res.status(200).json({
            success: true,
            commission_pct: settings ? parseFloat(settings.commission_pct) : 4.00,
            updated_at: settings ? settings.updated_at : null
        });
    } catch (error) {
        console.error("getCommissionSettings error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// PUT /api/admin/commission/settings
export const updateCommissionSettings = async (req, res) => {
    try {
        const { commission_pct } = req.body;
        const pct = Number(commission_pct);

        if (commission_pct === undefined || Number.isNaN(pct) || pct < 0 || pct > 100) {
            return res.status(400).json({ success: false, message: "commission_pct must be a number between 0 and 100." });
        }

        await db.promise().query(
            "UPDATE platform_settings SET commission_pct = ?, updated_by = ? WHERE id = 1",
            [pct, req.user.id]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "commission_updated", `Platform commission rate set to ${pct}%`]
        );

        return res.status(200).json({ success: true, message: "Commission rate updated.", commission_pct: pct });
    } catch (error) {
        console.error("updateCommissionSettings error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

/**
 * Snapshots the current commission rate onto an order the moment it's
 * marked 'delivered' — called from both the cook's and the admin's
 * order-status endpoints (the only two places status can become
 * 'delivered'). Idempotent via the "commission_amount IS NULL" guard, so
 * calling it twice (e.g. an admin re-saving the same status) never
 * re-charges commission at a possibly-changed rate.
 */
export const applyDeliveryCommission = async (orderId) => {
    try {
        const [[settings]] = await db.promise().query(
            "SELECT commission_pct FROM platform_settings WHERE id = 1"
        );
        const pct = settings ? parseFloat(settings.commission_pct) : 4.00;

        // delivered_at is the true, immutable delivery moment — unlike updated_at
        // (which changes on any later touch to the row), this is set once here
        // and never again, so commission period grouping stays stable forever.
        await db.promise().query(
            `UPDATE orders
             SET commission_pct = ?, commission_amount = ROUND(total_amount * ? / 100, 2), delivered_at = NOW()
             WHERE id = ? AND commission_amount IS NULL`,
            [pct, pct, orderId]
        );
    } catch (error) {
        // Never let a commission-snapshot failure block the delivery status
        // update itself — log it and move on.
        console.error(`❌ applyDeliveryCommission failed for order #${orderId}:`, error.message);
    }
};

/**
 * Per-cook commission totals for a given period — "how much does each cook
 * owe for this month". Shared by getCommissionSummary (admin dashboard
 * view) and generateMonthlySettlements (the actual dues generator), so
 * there is exactly one place that computes this.
 */
const getCommissionByCook = async (month, year) => {
    const [rows] = await db.promise().query(
        `SELECT
            o.cook_id,
            u.full_name AS owner_name,
            cp.kitchen_name,
            COUNT(*) AS order_count,
            COALESCE(SUM(o.total_amount), 0) AS gross_total,
            COALESCE(SUM(o.commission_amount), 0) AS commission_total
         FROM orders o
         JOIN users u ON u.id = o.cook_id
         LEFT JOIN cook_profiles cp ON cp.user_id = o.cook_id
         WHERE o.status = 'delivered' AND o.commission_amount IS NOT NULL
           AND MONTH(o.delivered_at) = ? AND YEAR(o.delivered_at) = ?
         GROUP BY o.cook_id, u.full_name, cp.kitchen_name
         ORDER BY commission_total DESC`,
        [month, year]
    );
    return rows;
};

/**
 * GET /api/admin/commission/summary?month=&year=
 * Defaults to the current month. Only counts orders that actually got a
 * commission snapshot (delivered under this feature) — orders delivered
 * before it shipped have commission_amount NULL, correctly contributing $0
 * (there was genuinely no commission at MVP).
 */
export const getCommissionSummary = async (req, res) => {
    try {
        const now = new Date();
        const month = parseInt(req.query.month) || now.getMonth() + 1;
        const year = parseInt(req.query.year) || now.getFullYear();

        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            return res.status(400).json({ success: false, message: "Invalid month or year." });
        }

        const [[totals]] = await db.promise().query(
            `SELECT
                COALESCE(SUM(commission_amount), 0) AS total_commission,
                COALESCE(SUM(total_amount), 0) AS total_gross,
                COUNT(*) AS order_count
             FROM orders
             WHERE status = 'delivered' AND commission_amount IS NOT NULL
               AND MONTH(delivered_at) = ? AND YEAR(delivered_at) = ?`,
            [month, year]
        );

        const byCook = await getCommissionByCook(month, year);

        // Last 6 months trend, same fill-missing-months-with-zero approach
        // used by the cook earnings screen's chart. Grouped by delivered_at,
        // not updated_at — see applyDeliveryCommission's comment for why.
        const [monthlyRows] = await db.promise().query(
            `SELECT
                DATE_FORMAT(delivered_at, '%Y-%m') AS ym,
                COALESCE(SUM(commission_amount), 0) AS commission,
                COALESCE(SUM(total_amount - commission_amount), 0) AS cook_net
             FROM orders
             WHERE status = 'delivered' AND commission_amount IS NOT NULL
               AND delivered_at >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH)
             GROUP BY ym
             ORDER BY ym ASC`
        );
        const trend = [];
        for (let i = 5; i >= 0; i--) {
            const d = new Date();
            d.setDate(1);
            d.setMonth(d.getMonth() - i);
            const ym = d.toISOString().slice(0, 7);
            const existing = monthlyRows.find(r => r.ym === ym);
            trend.push({
                month: d.toLocaleString('en-US', { month: 'short' }),
                commission: existing ? parseFloat(existing.commission) : 0,
                cookNet: existing ? parseFloat(existing.cook_net) : 0
            });
        }

        const [[settings]] = await db.promise().query(
            "SELECT commission_pct FROM platform_settings WHERE id = 1"
        );

        return res.status(200).json({
            success: true,
            month,
            year,
            commission_pct: settings ? parseFloat(settings.commission_pct) : 4.00,
            total_commission: parseFloat(totals.total_commission),
            total_gross: parseFloat(totals.total_gross),
            order_count: totals.order_count,
            by_cook: byCook.map(r => ({
                cook_id: r.cook_id,
                owner_name: r.owner_name,
                kitchen_name: r.kitchen_name,
                order_count: r.order_count,
                gross_total: parseFloat(r.gross_total),
                commission_total: parseFloat(r.commission_total)
            })),
            trend
        });
    } catch (error) {
        console.error("getCommissionSummary error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

/**
 * Commission Settlement — how the admin actually collects commission money
 * from cooks. Admin has no eSewa merchant account, so this mirrors the same
 * "manual QR + screenshot + verify" pattern already used for customer→cook
 * order payments (see orderController.js) and for refunds (admin resolves
 * money movement out-of-band, this just tracks the state): the admin posts
 * their own QR (platform_settings.bank_details), a cook pays it externally
 * once a month for their accrued commission, uploads a screenshot, and the
 * admin verifies it. commission_settlements.amount_due is frozen at
 * generation time — same convention as orders.commission_amount — so a
 * later rate change or order edit never silently shifts what a cook
 * already owes for a closed period.
 */

// PUT /api/commission/admin-qr — admin only
export const updateAdminBankDetails = async (req, res) => {
    try {
        const { bank_details } = req.body;
        if (!bank_details) {
            return res.status(400).json({ success: false, message: "bank_details is required." });
        }

        await db.promise().query(
            "UPDATE platform_settings SET bank_details = ? WHERE id = 1",
            [JSON.stringify(bank_details)]
        );

        return res.status(200).json({ success: true, message: "Platform payment QR updated.", bank_details });
    } catch (error) {
        console.error("updateAdminBankDetails error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// GET /api/commission/admin-qr — cook or admin
export const getAdminQr = async (req, res) => {
    try {
        const [[settings]] = await db.promise().query(
            "SELECT bank_details FROM platform_settings WHERE id = 1"
        );
        let bankDetails = null;
        try {
            bankDetails = settings && settings.bank_details ? JSON.parse(settings.bank_details) : null;
        } catch (_) {
            bankDetails = null;
        }
        return res.status(200).json({ success: true, bank_details: bankDetails });
    } catch (error) {
        console.error("getAdminQr error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

/**
 * Generates (or re-runs safely) the commission_settlements rows for one
 * period. Idempotent via INSERT IGNORE + the (cook_id, month, year) unique
 * key — safe to call twice (e.g. a server restart mid-run, or an admin
 * re-triggering it for testing) without double-charging anyone.
 * Returns the number of new settlement rows created.
 */
export const generateMonthlySettlements = async (month, year) => {
    const byCook = await getCommissionByCook(month, year);
    let created = 0;

    for (const row of byCook) {
        if (parseFloat(row.commission_total) <= 0) continue;

        const [result] = await db.promise().query(
            `INSERT IGNORE INTO commission_settlements (cook_id, month, year, amount_due, order_count)
             VALUES (?, ?, ?, ?, ?)`,
            [row.cook_id, month, year, row.commission_total, row.order_count]
        );

        if (result.affectedRows > 0) {
            created++;
            await notifyCommissionDue(row.cook_id, row.commission_total, month, year);
        }
    }

    return created;
};

// POST /api/commission/settlements/generate?month=&year= — admin only.
// Defaults to the PREVIOUS month (the cron job runs on the 1st, for the
// month that just closed) — also the primary way to test this without
// waiting for a real month boundary or the cron schedule.
export const generateSettlementsNow = async (req, res) => {
    try {
        let month = parseInt(req.query.month);
        let year = parseInt(req.query.year);

        if (!month || !year) {
            const now = new Date();
            const prevMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
            month = prevMonthDate.getMonth() + 1;
            year = prevMonthDate.getFullYear();
        }

        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            return res.status(400).json({ success: false, message: "Invalid month or year." });
        }

        const created = await generateMonthlySettlements(month, year);
        return res.status(200).json({
            success: true,
            message: `Generated ${created} new settlement(s) for ${month}/${year}.`,
            month,
            year,
            created
        });
    } catch (error) {
        console.error("generateSettlementsNow error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// GET /api/commission/settlements?status=&month=&year= — admin only
export const listSettlements = async (req, res) => {
    try {
        const { status, month, year } = req.query;

        let query = `
            SELECT s.*,
                   u.full_name AS cook_name, u.phone AS cook_phone,
                   cp.kitchen_name,
                   verifier.full_name AS verified_by_name,
                   (s.status = 'pending' AND (s.year < YEAR(CURDATE())
                        OR (s.year = YEAR(CURDATE()) AND s.month < MONTH(CURDATE())))) AS is_overdue
            FROM commission_settlements s
            JOIN users u ON u.id = s.cook_id
            LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
            LEFT JOIN users verifier ON verifier.id = s.verified_by
            WHERE 1=1`;
        const params = [];

        if (status) {
            query += " AND s.status = ?";
            params.push(status);
        }
        if (month) {
            query += " AND s.month = ?";
            params.push(parseInt(month));
        }
        if (year) {
            query += " AND s.year = ?";
            params.push(parseInt(year));
        }

        query += " ORDER BY s.year DESC, s.month DESC, s.amount_due DESC";

        const [settlements] = await db.promise().query(query, params);

        // MySQL returns the computed boolean as 0/1 — normalize for Gson/JS clients.
        settlements.forEach(s => { s.is_overdue = !!s.is_overdue; });

        return res.status(200).json({ success: true, settlements });
    } catch (error) {
        console.error("listSettlements error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// PUT /api/commission/settlements/:id/verify — admin only
export const verifySettlement = async (req, res) => {
    try {
        const adminId = req.user.id;
        const { id } = req.params;
        const { status, admin_notes } = req.body;

        const ALLOWED_STATUSES = ["verified", "rejected"];
        if (!status || !ALLOWED_STATUSES.includes(status)) {
            return res.status(400).json({ success: false, message: `status must be one of: ${ALLOWED_STATUSES.join(", ")}` });
        }

        const [settlements] = await db.promise().query("SELECT * FROM commission_settlements WHERE id = ?", [id]);
        if (settlements.length === 0) {
            return res.status(404).json({ success: false, message: "Settlement not found." });
        }
        const settlement = settlements[0];

        if (settlement.status !== "submitted") {
            return res.status(400).json({ success: false, message: `Cannot ${status === "verified" ? "verify" : "reject"} a settlement that hasn't been submitted (current status: ${settlement.status}).` });
        }

        await db.promise().query(
            `UPDATE commission_settlements
             SET status = ?, admin_notes = ?, verified_by = ?, verified_at = NOW()
             WHERE id = ?`,
            [status, admin_notes || null, adminId, id]
        );

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [adminId, `commission_settlement_${status}`, `Settlement #${id} (cook ${settlement.cook_id}, ₹${settlement.amount_due}) marked ${status}`]
        );

        if (status === "verified") {
            await notifyCommissionSettlementVerified(settlement.cook_id, settlement.amount_due, settlement.month, settlement.year);
        } else {
            await notifyCommissionSettlementRejected(settlement.cook_id, settlement.amount_due, admin_notes);
        }

        return res.status(200).json({ success: true, message: `Settlement marked ${status}.` });
    } catch (error) {
        console.error("verifySettlement error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// GET /api/commission/settlements/current — cook only. The cook's own
// settlement for the current month/year, or null if it hasn't been
// generated yet (happens on the 1st of next month, once this month closes).
export const getMyCurrentSettlement = async (req, res) => {
    try {
        const cookId = req.user.id;
        const now = new Date();

        const [[settlement]] = await db.promise().query(
            `SELECT * FROM commission_settlements WHERE cook_id = ? AND month = ? AND year = ?`,
            [cookId, now.getMonth() + 1, now.getFullYear()]
        );

        // Also surface the most recent PAST unresolved settlement (pending/submitted/rejected)
        // so a cook who missed last month's due still sees it, not just the current month.
        const [[pastDue]] = await db.promise().query(
            `SELECT * FROM commission_settlements
             WHERE cook_id = ? AND status IN ('pending', 'submitted', 'rejected')
               AND (year < ? OR (year = ? AND month < ?))
             ORDER BY year DESC, month DESC LIMIT 1`,
            [cookId, now.getFullYear(), now.getFullYear(), now.getMonth() + 1]
        );

        return res.status(200).json({
            success: true,
            current: settlement || null,
            past_due: pastDue || null
        });
    } catch (error) {
        console.error("getMyCurrentSettlement error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// GET /api/commission/settlements/mine — cook only, full history
export const listMySettlements = async (req, res) => {
    try {
        const cookId = req.user.id;
        const [settlements] = await db.promise().query(
            `SELECT * FROM commission_settlements WHERE cook_id = ? ORDER BY year DESC, month DESC`,
            [cookId]
        );
        return res.status(200).json({ success: true, settlements });
    } catch (error) {
        console.error("listMySettlements error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// PUT /api/commission/settlements/:id/screenshot — cook only.
// Cook uploads the QR-payment screenshot to /api/upload/document first (same
// two-step pattern orderController.js's uploadPaymentScreenshot already
// uses), then posts the resulting URL here.
export const uploadSettlementScreenshot = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const { payment_screenshot_url } = req.body;

        if (!payment_screenshot_url) {
            return res.status(400).json({ success: false, message: "payment_screenshot_url is required." });
        }

        const [settlements] = await db.promise().query("SELECT * FROM commission_settlements WHERE id = ?", [id]);
        if (settlements.length === 0) {
            return res.status(404).json({ success: false, message: "Settlement not found." });
        }
        const settlement = settlements[0];

        if (settlement.cook_id !== cookId) {
            return res.status(403).json({ success: false, message: "This settlement does not belong to you." });
        }
        if (!["pending", "rejected"].includes(settlement.status)) {
            return res.status(400).json({ success: false, message: `Cannot submit payment proof for a settlement that's already "${settlement.status}".` });
        }

        await db.promise().query(
            `UPDATE commission_settlements
             SET payment_screenshot_url = ?, status = 'submitted', submitted_at = NOW()
             WHERE id = ?`,
            [payment_screenshot_url, id]
        );

        const [[cook]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [cookId]);
        const [admins] = await db.promise().query("SELECT id FROM users WHERE role = 'admin' AND is_active = TRUE");
        for (const admin of admins) {
            await notifyCommissionSettlementSubmitted(admin.id, id, cook?.full_name || "A cook", settlement.amount_due);
        }

        return res.status(200).json({ success: true, message: "Payment proof submitted. An admin will verify it shortly." });
    } catch (error) {
        console.error("uploadSettlementScreenshot error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};
