import db from "../config/db.js";
import { getNptNow, getNptMonthYear, toNpt } from "../utils/nepaliTime.js";

/**
 * Cook earnings.
 *
 * PERIOD BASIS (this used to be wrong, and visibly so): every aggregate here
 * buckets by the DELIVERY moment converted to Nepal time — the same basis the
 * commission ledger uses (see commissionController.getCommissionByCook and
 * utils/nepaliTime.js). It previously bucketed by created_at in whatever
 * timezone the DB session happened to be in, so around a month boundary the
 * earnings screen and the commission bill disagreed about which month an order
 * belonged to. A cook seeing "₹8,400 earned in July" next to a July commission
 * bill computed from a different set of orders has no way to check the platform's
 * arithmetic, which is the one thing this screen exists to let them do.
 *
 * COALESCE(delivered_at, created_at): delivered_at is only set by the commission
 * snapshot, so orders delivered before that feature shipped have NULL there.
 * Falling back to created_at keeps their revenue visible instead of erasing a
 * cook's history. Those rows also have commission_amount NULL, so they
 * contribute ₹0 commission and are excluded from the commission ledger anyway —
 * the two views cannot disagree about them.
 *
 * REFUNDS: excluded, matching the commission ledger's edge case 1. A
 * delivered-then-refunded order gave the money back to the customer, so
 * counting it as the cook's earnings overstates what they actually kept.
 */

/** The moment an order counts as earned, in NPT. `a` is the table alias prefix. */
const earnedAt = (a = "") => toNpt(`COALESCE(${a}delivered_at, ${a}created_at)`);

/** Refund exclusion, matching the commission ledger. */
const notRefunded = (a = "") => `(${a}refund_status IS NULL OR ${a}refund_status <> 'refunded')`;

/** yyyy-MM-dd for "today" in Nepal. */
const nptTodayIso = () => getNptNow().toISOString().slice(0, 10);

/**
 * First meal image for each of a batch of orders, in ONE query.
 *
 * This was a per-row query inside a Promise.all — 10 extra round trips on the
 * summary screen and up to `limit` (20 by default, unbounded by the API) on the
 * transactions list. Same output, one trip.
 */
const firstMealImages = async (orderIds) => {
    const byOrder = new Map();
    if (orderIds.length === 0) return byOrder;
    const [rows] = await db.promise().query(
        `SELECT oi.order_id, MIN(m.image_url) AS image_url
         FROM order_items oi
         JOIN meals m ON oi.meal_id = m.id
         WHERE oi.order_id IN (?) AND m.image_url IS NOT NULL
         GROUP BY oi.order_id`,
        [orderIds]
    );
    for (const r of rows) byOrder.set(r.order_id, r.image_url);
    return byOrder;
};

/** Shared row → API shape for a transaction. */
const shapeTransaction = (t, images) => ({
    orderId: t.order_id,
    customerName: t.customer_name,
    amount: parseFloat(t.amount),
    paymentMethod: t.payment_method || 'cod',
    time: t.time,
    date: t.date,
    imageUrl: images.get(t.order_id) || null
});

/**
 * GET /api/orders/cook/earnings/summary
 * Cook's earnings summary with transactions and breakdowns.
 * Supports optional query params: ?month=6&year=2026
 */
