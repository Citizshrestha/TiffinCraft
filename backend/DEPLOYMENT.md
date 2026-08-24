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
| **`/api/config` returns a LAN address** | `server.js` serves a `lan.baseUrl` built from `getLanIp()`. On Render that is a meaningless internal IP. The Android app self-heals its `BASE_URL` from this endpoint, so **verify the app doesn't adopt the LAN value in production** before relying on it. |
| **CORS is allow-all** | `server.js` uses `origin: true` and logs "Allowing all origins for development". `allowedOrigins` is computed from `CLIENT_URL` on the line above and then never used — dead code. Tighten before real launch. |

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

- `SMTP_PASS` — **not** `SMTP_PASSWORD` (`utils/emailService.js:13`)
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

---

## 6. What a successful deploy looks like

Render's log should show, in order:

```
MySQL Connected Successfully (TiDB, SSL)
✅ Database connected
✅ Firebase Admin initialized (FIREBASE_SERVICE_ACCOUNT_JSON)
✅ Subscription cron job scheduled (daily at 06:00)
✅ eSewa booking cleanup cron job scheduled (every 5 minutes)
✅ Commission settlement cron job scheduled (01:00 on the 1st of each month)

✅ Server running on port 10000
🌍 Public base:      https://<your-service>.onrender.com
🔍 Health check:     https://<your-service>.onrender.com/api/health
🔌 Socket.IO:        ready
✅ SMTP ready — smtp-relay.brevo.com
```

Failure signatures:

| Log line | Cause |
|---|---|
| `❌ Missing required environment variable(s): …` | Preflight exit. Add the named vars |
| `⚠️  NODE_ENV=production but N variable(s) are unset` | Server runs, but the listed features are broken |
| `Access denied … #user-name-prefix` | `DB_USER` lacks the TiDB cluster prefix, or wrong password |
| `⚠️  CA file not readable … falling back` | Cert not committed. Non-fatal |
| `⚠️  Firebase not configured` | No FCM. API still works |

---

## 7. Live URL

**Not yet deployed** — fill in after the first successful deploy:

```
BASE URL:  https://__________________.onrender.com
HEALTH:    https://__________________.onrender.com/api/health
```

Then set `PUBLIC_BASE_URL` to that base URL and redeploy, since eSewa
redirect/callback URLs are built from it.

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
> eSewa cleanup, monthly commission) only fire while the process is alive. A
> restart across 06:00 means that day's subscription orders are not generated.
> Moving those to an external scheduler hitting a protected endpoint is the
> durable fix.
