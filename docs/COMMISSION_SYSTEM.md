# TiffinCraft — Commission System

**How the platform gets paid: 5% of a cook's delivered revenue, billed monthly (or paid early on the
cook's initiative), settled manually.**

**No online payment integration.** The cook pays the platform's QR outside the app and uploads a
screenshot; an admin verifies it. There is no eSewa/Khalti API in this flow and none is planned.

---

## 1. Money flow

```
delivery ──► commission snapshotted on the order (5% of total_amount, rounded to paisa)
                          │
       ┌──────────────────┴───────────────────┐
       │ month closes                         │ cook taps "Pay This Month Now"
       │ cron 01:00 on the 1st (NPT)          │ POST /settlements/settle-now
       ▼                                      ▼
          commission_settlements row per cook  ── push: "you owe ₹X by <due date>"
                          │
   cook pays the admin QR │ eSewa / Khalti / bank — outside the app
                          ▼
        cook uploads a screenshot  ──► status 'submitted'  ── push to admin
                          │
        admin opens Admin ▸ Commission Settlements, zooms the proof
                          ▼
        verify ──► 'verified'   ── push to cook, Socket.IO refresh
        reject ──► 'rejected'   ── push to cook with the reason, cook re-uploads
```

Nothing in this chain moves money programmatically. Every rupee is moved by a human and *recorded*
here.

---

## 2. Rate policy

| Item | Value |
|---|---|
| Rate | **5%** of `orders.total_amount`, global (not per-cook) |
| Where it lives | `platform_settings.commission_pct` (id = 1), default `5.00` |
| Code fallback | `DEFAULT_COMMISSION_PCT` in `backend/utils/commissionSnapshot.js` — the single source |
| Changed by | Admin ▸ Commission Settlements ▸ Update Rate; versioned in `commission_rate_history` |
| On change | Every active cook gets a push + an automated chat message (`notifyAllCooksOfRateChange`) |
| Retroactive? | **No.** The rate is snapshotted per order at delivery, so past orders keep their old rate |

---

## 3. The monthly cycle

| Step | When | Who |
|---|---|---|
| Snapshot commission on an order | The moment it becomes `delivered` | `utils/commissionSnapshot.js` |
| Generate settlements for the closed month | 01:00 on the 1st, **NPT** | `jobs/commissionSettlementJob.js` |
| Settle the OPEN month early | Any time the cook chooses | cook app → `settleAccruedNow` |
| Due date | 1st of the month after the period **+ 15 days** grace | set in SQL at generation |
| Reminders | 3 days before due, on the due date, then weekly once overdue | daily cron 09:00 |
| Cook pays + uploads proof | Any time | cook app |
| Admin verifies | Any time | admin panel |

All period boundaries are **Nepal Time (UTC+05:45)**, converted from UTC-stored `delivered_at` via
`CONVERT_TZ` (`utils/nepaliTime.js` → `toNpt()`). The DB stores UTC — verified: `NOW()` equals
`UTC_TIMESTAMP()` on the live TiDB instance.

Reminders are throttled by `commission_settlements.last_reminder_at`, so a restart or a duplicate
cron fire cannot re-notify the same cook twice in a day.

---

## 4. Status transitions

```
              (generated)
                  │
                  ▼
    ┌────────► pending ──────────────► verified   (paid in full)
    │             │  ▲                    ▲
    │  cook       │  │ part payment       │ admin records an off-platform
    │  uploads    │  │ banked, stays      │ payment (EC4, notes required)
    │  proof      ▼  │ pending (EC3)      │
    │          submitted ────────────────┘
    │             │
    └── rejected ◄┘   admin rejects with a reason; cook re-uploads
```

`pending` + past `due_date` renders as **overdue** — a display state only; the row stays `pending`.
Policy is warn-only: an overdue cook is never blocked or suspended.

---

## 5. Endpoints

