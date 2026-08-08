import db from "../config/db.js";

/**
 * GET /api/orders/cook/earnings/summary
 * Get cook's earnings summary with transactions and breakdowns
 * Supports optional query params: ?month=6&year=2026
 */
export const getCookEarningsSummary = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { month, year } = req.query;

        // If month and year are provided, filter by that specific month
        const filterByMonth = month && year;
        const filterMonth = parseInt(month);
        const filterYear = parseInt(year);

        // Validate month/year if provided
        if (filterByMonth && (filterMonth < 1 || filterMonth > 12 || filterYear < 2000 || filterYear > 2100)) {
            return res.status(400).json({
                success: false,
                message: "Invalid month or year"
            });
        }

        // Get today's total
        const [[todayResult]] = await db.promise().query(
            `SELECT COALESCE(SUM(total_amount), 0) as total
             FROM orders
             WHERE cook_id = ?
             AND status = 'delivered'
             AND DATE(created_at) = CURDATE()`,
            [cookId]
        );
        const todayTotal = parseFloat(todayResult.total) || 0;

        // Get this week's total (Monday to Sunday)
        const [[weekResult]] = await db.promise().query(
            `SELECT COALESCE(SUM(total_amount), 0) as total
             FROM orders
             WHERE cook_id = ?
             AND status = 'delivered'
             AND YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)`,
            [cookId]
        );
        const thisWeekTotal = parseFloat(weekResult.total) || 0;

        // Get this month's total and order count (or filtered month if provided).
        // commission_amount is only set on orders delivered after the commission
        // feature shipped (NULL on older ones), so COALESCE it to 0 rather than
        // letting SUM() silently skip those rows' contribution to the total.
        let thisMonthQuery, thisMonthParams;
        if (filterByMonth) {
            thisMonthQuery = `
                SELECT
                    COALESCE(SUM(total_amount), 0) as total,
                    COALESCE(SUM(commission_amount), 0) as commission,
                    COUNT(*) as order_count
                FROM orders
                WHERE cook_id = ?
                AND status = 'delivered'
                AND MONTH(created_at) = ?
                AND YEAR(created_at) = ?`;
            thisMonthParams = [cookId, filterMonth, filterYear];
        } else {
            thisMonthQuery = `
                SELECT
                    COALESCE(SUM(total_amount), 0) as total,
                    COALESCE(SUM(commission_amount), 0) as commission,
                    COUNT(*) as order_count
                FROM orders
                WHERE cook_id = ?
                AND status = 'delivered'
                AND MONTH(created_at) = MONTH(CURDATE())
                AND YEAR(created_at) = YEAR(CURDATE())`;
            thisMonthParams = [cookId];
        }

        const [[monthResult]] = await db.promise().query(thisMonthQuery, thisMonthParams);
        const thisMonthTotal = parseFloat(monthResult.total) || 0;
        const thisMonthCommission = parseFloat(monthResult.commission) || 0;
        const thisMonthNetTotal = thisMonthTotal - thisMonthCommission;
        const thisMonthOrderCount = parseInt(monthResult.order_count) || 0;

        // Get recent transactions (last 10, or filtered by month if provided)
        let recentQuery, recentParams;
        if (filterByMonth) {
            recentQuery = `
                SELECT
                    o.id as order_id,
                    u.full_name as customer_name,
                    o.total_amount as amount,
                    o.payment_method,
                    DATE_FORMAT(o.created_at, '%h:%i %p') as time,
                    DATE_FORMAT(o.created_at, '%d %M %Y') as date,
                    o.created_at
                FROM orders o
                JOIN users u ON o.customer_id = u.id
                WHERE o.cook_id = ?
                AND o.status = 'delivered'
                AND MONTH(o.created_at) = ?
                AND YEAR(o.created_at) = ?
                ORDER BY o.created_at DESC
                LIMIT 10`;
            recentParams = [cookId, filterMonth, filterYear];
        } else {
            recentQuery = `
                SELECT
                    o.id as order_id,
                    u.full_name as customer_name,
                    o.total_amount as amount,
                    o.payment_method,
                    DATE_FORMAT(o.created_at, '%h:%i %p') as time,
                    DATE_FORMAT(o.created_at, '%d %M %Y') as date,
                    o.created_at
                FROM orders o
                JOIN users u ON o.customer_id = u.id
                WHERE o.cook_id = ?
                AND o.status = 'delivered'
                ORDER BY o.created_at DESC
                LIMIT 10`;
            recentParams = [cookId];
        }

        const [recentTransactions] = await db.promise().query(recentQuery, recentParams);

        // Format recent transactions
        const formattedTransactions = recentTransactions.map(t => ({
            orderId: t.order_id,
            customerName: t.customer_name,
            amount: parseFloat(t.amount),
            paymentMethod: t.payment_method || 'cod',
            time: t.time,
            date: t.date
        }));

        // Get weekly breakdown (last 7 days)
        const [weeklyBreakdown] = await db.promise().query(
            `SELECT
                DATE_FORMAT(created_at, '%Y-%m-%d') as date,
                COALESCE(SUM(total_amount), 0) as amount
             FROM orders
             WHERE cook_id = ?
             AND status = 'delivered'
             AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
             GROUP BY date
             ORDER BY date ASC`,
            [cookId]
        );

        // Ensure all 7 days are represented (fill missing days with 0)
        const last7Days = [];
        for (let i = 6; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            const dateStr = date.toISOString().split('T')[0];

            const existingDay = weeklyBreakdown.find(d => d.date === dateStr);
            last7Days.push({
                date: dateStr,
                amount: existingDay ? parseFloat(existingDay.amount) : 0
            });
        }

        // Get daily breakdown (last 30 days) for the trend chart's Month view
        const [daily30Breakdown] = await db.promise().query(
            `SELECT
                DATE_FORMAT(created_at, '%Y-%m-%d') as date,
                COALESCE(SUM(total_amount), 0) as amount
             FROM orders
             WHERE cook_id = ?
             AND status = 'delivered'
             AND created_at >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
             GROUP BY date
             ORDER BY date ASC`,
            [cookId]
        );
        const last30Days = [];
        for (let i = 29; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            const dateStr = date.toISOString().split('T')[0];
            const existingDay = daily30Breakdown.find(d => d.date === dateStr);
            last30Days.push({
                date: dateStr,
                amount: existingDay ? parseFloat(existingDay.amount) : 0
            });
        }

        // Get monthly breakdown (last 12 months) for the trend chart's Year view
        const [monthlyBreakdown] = await db.promise().query(
            `SELECT
                DATE_FORMAT(created_at, '%Y-%m') as month,
                COALESCE(SUM(total_amount), 0) as amount
             FROM orders
             WHERE cook_id = ?
             AND status = 'delivered'
             AND created_at >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH)
             GROUP BY month
             ORDER BY month ASC`,
            [cookId]
        );

        // Ensure all 12 months are represented (fill missing months with 0),
        // same approach as last7Days above, so the trend chart gets a
        // continuous X-axis instead of gaps for months with no orders.
        const last6Months = [];
        for (let i = 11; i >= 0; i--) {
            const date = new Date();
            date.setDate(1); // avoid month-length overflow (e.g. Mar 31 -> Feb 31 doesn't exist)
            date.setMonth(date.getMonth() - i);
            const monthStr = date.toISOString().slice(0, 7); // "YYYY-MM"

            const existingMonth = monthlyBreakdown.find(m => m.month === monthStr);
            last6Months.push({
                month: monthStr,
                amount: existingMonth ? parseFloat(existingMonth.amount) : 0
            });
        }
        const formattedMonthlyBreakdown = last6Months;

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
                monthlyBreakdown: formattedMonthlyBreakdown
            }
        });

    } catch (error) {
        console.error("getCookEarningsSummary error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};

/**
 * GET /api/orders/cook/earnings/transactions
 * Get all cook's earning transactions with pagination
 * Supports query params: ?page=1&limit=20&search=keyword
 */
export const getCookEarningsTransactions = async (req, res) => {
    try {
        const cookId = req.user.id;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const search = req.query.search || '';
        const offset = (page - 1) * limit;

        // Build search condition
        let searchCondition = '';
        let queryParams = [cookId];

        if (search) {
            searchCondition = 'AND (u.full_name LIKE ? OR o.id LIKE ?)';
            const searchPattern = `%${search}%`;
            queryParams.push(searchPattern, searchPattern);
        }

        // Get total count
        const countQuery = `
            SELECT COUNT(*) as total
            FROM orders o
            JOIN users u ON o.customer_id = u.id
            WHERE o.cook_id = ?
            AND o.status = 'delivered'
            ${searchCondition}`;

        const [[countResult]] = await db.promise().query(countQuery, queryParams);
        const totalTransactions = countResult.total;
        const totalPages = Math.ceil(totalTransactions / limit);

        // Get paginated transactions
        queryParams.push(limit, offset);
        const transactionsQuery = `
            SELECT
                o.id as order_id,
                u.full_name as customer_name,
                o.total_amount as amount,
                o.payment_method,
                DATE_FORMAT(o.created_at, '%h:%i %p') as time,
                DATE_FORMAT(o.created_at, '%d %M %Y') as date,
                o.created_at
            FROM orders o
            JOIN users u ON o.customer_id = u.id
            WHERE o.cook_id = ?
            AND o.status = 'delivered'
            ${searchCondition}
            ORDER BY o.created_at DESC
            LIMIT ? OFFSET ?`;

        const [transactions] = await db.promise().query(transactionsQuery, queryParams);

        // Format transactions
        const formattedTransactions = transactions.map(t => ({
            orderId: t.order_id,
            customerName: t.customer_name,
            amount: parseFloat(t.amount),
            paymentMethod: t.payment_method || 'cod',
            time: t.time,
            date: t.date
        }));

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
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};
