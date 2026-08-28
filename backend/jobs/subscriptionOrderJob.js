import cron from "node-cron";
import db from "../config/db.js";
import {
    notifySubscriptionDeliverySkipped,
    notifySubscriptionDeliverySkippedToCook,
    notifySubscriptionCompleted,
    notifySubscriptionCompletedToCook
} from "../utils/notificationHelper.js";
import {
    getNptToday,
    addDays,
    daysBetween,
    dateOnly,
    SQL_NPT_TODAY
} from "../utils/nptTime.js";
import {
    DAY_STATUS,
    NON_DELIVERY_STATUSES,
    applyDayStatus,
    getDayRow,
    isCookClosedOn,
    placeDayOrder,
    logDayEvent
} from "../utils/subscriptionDailyLog.js";
import { expireDanglingCustomMealRequests } from "../controllers/customMealController.js";

/**
 * Nightly subscription lifecycle job. Five independent passes, run in order:
 *
 *   1. reconcileYesterday()  — settle yesterday's open days as delivered/missed
 *   2. activateScheduledSubscriptions() — 'scheduled' → 'active' on the start date
 *   3. createTodaysDeliveries() — one day row + one order per active subscription
 *   4. completeExpiredSubscriptions() — close out any window whose end_date passed
 *   5. expireDanglingCustomMealRequests() — close swaps for dead days/subscriptions
 *
 * Each pass is wrapped independently: a failure in reconciliation must not stop
 * today's meals from being ordered.
 *
 * ── Two things changed from the previous version, both deliberate ────────────
 *
 * DELIVERY CADENCE IS NOW DAILY. The old job advanced next_delivery_date by
 * getDurationDays(duration) — 7, 14 or 30 days — so a "1 Week" plan delivered
 * exactly ONCE and then waited a week. A per-day log, per-day skip toggles, a
 * 14-day calendar and a "today's deliveries" list are all meaningless under that
 * cadence. Deliveries now advance one day at a time.
 *
 * THE SUBSCRIPTION IS A CALENDAR WINDOW, NOT A MEAL-CREDIT POOL. start_date and
 * end_date are fixed once, at verification, and never move again. A skipped day,
 * a closed kitchen, or an unavailable plan item means no meal and no charge for
 * that day — it does NOT push end_date out. `meals_remaining` survives only as
 * bookkeeping (how many days were actually charged); it no longer decides when a
 * subscription completes. end_date does, via findNextDeliveryDate() returning
 * null and via pass 4.
 *
 * DATES ARE NPT, NOT CURDATE(). The server and TiDB both run UTC, so between
 * 00:00 and 05:45 NPT, CURDATE() names YESTERDAY. This job runs at 06:00 NPT
 * where the two happen to agree, but an admin-triggered run or a schedule change
 * would silently process the wrong day. Every date here comes from getNptToday()
 * or SQL_NPT_TODAY.
 */

/**
 * Settle every day row that is still 'scheduled' after its date has passed.
 *
 * Without this pass, a day sits 'scheduled' forever and there is no way to tell
 * a meal that was delivered from one that never happened — which is precisely
 * the record a billing dispute needs.
 *
 * A day is 'delivered' only if its order actually reached delivered/completed.
 * Anything else is 'missed', with the order's real status kept in `reason`.
 * Deliberately NOT refunding the credit on a 'missed' day: whether a cook owes a
 * make-up meal is a support decision with a human in it, and a cron that
 * silently hands back credits would be trivially abusable. The row records the
 * facts; a human decides the remedy.
 */
const reconcileYesterday = async () => {
    const today = getNptToday();
    try {
        // Everything still open before today, not just yesterday — if the job
        // didn't run for two days, day two must not be left dangling forever.
        const [open] = await db.promise().query(
            `SELECT sdl.id, sdl.subscription_id,
                    DATE_FORMAT(sdl.delivery_date, '%Y-%m-%d') AS delivery_date,
                    sdl.order_id, o.status AS order_status
             FROM subscription_daily_log sdl
             LEFT JOIN orders o ON o.id = sdl.order_id
             WHERE sdl.status = 'scheduled'
               AND sdl.delivery_date < ?`,
            [today]
        );

        if (open.length === 0) {
            console.log("ℹ️  Reconcile: no past days left open.");
            return;
        }

        let delivered = 0;
        let missed = 0;
        for (const row of open) {
            const wasDelivered = row.order_status === "delivered" || row.order_status === "completed";
            const newStatus = wasDelivered ? DAY_STATUS.DELIVERED : DAY_STATUS.MISSED;
            const reason = wasDelivered
                ? null
                : `Auto-settled: order ${row.order_id ? `#${row.order_id} was "${row.order_status}"` : "was never placed"} at end of day.`;

            // Guarded on status = 'scheduled' so a late manual update by the cook
            // between the read above and this write is not overwritten.
            const [upd] = await db.promise().query(
                `UPDATE subscription_daily_log
                 SET status = ?, toggled_by = 'system', reason = COALESCE(?, reason), toggled_at = NOW()
                 WHERE id = ? AND status = 'scheduled'`,
                [newStatus, reason, row.id]
            );
            if (upd.affectedRows === 0) continue;

            if (wasDelivered) {
                delivered++;
            } else {
                missed++;
            }

            // Only the bad outcome is audited. A delivered day is the expected
            // path and writing an event for every one of them would bury the
            // exceptions that a dispute actually needs to find.
            if (!wasDelivered) {
                await logDayEvent({
                    subscriptionId: row.subscription_id,
                    event: "day_missed",
                    actor: "system",
                    detail: `${row.delivery_date} settled as missed. ${reason} Credit was NOT returned — needs a human decision.`
                });
            }
        }

        console.log(`✅ Reconcile: ${delivered} delivered, ${missed} missed.`);
    } catch (err) {
        console.error("❌ Reconcile pass error:", err.message);
    }
};

