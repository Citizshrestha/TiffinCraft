# TiffinCraft Backend — Deployment

Target: **Render** (free tier) → **TiDB Cloud Serverless** (`ap-southeast-1`).

---

## 1. What was changed in the codebase, and why

Five changes, all configuration/robustness. **No business logic, API contract,
or database schema was touched.**

### 1.1 `config/db.js` — missing CA cert no longer crashes boot *(deploy blocker)*

Was:
```js
poolConfig.ssl = { ca: fs.readFileSync(path.join(__dirname, "../certs/isrgrootx1.pem")), minVersion: "TLSv1.2" };
```
`readFileSync` ran unconditionally whenever `DB_USE_SSL=true`. `certs/` was
**never committed to git** (`git status` showed it untracked), so on a fresh
deploy the file is absent, `readFileSync` throws at module load, and the process
dies before Express starts.

Now the file is read inside a `try/catch` and falls back to Node's bundled root
CA store. TiDB Cloud's gateway certificate chains to **ISRG Root X1**, a public
Let's Encrypt root that Node already trusts, so TLS is still fully enforced —
the explicit file is belt-and-braces, not a requirement.

**Verified:** with `certs/` renamed away, the server booted, logged the warning,
and `/api/health/db` returned `{"status":"ok","database":"connected"}` over TLS.

### 1.2 `config/firebaseAdmin.js` — added `FIREBASE_SERVICE_ACCOUNT_JSON`

Previously the credential order was: service-account **file path**, then three
separate env vars. A hosted platform gives you env vars, not a filesystem to
drop secrets onto, so `FIREBASE_SERVICE_ACCOUNT_JSON` (the whole JSON in one
variable) was added as the **first-choice** source. The two existing paths still
work unchanged, so local development is unaffected.

Also repairs `\n` → real newlines in `private_key`, since pasting a JSON blob
into a dashboard textarea commonly mangles them. Parse failures log without
echoing the value (it contains a private key) and fall through to the next
source. Firebase being unconfigured disables push notifications; it never takes
the API down.

**Verified:** local boot still logs `✅ Firebase Admin initialized (service-account file)`.

### 1.3 `server.js` — `/api/health` no longer touches the database

Was: acquired a real pooled DB connection on every request. An uptime monitor
pinging every 5 minutes is ~8,600 TiDB round-trips a month, and TiDB Cloud
Serverless bills by **Request Unit** — quota spent to answer "is the process
up?", which needs no database.

`/api/health` is now a static 200 (`status`, `uptime_seconds`, `timestamp`).
The DB check moved to a new **`/api/health/db`**. Verified nothing consumed the
old response shape (no Android, no Admin dashboard reference), so this breaks no
client.

### 1.4 `server.js` — production environment preflight (new)

`NODE_ENV=production` is a **switch, not a label** in this codebase: it disables
every sandbox fallback in `utils/esewaClient.js` and `utils/esewaEpayClient.js`.
Setting it without the four `ESEWA_EPAY_*` variables leaves the server starting
happily and then signing payment forms with `null` — silent checkout failure.

The preflight now `exit(1)`s on genuinely fatal gaps (`DB_HOST`, `DB_USER`,
`DB_NAME`, `JWT_SECRET`) and prints a loud itemised warning for
production-only gaps, each naming the concrete consequence. Follows the
"fail loud at boot" convention already stated in `esewaClient.js`.

The startup banner also prints the real public URL in production instead of
`localhost` / `10.0.2.2` emulator addresses.

### 1.5 `controllers/adminController.js` — no `localhost` in production email

The cook-approval email's "Get Started" button fell back to
`http://localhost:3000`. Now resolves `CLIENT_URL` (first entry of the
comma-separated list) → `PUBLIC_BASE_URL` → localhost as a dev-only last resort.

### 1.6 `package.json` — added `engines`

`"node": ">=20.0.0"`. Local development runs Node v25.7.0, which is a
**non-LTS** release; pinning that exactly would be worse than a floor, since
Render should be free to use a current LTS. `start` (`node server.js`) and
`"type": "module"` were already correct.

---

## 2. Known issues deliberately NOT changed

Both would be API-behaviour changes, which were out of scope for this task.

