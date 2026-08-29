/**
 * The commission snapshot primitive, extracted out of commissionController so
 * anything that can mark an order delivered can charge commission on it
 * without a util → controller import.
 *
 * That import direction was the actual root cause of the biggest hole in this
 * feature: subscription deliveries earned zero commission, because the only
 * two callers of the snapshot were the two order-status *controllers*, and the
 * subscription delivery path lives in utils/subscriptionDailyLog.js which
 * could not reasonably reach into a controller. Moving the primitive here lets
 * every delivery path — order, subscription day, backfill script — go through
 * one implementation.
 */

import db from "../config/db.js";

/**
 * Fallback rate used only when platform_settings row 1 is missing entirely
 * (a fresh DB before the migration seeds it).
 *
 * Single source of truth on purpose: this used to be a literal 4.00 in three
 * places in commissionController and 5.00 in two places in commissionHelper,
 * so the same missing-settings condition charged two different rates depending
 * on which file noticed first. Confirmed business rate is 5%.
 */
export const DEFAULT_COMMISSION_PCT = 5.00;

/** The platform's current commission rate, as a number. */
export const getCommissionPct = async (executor = null) => {
    const runner = executor || db.promise();
    const [[settings]] = await runner.query(
        "SELECT commission_pct FROM platform_settings WHERE id = 1"
    );
    return settings ? parseFloat(settings.commission_pct) : DEFAULT_COMMISSION_PCT;
};

/**
 * Snapshots the current commission rate onto an order the moment it is marked
 * 'delivered'. Idempotent via the "commission_amount IS NULL" guard, so
 * calling it twice (an admin re-saving the same status, a cron re-reconciling
 * a day) never re-charges at a possibly-changed rate.
 *
 * Rounding (documented, not "fixed"): commission is snapshotted PER ORDER as
 * ROUND(total_amount * pct / 100, 2). A month's amount_due is the SUM of these
 * already-rounded per-order snapshots, not a fresh ROUND(month_gross * pct /
 * 100, 2). The two can differ by a few paisa. The per-order snapshot is the
 * source of truth deliberately — it is what lets a rate change never
 * retroactively alter a closed period (edge case 9). Do not "fix" the drift by
 * recomputing monthly totals from the gross; that defeats snapshotting.
 *
 * `executor` must be passed when the caller is inside a transaction. Without
 * it this writes on its own pool connection, which would commit the commission
 * even if the caller's transaction later rolled back — leaving a cook billed
 * for a delivery that was undone.
 */
export const applyDeliveryCommission = async (orderId, executor = null) => {
    try {
        const runner = executor || db.promise();
        const pct = await getCommissionPct(runner);

        // delivered_at is the true, immutable delivery moment — unlike
        // updated_at (which changes on any later touch to the row), this is set
        // once here and never again, so commission period grouping stays stable
        // forever. COALESCE keeps an existing value: a re-run must not move the
        // delivery moment into a different billing month.
        await runner.query(
            `UPDATE orders
             SET commission_pct = ?,
                 commission_amount = ROUND(total_amount * ? / 100, 2),
                 delivered_at = COALESCE(delivered_at, NOW())
             WHERE id = ? AND commission_amount IS NULL`,
            [pct, pct, orderId]
        );
    } catch (error) {
        // Never let a commission-snapshot failure block the delivery itself —
        // log it loudly and move on. backfill_commission.mjs exists to sweep up
        // anything that lands here.
        console.error(`❌ applyDeliveryCommission failed for order #${orderId}:`, error.message);
    }
};

/**
 * Marks a subscription day's order delivered and snapshots commission on it.
 *
 * Subscription orders are inserted as 'confirmed'/'paid' by placeDayOrder and
 * nothing ever moved them to 'delivered' — so they were invisible to the
 * commission ledger AND to the cook's own earnings screen, both of which filter
 * on status = 'delivered'. This is the bridge between the day log and the
 * orders table.
 *
 * Returns true only if this call is what marked the order delivered, so callers
 * can log it without double-counting on a re-run.
 */
export const settleSubscriptionDayOrder = async (orderId, executor = null) => {
    if (!orderId) return false;
    try {
        const runner = executor || db.promise();
        const [res] = await runner.query(
            `UPDATE orders SET status = 'delivered' WHERE id = ? AND status <> 'delivered'`,
            [orderId]
        );
        // Snapshot regardless of whether the status write was a no-op: the order
        // may already have been 'delivered' from an earlier partial run that
        // failed before commission landed. The IS NULL guard makes this safe.
        await applyDeliveryCommission(orderId, runner);
        return res.affectedRows > 0;
    } catch (error) {
        console.error(`❌ settleSubscriptionDayOrder failed for order #${orderId}:`, error.message);
        return false;
    }
};

export default { DEFAULT_COMMISSION_PCT, getCommissionPct, applyDeliveryCommission, settleSubscriptionDayOrder };