/**
 * Flip 'scheduled' subscriptions to 'active' once their customer-chosen start
 * date arrives.
 *
 * `start_date <= today` rather than `= today`: if the job misses a day, a
 * subscription whose start date has passed must still activate rather than stay
 * scheduled forever waiting for a date that will never come round again.
 */
const activateScheduledSubscriptions = async () => {
    try {
        const [due] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d') AS start_date
             FROM subscriptions s
             WHERE s.status = 'scheduled'
               AND s.payment_status = 'verified'
               AND s.start_date IS NOT NULL
               AND s.start_date <= ${SQL_NPT_TODAY}`
        );

        for (const sub of due) {
            const [upd] = await db.promise().query(
                `UPDATE subscriptions
                 SET status = 'active', next_delivery_date = ${SQL_NPT_TODAY}
                 WHERE id = ? AND status = 'scheduled'`,
                [sub.id]
            );
            if (upd.affectedRows === 0) continue;

            await logDayEvent({
                subscriptionId: sub.id,
                event: "activated",
                actor: "system",
                detail: `Start date ${sub.start_date} reached — subscription is now active.`
            });
            console.log(`▶️  Activated subscription ${sub.id} (start date ${sub.start_date}).`);
        }

        if (due.length === 0) console.log("ℹ️  No scheduled subscriptions starting today.");
    } catch (err) {
        console.error("❌ Activation pass error:", err.message);
    }
};

/**
 * The first date from `fromDate` onward that isn't already skipped or closed,
 * so "Next delivery" never shows a date the customer already opted out of.
 *
 * Bounded by end_date, and returns null when the subscription has no remaining
 * delivery day at all.
 */
const findNextDeliveryDate = async (subscriptionId, fromDate, endDate) => {
    const end = dateOnly(endDate);
    if (end && daysBetween(fromDate, end) < 0) return null;

    // Open-ended (legacy) subscriptions get a bounded look-ahead rather than an
    // unbounded walk.
    const horizon = end || addDays(fromDate, 60);

    const [rows] = await db.promise().query(
        `SELECT DATE_FORMAT(delivery_date, '%Y-%m-%d') AS delivery_date
         FROM subscription_daily_log
         WHERE subscription_id = ? AND delivery_date BETWEEN ? AND ?
           AND status IN (?)`,
        [subscriptionId, fromDate, horizon, NON_DELIVERY_STATUSES]
    );
    const blocked = new Set(rows.map(r => r.delivery_date));

    const span = daysBetween(fromDate, horizon);
    for (let i = 0; i <= span; i++) {
        const candidate = addDays(fromDate, i);
        if (!blocked.has(candidate)) return candidate;
    }
    return null;
};

/**
 * Create today's day row and place today's order for every active subscription.
 *
 * Per-subscription transaction, not one big one: a plan with a broken item must
 * not stop every other customer in the country from getting fed.
 */
const createTodaysDeliveries = async () => {
    const today = getNptToday();
    const tomorrow = addDays(today, 1);

    try {
        const [subs] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id, s.plan_id, s.delivery_address,
                    s.meals_remaining, s.meals_total,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d') AS end_date,
                    p.name AS plan_name, p.duration, p.price_per_delivery
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.status = 'active'
               AND s.payment_status = 'verified'
               AND (s.start_date IS NULL OR s.start_date <= ?)
               AND (s.end_date IS NULL OR s.end_date >= ?)`,
            [today, today]
        );

        if (subs.length === 0) {
            console.log("ℹ️  No active subscriptions due today.");
            return;
        }

        for (const sub of subs) {
            // meals_remaining is bookkeeping only now — it records how many meals
            // were actually charged, and is NOT what ends a subscription.
            // Completion is bounded by end_date alone (calendar-day model): a
            // 7-day plan ends 7 calendar days after it starts no matter how many
            // of those days were skipped, closed, or delivered.
            const hasCredits = sub.meals_remaining !== null && sub.meals_remaining !== undefined;

            // Already handled — the customer skipped today, the cook closed it, or
            // an earlier run (or the verify-at-activation path) already ordered.
            // Re-ordering here would charge a second credit for one day's food.
            const existing = await getDayRow(null, sub.id, today);
            if (existing) {
                console.log(`⏭️  Subscription ${sub.id}: ${today} already logged as "${existing.status}".`);
                continue;
            }

            const connection = await db.promise().getConnection();
            try {
                await connection.beginTransaction();

                // A closure declared before this subscription activated has no
                // fan-out row for it, so the closure table is the only place the
                // information exists.
                if (await isCookClosedOn(connection, sub.cook_id, today)) {
                    await applyDayStatus(connection, {
                        subscriptionId: sub.id,
                        deliveryDate: today,
                        status: DAY_STATUS.COOK_UNAVAILABLE,
                        toggledBy: "cook",
                        reason: "Kitchen closed for this date",
                        creditDeducted: false
                    });
                    await connection.query(
                        `UPDATE subscriptions SET next_delivery_date = ? WHERE id = ?`,
                        [tomorrow, sub.id]
                    );
                    await connection.commit();

                    await logDayEvent({
                        subscriptionId: sub.id,
                        event: "cook_unavailable",
                        actor: "system",
                        detail: `${today} skipped — cook had marked the date unavailable. No charge; the ${sub.end_date || "subscription"} end date is unchanged.`
                    });
                    console.log(`🚫 Subscription ${sub.id}: cook closed ${today}, no credit charged.`);
                    continue;
                }

                // Row first with credit_deducted = FALSE, order second, credit
                // charged only once the order exists. The reverse order charges a
                // meal for food that a broken plan means nobody ever cooked.
                await applyDayStatus(connection, {
                    subscriptionId: sub.id,
                    deliveryDate: today,
                    status: DAY_STATUS.SCHEDULED,
                    toggledBy: "system",
                    reason: null,
                    creditDeducted: false
                });

                const order = await placeDayOrder(connection, sub, today);

                if (!order.placed) {
                    // An unavailable plan meal is the cook's problem, not the
                    // customer's: the day becomes a free closure day. The window
                    // itself does not move (calendar-day model).
                    const names = order.items.map(i => i.meal_name).join(", ");
                    await applyDayStatus(connection, {
                        subscriptionId: sub.id,
                        deliveryDate: today,
                        status: DAY_STATUS.COOK_UNAVAILABLE,
                        toggledBy: "system",
                        reason: `Unavailable in the kitchen today: ${names}`,
                        creditDeducted: false
                    });
                    await connection.query(
                        `UPDATE subscriptions SET next_delivery_date = ? WHERE id = ?`,
                        [tomorrow, sub.id]
                    );
                    await connection.commit();

                    const [[customer]] = await db.promise().query(
                        "SELECT full_name FROM users WHERE id = ?", [sub.customer_id]
                    );
                    await notifySubscriptionDeliverySkipped(sub.customer_id, sub.id, sub.plan_name);
                    await notifySubscriptionDeliverySkippedToCook(
                        sub.cook_id, sub.id, sub.plan_name, customer?.full_name || "A subscriber"
                    );
                    await logDayEvent({
                        subscriptionId: sub.id,
                        event: "day_unavailable",
                        actor: "system",
                        detail: `${today} not delivered — unavailable meal(s): ${names}. No charge; the ${sub.end_date || "subscription"} end date is unchanged.`
                    });
                    console.log(`⏭️  Subscription ${sub.id}: unavailable meal(s) — ${names}.`);
                    continue;
                }

                // The order exists, so the day is genuinely served: charge it.
                await connection.query(
                    `UPDATE subscription_daily_log SET credit_deducted = TRUE
                     WHERE subscription_id = ? AND delivery_date = ?`,
                    [sub.id, today]
                );
                if (hasCredits) {
                    await connection.query(
                        `UPDATE subscriptions SET meals_remaining = GREATEST(meals_remaining - 1, 0) WHERE id = ?`,
                        [sub.id]
                    );
                }

                await connection.commit();
                console.log(`✅ Subscription ${sub.id}: order ${order.orderId} placed for ${today}.`);

                // Outside the transaction — next_delivery_date is a display field,
                // and the look-ahead query it needs shouldn't hold delivery locks.
                const nextDate = await findNextDeliveryDate(sub.id, tomorrow, sub.end_date);
                await db.promise().query(
                    `UPDATE subscriptions SET next_delivery_date = ? WHERE id = ?`,
                    [nextDate, sub.id]
                );

                // end_date is the only thing that ends a subscription now, and
                // findNextDeliveryDate already returns null once tomorrow is past
                // it. A remaining meal count of zero is no longer a completion
                // reason — the window, not the credit pool, decides.
                if (nextDate === null) {
                    await completeSubscription(sub, "The subscription's last calendar day has passed.");
                }
            } catch (err) {
                await connection.rollback();
                console.error(`❌ Subscription ${sub.id}: could not place today's order:`, err.message);
            } finally {
                connection.release();
            }
        }
    } catch (err) {
        console.error("❌ Delivery pass error:", err.message);
    }
};

