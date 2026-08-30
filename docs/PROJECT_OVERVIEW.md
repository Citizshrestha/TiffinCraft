# TiffinCraft — Project Overview

**Tagline:** Homemade meals, crafted with love.
**One-liner:** A subscription-first, hyperlocal Android platform that connects verified home cooks with students and working professionals who need affordable, hygienic daily tiffin.

---

## 1. Short Description

TiffinCraft is a full-stack food-subscription platform built around the *tiffin* (daily home-cooked meal) model rather than the restaurant model. Home cooks — largely housewives with cooking skill but no digital reach — publish a daily menu and weekly/monthly meal plans. Customers discover cooks near them on a map, subscribe for a week or month (or order a one-off meal), pay via eSewa or manual QR, and track each day's delivery.

The system has three clients over one API:

| Surface | Users | Stack |
|---|---|---|
| Android app | Customers + Cooks (role-based UI) | Native Android, Java |
| Admin dashboard | Platform operators | React + Vite + TypeScript + Tailwind |
| REST + WebSocket API | all of the above | Node.js + Express + MySQL + Socket.IO |

---

## 2. Problem Statement

**Customer side**
- Students and working professionals living away from home have no reliable source of affordable, hygienic, home-style food on a *daily* basis.
- Mainstream delivery apps (Swiggy, Zomato, Foodmandu) are restaurant-centric, per-order, and priced for occasional indulgence — not for eating twice a day, every day.
- No subscription-based homemade tiffin platform exists in the Nepali market.
- Offline tiffin services are opaque: no menu visibility, no ratings, no digital payment trail, no way to skip a day.

**Home-cook side**
- Skilled home cooks are invisible beyond their own neighbourhood and depend entirely on word of mouth, typically serving only 5–8 customers.
- Traditional tiffin agencies and aggregators take **35–80%** cuts, leaving the person who actually cooked with very little.
- No digital tooling: no order log, no income tracking, no customer communication channel.
- The target cook is often non-technical, so any solution must work with a very low-friction interface.

**The gap TiffinCraft closes:** a trust-layer + subscription-billing + logistics-coordination system purpose-built for recurring homemade meals, with a transparent, low platform commission instead of an agency cut.

---

## 3. Literature Survey

*Comparable systems and the gap each leaves open. Assessed from publicly observable product behaviour, not from source.*

| System / Body of work | Model | Limitation for the tiffin use case |
|---|---|---|
| **Swiggy / Zomato / Foodmandu / Pathao Food** | On-demand, per-order restaurant aggregation | Optimised for discovery + speed, not recurrence. No meal-plan billing, no skip-a-day, high commissions (~18–30%), restaurant-licence requirements exclude home kitchens. |
| **Home-chef marketplaces (Cookr, Homely, HomeMade-style apps)** | Home cooks, but still per-order | Solve supply-side inclusion but keep the transactional per-order model; the customer's daily-meal problem stays unsolved. |
| **Meal-kit / meal-plan subscriptions (HelloFresh, Freshmenu Plus)** | Recurring, prepaid | Centralised industrial kitchens; no hyperlocal cook, no cultural/home-style customisation, unavailable in the target market. |
| **Offline tiffin agencies / dabbawala networks** | Recurring, hyperlocal — the closest functional match | Entirely manual. No digital discovery, no ratings/trust signal, cash-only, no attendance log, extractive middleman margins. |
| **Academic work on hyperlocal marketplaces & two-sided platform trust** | — | Consistently identifies *trust signalling* (verification, reviews) and *transparent settlement* as the deciding factors in supply-side retention on informal-economy platforms. TiffinCraft implements both as first-class features. |
| **Subscription-commerce / churn literature** | — | Recurring prepaid revenue plus a low-friction pause/skip mechanism is the standard defence against churn in perishable-goods subscriptions — hence TiffinCraft's daily skip log rather than hard cancellation. |

**Conclusion of the survey:** existing platforms optimise either for *on-demand restaurant food* or for *centralised subscriptions*. No reviewed system combines hyperlocal home cooks, subscription billing, per-day attendance control, and a cook-favourable commission in one product — which is the space TiffinCraft occupies.

---

## 4. Primary Objectives of the Proposed Solution

