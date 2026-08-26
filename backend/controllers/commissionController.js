import db from "../config/db.js";
import {
    notifyCommissionDue,
    notifyCommissionSettlementSubmitted,
    notifyCommissionSettlementVerified,
    notifyCommissionSettlementRejected
} from "../utils/notificationHelper.js";
import { getNptNow, getNptMonthYear, toNpt } from "../utils/nepaliTime.js";

/**
 * Platform commission — one global rate admins can change, snapshotted onto
 * each order at delivery time (see applyDeliveryCommission) so a later rate
 * change never rewrites what past orders actually owed.
 */

/**
 * due_date is a DATE (a calendar day), but mysql2 hands DATE columns back as a
 * JS Date at LOCAL midnight — so JSON.stringify turns 2026-09-16 into
 * "2026-09-15T18:15:00.000Z" on any host east of UTC. Verified at GMT+05:45.
 * Clients then read substring(0,10) and get the date a day early, and the bug
 * silently disappears on a UTC server, which is the worst possible failure
 * mode. Emit the calendar day as a plain yyyy-MM-dd string instead.
 *
 * Uses the LOCAL date parts deliberately: mysql2 built the Date from the DB's
 * calendar day at local midnight, so getFullYear/getMonth/getDate are exactly
 * the digits the DB stored.
 */
const asCalendarDay = (v) => {
    if (!v) return null;
    if (typeof v === "string") return v.slice(0, 10);
    const p = (n) => String(n).padStart(2, "0");
    return `${v.getFullYear()}-${p(v.getMonth() + 1)}-${p(v.getDate())}`;
};

