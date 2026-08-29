/**
 * Shared primitives for the per-day subscription delivery log.
 *
 * Lives in utils/ because three callers need identical semantics and any drift
 * between them is a correctness bug, not a style issue:
 *   - subscriptionController  (customer skips a day, calendar read, verify)
 *   - cookDeliveryController  (cook closes a whole date, today's deliveries)
 *   - jobs/subscriptionOrderJob (nightly activation, ordering, reconciliation)
 *
 * Two invariants everything here exists to protect:
 *
 *  1. 'delivered' is TERMINAL. Food that has been handed over cannot be
 *     retroactively turned into "cook was unavailable" — that would let a
 *     closure backdate away a delivery the customer actually received, and the
 *     credit for it.
 *
 *  2. Writes are decided by the UNIQUE KEY (subscription_id, delivery_date),
 *     never by check-then-write in application code. A customer skipping a day
 *     at the same moment as their cook closing it is a genuine race; the
 *     constraint resolves it deterministically and both attempts are recorded.
 */

import db from "../config/db.js";
import { dateOnly } from "./nptTime.js";
import { settleSubscriptionDayOrder } from "./commissionSnapshot.js";

/** Every state a single delivery day can be in. Mirrors the table's ENUM. */
export const DAY_STATUS = {
    SCHEDULED: "scheduled",
    /** Cook handed the meal over; waiting on the customer to confirm receipt. */
    SENT: "sent",
    CUSTOMER_SKIPPED: "customer_skipped",
    COOK_UNAVAILABLE: "cook_unavailable",
    DELIVERED: "delivered",
    MISSED: "missed"
};

/** Statuses that mean "no meal is coming, and no credit was spent". */
export const NON_DELIVERY_STATUSES = [DAY_STATUS.CUSTOMER_SKIPPED, DAY_STATUS.COOK_UNAVAILABLE];

/** Customer-facing wording for each day status. Kept server-side so the
 *  Android app and any future web client can't drift apart on it. */
export const DAY_STATUS_LABELS = {
    [DAY_STATUS.SCHEDULED]: "Scheduled",
    [DAY_STATUS.SENT]: "Sent",
    [DAY_STATUS.CUSTOMER_SKIPPED]: "You skipped",
    [DAY_STATUS.COOK_UNAVAILABLE]: "Kitchen closed",
    [DAY_STATUS.DELIVERED]: "Delivered",
    [DAY_STATUS.MISSED]: "Missed"
};

/**
 * Append-only audit record of a lifecycle/day transition.
 *
 * Reuses subscription_payment_events rather than adding a second audit table:
 * that table's stated purpose is dispute resolution for a subscription, and a
 * dispute about "I was charged for a day you never delivered" needs the payment
 * events and the day events on one timeline, ordered, not in two tables that
 * have to be manually interleaved.
 *
 * Never throws — an unwritable audit row must not fail the operation it
 * describes, but it is loudly logged.
 */
export async function logDayEvent({ subscriptionId, event, actor = "system", detail = null, executor = null }) {
    const runner = executor || db.promise();
    try {
        await runner.query(
            `INSERT INTO subscription_payment_events (subscription_id, event, detail)
             VALUES (?, ?, ?)`,
            [subscriptionId, String(event).slice(0, 40), `[${actor}] ${detail || ""}`.slice(0, 500)]
        );
    } catch (err) {
        console.error(`❌ Could not audit "${event}" on subscription ${subscriptionId}:`, err.message);
    }
}

/**
 * Bridge from a delivered subscription day to the orders ledger.
 *
 * placeDayOrder inserts subscription orders as 'confirmed'/'paid' and nothing
 * ever moved them on, so every subscription delivery was invisible to BOTH the
 * commission ledger and the cook's own earnings screen — each of which filters
 * on orders.status = 'delivered'. Commission on subscription revenue was
 * therefore ₹0 no matter how much a cook delivered.
 *
 * Done here rather than in the three callers so it cannot drift: the customer
 * confirming receipt (subscriptionController.markDayReceived), a cook-side
 * delivery, and the nightly reconcile all reach 'delivered' through
 * applyDayStatus, and all three now charge commission identically.
 *
 * Never throws — a commission failure must not undo a delivery the customer has
 * already received. settleSubscriptionDayOrder swallows and logs its own
 * errors, and backfill_commission.mjs sweeps up anything that lands there.
 */