export const getCookEarningsSummary = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { month, year } = req.query;

        const filterByMonth = month && year;
        const filterMonth = parseInt(month);
        const filterYear = parseInt(year);

        if (filterByMonth && (filterMonth < 1 || filterMonth > 12 || filterYear < 2000 || filterYear > 2100)) {
            return res.status(400).json({ success: false, message: "Invalid month or year" });
        }

        // "Today" and "this month" are Nepal-time notions, computed in JS rather
        // than with CURDATE(): CURDATE() is the DB server's clock, which on a
        // managed DB is UTC and therefore still yesterday for the first 5h45m of
        // every Nepali day.
        const today = nptTodayIso();
        const npt = getNptMonthYear();
        const periodMonth = filterByMonth ? filterMonth : npt.month;
        const periodYear = filterByMonth ? filterYear : npt.year;

        const [[todayResult]] = await db.promise().query(
            `SELECT COALESCE(SUM(total_amount), 0) as total
             FROM orders
             WHERE cook_id = ? AND status = 'delivered' AND ${notRefunded()}
             AND DATE(${earnedAt()}) = ?`,
            [cookId, today]
        );
        const todayTotal = parseFloat(todayResult.total) || 0;

        // Monday-to-Sunday week, per YEARWEEK mode 1.
        const [[weekResult]] = await db.promise().query(
            `SELECT COALESCE(SUM(total_amount), 0) as total
             FROM orders
             WHERE cook_id = ? AND status = 'delivered' AND ${notRefunded()}
             AND YEARWEEK(${earnedAt()}, 1) = YEARWEEK(?, 1)`,
            [cookId, today]
        );
        const thisWeekTotal = parseFloat(weekResult.total) || 0;

        // commission_amount is only set on orders delivered after the commission
        // feature shipped (NULL on older ones), so COALESCE it to 0 rather than
        // letting SUM() silently skip those rows' contribution to the total.
        const [[monthResult]] = await db.promise().query(
            `SELECT
                COALESCE(SUM(total_amount), 0) as total,
                COALESCE(SUM(commission_amount), 0) as commission,
                COUNT(*) as order_count
             FROM orders
             WHERE cook_id = ? AND status = 'delivered' AND ${notRefunded()}
             AND MONTH(${earnedAt()}) = ? AND YEAR(${earnedAt()}) = ?`,
            [cookId, periodMonth, periodYear]
        );
        const thisMonthTotal = parseFloat(monthResult.total) || 0;
        const thisMonthCommission = parseFloat(monthResult.commission) || 0;
        const thisMonthNetTotal = thisMonthTotal - thisMonthCommission;
        const thisMonthOrderCount = parseInt(monthResult.order_count) || 0;

        // Recent transactions (last 10, or that month's last 10 when filtered).
        const monthFilterSql = filterByMonth
            ? `AND MONTH(${earnedAt("o.")}) = ? AND YEAR(${earnedAt("o.")}) = ?`
            : "";
        const [recentTransactions] = await db.promise().query(
            `SELECT
                o.id as order_id,
                u.full_name as customer_name,
                o.total_amount as amount,
                o.payment_method,
                DATE_FORMAT(${earnedAt("o.")}, '%h:%i %p') as time,
                DATE_FORMAT(${earnedAt("o.")}, '%d %M %Y') as date
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             WHERE o.cook_id = ? AND o.status = 'delivered' AND ${notRefunded("o.")}
             ${monthFilterSql}
             ORDER BY ${earnedAt("o.")} DESC
             LIMIT 10`,
            filterByMonth ? [cookId, filterMonth, filterYear] : [cookId]
        );
        const recentImages = await firstMealImages(recentTransactions.map(t => t.order_id));
        const formattedTransactions = recentTransactions.map(t => shapeTransaction(t, recentImages));

        // Trend series. All three are built the same way: aggregate in SQL, then
        // fill the gaps in JS so the chart gets a continuous X-axis instead of
        // holes on days with no orders. Boundaries are NPT dates passed in as
        // parameters, and the JS fill walks the same NPT calendar, so a series
        // point and its label always refer to the same day.
        const buildSeries = async (days, format, unit) => {
            const [rows] = await db.promise().query(
                `SELECT DATE_FORMAT(${earnedAt()}, ?) as bucket,
                        COALESCE(SUM(total_amount), 0) as amount
                 FROM orders
                 WHERE cook_id = ? AND status = 'delivered' AND ${notRefunded()}
                 AND ${earnedAt()} >= DATE_SUB(?, INTERVAL ${days - 1} ${unit})
                 GROUP BY bucket
                 ORDER BY bucket ASC`,
                [format, cookId, today]
            );
            const byBucket = new Map(rows.map(r => [r.bucket, parseFloat(r.amount)]));
            const out = [];
            for (let i = days - 1; i >= 0; i--) {
                const d = getNptNow();
                if (unit === "MONTH") {
                    // Day 1 first: stepping back a month from the 31st would
                    // otherwise overflow into the wrong month.
                    d.setUTCDate(1);
                    d.setUTCMonth(d.getUTCMonth() - i);
                } else {
                    d.setUTCDate(d.getUTCDate() - i);
                }
                const key = d.toISOString().slice(0, unit === "MONTH" ? 7 : 10);
                out.push({ key, amount: byBucket.get(key) || 0 });
            }
            return out;
        };

        const last7Days = (await buildSeries(7, '%Y-%m-%d', 'DAY'))
            .map(p => ({ date: p.key, amount: p.amount }));
        const last30Days = (await buildSeries(30, '%Y-%m-%d', 'DAY'))
            .map(p => ({ date: p.key, amount: p.amount }));
        const last12Months = (await buildSeries(12, '%Y-%m', 'MONTH'))
            .map(p => ({ month: p.key, amount: p.amount }));

        return res.status(200).json({
            success: true,
            earnings: {
                thisWeekTotal,
                thisMonthTotal,
                thisMonthCommission,
                thisMonthNetTotal,
                thisMonthOrderCount,
                todayTotal,
                recentTransactions: formattedTransactions,
                weeklyBreakdown: last7Days,
                dailyBreakdown: last30Days,
                monthlyBreakdown: last12Months
            }
        });

    } catch (error) {
        console.error("getCookEarningsSummary error:", error);
        return res.status(500).json({ success: false, message: "Server error", error: error.message });
    }
};

