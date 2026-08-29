import db from "../config/db.js";
import crypto from "crypto";
import {
    notifyCommissionDue,
    notifyCommissionDueReminder,
    notifyCommissionSettlementSubmitted,
    notifyCommissionSettlementVerified,
    notifyCommissionSettlementRejected
} from "../utils/notificationHelper.js";
import { getNptNow, getNptMonthYear, getNptPreviousMonthYear, toNpt } from "../utils/nepaliTime.js";
import { notifyAllCooksOfRateChange, getCommissionRateHistory } from "../utils/commissionHelper.js";
import {
    DEFAULT_COMMISSION_PCT,
    applyDeliveryCommission as snapshotDeliveryCommission
} from "../utils/commissionSnapshot.js";

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
            commission_pct: settings ? parseFloat(settings.commission_pct) : DEFAULT_COMMISSION_PCT,
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
        const { commission_pct, change_reason } = req.body;
        const pct = Number(commission_pct);

        if (commission_pct === undefined || Number.isNaN(pct) || pct < 0 || pct > 100) {
            return res.status(400).json({ success: false, message: "commission_pct must be a number between 0 and 100." });
        }

        // Get current rate before updating
        const [[currentSettings]] = await db.promise().query(
            "SELECT commission_pct FROM platform_settings WHERE id = 1"
        );
        const oldRate = currentSettings ? parseFloat(currentSettings.commission_pct) : DEFAULT_COMMISSION_PCT;

        // Check if rate actually changed
        if (Math.abs(oldRate - pct) < 0.01) {
            return res.status(200).json({ 
                success: true, 
                message: "Commission rate unchanged.", 
                commission_pct: pct,
                no_change: true
            });
        }

        // Update the rate
        await db.promise().query(
            "UPDATE platform_settings SET commission_pct = ?, updated_by = ? WHERE id = 1",
            [pct, req.user.id]
        );

        // Log admin action
        await db.promise().query(
            `INSERT INTO admin_records (admin_id, action_type, description)
             VALUES (?, ?, ?)`,
            [req.user.id, "commission_updated", `Platform commission rate changed from ${oldRate}% to ${pct}%` + (change_reason ? ` - Reason: ${change_reason}` : '')]
        );

        // Notify all active cooks via notifications AND chat messages
        const io = req.app.get("io");
        const { notifiedCount, chatsSent } = await notifyAllCooksOfRateChange(
            oldRate,
            pct,
            req.user.id,
            change_reason,
            io
        );

        return res.status(200).json({ 
            success: true, 
            message: `Commission rate updated from ${oldRate}% to ${pct}%. Notified ${notifiedCount} cooks.`, 
            commission_pct: pct,
            old_rate: oldRate,
            new_rate: pct,
            notified_cooks: notifiedCount,
            chats_sent: chatsSent
        });
    } catch (error) {
        console.error("updateCommissionSettings error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

/**
 * Commission is charged by utils/commissionSnapshot.js, which is the single
 * implementation for every delivery path (cook order status, admin order
 * status, subscription day, backfill script). Re-exported here because
 * orderController and adminController have always imported it from this
 * module; moving it would be a rename for no gain.
 *
 * Edge case 2 (late order into a closed period) — DECISION: INSERT IGNORE in
 * generateMonthlySettlements means a period's settlement is generated once and
 * never recomputed, even if an order delivered later logically belongs to that
 * period. This is INTENTIONAL, not a silent loss: such an order's
 * commission_amount is still snapshotted on the order row and still counts in
 * getCommissionSummary's all-time and trend totals — it is simply not re-added
 * to that closed period's frozen amount_due. It rolls into whichever period's
 * settlement generation next picks it up by delivered_at. Net effect: no
 * commission is ever lost, only ever deferred by at most one billing cycle, and
 * a closed period's amount_due never changes after a cook was told what it was.
 *
 * See commissionSnapshot.js for the per-order rounding decision (edge case 9).
 */
export const applyDeliveryCommission = snapshotDeliveryCommission;

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
const getCommissionByCook = async (month, year, { unbilledOnly = false } = {}) => {
    // Reporting (admin summary) wants "everything delivered in this month",
    // period-pure, so it stays the default. Billing wants "everything not yet
    // charged to a settlement", which is a different question the moment a cook
    // can pay an open month early — see the carry-in clause below.
    const billingScope = unbilledOnly
        ? `AND o.commission_settlement_id IS NULL
           AND (
                (MONTH(${toNpt("o.delivered_at")}) = ? AND YEAR(${toNpt("o.delivered_at")}) = ?)
                OR (
                    -- Carry-in: an order whose own period already has a settlement
                    -- row can never be billed there (uniq_cook_period + the
                    -- INSERT IGNORE freeze), so it would otherwise be lost. This is
                    -- exactly what an early payment leaves behind: orders delivered
                    -- after the cook settled the month. Bounded to periods at or
                    -- before the one being generated so a future month is untouched.
                    (YEAR(${toNpt("o.delivered_at")}) < ?
                     OR (YEAR(${toNpt("o.delivered_at")}) = ? AND MONTH(${toNpt("o.delivered_at")}) <= ?))
                    AND EXISTS (
                        SELECT 1 FROM commission_settlements s
                        WHERE s.cook_id = o.cook_id
                          AND s.month = MONTH(${toNpt("o.delivered_at")})
                          AND s.year  = YEAR(${toNpt("o.delivered_at")})
                    )
                )
           )`
        : `AND MONTH(${toNpt("o.delivered_at")}) = ? AND YEAR(${toNpt("o.delivered_at")}) = ?`;

    const params = unbilledOnly
        ? [month, year, year, year, month]
        : [month, year];

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
           ${billingScope}
         GROUP BY o.cook_id, u.full_name, u.phone, cp.kitchen_name
         ORDER BY commission_total DESC`,
        params
    );
    return rows;
};

/**
 * Marks the orders a settlement covers, so nothing is ever billed twice.
 * Same scope as getCommissionByCook's billing query — including the carry-in —
 * because the sum and the stamp must agree exactly or a rupee is either lost or
 * charged twice.
 *
 * ponytail: not in a transaction with the INSERT. An order delivered in the
 * millisecond between the SUM and this UPDATE gets stamped without being
 * charged; wrap both in a transaction if that ever shows up in reconciliation.
 */
const stampBilledOrders = async (cookId, settlementId, month, year) => {
    const [res] = await db.promise().query(
        `UPDATE orders o
         SET o.commission_settlement_id = ?
         WHERE o.cook_id = ? AND o.status = 'delivered' AND o.commission_amount IS NOT NULL
           AND (o.refund_status IS NULL OR o.refund_status != 'refunded')
           AND o.commission_settlement_id IS NULL
           AND (
                (MONTH(${toNpt("o.delivered_at")}) = ? AND YEAR(${toNpt("o.delivered_at")}) = ?)
                OR (
                    (YEAR(${toNpt("o.delivered_at")}) < ?
                     OR (YEAR(${toNpt("o.delivered_at")}) = ? AND MONTH(${toNpt("o.delivered_at")}) <= ?))
                    AND EXISTS (
                        SELECT 1 FROM commission_settlements s
                        WHERE s.cook_id = o.cook_id
                          AND s.month = MONTH(${toNpt("o.delivered_at")})
                          AND s.year  = YEAR(${toNpt("o.delivered_at")})
                    )
                )
           )`,
        [settlementId, cookId, month, year, year, year, month]
    );
    return res.affectedRows;
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
        const pct = settings ? parseFloat(settings.commission_pct) : DEFAULT_COMMISSION_PCT;

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
    // unbilledOnly: a cook who paid this month early already has a settlement
    // holding those orders. Re-summing the raw month would bill them twice.
    const byCook = await getCommissionByCook(month, year, { unbilledOnly: true });
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
            await stampBilledOrders(row.cook_id, result.insertId, month, year);
            // insertId lets the push deep-link straight to this settlement instead
            // of dumping the cook on a generic list and making them find it.
            await notifyCommissionDue(row.cook_id, row.commission_total, month, year, result.insertId);
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
            // NPT, not server-local: on a UTC-clock host the two disagree about
            // which month just closed for the first 5h45m of every 1st.
            const prev = getNptPreviousMonthYear();
            month = prev.month;
            year = prev.year;
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
                await notifyCommissionSettlementVerified(settlement.cook_id, settlement.amount_due, settlement.month, settlement.year, settlement.id);
            }
        } else if (settlement.cook_id) {
            await notifyCommissionSettlementRejected(settlement.cook_id, settlement.amount_due, admin_notes, settlement.id);
        }

        // Live refresh for a cook sitting on the commission screen right now.
        // Without this they stare at "Awaiting admin verification" until they
        // think to pull-to-refresh, which is exactly when they message support.
        try {
            const io = req.app.get("io");
            if (io && settlement.cook_id) {
                io.to(`user_${settlement.cook_id}`).emit("commissionSettlementUpdated", {
                    settlement_id: Number(id),
                    status: newStatus,
                    amount_paid: (newPaidPaisa / 100).toFixed(2),
                    amount_remaining: (remainingPaisa / 100).toFixed(2),
                    partial: isPartial
                });
            }
        } catch (emitErr) {
            console.error("commissionSettlementUpdated emit failed:", emitErr.message);
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

/**
 * Live, not-yet-billed commission for one cook in one month: delivered, non-
 * refunded orders that no settlement has stamped yet. Shared by the cook's
 * current-status endpoint and settle-now so "what you still owe this month"
 * can never be computed two different ways. Same refund exclusion and NPT
 * timezone rule as getCommissionByCook.
 */
const getUnbilledAccrual = async (cookId, month, year) => {
    const [[row]] = await db.promise().query(
        `SELECT COUNT(*) AS order_count, COALESCE(SUM(commission_amount), 0) AS amount
         FROM orders
         WHERE cook_id = ? AND status = 'delivered' AND commission_amount IS NOT NULL
           AND (refund_status IS NULL OR refund_status != 'refunded')
           AND commission_settlement_id IS NULL
           AND MONTH(${toNpt("delivered_at")}) = ? AND YEAR(${toNpt("delivered_at")}) = ?`,
        [cookId, month, year]
    );
    return { amount: parseFloat(row.amount) || 0, order_count: row.order_count };
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

        const accruing = await getUnbilledAccrual(cookId, month, year);

        return res.status(200).json({
            success: true,
            current: withCalendarDueDate(settlement) || null,
            past_due: withCalendarDueDate(pastDue) || null,
            accruing: {
                amount: accruing.amount,
                order_count: accruing.order_count,
                month,
                year,
                // Cooks asked to clear this before the month closes rather than
                // carry a growing balance. settle-now turns the accrual into a
                // real settlement they can pay and upload proof for today.
                payable_now: accruing.amount > 0,
                note: "Payable now, or billed automatically once this month closes."
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

/**
 * POST /api/commission/settlements/settle-now — cook only.
 *
 * Turns the OPEN month's live accrual into a real settlement the cook can pay
 * today, instead of waiting for the 1st-of-month cron. Everything downstream
 * (platform QR, eSewa, screenshot upload, admin verification, notifications) is
 * the existing flow untouched — this only materialises the row it needs.
 *
 * Idempotent-ish by the unique (cook_id, month, year) key: a period can hold
 * exactly one settlement. So if a bill already exists for this month, that bill
 * is what the cook pays; we never create a second one.
 *
 * Orders are stamped with the settlement id, so the month-close cron cannot bill
 * them again, and deliveries made AFTER the early payment stay unbilled and roll
 * into the next generation via getCommissionByCook's carry-in clause. No rupee
 * is lost and none is charged twice.
 */
export const settleAccruedNow = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { month, year } = getNptMonthYear();

        const [[existing]] = await db.promise().query(
            `SELECT * FROM commission_settlements WHERE cook_id = ? AND month = ? AND year = ?`,
            [cookId, month, year]
        );

        if (existing) {
            // pending/rejected are payable — hand the cook the bill they already
            // have rather than erroring at them for tapping "Pay now" twice.
            if (["pending", "rejected"].includes(existing.status)) {
                return res.status(200).json({
                    success: true,
                    message: "This month's commission bill is ready to pay.",
                    settlement: withCalendarDueDate(existing)
                });
            }
            if (existing.status === "submitted") {
                return res.status(409).json({
                    success: false,
                    message: "Your payment for this month is already awaiting admin verification."
                });
            }

            // A settled bill for this month does NOT mean the month is done: orders
            // delivered after it was paid are still unbilled, and the
            // (cook_id, month, year) unique key means they cannot get a bill of
            // their own until the month-close cron runs. Cooks asked to clear that
            // remainder now rather than carry it, so top the SAME bill up:
            // amount_due grows by the new accrual, amount_paid keeps the already-
            // verified receipt, and the row drops back to 'pending' as a part-paid
            // bill — precisely the state the partial-payment flow (EC3) already
            // handles end to end, for the cook and for the admin.
            const topUp = await getUnbilledAccrual(cookId, month, year);
            if (topUp.amount <= 0) {
                return res.status(409).json({
                    success: false,
                    message: "This month's commission is already settled. Anything you deliver from now on is billed in the next cycle."
                });
            }

            // The old proof was verified against the old amount. Leaving it
            // attached would let it be verified a second time, crediting money
            // that never arrived — so the cook uploads fresh proof for the
            // balance. verified_by/verified_at stay: that verification did happen.
            await db.promise().query(
                `UPDATE commission_settlements
                 SET amount_due = amount_due + ?, order_count = order_count + ?,
                     status = 'pending', payment_screenshot_url = NULL,
                     payment_screenshot_hash = NULL, submitted_at = NULL
                 WHERE id = ?`,
                [topUp.amount.toFixed(2), topUp.order_count, existing.id]
            );
            await stampBilledOrders(cookId, existing.id, month, year);
            await notifyCommissionDue(cookId, topUp.amount, month, year, existing.id);

            const [[toppedUp]] = await db.promise().query(
                "SELECT * FROM commission_settlements WHERE id = ?",
                [existing.id]
            );
            return res.status(200).json({
                success: true,
                message: `₹${topUp.amount.toFixed(2)} added to this month's bill. Pay it and upload your payment screenshot.`,
                settlement: withCalendarDueDate(toppedUp)
            });
        }

        const accrued = await getUnbilledAccrual(cookId, month, year);

        const amount = accrued.amount;
        // EC7's rule, applied early: no ₹0 bill. Silence is the correct answer.
        if (amount <= 0) {
            return res.status(400).json({
                success: false,
                message: "You have no commission to pay yet — nothing has been delivered this month."
            });
        }

        const [[cook]] = await db.promise().query(
            `SELECT u.full_name, u.phone, cp.kitchen_name
             FROM users u LEFT JOIN cook_profiles cp ON cp.user_id = u.id
             WHERE u.id = ?`,
            [cookId]
        );

        // Same due_date arithmetic as generateMonthlySettlements, in SQL for the
        // same reason: one place decides what "1st of next month + 15 days" means.
        const [result] = await db.promise().query(
            `INSERT INTO commission_settlements
                 (cook_id, month, year, amount_due, order_count, due_date,
                  cook_name_snapshot, cook_phone_snapshot, kitchen_name_snapshot)
             VALUES (?, ?, ?, ?, ?, DATE_ADD(DATE_ADD(MAKEDATE(?, 1), INTERVAL ? MONTH), INTERVAL 15 DAY), ?, ?, ?)`,
            [cookId, month, year, amount, accrued.order_count, year, month,
             cook?.full_name || null, cook?.phone || null, cook?.kitchen_name || null]
        );

        await stampBilledOrders(cookId, result.insertId, month, year);

        // The cook triggered this, so the in-app notification is a receipt, not
        // an alert — but it is what makes the bill findable later from the
        // notification list, exactly like a cron-generated one.
        await notifyCommissionDue(cookId, amount, month, year, result.insertId);

        const [[created]] = await db.promise().query(
            "SELECT * FROM commission_settlements WHERE id = ?",
            [result.insertId]
        );

        return res.status(201).json({
            success: true,
            message: "Commission bill created. Pay it and upload your payment screenshot.",
            settlement: withCalendarDueDate(created)
        });
    } catch (error) {
        console.error("settleAccruedNow error:", error);
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

        // Dedupe on the actual image bytes. Without this a cook could re-submit
        // one genuine payment screenshot every month and settle each period with
        // a single real payment. The same guard already protects subscription
        // payment proofs (subscription_payment_screenshot_hash), so this mirrors
        // a pattern that is already in production rather than inventing one.
        const screenshotHash = await hashRemoteImage(payment_screenshot_url);

        try {
            await db.promise().query(
                `UPDATE commission_settlements
                 SET payment_screenshot_url = ?, payment_screenshot_hash = ?,
                     status = 'submitted', submitted_at = NOW()
                 WHERE id = ?`,
                [payment_screenshot_url, screenshotHash, id]
            );
        } catch (err) {
            if (err.code === "ER_DUP_ENTRY") {
                return res.status(409).json({
                    success: false,
                    message: "This screenshot has already been submitted for another commission payment. Please upload the screenshot of this month's payment."
                });
            }
            throw err;
        }

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


// GET /api/commission/rate-history — admin only
// Returns the history of all commission rate changes
export const getCommissionRateHistoryEndpoint = async (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 20;
        const history = await getCommissionRateHistory(limit);
        
        return res.status(200).json({
            success: true,
            history
        });
    } catch (error) {
        console.error("getCommissionRateHistoryEndpoint error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};


/**
 * SHA-256 of the bytes behind an uploaded screenshot URL.
 *
 * Hashed server-side from the real bytes rather than trusting a client-sent
 * hash — a client that computes its own hash can trivially send a random one
 * and defeat the dedupe it is supposed to be subject to.
 *
 * FAILS OPEN, deliberately: if the image cannot be fetched, this returns null
 * and the submission proceeds un-deduped. Blocking a cook's genuine payment
 * because Cloudinary was briefly unreachable is a worse outcome than letting a
 * duplicate through to an admin who is going to eyeball the screenshot anyway.
 * The failure is logged loudly so it is visible if it stops being rare.
 */
const SCREENSHOT_HASH_MAX_BYTES = 10 * 1024 * 1024; // uploads are capped at 5MB; 2x headroom
const SCREENSHOT_HASH_TIMEOUT_MS = 8000;

const hashRemoteImage = async (url) => {
    try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), SCREENSHOT_HASH_TIMEOUT_MS);
        let buf;
        try {
            const resp = await fetch(url, { signal: controller.signal });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const len = Number(resp.headers.get("content-length") || 0);
            if (len > SCREENSHOT_HASH_MAX_BYTES) throw new Error(`too large (${len} bytes)`);
            buf = Buffer.from(await resp.arrayBuffer());
        } finally {
            clearTimeout(timer);
        }
        if (buf.length > SCREENSHOT_HASH_MAX_BYTES) throw new Error(`too large (${buf.length} bytes)`);
        return crypto.createHash("sha256").update(buf).digest("hex");
    } catch (err) {
        console.error(`⚠️  Could not hash commission screenshot (dedupe skipped for this submission): ${err.message}`);
        return null;
    }
};

