/**
 * D2 — backfill commission snapshots onto delivered orders that never got one.
 *
 * WHY THIS EXISTS
 * The commission pipeline is correct end to end but produces ₹0, because every
 * order currently sitting at status='delivered' has commission_amount IS NULL
 * AND delivered_at IS NULL. Those rows were seeded straight into 'delivered'
 * rather than transitioning through the cook/admin status endpoint, which is
 * the only caller of applyDeliveryCommission() — so nothing was ever
 * snapshotted. getCommissionByCook filters on
 * `commission_amount IS NOT NULL AND MONTH(CONVERT_TZ(delivered_at,...)) = ?`,
 * so those orders are invisible to it, generateMonthlySettlements creates zero
 * rows, and no cook ever owes anything.
 *
 * WHAT IT DOES
 * For orders WHERE status='delivered' AND commission_amount IS NULL:
 *   delivered_at      := COALESCE(delivered_at, updated_at)
 *   commission_pct    := the CURRENT platform_settings.commission_pct
 *   commission_amount := ROUND(total_amount * pct / 100, 2)
 *
 * That mirrors applyDeliveryCommission() exactly, including its per-order
 * ROUND — so backfilled rows aggregate identically to organically-snapshotted
 * ones and no reconciliation drift is introduced.
 *
 * SAFETY
 *  - DRY RUN BY DEFAULT. Prints every affected row and the resulting totals and
 *    exits without writing. Pass --apply to actually write.
 *  - Idempotent: the UPDATE is guarded by `commission_amount IS NULL`, the same
 *    guard applyDeliveryCommission uses, so re-running can never double-charge
 *    or re-rate an order that already has a snapshot.
 *  - Refund-aware: reports refunded orders separately and never snapshots them
 *    (edge case 1 — commission is not owed on money handed back).
 *
 * THIS CREATES REAL DEBTS AGAINST REAL COOKS. Read the dry-run output and
 * confirm the numbers before ever passing --apply.
 *
 * SUBSCRIPTION PASS (pass 0, runs first)
 * Subscription deliveries were a second, larger hole: placeDayOrder inserts the
 * day's order as 'confirmed'/'paid' and, before utils/commissionSnapshot.js
 * existed, nothing ever moved it to 'delivered'. So a subscription day marked
 * delivered in subscription_daily_log left its order sitting at 'confirmed' —
 * invisible to the commission ledger AND to the cook's own earnings screen, both
 * of which filter on status = 'delivered'. Pass 0 promotes those orders:
 *   status       := 'delivered'
 *   delivered_at := COALESCE(delivered_at, sdl.toggled_at, sdl.created_at)
 * using the day log's own toggle moment, which is the real delivery time and is
 * what decides the billing period — NOT the order's updated_at, which for these
 * rows is whenever the order was last touched for any reason.
 *
 * Promoting them first means the ordinary pass below then snapshots them with no
 * special-casing: one code path, one rate, one rounding rule.
 *
 *   node scripts/backfill_commission.mjs            # dry run, writes nothing
 *   node scripts/backfill_commission.mjs --apply    # writes
 */

import db from "../config/db.js";
import { toNpt } from "../utils/nepaliTime.js";

const APPLY = process.argv.includes("--apply");
const p = db.promise();
const rs = (n) => `₹${Number(n).toFixed(2)}`;