All under `/api/commission`, all authenticated.

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/settings` | admin | Current rate |
| PUT | `/settings` | admin | Change rate + notify all cooks |
| GET | `/summary?month=&year=` | admin | Per-cook rollup, all-time total, 6-month trend |
| GET | `/rate-history` | admin | Every rate change, who and why |
| GET | `/admin-qr` | admin, cook | The platform's payment QRs |
| PUT | `/admin-qr` | admin | Set the QRs |
| POST | `/settlements/generate?month=&year=` | admin | Run generation on demand (idempotent) |
| GET | `/settlements?status=` | admin | All settlements, with `is_overdue` computed server-side |
| PUT | `/settlements/:id/verify` | admin | Verify / reject / record a partial payment |
| GET | `/settlements/current` | cook | This month's bill + what is accruing |
| GET | `/settlements/mine` | cook | Full settlement history |
| PUT | `/settlements/:id/screenshot` | cook | Attach a payment proof (hash-deduped, 409 on reuse) |
| POST | `/settlements/settle-now` | cook | Bill the open month's accrual now so it can be paid early |

---

## 6. Notifications

`notifications.type` is a plain `VARCHAR(50)`; `createNotification()` writes the row **and** fires
FCM via `config/firebaseAdmin.js`. Every commission push carries `{ type, settlementId }` so the
client deep-links to the exact settlement.

| Event | `type` | Who is told | Channel |
|---|---|---|---|
| Settlement generated | `commission_due` | cook | in-app + push |
| 3 days before / on / after due date | `commission_due` | cook | in-app + push |
| Proof submitted | `commission_submitted` | admin | in-app + push |
| Verified | `commission_verified` | cook | in-app + push + Socket.IO `commissionSettlementUpdated` |
| Rejected | `commission_rejected` | cook | in-app + push (carries the admin's reason) |
| Rate changed | `commission_rate_change` | all active cooks | in-app + push + chat message |

Android routing lives in `NotificationActivity` — all six types open
`CommissionSettlementActivity`, with `settlement_id` passed through as an extra.

---

## 7. Edge cases (EC1–EC10)

These are transcribed from the decision comments in `controllers/commissionController.js`,
`utils/commissionSnapshot.js`, and `database/migration_commission_cook_fk.sql`. They are the real
spec — read them before changing anything in this area.

**EC1 — Refunds.** Refunded orders are excluded entirely rather than credited to a future period: a
delivered-then-refunded order gave the money back, so there is nothing to tax. This affects **open**
periods only. Once a settlement exists its `amount_due` is frozen; a later refund does not reopen or
edit it. Not clawed back — intentional, mirroring EC2.

**EC2 — Late order into a closed period.** `INSERT IGNORE` means a period is generated once and never
recomputed. An order delivered late still gets its own snapshot and still counts in all-time and
trend totals; it is simply not re-added to a closed period a cook was already billed for. It rolls
into the next cycle — literally, now: `getCommissionByCook(..., { unbilledOnly: true })`'s **carry-in**
clause pulls any unbilled order whose own period already holds a settlement into the period being
generated. No commission is lost, only ever deferred by at most one cycle.

**EC3 — Partial payment.** `amount_paid` accumulates across installments. A short payment is banked
but the settlement stays `pending` — still listed, still chaseable, still flagged overdue — and only
flips to `verified` once the accumulated total covers `amount_due`. `amount_due` is never rewritten.
Omitting `amount_paid` means "paid in full". Comparisons are done in integer paisa.

**EC4 — Off-platform payment.** A cook who pays by direct bank transfer never reaches `submitted`.
The admin may verify straight from `pending`, but **only** with non-empty `admin_notes` — money that
moved outside the screenshot flow must leave a paper trail. From `submitted`, notes stay optional.

**EC5 — Timezone.** Every month/year boundary is NPT, via `CONVERT_TZ` on UTC-stored `delivered_at`.
The generation cron computes its period with `getNptPreviousMonthYear()`, not `new Date()`.

**EC6 — Cook deletion.** The `cook_id` FK is `ON DELETE SET NULL`, not `CASCADE` — a cook could
otherwise erase their own debt by deleting their account (which Google Play requires be possible).
The settlement survives with denormalised name/kitchen snapshot columns, since a NULL `cook_id` can
no longer JOIN to `users`.

**EC7 — Zero earnings.** `commission_total <= 0` generates no row and no notification. Silence, not
a ₹0 bill.

**EC8 — Screenshot reuse.** The SHA-256 of the image bytes is hashed **server-side** from the fetched
Cloudinary object (never trusted from the client) and stored in a UNIQUE column. Re-submitting the
same screenshot for a second month returns `409` with a readable message; the cook app shows a
"Screenshot already used → Choose another" dialog. NULLs are not deduped, so unsubmitted settlements
do not collide.

**EC9 — Rounding drift.** Commission is `ROUND(total_amount * pct / 100, 2)` **per order**; a month's
`amount_due` is the SUM of those already-rounded snapshots, not a fresh round of the monthly gross.
The two can differ by a few paisa. **Do not "fix" this** by recomputing from the gross — the
per-order snapshot is what makes a rate change non-retroactive.

**EC11 — Early payment of an open month.** A cook may bill themselves mid-month
(`POST /settlements/settle-now`). The row is a normal settlement — same due date arithmetic, same
pay/upload/verify/notify path — so nothing downstream knows the difference. Two invariants make it
safe, both resting on `orders.commission_settlement_id`:

- **Never billed twice.** Every order a settlement covers is stamped with its id, and the billing
  scope only ever sums unstamped orders. The month-close cron therefore finds nothing left of an
  early-paid month.
- **Never lost.** Orders delivered *after* the early payment stay unstamped. Their own period is
  frozen (one settlement per `uniq_cook_period`), so EC2's carry-in bills them in the next cycle.

One bill per period, always: a second tap returns the existing `pending`/`rejected` bill (200) and a
`submitted`/`verified` month returns 409. `accruing` in `/settlements/current` excludes stamped
orders, which is what makes the cook's banner and accrual card drop to zero the moment they settle.

**EC10 — Inactive/suspended cook.** Settlements generate regardless of `users.is_active`. A suspended
cook who delivered orders still owes that commission; suspension must not erase debt.

---

## 8. Ops runbook

```bash
# 1. Schema (idempotent — safe to re-run, second run is all SKIPs)
node backend/scripts/run_commission_hardening_migration.js