async function chargeCommissionForDeliveredDay(executor, subscriptionId, date, knownOrderId) {
    let orderId = knownOrderId;
    if (!orderId) {
        // The UPDATE path never writes order_id, so on a scheduled → delivered
        // transition the id lives only on the existing row.
        const [[row]] = await executor.query(
            `SELECT order_id FROM subscription_daily_log
             WHERE subscription_id = ? AND delivery_date = ?`,
            [subscriptionId, date]
        );
        orderId = row ? row.order_id : null;
    }
    // A day can legitimately be delivered with no order row (a legacy day, or
    // one whose order failed to place). Nothing to charge — not an error.
    if (!orderId) return;
    await settleSubscriptionDayOrder(orderId, executor);
}

/**
 * Set one day's status, atomically and without ever clobbering a delivery.
 *
 * `executor` must be a mysql2 promise connection or pool — pass the transaction
 * connection when this is part of a bulk operation so the whole fan-out commits
 * or rolls back together.
 *
 * `onlyFrom` is the list of previous statuses this write may overwrite. Pass it
 * whenever the caller COMPENSATES for the write (extends the cycle, refunds a
 * credit): without it, a day that is already settled as non-delivering gets
 * rewritten and compensated a second time. That was a real bug — a customer
 * skipped a day and was given an extra day, then the cook closed the same date
 * and the same single non-delivered day bought a SECOND extra day. Omit it
 * (null) only for callers that are deciding a day's fate for the first time,
 * i.e. the cron.
 *
 * Returns { applied, created, previousStatus, blockedBy }:
 *   applied     — the row now holds `status` because of THIS call
 *   created     — the row didn't exist and was inserted (vs updated in place)
 *   blockedBy   — why nothing changed:
 *                 'delivered'       — terminal, never rewritten
 *                 'already_set'     — the row already held exactly this status
 *                 'not_overwritable'— settled as something `onlyFrom` excludes
 *                 'raced'           — a concurrent write changed it underneath us
 *                 null              — nothing was blocked
 *
 * The INSERT IGNORE / guarded-UPDATE pair is deliberate. The obvious
 * alternative, INSERT ... ON DUPLICATE KEY UPDATE with an
 * IF(status='delivered', status, VALUES(status)) guard, is subtly wrong on
 * MySQL/TiDB: assignments are evaluated left to right, so any guard listed
 * after the `status` assignment reads the NEW value and always passes.
 */