1. **Enable map-based discovery of skilled home cooks** — put every verified cook on an interactive map so a customer can see, at a glance, exactly who is cooking within walking distance of them. Discovery is anchored to the device's live location and each pin opens the cook's kitchen profile, today's menu, star rating, review history, and verification badge — turning cooks who were previously reachable only by word of mouth into a browsable, comparable, trustworthy local supply.
2. **Enable subscription-first ordering** — weekly and monthly meal plans as the default purchase unit, with per-day skip/unavailability handling, instead of forcing repeated single orders.
3. **Maximise cook earnings** — a single, low, transparent, admin-configurable platform commission with an auditable settlement history, replacing 35–80% agency cuts.
4. **Keep the cook-side interface usable by non-technical users** — task-oriented screens (add today's meal, today's orders, earnings) rather than a generic dashboard.
5. **Build trust in an informal-economy market** — ratings, reviews, verification, in-app chat, order status tracking, and a refund/dispute path.
6. **Support the local payment reality** — eSewa online payment plus manual QR-with-proof verification plus cash on delivery, since card penetration is low.
7. **Give the platform operator real control** — an admin dashboard for user CRUD, commission rate management, subscription oversight, and payment verification.
8. **Communicate in real time** — live order status, chat, and push notifications so neither side has to phone the other.

---

## 5. System Architecture

```
┌──────────────────────────┐        ┌──────────────────────────┐
│   Android App (Java)     │        │  Admin Dashboard (React) │
│  Customer UI │ Cook UI   │        │   Vite + TS + Tailwind   │
└──────────┬───────────────┘        └────────────┬─────────────┘
           │ Retrofit/OkHttp (HTTPS)             │ fetch client
           │ Socket.IO client                    │
           └──────────────┬──────────────────────┘
                          │  JWT: Authorization: Bearer <token>
                 ┌────────▼─────────────────────────────────┐
                 │      Node.js + Express 5 API server      │
                 │  server.js → routes → controllers        │
                 │  middleware: auth (JWT), RBAC,           │
                 │  express-validator, rate-limit           │
                 │  services / utils: commission, notify,   │
                 │  image URL, subscription cron            │
                 │  Socket.IO: order rooms, cook rooms,     │
                 │             chat rooms, typing           │
                 │  node-cron: daily subscription rollover  │
                 └───┬─────────┬─────────┬─────────┬────────┘
                     │         │         │         │
              ┌──────▼──┐ ┌────▼────┐ ┌──▼─────┐ ┌▼──────────┐
              │ MySQL   │ │Cloudinary│ │Firebase│ │  eSewa    │
              │(mysql2  │ │  images  │ │  FCM   │ │  ePay     │
              │ pool)   │ │          │ │  push  │ │  gateway  │
              └─────────┘ └──────────┘ └────────┘ └───────────┘
                                  + Google Maps / Play Location
                                  + Google OAuth (passport)
                                  + Nodemailer (email)
```

**Layering (backend):** `routes/` → `middleware/` (auth, role guard, validation) → `controllers/` (26 controllers) → `utils/` + `services/` → `config/db.js` (pooled, parameterised `mysql2` queries). No ORM; SQL is explicit and migration-driven under `backend/database/`.

**Android:** MVC-ish — `activities/` (customer / cook / common) → `api/ApiService.java` (Retrofit interface) + `RetrofitClient.java` → `models/` (Gson DTOs) → `session/` (token + user store) → `adapters/` for RecyclerViews. Base URL is resolved at runtime via `/api/config`, so the same APK works against localhost, LAN, tunnel, or production.

**Data model (28 tables), grouped:**
- *Identity:* `users`, `cook_profiles`, `admin_records`
- *Catalogue:* `meals`, `cook_daily_availability`, `combo_deals`, `combo_deal_items`, `custom_meal_requests`
- *Transactional:* `cart`, `cart_items`, `orders`, `order_items`, `payments`, `refund_requests`
- *Subscription core:* `subscription_plans`, `subscription_plan_items`, `subscriptions`, `subscription_daily_log`, `subscription_payment_events`
- *Money / platform:* `commission_rate_history`, `commission_settlements`, `platform_settings`, `referrals`
- *Engagement:* `reviews`, `favorites`, `notifications`, `conversations`, `chat_messages`

---

## 6. Main Features

**Customer**
- Map-based nearby home-cook discovery (Google Maps + one-shot location)
- Cook profile, full menu, photos, ratings and reviews
- Weekly / monthly subscription plans; request → cook approval → payment → active
- Per-day skip; cook-unavailable days logged and reconciled
- Multi-cook cart — checkout splits into one order per cook automatically
- One-off orders, combo deals, custom meal requests
- Order tracking with live status updates
- Payment: eSewa ePay, manual QR with uploaded proof, cash on delivery
- Favourites, referral rewards, in-app chat with the cook, push notifications
- Refund request flow

**Cook**
- Kitchen profile setup and verification badge
- Meal CRUD with photo upload; daily availability toggle; "Add Today's Meal" quick action
- Subscription plan builder (plan items, weekly/monthly duration)
- Incoming subscription requests: approve / reject; payment-proof verification
- Order queue with status transitions
- Earnings dashboard with visual trends (MPAndroidChart), transaction history, pending commission
- Chat with customers; FCM push on every new order