/**
 * Daily sweep that reminds cooks about unpaid commission.
 *
 * Cadence, and why: one nudge 3 days BEFORE the due date (early enough to act
 * on, late enough not to be ignored), one ON the due date, then weekly once
 * overdue. Not daily-once-overdue — a cook who is short on cash and gets
 * pinged every morning mutes the app, and then never sees the notification
 * that actually matters.
 *
 * Throttled by last_reminder_at so a restart, a duplicate cron fire, or two
 * app instances sharing one DB cannot double-send. The throttle is compared on
 * the NPT calendar day, not a 24h window, so "already reminded today" means
 * what a cook would mean by it.
 *
 * Amount chased is what is STILL OWED (amount_due - amount_paid): a cook who
 * has part-paid must never be asked for the original figure again.
 */
export const sendCommissionDueReminders = async () => {
    const { month: nptMonth, year: nptYear } = getNptMonthYear();
    const nptToday = getNptNow();
    const todayIso = nptToday.toISOString().slice(0, 10);

    const [rows] = await db.promise().query(
        `SELECT id, cook_id, month, year, amount_due, amount_paid,
                DATE_FORMAT(due_date, '%Y-%m-%d') AS due_date,
                DATE_FORMAT(last_reminder_at, '%Y-%m-%d') AS last_reminder_day,
                DATEDIFF(due_date, ?) AS days_until_due,
                DATEDIFF(?, last_reminder_at) AS days_since_reminder
         FROM commission_settlements
         WHERE status = 'pending'
           AND cook_id IS NOT NULL
           AND due_date IS NOT NULL
           AND amount_due > amount_paid`,
        [todayIso, todayIso]
    );

    let sent = 0;
    for (const r of rows) {
        // At most one reminder per settlement per calendar day, whatever else
        // below says. This is the spam guard and it comes first.
        if (r.last_reminder_day === todayIso) continue;

        const daysUntilDue = r.days_until_due;
        const isOverdue = daysUntilDue < 0;

        let shouldSend = false;
        if (daysUntilDue === 3 || daysUntilDue === 0) {
            shouldSend = true;
        } else if (isOverdue) {
            // First overdue reminder fires immediately, then weekly.
            shouldSend = r.days_since_reminder === null || r.days_since_reminder >= 7;
        }
        if (!shouldSend) continue;

        const remaining = Math.max(0, Number(r.amount_due) - Number(r.amount_paid || 0));
        if (remaining <= 0) continue;

        const dueLabel = r.due_date
            ? new Date(`${r.due_date}T00:00:00Z`).toLocaleDateString("en-US",
                { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" })
            : null;

        try {
            await notifyCommissionDueReminder(
                r.cook_id, remaining, r.month, r.year, r.id, dueLabel, isOverdue
            );
            // Stamped only after the notification actually went out, so a failed
            // send is retried tomorrow instead of being silently skipped.
            await db.promise().query(
                "UPDATE commission_settlements SET last_reminder_at = NOW() WHERE id = ?",
                [r.id]
            );
            sent++;
        } catch (err) {
            console.error(`❌ Commission reminder failed for settlement #${r.id}:`, err.message);
        }
    }

    return { candidates: rows.length, sent, period: `${nptMonth}/${nptYear}` };
};