const main = async () => {
    let subPassCommission = 0;
    const [[settings]] = await p.query("SELECT commission_pct FROM platform_settings WHERE id = 1");
    if (!settings) {
        console.error("ABORT: platform_settings row id=1 is missing — cannot determine the rate.");
        process.exit(1);
    }
    const pct = parseFloat(settings.commission_pct);

    console.log("=".repeat(72));
    console.log(APPLY ? "  D2 BACKFILL — APPLY MODE (WILL WRITE)" : "  D2 BACKFILL — DRY RUN (writes nothing)");
    console.log("=".repeat(72));
    console.log(`Rate to be applied : ${pct}%  (live platform_settings.commission_pct)`);

    // ---------------------------------------------------------------------
    // PASS 0 — subscription days delivered in the day log whose order was
    // never promoted out of 'confirmed'.
    // ---------------------------------------------------------------------
    const [subRows] = await p.query(
        `SELECT o.id, o.cook_id, u.full_name AS cook_name, cp.kitchen_name,
                o.total_amount, o.status,
                sdl.delivery_date,
                CAST(COALESCE(o.delivered_at, sdl.toggled_at, sdl.created_at) AS CHAR) AS effective_utc,
                DATE_FORMAT(${toNpt("COALESCE(o.delivered_at, sdl.toggled_at, sdl.created_at)")}, '%Y-%m') AS npt_period,
                ROUND(o.total_amount * ? / 100, 2) AS proposed_commission
         FROM subscription_daily_log sdl
         JOIN orders o ON o.id = sdl.order_id
         JOIN users u ON u.id = o.cook_id
         LEFT JOIN cook_profiles cp ON cp.user_id = o.cook_id
         WHERE sdl.status = 'delivered'
           AND sdl.order_id IS NOT NULL
           AND o.status <> 'delivered'
           AND (o.refund_status IS NULL OR o.refund_status != 'refunded')
         ORDER BY npt_period, o.cook_id, o.id`,
        [pct]
    );

    console.log(`
PASS 0 — subscription days delivered but order not promoted: ${subRows.length}`);
    if (subRows.length) {
        // Per-cook rollup rather than per-order: this list can run to hundreds of
        // rows on a live DB, and the number that actually needs eyeballing before
        // --apply is "how much new debt per cook", not every individual day.
        const byCook = new Map();
        for (const r of subRows) {
            const cur = byCook.get(r.cook_id) || {
                cook: r.kitchen_name || r.cook_name, days: 0, gross: 0, commission: 0,
                first: r.delivery_date, last: r.delivery_date
            };
            cur.days += 1;
            cur.gross += parseFloat(r.total_amount);
            cur.commission += parseFloat(r.proposed_commission);
            if (r.delivery_date < cur.first) cur.first = r.delivery_date;
            if (r.delivery_date > cur.last) cur.last = r.delivery_date;
            byCook.set(r.cook_id, cur);
        }
        subPassCommission = [...byCook.values()].reduce((a, c) => a + c.commission, 0);
        console.log("cook_id  kitchen/owner                  days   gross         commission    delivery range");
        for (const [id, c] of byCook) {
            const range = `${String(c.first).slice(0, 10)} → ${String(c.last).slice(0, 10)}`;
            console.log(
                `${String(id).padEnd(8)} ${String(c.cook || "—").slice(0, 30).padEnd(30)} ` +
                `${String(c.days).padEnd(6)} ${rs(c.gross).padEnd(13)} ${rs(c.commission).padEnd(13)} ${range}`
            );
        }

        if (APPLY) {
            // One UPDATE per order rather than a single multi-table statement:
            // delivered_at has to come from that order's own day-log row, and a
            // joined UPDATE that picks the wrong day would move the order into
            // the wrong billing month. Volume here is small enough that the extra
            // round trips do not matter.
            let promoted = 0;
            for (const r of subRows) {
                const [u] = await p.query(
                    `UPDATE orders SET status = 'delivered',
                                       delivered_at = COALESCE(delivered_at, ?)
                     WHERE id = ? AND status <> 'delivered'`,
                    [r.effective_utc, r.id]
                );
                promoted += u.affectedRows;
            }
            console.log(`PASS 0 APPLIED — orders promoted to 'delivered': ${promoted}`);
        } else {
            console.log("PASS 0 DRY RUN — these orders would be promoted to 'delivered' and then snapshotted below.");
        }
    }

    // Candidates. delivered_at is COALESCEd to updated_at because these rows
    // never went through applyDeliveryCommission, which is what normally sets
    // delivered_at — updated_at is the best available proxy for when the row
    // reached 'delivered'.
    const [rows] = await p.query(
        `SELECT o.id, o.cook_id, u.full_name AS cook_name, cp.kitchen_name,
                o.total_amount, o.refund_status,
                CAST(COALESCE(o.delivered_at, o.updated_at) AS CHAR)                AS effective_delivered_at_utc,
                CAST(${toNpt("COALESCE(o.delivered_at, o.updated_at)")} AS CHAR)     AS effective_delivered_at_npt,
                DATE_FORMAT(${toNpt("COALESCE(o.delivered_at, o.updated_at)")}, '%Y-%m') AS npt_period,
                ROUND(o.total_amount * ? / 100, 2)                                  AS proposed_commission
         FROM orders o
         JOIN users u ON u.id = o.cook_id
         LEFT JOIN cook_profiles cp ON cp.user_id = o.cook_id
         WHERE o.status = 'delivered' AND o.commission_amount IS NULL
         ORDER BY npt_period, o.cook_id, o.id`,
        [pct]
    );

    if (rows.length === 0) {
        console.log("\nNothing to do — no delivered order is missing a commission snapshot.");
        return;
    }

    // Refunded orders are reported but never snapshotted (edge case 1).
    const refunded = rows.filter(r => r.refund_status === "refunded");
    const eligible = rows.filter(r => r.refund_status !== "refunded");

    console.log(`\nDelivered orders missing a snapshot : ${rows.length}`);
    console.log(`  eligible for backfill             : ${eligible.length}`);
    console.log(`  skipped, already refunded         : ${refunded.length}`);

    console.log("\n--- PER-ORDER DETAIL (eligible) ---");
    console.log("order  cook  gross        commission   NPT period   effective delivered_at (NPT)");
    for (const r of eligible) {
        console.log(
            `#${String(r.id).padEnd(5)} ${String(r.cook_id).padEnd(5)} ` +
            `${rs(r.total_amount).padEnd(12)} ${rs(r.proposed_commission).padEnd(12)} ` +
            `${String(r.npt_period).padEnd(12)} ${r.effective_delivered_at_npt}`
        );
    }
    if (refunded.length) {
        console.log("\n--- SKIPPED (refunded — no commission owed on refunded money) ---");
        for (const r of refunded) console.log(`#${r.id}  cook ${r.cook_id}  ${rs(r.total_amount)}`);
    }

    // Resulting settlements, grouped exactly the way generateMonthlySettlements
    // will group them once these snapshots exist: by NPT period, then by cook.
    const byPeriodCook = new Map();
    for (const r of eligible) {
        const key = `${r.npt_period}|${r.cook_id}`;
        const cur = byPeriodCook.get(key) || {
            period: r.npt_period, cook_id: r.cook_id,
            cook: r.kitchen_name || r.cook_name, orders: 0, gross: 0, commission: 0
        };
        cur.orders += 1;
        cur.gross += parseFloat(r.total_amount);
        cur.commission += parseFloat(r.proposed_commission);
        byPeriodCook.set(key, cur);
    }

    console.log("\n--- RESULTING SETTLEMENTS (what generateMonthlySettlements would then create) ---");
    console.log("period    cook_id  kitchen/owner                  orders  gross         amount_due   due_date");
    let grandGross = 0, grandCommission = 0;
    for (const s of [...byPeriodCook.values()].sort((a, b) => a.period.localeCompare(b.period) || a.cook_id - b.cook_id)) {
        const [y, m] = s.period.split("-").map(Number);
        const [[{ due }]] = await p.query(
            "SELECT CAST(DATE_ADD(DATE_ADD(MAKEDATE(?,1), INTERVAL ? MONTH), INTERVAL 15 DAY) AS CHAR) due",
            [y, m]
        );
        console.log(
            `${s.period}   ${String(s.cook_id).padEnd(8)} ${String(s.cook || "—").slice(0, 30).padEnd(30)} ` +
            `${String(s.orders).padEnd(7)} ${rs(s.gross).padEnd(13)} ${rs(s.commission).padEnd(12)} ${due}`
        );
        grandGross += s.gross;
        grandCommission += s.commission;
    }

    // Independent re-derivation: apply the rate to the aggregate gross and
    // compare against the sum of per-order rounded snapshots. Any difference is
    // pure rounding drift and is expected — see applyDeliveryCommission's
    // rounding note. A large gap means something is wrong.
    const flat = Math.round(grandGross * pct) / 100;
    console.log("\n--- ARITHMETIC CHECK ---");
    console.log(`gross of eligible orders                       : ${rs(grandGross)}`);
    console.log(`sum of per-order ROUND()ed snapshots           : ${rs(grandCommission)}`);
    console.log(`independent check: ${pct}% of aggregate gross      : ${rs(flat)}`);
    console.log(`rounding drift                                 : ${rs(Math.abs(grandCommission - flat))}`);
    console.log(`\nTOTAL NEW COMMISSION DEBT IF APPLIED           : ${rs(grandCommission)}`);
    console.log(`spread across ${byPeriodCook.size} settlement row(s).`);

    if (!APPLY) {
        // In dry run, pass 0 has not promoted anything, so the pass above cannot
        // see those orders. Printing only its total would understate the real debt.
        if (subPassCommission > 0) {
            console.log(`\nNOTE: the total above EXCLUDES pass 0 — those orders are still 'confirmed', so`);
            console.log(`      the pass above cannot see them yet. Pass 0 adds ${rs(subPassCommission)} on top,`);
            console.log(`      for a combined new debt of ${rs(grandCommission + subPassCommission)} once applied.`);
        }
        console.log("\n" + "=".repeat(72));
        console.log("DRY RUN — nothing was written. Re-run with --apply to commit these snapshots.");
        console.log("=".repeat(72));
        return;
    }

    const [result] = await p.query(
        `UPDATE orders
         SET delivered_at      = COALESCE(delivered_at, updated_at),
             commission_pct    = ?,
             commission_amount = ROUND(total_amount * ? / 100, 2)
         WHERE status = 'delivered'
           AND commission_amount IS NULL
           AND (refund_status IS NULL OR refund_status != 'refunded')`,
        [pct, pct]
    );
    console.log(`\nAPPLIED. Rows updated: ${result.affectedRows}`);
    console.log("Next: POST /api/commission/settlements/generate?month=&year= to create the settlement rows.");
};

main()
    .then(() => process.exit(0))
    .catch((e) => { console.error("FAILED:", e.message); process.exit(1); });