# 1b. Pay-early support: orders.commission_settlement_id + a backfill that stamps
#     orders existing settlements already cover. MUST run before the next cycle —
#     an unstamped historical order looks unbilled to the carry-in rule (EC11).
node backend/scripts/run_commission_pay_early_migration.js

# 2. Backfill orders delivered before commission shipped, and subscription
#    days whose order was never promoted to 'delivered'. Dry run first.
node backend/scripts/backfill_commission.mjs           # prints a per-cook table, writes nothing
node backend/scripts/backfill_commission.mjs --apply   # only after reviewing that table

# 3. Force a billing cycle without waiting for the 1st (admin token required)
curl -X POST "$API/api/commission/settlements/generate?month=7&year=2026" \
     -H "Authorization: Bearer $ADMIN_TOKEN"
```

The backfill **bills cooks for real money they were never charged**. Review the dry-run table with
whoever owns the business decision before `--apply`.

Crons only fire while the process is alive. On a sleeping free-tier host the 1st-of-month run can be
missed entirely — the manual generate endpoint is the recovery path, and it is idempotent.

---

## 9. Known limitations

- **Manual settlement.** No payment-gateway integration; every verification is a human reading a
  screenshot — including an early payment, which opens the eSewa app but is still settled by proof.
  Deliberate.
- **No enforcement.** An overdue cook is warned, never blocked or suspended. Policy decision.
- **Closed periods are frozen.** Late orders and late refunds land in the next cycle (EC1, EC2) — and
  an early-paid month is closed from the moment it is paid (EC11).
- **Cook-side partial payments are not enterable.** Only an admin can record a part payment; the cook
  app has no amount field, so a cook who underpays cannot self-declare it.
- **A missed cron is silent.** Nothing alerts if the 1st-of-month generation never ran; it is noticed
  when a cook asks why there is no bill.

---

## Appendix — Verification notes

**Confirmed** (re-derived by running the thing, not by reading it):

- Pay-early, on the live TiDB instance (29 Aug 2026): migration ran clean (column + index added,
  backfill stamped 0 — no settlements existed yet). `GET /settlements/current` returned
  `accruing 45.00, payable_now true`; `POST /settlements/settle-now` created settlement #1 for
  ₹45.00, `due_date 2026-09-16`, status `pending`; a second POST returned the same row rather than a
  duplicate; `accruing` then read `0.00`, which is what hides the banner. The order was stamped
  `commission_settlement_id = 1`.
- The carry-in scope, read-only against the real row: generating **Sept 2026** matches the 28-Aug
  order via `carry_in` (so a post-early-payment delivery is billed next cycle, not lost); generating
  **July 2026** matches neither branch (an earlier period is never swept). Combined with the stamp +
  `IS NULL` filter, that is the no-double-bill / no-loss pair in EC11.
- `cd frontend && ./gradlew assembleDebug` passes with the pay-early UI wired.

- Migration `run_commission_hardening_migration.js` ran clean, and a second run was all `SKIP` +
  `platform_settings default 5.00`. `orders.delivered_at`, `payment_screenshot_hash`,
  `uniq_commission_screenshot_hash`, `last_reminder_at` all present; `commission_pct = 5.00`.
- The DB stores UTC: `SELECT NOW(), UTC_TIMESTAMP()` returned the same instant on the live TiDB
  instance, so every `CONVERT_TZ('+00:00','+05:45')` in this system is correct.
- Backend boots clean and logs all four crons, including
  `Commission crons scheduled (settlements 01:00 on the 1st, reminders daily 09:00)`.
- A delivered order snapshots `ROUND(total * 5 / 100, 2)` with `commission_pct` and `delivered_at`
  set, and re-marking it changes neither the amount nor the timestamp.
- A subscription day walked `scheduled → sent → delivered` promotes its linked order to `delivered`
  **and** charges commission (the hole this work closed); `delivered` stays terminal and a second
  attempt is blocked rather than double-charged.
- The earnings screen and the commission bill agree, including on an order delivered at
  `2026-07-31 18:30 UTC` (= 1 Aug 00:15 NPT) that raw-UTC bucketing would have billed to July; and a
  refunded order drops out of **both** views.
- `cd frontend && ./gradlew assembleDebug` passes; `cd Admin && npm run build` passes.
- Commission routes are mounted and auth-protected (401, not 404, without a token).

**Likely** (strong inference from code read end-to-end, not executed):

- Duplicate-screenshot submission returns 409 — the UNIQUE key exists and the `ER_DUP_ENTRY` branch
  is in `uploadSettlementScreenshot`, but no two real screenshots were submitted.
- The FCM/Socket.IO round trip (cook submits → admin push → admin verifies → cook push + live
  refresh). All senders and both client routes are wired; not walked on two devices.
- Reminder cadence (3 days before / on / after due date, weekly once overdue). The query and the
  `last_reminder_at` throttle read correctly; no settlement was aged to trigger one.
- The month-close cron skipping an early-paid month. `INSERT IGNORE` + `uniq_cook_period` has always
  behaved this way and the billing scope now excludes stamped orders, but a real generation run was
  not executed — it would create rows and fire notifications for every cook on the live database.

**Assumption:**

- 15-day grace on the due date and 5% as the business rate — taken as given from the product
  decision, not derived from anything in the code.

**Not addressed:**

- Gateway integration (eSewa/Khalti), blocking or suspending non-paying cooks, and the EC9 paisa
  drift — all deliberately out of scope.
- Backfill `--apply` on production data: the dry run reports zero rows on the current database, so
  the write path is untested against real history.
- EC6 (deleted cook) and EC7 (zero-earnings cook) were read, not exercised — asserting them means
  deleting a real user and running a real generation cycle, both of which send real notifications.