/*
 * ── Calendar-day model: there is no extendCycleByADay any more ────────────────
 * A subscription is a window of N calendar days, fixed once at verification.
 * A day nobody was charged for (kitchen closed, plan meal unavailable, customer
 * skipped) simply passes with no meal; it does not push end_date out. The old
 * helper here extended end_date on every such day, which meant the end date the
 * customer and cook agreed to drifted silently — and three independent actors
 * (this job, the cook's closure fan-out, and the customer's skip) could each
 * stretch the same window.
 */

/**
 * Close a subscription out and tell both sides.
 *
 * Guarded on status = 'active' so this can be called from more than one place in
 * the pass above without double-notifying, and so it can't resurrect-then-close
 * something a customer cancelled in the meantime.
 */
const completeSubscription = async (sub, why) => {
    try {
        const [upd] = await db.promise().query(
            `UPDATE subscriptions SET status = 'completed', next_delivery_date = NULL
             WHERE id = ? AND status = 'active'`,
            [sub.id]
        );
        if (upd.affectedRows === 0) return;

        await logDayEvent({
            subscriptionId: sub.id,
            event: "completed",
            actor: "system",
            detail: why
        });

        const [[customer]] = await db.promise().query(
            "SELECT full_name FROM users WHERE id = ?", [sub.customer_id]
        );
        await notifySubscriptionCompleted(sub.customer_id, sub.id, sub.plan_name, sub.meals_total);
        await notifySubscriptionCompletedToCook(
            sub.cook_id, sub.id, customer?.full_name || "A subscriber", sub.plan_name
        );
        console.log(`🏁 Subscription ${sub.id} completed — ${why}`);
    } catch (err) {
        console.error(`❌ Could not complete subscription ${sub.id}:`, err.message);
    }
};