**Admin**
- Real JWT auth; session persistence
- Manage Users: tabbed CRUD (All / Customers / Cooks / Admins), search, pagination, deactivate, FK-safe delete
- Commission rate management — changing the rate notifies **every active cook** via in-app notification, an automated chat message, and a Socket.IO event, and appends to `commission_rate_history`
- Subscription and payment oversight; settlement records

**Platform**
- JWT auth + role-based access control; bcrypt (12 rounds); parameterised SQL; express-validator; rate limiting
- Socket.IO rooms for orders, cooks, and chat, with typing indicators
- `node-cron` daily subscription rollover / logging
- Cloudinary image hosting with local `/uploads` fallback resolved by `ImageUrlHelper`
- Google OAuth sign-in; Nodemailer transactional email

---

## 7. Achievements

- **End-to-end system delivered across three codebases** — Android app, React admin panel, and API — all sharing one authentication and authorisation model.
- **26 controllers / 21 route modules / 28 tables** implementing the full lifecycle: register → discover → subscribe → approve → pay → verify → deliver daily → review → settle commission → refund.
- **Full subscription lifecycle**, not just recurring billing: request/approval handshake, payment verification states, per-day attendance log with actor attribution (`customer` / `cook` / `system`) for dispute resolution, pause/resume/cancel/complete states.
- **Real payment integration** — eSewa ePay working, plus a manual-QR-with-proof path so cooks without a merchant account can still transact.
- **Live commission engine** — admin-configurable rate, historical audit trail, per-cook pending-commission calculation, and multi-channel automatic notification of rate changes.
- **Real-time layer working** — Socket.IO order/cook/chat rooms plus FCM push notifications on both platforms.
- **Multi-cook checkout** — a single cart containing meals from several cooks splits into per-cook orders with independent notification, which most single-vendor cart implementations do not handle.
- **Runtime-resolved API base URL** — one APK works across emulator, LAN, tunnel, and production without a rebuild.
- **Security-by-default posture** — bcrypt at 12 rounds, RBAC middleware on every protected route, parameterised queries throughout, secrets and the Maps API key kept in gitignored config with build-safe empty defaults.

---

## 8. Tech Stack

**Backend** — Node.js ≥ 20 (ESM), Express 5.2, MySQL via `mysql2` 3.22 (pooled), Socket.IO 4.8, `jsonwebtoken` 9, `bcryptjs` 3, `express-validator` 7, `express-rate-limit` 8, `multer` 2, `cloudinary` 2, `firebase-admin` 14 (FCM), `node-cron` 4, `nodemailer` 9, `passport` + `passport-google-oauth20` + `passport-facebook`, `cors`, `dotenv`, `uuid`, `localtunnel`; `nodemon` for dev.

**Android** — Native Java, `compileSdk`/`targetSdk` 36, `minSdk` 26, Java 11, ViewBinding. Retrofit 2.9 + Gson converter, OkHttp 4.12 + logging interceptor, `socket.io-client` 2.1.1, Glide 4.16, Material Components, ConstraintLayout, RecyclerView 1.4, SwipeRefreshLayout, CircleImageView, ExifInterface, Play Services Auth / Maps 19.2 / Location 21.3, Firebase BoM 33.5 + Messaging, MPAndroidChart 3.1, Facebook Shimmer 0.5.

**Admin dashboard** — React + Vite + TypeScript + TailwindCSS, custom fetch client with JWT header injection and typed `ApiError`.

**Data & infra** — MySQL with hand-written, idempotent, re-runnable SQL migrations; Cloudinary for media; eSewa ePay gateway; Google Maps Platform; Firebase Cloud Messaging; Gradle (Kotlin DSL) for Android; Git.

---

## 9. What's New — USP

1. **Subscription-first, not order-first.** The core unit is a weekly/monthly meal plan with a *daily attendance log*, not a basket. Skipping tomorrow's lunch is one tap and is reconciled against billing — a primitive no restaurant-delivery app has.
2. **Cook-favourable, transparent commission.** A single low platform rate versus the 35–80% taken by traditional tiffin agencies, with every rate change versioned in `commission_rate_history` and announced to cooks automatically through notification, chat, and socket event. Cooks are never surprised by a margin change.
3. **A request → approval handshake before money moves.** The cook accepts or declines a subscription request before payment. This respects real home-kitchen capacity constraints instead of assuming infinite restaurant throughput.
4. **Built for the actual local payment landscape.** eSewa, manual QR with uploaded proof and admin/cook verification, and COD — because requiring a card or merchant account would exclude most of the intended supply side.
5. **Designed for non-technical women entrepreneurs.** The cook app is task-shaped ("Add Today's Meal", "Today's Orders", "My Earnings"), not an admin console. Supply-side inclusion is a design constraint, not a marketing line.
6. **Multi-cook subscriptions and carts.** A customer can source lunch from one cook and dinner from another; the system fans out orders, notifications, and settlements correctly.
7. **Per-day dispute-ready audit trail.** Every scheduled day resolves to `delivered`, `customer_skipped`, `cook_unavailable`, or `missed`, with the acting party recorded — so "I paid for 30 days and got 26" is answerable from data.
8. **Custom meal requests and combo deals** — customers can negotiate a dish outside the standing menu, preserving the flexibility of the informal arrangement TiffinCraft is digitising.