export async function applyDayStatus(executor, {
    subscriptionId,
    deliveryDate,
    status,
    toggledBy = "system",
    reason = null,
    creditDeducted = false,
    orderId = null,
    onlyFrom = null
}) {
    const date = dateOnly(deliveryDate);
    if (!date) throw new Error(`applyDayStatus: unusable deliveryDate "${deliveryDate}"`);

    const [ins] = await executor.query(
        `INSERT IGNORE INTO subscription_daily_log
            (subscription_id, delivery_date, status, toggled_by, reason, credit_deducted, order_id, toggled_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, NOW())`,
        [subscriptionId, date, status, toggledBy, reason, creditDeducted ? 1 : 0, orderId]
    );
    if (ins.affectedRows === 1) {
        if (status === DAY_STATUS.DELIVERED) {
            await chargeCommissionForDeliveredDay(executor, subscriptionId, date, orderId);
        }
        return { applied: true, created: true, previousStatus: null, blockedBy: null };
    }

    // A row already exists. This read is for REPORTING which branch the caller
    // should take; the actual protection is in the UPDATE's WHERE clause below,
    // so a write landing between this read and that one still loses correctly.
    const [[existing]] = await executor.query(
        `SELECT status, credit_deducted FROM subscription_daily_log
         WHERE subscription_id = ? AND delivery_date = ?`,
        [subscriptionId, date]
    );
    const previousStatus = existing ? existing.status : null;

    if (previousStatus === DAY_STATUS.DELIVERED) {
        return { applied: false, created: false, previousStatus, blockedBy: "delivered" };
    }
    if (previousStatus === status) {
        return { applied: false, created: false, previousStatus, blockedBy: "already_set" };
    }
    if (onlyFrom && !onlyFrom.includes(previousStatus)) {
        return { applied: false, created: false, previousStatus, blockedBy: "not_overwritable" };
    }

    // `status <> 'delivered'` stays even when onlyFrom already excludes it: it is
    // the invariant this module exists to guarantee, and it must not depend on
    // every caller remembering to pass the right list.
    const restrict = onlyFrom ? " AND status IN (?)" : "";
    const params = [status, toggledBy, reason, creditDeducted ? 1 : 0, subscriptionId, date];
    if (onlyFrom) params.push(onlyFrom);

    const [upd] = await executor.query(
        `UPDATE subscription_daily_log
         SET status = ?, toggled_by = ?, reason = ?, credit_deducted = ?, toggled_at = NOW()
         WHERE subscription_id = ? AND delivery_date = ? AND status <> 'delivered'${restrict}`,
        params
    );

    if (upd.affectedRows === 1) {
        // Only on an applied transition: a blocked or raced write must not
        // charge commission a second time (the snapshot's IS NULL guard makes
        // that harmless, but not charging at all is clearer than relying on it).
        if (status === DAY_STATUS.DELIVERED) {
            await chargeCommissionForDeliveredDay(executor, subscriptionId, date, orderId);
        }
        return { applied: true, created: false, previousStatus, blockedBy: null };
    }

    // Lost a race: re-read rather than guess, because this outcome is written
    // into the audit trail and "blocked by delivered" when it was actually
    // something else is exactly the kind of wrong detail a dispute turns on.
    const [[now]] = await executor.query(
        `SELECT status FROM subscription_daily_log WHERE subscription_id = ? AND delivery_date = ?`,
        [subscriptionId, date]
    );
    const current = now ? now.status : null;
    return {
        applied: false,
        created: false,
        previousStatus: current,
        blockedBy: current === DAY_STATUS.DELIVERED ? "delivered" : "raced"
    };
}

/** One day's row, or null. Dates come back as 'YYYY-MM-DD' strings. */
export async function getDayRow(executor, subscriptionId, deliveryDate) {
    const date = dateOnly(deliveryDate);
    if (!date) return null;
    const [rows] = await (executor || db.promise()).query(
        `SELECT id, subscription_id, DATE_FORMAT(delivery_date, '%Y-%m-%d') AS delivery_date,
                status, toggled_by, reason, credit_deducted, order_id, toggled_at
         FROM subscription_daily_log
         WHERE subscription_id = ? AND delivery_date = ?`,
        [subscriptionId, date]
    );
    return rows.length > 0 ? rows[0] : null;
}

/**
 * Every logged day for a subscription within an inclusive date window, keyed by
 * date so the calendar builder can merge real rows over projected ones in O(1).
 *
 * DATE_FORMAT rather than returning the raw DATE column: mysql2 hydrates DATE
 * into a JS Date at local midnight, which serialises to JSON as
 * "2026-09-03T00:00:00.000Z" — the exact string the app was displaying to
 * users. Formatting in SQL means a date column can never reach a client as a
 * timestamp.
 */
export async function getDayRowsInRange(executor, subscriptionId, fromDate, toDate) {
    const [rows] = await (executor || db.promise()).query(
        `SELECT DATE_FORMAT(delivery_date, '%Y-%m-%d') AS delivery_date,
                status, toggled_by, reason, credit_deducted, order_id
         FROM subscription_daily_log
         WHERE subscription_id = ? AND delivery_date BETWEEN ? AND ?
         ORDER BY delivery_date ASC`,
        [subscriptionId, dateOnly(fromDate), dateOnly(toDate)]
    );
    const byDate = new Map();
    for (const row of rows) byDate.set(row.delivery_date, row);
    return byDate;
}

/**
 * Dates in [fromDate, toDate] the cook has declared closed, as a Set of
 * 'YYYY-MM-DD'.
 *
 * Needed on top of the day rows because a closure recorded BEFORE a
 * subscription activated never got a fan-out row for it — the fan-out can only
 * reach subscribers that exist at the time. Without this, a customer who
 * subscribes on the 1st for a start date of the 5th would see "Scheduled" for a
 * day the cook already announced they were closed.
 */