/**
 * Close out every active subscription whose last calendar day is already behind
 * us.
 *
 * This exists because createTodaysDeliveries only SELECTs rows with
 * `end_date >= today`, so an expired row is invisible to it and would sit at
 * 'active' forever. Under the old meal-credit model the `meals_remaining <= 0`
 * gate happened to catch most of them; with end_date as the sole bound there has
 * to be an explicit sweep.
 *
 * completeSubscription is guarded on `status = 'active'`, so running this every
 * night is idempotent.
 */
const completeExpiredSubscriptions = async () => {
    const today = getNptToday();
    try {
        const [subs] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id, s.meals_total,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d') AS end_date,
                    p.name AS plan_name
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.status = 'active' AND s.end_date IS NOT NULL AND s.end_date < ?`,
            [today]
        );
        for (const sub of subs) {
            await completeSubscription(
                sub,
                `Subscription window ended on ${sub.end_date}.`
            );
        }
        if (subs.length) console.log(`🏁 Closed ${subs.length} expired subscription(s).`);
    } catch (err) {
        console.error("❌ completeExpiredSubscriptions failed:", err.message);
    }
};

/**
 * Runs the whole nightly lifecycle. Exported separately from the cron wiring so
 * it can be invoked directly — by tests, or an admin "run it now" trigger — same
 * reasoning as commissionSettlementJob.js's generateMonthlySettlements.
 */
export const processDueSubscriptions = async () => {
    console.log(`🔁 Subscription job starting for ${getNptToday()} (NPT)...`);
    await reconcileYesterday();
    await activateScheduledSubscriptions();
    await createTodaysDeliveries();
    // Order matters: expire the window BEFORE cleaning up dangling custom meal
    // requests, so a request hanging off a subscription that ended last night is
    // seen as belonging to a completed subscription and closed in the same pass.
    await completeExpiredSubscriptions();
    try {
        const expired = await expireDanglingCustomMealRequests();
        if (expired) console.log(`🧹 ${expired} dangling custom meal request(s) closed.`);
    } catch (err) {
        console.error("❌ expireDanglingCustomMealRequests failed:", err.message);
    }
    console.log("🔁 Subscription job finished.");
};

/** Wires processDueSubscriptions into the actual daily 06:00 cron schedule. */
export const startSubscriptionJob = () => {
    cron.schedule("0 6 * * *", processDueSubscriptions);
    console.log("✅ Subscription cron job scheduled (daily at 06:00)");
};