---

## 10. Revenue Model

**Implemented in the codebase**

| Stream | Mechanism | Status |
|---|---|---|
| **Platform commission on cook earnings** | **5%** (admin-settable), snapshotted per order the moment it becomes `delivered` — including subscription days; billed monthly into `commission_settlements`, rate versioned in `commission_rate_history`, settled manually against an uploaded payment screenshot. See `docs/COMMISSION_SYSTEM.md` | Implemented |
| **Subscription volume** | Weekly/monthly prepaid plans generate predictable, recurring commissionable GMV rather than lumpy per-order revenue | Implemented |
| **Referral-driven growth** | `referrals` table; rewards lower customer acquisition cost, which is the main cost line in a low-ticket business | Implemented |

**Designed for / next**

| Stream | Rationale |
|---|---|
| **Featured / promoted cook placement** | Cooks pay for higher position in nearby-discovery — high margin, no delivery cost, and `platform_settings` already provides the config surface |
| **Verified / Premium cook badge (SaaS fee)** | Flat monthly fee for verification, priority support, and richer analytics; converts commission-averse high-volume cooks to a fixed fee |
| **Customer convenience fee on delivery** | Delivery charges vary by distance and delivery person; a small platform fee per delivery is a natural second take-rate |
| **Combo-deal and campaign merchandising** | Paid participation in curated combo campaigns (`combo_deals` infrastructure exists) |
| **Corporate / hostel bulk plans** | Institutional monthly contracts — highest revenue per acquisition, lowest churn, and the same subscription engine serves them unchanged |

**Unit-economics logic:** revenue scales with recurring GMV, so the levers are subscription retention (defended by skip-instead-of-cancel) and cook retention (defended by a low, transparent, auditable commission). A deliberately below-market take rate is the acquisition strategy for the supply side, with promoted placement and premium badges as the higher-margin layer added once liquidity exists.

---

## 11. Conclusion

TiffinCraft addresses a demand that mainstream food-delivery infrastructure structurally cannot serve: the need for an affordable, hygienic, home-cooked meal *every day*, sourced from a cook a few streets away. By making the subscription — not the order — the primitive, and by pairing it with a per-day attendance log, a cook-approval handshake, and locally viable payment methods, the platform digitises the offline tiffin arrangement without inheriting its opacity or its extractive margins.

The implementation is a complete three-surface system — a native Android app for both sides of the market, a React admin console, and a Node/Express/MySQL API with real-time and push layers — covering discovery, subscription, payment, daily fulfilment, communication, review, commission settlement, and refunds. Its distinguishing contribution is not any single feature but the combination: hyperlocal home-cook supply, subscription billing with day-level control, and a transparent low commission, in one product built for a market where none of the three existed together.

The larger outcome is economic rather than technical. The same platform that lets a student eat home food for a month at a predictable price also lets a home cook go from 5–8 word-of-mouth customers to a discoverable, reviewable, digitally paid business — keeping the large majority of what they earn. That is the result TiffinCraft is built to produce.

---

## Appendix — Verification notes

- **Confirmed** (read from the repository): tech stack and versions (`backend/package.json`, `frontend/app/build.gradle.kts`), the 28 table names (`backend/database/*.sql`), the 26 controllers and 21 route modules, the commission rate-change notification behaviour (`backend/utils/commissionHelper.js`), subscription status/payment enums and the daily-log actor field (`migration_subscription_*.sql`), the problem statement and value proposition (`docs/01_PRD.md`), and the feature list (`README.md`, `context.md`).
- **Likely** (inferred, not verified against running code): exact commission percentage currently configured, and per-feature production readiness beyond what `README.md` claims.
- **Assumption** (written so the document is complete; adjust to your actual plan): the "Designed for / next" revenue streams and the competitor commission percentages in the literature survey are market context, not repository facts.
- **Not addressed:** no build or runtime verification was performed for this task — it is a documentation deliverable only. No numbers here depend on a live run.