| Issue | Detail |
|---|---|
| **`/api/config` returns a LAN address** | `server.js` serves a `lan.baseUrl` built from `getLanIp()`. On Render that is a meaningless internal IP (confirmed live: `10.27.232.60`). The endpoint is left as-is; the Android app was fixed instead — `ServerConfig.discoverAndCacheSync()` now refuses any address whose host differs from the one it queried, so it cannot adopt this value. See §9.4. |

**CORS was previously listed here as unchanged — it has since been fixed.**
See §9.2.

---

## 3. Render configuration

| Setting | Value |
|---|---|
| Repository | this repo, branch `main` |
| **Root Directory** | `backend` |
| Runtime | Node |
| Build Command | `npm install` |
| Start Command | `npm start` |
| Region | Singapore (closest to Nepal) |
| Instance Type | Free |
| Health Check Path | `/api/health` |

`PORT` must **not** be set manually — Render injects it, and
`server.js` already reads `process.env.PORT` and binds `0.0.0.0`.

---

## 4. Environment variables

Full annotated checklist with where to obtain each value:
**[`.env.render-reference.md`](./.env.render-reference.md)** — names only, no secrets.

Name traps that silently half-break the app:

- `SMTP_PASS` — **not** `SMTP_PASSWORD` (`utils/emailService.js`)
- `BREVO_API_KEY` — **required on Render**, and a different value from `SMTP_PASS`.
  Free Render web services block outbound SMTP ports 25/465/587
  ([changelog](https://render.com/changelog/free-web-services-will-no-longer-allow-outbound-traffic-to-smtp-ports)),
  so without it every email — reset codes, registration OTP, cook approvals —
  fails on connectionTimeout while the endpoint still returns 500 to the app.
- `ESEWA_EPAY_*` and `ESEWA_*` are **two different eSewa integrations**.
  Checkout runs on ePay v2, so the `ESEWA_EPAY_*` set is the one that matters.

Fatal if missing (server exits): `DB_HOST`, `DB_USER`, `DB_NAME`, `JWT_SECRET`.

---

## 5. Prerequisite before the first deploy

**`backend/certs/isrgrootx1.pem` is untracked and must be committed**, or the
deploy will run on Node's bundled CA store instead (which works, but you lose
the explicit pin):

```bash
git add backend/certs/isrgrootx1.pem
```

**Run the commission hardening migration once against the deploy database.** It adds
`orders.delivered_at` (every billing period groups on it), the settlement
screenshot-hash unique key, and `last_reminder_at`; without it commission is
never charged. It is idempotent — a second run prints only `SKIP`:

```bash
cd backend && node scripts/run_commission_hardening_migration.js
```

---

## 6. What a successful deploy looks like

Render's log should show, in order:

```
MySQL Connected Successfully (TiDB, SSL)
✅ Database connected
✅ Firebase Admin initialized (FIREBASE_SERVICE_ACCOUNT_JSON)
✅ Subscription cron job scheduled (daily at 06:00)
✅ eSewa booking cleanup cron job scheduled (every 5 minutes)
✅ Commission crons scheduled (settlements 01:00 on the 1st, reminders daily 09:00)

✅ Server running on port 10000
🌍 Public base:      https://<your-service>.onrender.com
🔍 Health check:     https://<your-service>.onrender.com/api/health
🔌 Socket.IO:        ready
✅ Mail transport — Brevo HTTPS API (SMTP is fallback only)
```

`✅ SMTP ready — smtp-relay.brevo.com` instead means `BREVO_API_KEY` is unset and
mail is going out over port 587 — correct locally, broken on Render.


Failure signatures:

| Log line | Cause |
|---|---|
| `❌ Missing required environment variable(s): …` | Preflight exit. Add the named vars |
| `⚠️  NODE_ENV=production but N variable(s) are unset` | Server runs, but the listed features are broken |
| `Access denied … #user-name-prefix` | `DB_USER` lacks the TiDB cluster prefix, or wrong password |
| `⚠️  CA file not readable … falling back` | Cert not committed. Non-fatal |
| `⚠️  Firebase not configured` | No FCM. API still works |
| `⚠️  SMTP not ready: … ETIMEDOUT` / `Greeting never received` | Host blocks outbound SMTP. Set `BREVO_API_KEY` |
| `⚠️  Brevo API send failed … 401 unauthorized` | `BREVO_API_KEY` wrong or revoked (it is not the SMTP key) |
| `⚠️  Brevo API send failed … sender not valid` | `SMTP_FROM_EMAIL` is not a verified sender in Brevo |

---

## 7. Live URL

Deployed **2026-08-24** (commit `a757584`), service `srv-da606nbncjis73aab9qg`.

```
BASE URL:  https://tiffincraft-xsrh.onrender.com
HEALTH:    https://tiffincraft-xsrh.onrender.com/api/health
DB HEALTH: https://tiffincraft-xsrh.onrender.com/api/health/db
```

**Still to do:** set `PUBLIC_BASE_URL=https://tiffincraft-xsrh.onrender.com`
and redeploy — eSewa redirect/callback URLs are built from it, and without it
`utils/publicUrl.js` falls back to Render's unroutable internal IP.

---

## 8. Keep-alive (required on the free tier)

Render's free tier sleeps a service after **15 minutes** of inactivity; the next
request then pays a cold start of ~30–50s, which reads as a broken app.

Set up an external monitor to ping the health route every **5 minutes** —
comfortably inside the 15-minute window:

1. Sign up free at **<https://uptimerobot.com>**
2. **Add New Monitor**
   - Monitor Type: `HTTP(s)`
   - Friendly Name: `TiffinCraft Backend Keep-Alive`
   - URL: `https://<your-service>.onrender.com/api/health`
   - Monitoring Interval: `5 minutes`
3. Save.

Point it at `/api/health` (static, no DB) and **not** `/api/health/db` — the
latter costs a TiDB Request Unit per hit.

`cron-job.org` is an equivalent free alternative.

> Free-tier caveat this does not solve: Render free instances still restart
> periodically, and the `node-cron` jobs (06:00 subscription generation, 5-min
> eSewa cleanup, monthly commission settlements at 01:00 on the 1st, 09:00 daily
> commission due-date reminders) only fire while the process is alive. A restart
> across 06:00 means that day's subscription orders are not generated; a restart
> across 01:00 on the 1st means no cook is billed that month — recover with
> `POST /api/commission/settlements/generate?month=&year=`, which is idempotent.
> Moving those to an external scheduler hitting a protected endpoint is the
> durable fix.

---

## 9. Post-deploy verification — run 2026-08-24

All checks run against the live service. Result: **the deploy is functionally
sound.** Four issues found, none caused by the deployment changes.

### Passed

| Check | Result |
|---|---|
| `/api/health` | 200, `0.14s` / `0.16s` warm. First hit was `4.09s` — cold start, matches Render's free-tier spin-down banner |
| `/api/health/db` | `{"status":"ok","database":"connected"}` in `0.19s` |
| **Real DB read** | `GET /api/meals` → 200, 5 rows with full column sets. TiDB is genuinely serving production traffic, not just reachable |
| TLS to TiDB | `MySQL Connected Successfully (TiDB, SSL)` with **no** CA-file warning — the committed cert is being read |
| `NODE_ENV` | Banner printed the localhost/emulator lines, confirming it is *not* `production`. Correct for now (see §9.1) |
| Auth rejection | no token → 401 `No token provided`; junk token → 401 `Invalid or expired token`; cook-only route unauthenticated → 401 |
| Error-path leaks | 404 → `Route not found.`; malformed JSON → 400 `Unexpected end of JSON input`; bad param → 404 `Meal not found.` No stack traces, no absolute paths, no host/credential strings |

### 9.1 `NODE_ENV` is not set to `production` — deliberate, but temporary

Verified from the startup banner. This is currently **correct**: setting it
without live `ESEWA_EPAY_*` credentials would null every sandbox default and
sign payment forms with `null` (§1.4). It also means Express runs in dev mode,
which is slower and more verbose. Flip it the same day the live eSewa keys land.

### 9.2 CORS accepts any origin — pre-existing, now internet-facing

```
Origin: https://evil.example.com
→ access-control-allow-origin: https://evil.example.com
  access-control-allow-credentials: true
```

`origin: true` reflected whatever origin was sent, and paired with
`credentials: true` that let any website script authenticated requests against
a logged-in user's browser session. Harmless on a LAN, materially different on
a public service. Socket.IO had a separate and also broken config
(`origin: process.env.CLIENT_URL || "*"`, which compares against the literal
string `"a,b"` when `CLIENT_URL` is a list, so it matched nothing).

**Fixed.** Both now share one `corsOrigin()` allowlist function built from
`CLIENT_URL`, defined above the Socket.IO server so the two cannot drift again:

- Allowlisted origins get `Access-Control-Allow-Origin`; everything else gets
  no header, so the browser blocks it.
- Rejection uses `callback(null, false)`, **not** `callback(new Error(...))` —
  the latter returns a 500 and makes a `CLIENT_URL` typo look like a crash.
- Requests with **no** `Origin` header are allowed: that is the Android app,
  curl, and eSewa's return trip. CORS is a browser protection and there is no
  browser to protect. Verified still `HTTP 200`.
- Loopback and `192.168.x` origins are allowed **only** when
  `NODE_ENV !== "production"`, for Admin dashboard development.
- Unknown origins are logged once each, not once per request.

Verified locally in both modes: allowlisted origins pass (including the second
entry of a comma-separated list, and with a trailing slash), `evil.example.com`
is blocked on both `GET` and preflight `OPTIONS`, `localhost` is blocked under
`NODE_ENV=production`, and no-Origin requests return 200.

> **This takes effect only once `CLIENT_URL` is correct on Render.** If it is
> unset or wrong, browser clients are blocked — look for
> `⚠️  CORS rejected origin: …` in the logs, which names the origin to add.

### 9.3 One meal's image 404s — pre-existing data, not a deploy fault

Of 5 meals, 4 have Cloudinary URLs and 1 has `/uploads/meals/eb24d7d3-….jpg`,
which returns **404** live. `uploads/` is gitignored (`.gitignore:14`), so that
file was never deployed, and Render's filesystem is ephemeral regardless.

**New uploads are fine:** every upload route (`authRoutes`, `cookRoutes`,
`customerDashboardRoutes`, `mealRoutes`, `uploadRoutes`) uses
`middleware/uploadMiddleware.js` → `multer.memoryStorage()` → Cloudinary.
`config/multer.js` still defines a `diskStorage` engine, but **no route
imports it** — dead code. So this is one stale legacy row, not an ongoing leak.
Re-upload that meal's image through the app to fix it.

### 9.4 `/api/config` returns Render's internal IP

```json
{"tunnel":null,"lan":{"baseUrl":"http://10.27.232.60:10000/api/"}}
```

`10.27.232.60` is inside Render's private network — unreachable from any phone.
Flagged in §2 as the thing to verify; now confirmed live.

**Fixed in the Android app**, not the backend (the endpoint still has legitimate
local-dev uses). `ServerConfig.discoverAndCacheSync()` now:

- returns immediately unless LAN discovery is explicitly enabled — it is **off
  by default**, since a stable hosted address leaves nothing to discover;
- refuses any discovered address whose **host differs from the host it just
  queried**. That is the precise guard: it catches a server reporting its own
  private address, without banning private ranges outright, so a developer
  whose own LAN happens to be `10.x` still works.

The failure mode this prevents is unrecoverable, which is why it is a hard
refusal rather than a warning: the app would cache a dead host, and every later
request — including the discovery call meant to repair it — would fail against
that same dead host.

Also in the same change: `PREFS_NAME` bumped to `TiffinCraftServerConfigV2`.
The cached `active_base_url` **wins over** the compiled-in default, so without a
new prefs file every already-installed device would have kept dialling the old
`192.168.100.115:5000` no matter what the new default said.

### 9.5 Open risk: cold start vs. OkHttp read timeout

Render's own banner says a spun-down instance can delay a request by "50 seconds
or more". `RetrofitClient` uses `readTimeout(40, SECONDS)`, so a full cold start
could time out before the instance finishes waking. The UptimeRobot keep-alive
(§8) is what makes this rare rather than routine, and `FailoverInterceptor`
retries `502/503/504` once. Not changed, because raising the timeout makes every
genuine failure hang 60s for a case the keep-alive already prevents. Revisit if
cold-start timeouts show up in practice.