/**
 * GET /api/orders/cook/earnings/transactions
 * All of a cook's earning transactions, paginated.
 * Supports query params: ?page=1&limit=20&search=keyword
 */
export const getCookEarningsTransactions = async (req, res) => {
    try {
        const cookId = req.user.id;
        const page = Math.max(1, parseInt(req.query.page) || 1);
        // Capped: the client sends 20, but an unbounded limit here is a trivial
        // way to make the server serialise a cook's entire order history.
        const limit = Math.min(100, Math.max(1, parseInt(req.query.limit) || 20));
        const search = req.query.search || '';
        const offset = (page - 1) * limit;

        let searchCondition = '';
        const queryParams = [cookId];
        if (search) {
            searchCondition = 'AND (u.full_name LIKE ? OR o.id LIKE ?)';
            const searchPattern = `%${search}%`;
            queryParams.push(searchPattern, searchPattern);
        }

        const [[countResult]] = await db.promise().query(
            `SELECT COUNT(*) as total
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             WHERE o.cook_id = ? AND o.status = 'delivered' AND ${notRefunded("o.")}
             ${searchCondition}`,
            queryParams
        );
        const totalTransactions = countResult.total;
        const totalPages = Math.ceil(totalTransactions / limit);

        const [transactions] = await db.promise().query(
            `SELECT
                o.id as order_id,
                u.full_name as customer_name,
                o.total_amount as amount,
                o.payment_method,
                DATE_FORMAT(${earnedAt("o.")}, '%h:%i %p') as time,
                DATE_FORMAT(${earnedAt("o.")}, '%d %M %Y') as date
             FROM orders o
             JOIN users u ON o.customer_id = u.id
             WHERE o.cook_id = ? AND o.status = 'delivered' AND ${notRefunded("o.")}
             ${searchCondition}
             ORDER BY ${earnedAt("o.")} DESC
             LIMIT ? OFFSET ?`,
            [...queryParams, limit, offset]
        );

        const images = await firstMealImages(transactions.map(t => t.order_id));
        const formattedTransactions = transactions.map(t => shapeTransaction(t, images));

        return res.status(200).json({
            success: true,
            transactions: formattedTransactions,
            pagination: {
                currentPage: page,
                totalPages,
                totalTransactions,
                hasNextPage: page < totalPages,
                hasPrevPage: page > 1
            }
        });

    } catch (error) {
        console.error("getCookEarningsTransactions error:", error);
        return res.status(500).json({ success: false, message: "Server error", error: error.message });
    }
};