export async function getCookClosuresInRange(executor, cookId, fromDate, toDate) {
    const [rows] = await (executor || db.promise()).query(
        `SELECT DATE_FORMAT(unavailable_date, '%Y-%m-%d') AS unavailable_date, reason
         FROM cook_daily_availability
         WHERE cook_id = ? AND unavailable_date BETWEEN ? AND ?`,
        [cookId, dateOnly(fromDate), dateOnly(toDate)]
    );
    const byDate = new Map();
    for (const row of rows) byDate.set(row.unavailable_date, row.reason);
    return byDate;
}

/** True if the cook has declared this single date closed. */
export async function isCookClosedOn(executor, cookId, date) {
    const [rows] = await (executor || db.promise()).query(
        `SELECT 1 FROM cook_daily_availability WHERE cook_id = ? AND unavailable_date = ? LIMIT 1`,
        [cookId, dateOnly(date)]
    );
    return rows.length > 0;
}

/**
 * Place the real order for one delivery day.
 *
 * Extracted here so the nightly cron and an on-the-spot activation (a cook
 * verifying payment at 2pm for a subscription whose start date is TODAY, hours
 * after the 06:00 batch has already run) go through identical code. When these
 * were two implementations, the second path silently didn't exist and the
 * customer lost a paid day.
 *
 * `executor` MUST be a transaction connection — this writes an order, its items
 * and the day row's order_id, and a partial result would leave a paid day with
 * an order the log doesn't know about.
 *
 * Returns { placed: true, orderId } or { placed: false, blockedBy:
 * 'unavailable_items', items: [...] } — an unavailable plan meal is a normal
 * outcome the caller reports to both sides, not an exception.
 */
export async function placeDayOrder(executor, subscription, deliveryDate) {
    const date = dateOnly(deliveryDate);

    const [items] = await executor.query(
        `SELECT spi.meal_id, spi.quantity, m.price, m.is_available, m.name AS meal_name
         FROM subscription_plan_items spi
         JOIN meals m ON spi.meal_id = m.id
         WHERE spi.plan_id = ?`,
        [subscription.plan_id]
    );
    if (items.length === 0) {
        throw new Error(`plan ${subscription.plan_id} has no items`);
    }

    // A meal the cook has marked unavailable must not ship as a partial order,
    // and must not silently vanish from what the customer paid for.
    const unavailable = items.filter(i => !i.is_available);
    if (unavailable.length > 0) {
        return { placed: false, blockedBy: "unavailable_items", items: unavailable };
    }

    // The plan's set price is what the customer subscribed at; item prices are
    // only summed for legacy plans that never had one. order_items still
    // records each item's real menu price for the kitchen.
    const totalAmount = subscription.price_per_delivery !== null && subscription.price_per_delivery !== undefined
        ? parseFloat(subscription.price_per_delivery)
        : items.reduce((sum, i) => sum + parseFloat(i.price) * i.quantity, 0);

    // 'paid'/'online': a subscription is settled ONCE up front, so billing
    // again on delivery would charge twice for the same food.
    const [result] = await executor.query(
        `INSERT INTO orders (customer_id, cook_id, total_amount, delivery_address, status, payment_method, payment_status, special_instructions)
         VALUES (?, ?, ?, ?, 'confirmed', 'online', 'paid', ?)`,
        [
            subscription.customer_id,
            subscription.cook_id,
            totalAmount,
            subscription.delivery_address,
            `Auto-subscription order — prepaid (${date})`
        ]
    );
    const orderId = result.insertId;

    for (const item of items) {
        await executor.query(
            `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time) VALUES (?, ?, ?, ?)`,
            [orderId, item.meal_id, item.quantity, item.price]
        );
    }

    await executor.query(
        `UPDATE subscription_daily_log SET order_id = ?
         WHERE subscription_id = ? AND delivery_date = ?`,
        [orderId, subscription.id, date]
    );

    return { placed: true, orderId };
}

export default {
    DAY_STATUS,
    NON_DELIVERY_STATUSES,
    DAY_STATUS_LABELS,
    logDayEvent,
    applyDayStatus,
    getDayRow,
    getDayRowsInRange,
    getCookClosuresInRange,
    isCookClosedOn,
    placeDayOrder
};