/** Returns the row with due_date flattened to yyyy-MM-dd. Null-safe. */
const withCalendarDueDate = (row) =>
    row ? { ...row, due_date: asCalendarDay(row.due_date) } : row;

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
 *
 * Rounding (documented, not "fixed"): commission is snapshotted PER ORDER
 * as ROUND(total_amount * pct / 100, 2). A month's amount_due
 * (generateMonthlySettlements) is the SUM of these already-rounded
 * per-order snapshots, not a fresh ROUND(month_gross * pct / 100, 2). The
 * two can differ by a few paisa. The per-order snapshot is kept as the
 * source of truth deliberately — it is what lets a rate change never
 * retroactively alter a closed period (see the module-level comment and
 * edge case 9). Do not "fix" this drift by recomputing monthly totals from
 * the gross; that would defeat the whole point of snapshotting.
 *
 * Edge case 2 (late order into a closed period) — DECISION: INSERT IGNORE
 * in generateMonthlySettlements means a period's settlement is generated
 * once and never recomputed, even if an order delivered later logically
 * belongs to that period (e.g. its delivered_at somehow lands in an
 * already-settled month). This is INTENTIONAL, not a silent loss: such an
 * order's commission_amount is still snapshotted here and still exists on
 * the order row and in getCommissionSummary's all-time/trend totals — it
 * is simply not re-added to that closed period's amount_due. It rolls into
 * whichever period's settlement generation next picks it up by
 * delivered_at, which for a genuinely late order is virtually always the
 * period it actually belongs to (settlement generation runs monthly, and
 * delivered_at is set once, at actual delivery time — so an order can only
 * be "late" into an already-closed period if its true delivery happened
 * after that period's settlement run, which itself only happens after the
 * period ended). Net effect: no commission is ever lost, only ever
 * deferred by at most one billing cycle, and a closed period's amount_due
 * never silently changes after a cook has been notified of it.
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
 *
 * Edge case 1 (refunds) — DECISION: exclude refunded orders entirely rather
 * than crediting a future period. A delivered-then-refunded order gave the
 * money back to the customer, so the cook never actually kept it — there is
 * nothing to tax. This only affects OPEN (not-yet-settled) periods: once a
 * settlement row exists for a period, its amount_due is frozen (see
 * generateMonthlySettlements) and a later refund does NOT reopen or edit it
 * — the closed-period freeze always wins. A refund that lands after a
 * period's settlement was already generated is therefore not clawed back;
 * this mirrors edge case 2's "late order in a closed period" tradeoff and
 * is intentional, not a bug.
 *
 * Edge case 5 (timezone) — DECISION: all period (month/year) boundaries for
 * commission use Nepal Time (UTC+05:45), converted from the DB's UTC-stored
 * delivered_at via CONVERT_TZ (see utils/nepaliTime.js's toNpt()). This
 * matches getCommissionSummary's JS-side trend-fill, which already builds
 * its month buckets from local date parts for the same reason.
 */
const getCommissionByCook = async (month, year) => {
    const [rows] = await db.promise().query(
        `SELECT
            o.cook_id,
            u.full_name AS owner_name,
            u.phone AS owner_phone,
            cp.kitchen_name,
            COUNT(*) AS order_count,
            COALESCE(SUM(o.total_amount), 0) AS gross_total,
            COALESCE(SUM(o.commission_amount), 0) AS commission_total
         FROM orders o
         JOIN users u ON u.id = o.cook_id
         LEFT JOIN cook_profiles cp ON cp.user_id = o.cook_id
         WHERE o.status = 'delivered' AND o.commission_amount IS NOT NULL
           AND (o.refund_status IS NULL OR o.refund_status != 'refunded')
           AND MONTH(${toNpt("o.delivered_at")}) = ? AND YEAR(${toNpt("o.delivered_at")}) = ?
         GROUP BY o.cook_id, u.full_name, u.phone, cp.kitchen_name
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
        const { month: nptMonth, year: nptYear } = getNptMonthYear();
        const month = parseInt(req.query.month) || nptMonth;
        const year = parseInt(req.query.year) || nptYear;

        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            return res.status(400).json({ success: false, message: "Invalid month or year." });
        }

        // Refunded orders excluded — same edge-case-1 decision as
        // getCommissionByCook. Timezone: NPT throughout (edge case 5).
        const [[totals]] = await db.promise().query(
            `SELECT
                COALESCE(SUM(commission_amount), 0) AS total_commission,
                COALESCE(SUM(total_amount), 0) AS total_gross,
                COUNT(*) AS order_count
             FROM orders
             WHERE status = 'delivered' AND commission_amount IS NOT NULL
               AND (refund_status IS NULL OR refund_status != 'refunded')
               AND MONTH(${toNpt("delivered_at")}) = ? AND YEAR(${toNpt("delivered_at")}) = ?`,
            [month, year]
        );

        const [[allTime]] = await db.promise().query(
            `SELECT COALESCE(SUM(commission_amount), 0) AS total_commission
             FROM orders
             WHERE status = 'delivered' AND commission_amount IS NOT NULL`
        );

        const byCook = await getCommissionByCook(month, year);

        // Last 6 months trend, same fill-missing-months-with-zero approach
        // used by the cook earnings screen's chart. Grouped by delivered_at
        // converted to NPT (edge case 5), not updated_at — see
        // applyDeliveryCommission's comment for why delivered_at is used.
        // Refunded orders excluded (edge case 1).
        const [monthlyRows] = await db.promise().query(
            `SELECT
                DATE_FORMAT(${toNpt("delivered_at")}, '%Y-%m') AS ym,
                COALESCE(SUM(commission_amount), 0) AS commission,
                COALESCE(SUM(total_amount - commission_amount), 0) AS cook_net
             FROM orders
             WHERE status = 'delivered' AND commission_amount IS NOT NULL
               AND (refund_status IS NULL OR refund_status != 'refunded')
               -- Generous pre-filter in raw UTC (delivered_at's storage tz);
               -- +1 month margin absorbs the NPT (+05:45) shift so no row
               -- that could land in the last 6 NPT-month buckets is
               -- excluded before the NPT-aware GROUP BY above runs.
               AND delivered_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 6 MONTH)
             GROUP BY ym
             ORDER BY ym ASC`
        );
        const trend = [];
        for (let i = 5; i >= 0; i--) {
            // getNptNow() encodes NPT wall-clock time in this Date's UTC
            // fields (see utils/nepaliTime.js) — so all reads below MUST use
            // the getUTC* accessors, not the local getFullYear()/getMonth(),
            // which would reinterpret those fields through the server
            // process's OS timezone and reintroduce edge case 5's bug.
            const d = getNptNow();
            d.setUTCDate(1);
            d.setUTCMonth(d.getUTCMonth() - i);
            const ym = `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
            const existing = monthlyRows.find(r => r.ym === ym);
            trend.push({
                month: d.toLocaleString('en-US', { month: 'short', timeZone: 'UTC' }),
                year: d.getUTCFullYear(),
                commission: existing ? parseFloat(existing.commission) : 0,
                cookNet: existing ? parseFloat(existing.cook_net) : 0
            });
        }

        const [[settings]] = await db.promise().query(
            "SELECT commission_pct FROM platform_settings WHERE id = 1"
        );
        const pct = settings ? parseFloat(settings.commission_pct) : 4.00;

        // Pending collection — commission not yet locked in because the orders
        // haven't been delivered. Estimated at the CURRENT rate (the rate that
        // will actually be snapshotted at delivery), unlike delivered orders
        // which keep their historical snapshot.
        const [[pending]] = await db.promise().query(
            `SELECT COUNT(*) AS order_count,
                    COALESCE(SUM(ROUND(total_amount * ? / 100, 2)), 0) AS estimated_commission
             FROM orders
             WHERE status NOT IN ('delivered', 'cancelled')`,
            [pct]
        );

        return res.status(200).json({
            success: true,
            month,
            year,
            commission_pct: pct,
            total_commission: parseFloat(totals.total_commission),
            all_time_commission: parseFloat(allTime.total_commission),
            total_gross: parseFloat(totals.total_gross),
            order_count: totals.order_count,
            pending_commission: parseFloat(pending.estimated_commission),
            pending_order_count: pending.order_count,
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
 *
 * Edge case 7 (zero earnings) — `commission_total <= 0` is skipped: no row,
 * no notification. Silence, not a ₹0 bill. Keep this check.
 *
 * Edge case 10 (inactive/suspended cook) — DECISION: settlements still
 * generate regardless of users.is_active. A suspended cook who delivered
 * orders before being suspended still owes that commission; suspending
 * their account must not erase the debt. getCommissionByCook deliberately
 * does not filter on is_active.
 *
 * D1 fix — due_date is set here, at generation time, to (1st of the month
 * after the period) + 15 days (confirmed grace period). Computed in SQL so
 * it's atomic with the INSERT and uses the DB's own DATE arithmetic rather
 * than re-deriving it in JS.
 */
export const generateMonthlySettlements = async (month, year) => {
    const byCook = await getCommissionByCook(month, year);
    let created = 0;

    for (const row of byCook) {
        if (parseFloat(row.commission_total) <= 0) continue;

        const [result] = await db.promise().query(
            `INSERT IGNORE INTO commission_settlements
                 (cook_id, month, year, amount_due, order_count, due_date,
                  cook_name_snapshot, cook_phone_snapshot, kitchen_name_snapshot)
             VALUES (?, ?, ?, ?, ?, DATE_ADD(DATE_ADD(MAKEDATE(?, 1), INTERVAL ? MONTH), INTERVAL 15 DAY), ?, ?, ?)`,
            [row.cook_id, month, year, row.commission_total, row.order_count, year, month,
             row.owner_name, row.owner_phone, row.kitchen_name]
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

        // D1 fix: overdue is now `pending AND due_date < CURDATE()` — due_date
        // carries the 15-day grace period set at generation time (see
        // generateMonthlySettlements). The old month/year comparison flagged
        // every settlement overdue the instant it was created.
        let query = `
            SELECT s.*,
                   COALESCE(u.full_name, s.cook_name_snapshot, '(deleted cook)') AS cook_name,
                   COALESCE(u.phone, s.cook_phone_snapshot) AS cook_phone,
                   COALESCE(cp.kitchen_name, s.kitchen_name_snapshot) AS kitchen_name,
                   (s.cook_id IS NULL) AS cook_deleted,
                   verifier.full_name AS verified_by_name,
                   (s.status = 'pending' AND s.due_date IS NOT NULL AND s.due_date < CURDATE()) AS is_overdue
            FROM commission_settlements s
            LEFT JOIN users u ON u.id = s.cook_id
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
        // due_date is flattened to yyyy-MM-dd (see asCalendarDay).
        const normalized = settlements.map(s => ({
            ...withCalendarDueDate(s),
            is_overdue: !!s.is_overdue,
            cook_deleted: !!s.cook_deleted,
        }));

        return res.status(200).json({ success: true, settlements: normalized });
    } catch (error) {
        console.error("listSettlements error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// Money comparisons are done in integer paisa. 145.00 - 100.00 - 45.00 is not
// reliably 0 in float, and "did the cook pay in full?" must not hinge on that.
const toPaisa = (v) => Math.round(Number(v) * 100);

// PUT /api/commission/settlements/:id/verify — admin only
//
// Edge case 4 (off-platform payment) — a cook who pays by direct bank
// transfer (not the in-app screenshot flow) never reaches status
// 'submitted', so the original hard requirement of status === 'submitted'
// left them stuck at 'pending' forever with no way for the admin to close
// it out. FIX: allow the transition from 'pending' too, but ONLY when the
// admin supplies a non-empty admin_notes explaining the off-platform
// payment (e.g. "Paid via bank transfer, ref #123, confirmed by owner") —
// this keeps a mandatory paper trail for money that moved outside the
// screenshot-verified flow. Transitioning from 'submitted' still works
// exactly as before and admin_notes stays optional there.
//
// Edge case 3 (partial payment) — status used to be all-or-nothing, so a cook
// who paid ₹100 against ₹145 left the admin with two bad options: verify
// (silently writing off ₹45) or reject (pretending nothing arrived). FIX: the
// optional `amount_paid` body field records what actually landed, accumulating
// across installments. A short payment is banked but the settlement stays
// 'pending' — so it is still listed, still chaseable, still flagged overdue —
// and only becomes 'verified' once the accumulated total covers amount_due.
// amount_due itself is never rewritten; it stays the frozen record of what was
// billed. Omitting amount_paid means "paid in full", preserving the old
// behaviour for every existing caller.
export const verifySettlement = async (req, res) => {
    try {
        const adminId = req.user.id;
        const { id } = req.params;
        const { status, admin_notes, amount_paid } = req.body;

        const ALLOWED_STATUSES = ["verified", "rejected"];
        if (!status || !ALLOWED_STATUSES.includes(status)) {
            return res.status(400).json({ success: false, message: `status must be one of: ${ALLOWED_STATUSES.join(", ")}` });
        }

        const [settlements] = await db.promise().query("SELECT * FROM commission_settlements WHERE id = ?", [id]);
        if (settlements.length === 0) {
            return res.status(404).json({ success: false, message: "Settlement not found." });
        }
        const settlement = settlements[0];

        const isOffPlatform = settlement.status === "pending";
        if (!isOffPlatform && settlement.status !== "submitted") {
            return res.status(400).json({ success: false, message: `Cannot ${status === "verified" ? "verify" : "reject"} a settlement that hasn't been submitted (current status: ${settlement.status}).` });
        }
        if (isOffPlatform && (!admin_notes || !admin_notes.trim())) {
            return res.status(400).json({ success: false, message: "admin_notes is required to record an off-platform payment for a settlement that was never submitted through the app." });
        }

        const duePaisa = toPaisa(settlement.amount_due);
        const alreadyPaidPaisa = toPaisa(settlement.amount_paid || 0);
        const outstandingPaisa = Math.max(0, duePaisa - alreadyPaidPaisa);

        // Only a 'verified' decision moves money. A rejection means the claimed
        // payment never arrived, so amount_paid is left exactly as it was.
        let receivedPaisa = 0;
        if (status === "verified") {
            if (amount_paid === undefined || amount_paid === null || amount_paid === "") {
                receivedPaisa = outstandingPaisa; // omitted = settled in full
            } else {
                const parsed = Number(amount_paid);
                if (!Number.isFinite(parsed) || parsed <= 0) {
                    return res.status(400).json({ success: false, message: "amount_paid must be a number greater than 0." });
                }
                receivedPaisa = toPaisa(parsed);
                if (receivedPaisa > outstandingPaisa) {
                    return res.status(400).json({
                        success: false,
                        message: `amount_paid (₹${(receivedPaisa / 100).toFixed(2)}) exceeds the ₹${(outstandingPaisa / 100).toFixed(2)} still outstanding on this settlement.`,
                    });
                }
            }
        }

        const newPaidPaisa = alreadyPaidPaisa + receivedPaisa;
        const isPartial = status === "verified" && newPaidPaisa < duePaisa;
        // A part-paid settlement drops back to 'pending' so it keeps showing up
        // in the pending/overdue lists and the cook can submit again for the
        // balance. verified_by/verified_at stay untouched — nothing was verified.
        const newStatus = isPartial ? "pending" : status;

        if (isPartial) {
            await db.promise().query(
                `UPDATE commission_settlements
                 SET status = ?, amount_paid = ?, admin_notes = ?
                 WHERE id = ?`,
                [newStatus, (newPaidPaisa / 100).toFixed(2), admin_notes || null, id]
            );
        } else {
            await db.promise().query(
                `UPDATE commission_settlements
                 SET status = ?, amount_paid = ?, admin_notes = ?, verified_by = ?, verified_at = NOW()
                 WHERE id = ?`,
                [newStatus, (newPaidPaisa / 100).toFixed(2), admin_notes || null, adminId, id]
            );
        }

        const remainingPaisa = Math.max(0, duePaisa - newPaidPaisa);
        const money = (p) => `₹${(p / 100).toFixed(2)}`;

        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [adminId, isPartial ? "commission_settlement_partial_payment" : `commission_settlement_${status}`,
             isPartial
                 ? `Settlement #${id} (cook ${settlement.cook_id}) part payment of ${money(receivedPaisa)} recorded — ${money(newPaidPaisa)} of ${money(duePaisa)} received, ${money(remainingPaisa)} still owed. Left pending.` +
                   (admin_notes ? ` [admin note: ${admin_notes}]` : "")
                 : `Settlement #${id} (cook ${settlement.cook_id}, ${money(duePaisa)}) marked ${status}` +
                   (isOffPlatform ? ` [off-platform payment recorded by admin: ${admin_notes}]` : "")]
        );

        if (status === "verified") {
            // cook_id is NULL when the cook deleted their account (EC6: the
            // settlement row deliberately outlives them). There's nobody left to
            // notify, but the admin must still be able to close the record out.
            // A partial payment is deliberately NOT notified as "verified" — the
            // cook still owes money and must not be told they're settled up.
            if (settlement.cook_id && !isPartial) {
                await notifyCommissionSettlementVerified(settlement.cook_id, settlement.amount_due, settlement.month, settlement.year);
            }
        } else if (settlement.cook_id) {
            await notifyCommissionSettlementRejected(settlement.cook_id, settlement.amount_due, admin_notes);
        }

        return res.status(200).json({
            success: true,
            message: isPartial
                ? `Part payment of ${money(receivedPaisa)} recorded. ${money(remainingPaisa)} still outstanding — settlement left pending.`
                : `Settlement marked ${newStatus}.`,
            amount_paid: (newPaidPaisa / 100).toFixed(2),
            amount_remaining: (remainingPaisa / 100).toFixed(2),
            status: newStatus,
        });
    } catch (error) {
        console.error("verifySettlement error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// GET /api/commission/settlements/current — cook only. The cook's own
// settlement for the current month/year, or null if it hasn't been
// generated yet (happens on the 1st of next month, once this month closes).
//
// D3 fix: settlement rows are only created once the month closes, so during
// the month itself `current` is structurally always null and a cook mid-
// month sees nothing about what they're accruing. `accruing` below is a
// LIVE, non-authoritative sum of the current month's already-delivered
// orders' commission_amount — clearly not a bill (no settlement row, no
// due_date, cannot be paid/submitted against). It goes to zero and stays
// that way if the cook has no delivered orders yet this month.
export const getMyCurrentSettlement = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { month, year } = getNptMonthYear();

        const [[settlement]] = await db.promise().query(
            `SELECT * FROM commission_settlements WHERE cook_id = ? AND month = ? AND year = ?`,
            [cookId, month, year]
        );

        // Also surface the most recent PAST unresolved settlement (pending/submitted/rejected)
        // so a cook who missed last month's due still sees it, not just the current month.
        const [[pastDue]] = await db.promise().query(
            `SELECT * FROM commission_settlements
             WHERE cook_id = ? AND status IN ('pending', 'submitted', 'rejected')
               AND (year < ? OR (year = ? AND month < ?))
             ORDER BY year DESC, month DESC LIMIT 1`,
            [cookId, year, year, month]
        );

        // Live, not-yet-due accrual for the current (open) month. Same
        // refund exclusion and NPT timezone rule as getCommissionByCook.
        const [[accruing]] = await db.promise().query(
            `SELECT COUNT(*) AS order_count, COALESCE(SUM(commission_amount), 0) AS amount
             FROM orders
             WHERE cook_id = ? AND status = 'delivered' AND commission_amount IS NOT NULL
               AND (refund_status IS NULL OR refund_status != 'refunded')
               AND MONTH(${toNpt("delivered_at")}) = ? AND YEAR(${toNpt("delivered_at")}) = ?`,
            [cookId, month, year]
        );

        return res.status(200).json({
            success: true,
            current: withCalendarDueDate(settlement) || null,
            past_due: withCalendarDueDate(pastDue) || null,
            accruing: {
                amount: parseFloat(accruing.amount) || 0,
                order_count: accruing.order_count,
                month,
                year,
                note: "Not yet due — commission accrues live and is billed once this month closes."
            }
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
        return res.status(200).json({ success: true, settlements: settlements.map(withCalendarDueDate) });
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
        // EC3: on a re-submission after a part payment the cook is paying the
        // BALANCE, not the original bill — notifying the admin of amount_due
        // would overstate what this screenshot is proof of.
        const outstanding = Math.max(0, toPaisa(settlement.amount_due) - toPaisa(settlement.amount_paid || 0)) / 100;
        for (const admin of admins) {
            await notifyCommissionSettlementSubmitted(admin.id, id, cook?.full_name || "A cook", outstanding);
        }

        return res.status(200).json({ success: true, message: "Payment proof submitted. An admin will verify it shortly." });
    } catch (error) {
        console.error("uploadSettlementScreenshot error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};
