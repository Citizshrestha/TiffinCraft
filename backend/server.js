import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import http from "http";
import { Server } from "socket.io";
import jwt from "jsonwebtoken";
import { transporter } from "./utils/emailService.js";
import db from "./config/db.js";
import authRoutes from "./routes/authRoutes.js";
import cookRoutes from "./routes/cookRoutes.js";
import mealRoutes from "./routes/mealRoutes.js";
import orderRoutes from "./routes/orderRoutes.js";
import reviewRoutes from "./routes/reviewRoutes.js";
import adminRoutes from "./routes/adminRoutes.js";
import chatRoutes from "./routes/chatRoutes.js";
import paymentRoutes from "./routes/paymentRoutes.js";
import esewaEpayRoutes from "./routes/esewaEpayRoutes.js";
import refundRoutes from "./routes/refundRoutes.js";
import commissionRoutes from "./routes/commissionRoutes.js";
import customerDashboardRoutes from "./routes/customerDashboardRoutes.js";
import notificationRoutes from "./routes/notificationRoutes.js";
import favoritesRoutes from "./routes/favoritesRoutes.js";
import cartRoutes from "./routes/cartRoutes.js";
import uploadRoutes from "./routes/uploadRoutes.js";
import referralRoutes from "./routes/referralRoutes.js";
import subscriptionRoutes from "./routes/subscriptionRoutes.js";
import subscriptionPlanRoutes from "./routes/subscriptionPlanRoutes.js";
import comboRoutes from "./routes/comboRoutes.js";
import customMealRoutes from "./routes/customMealRoutes.js";
import { startSubscriptionJob } from "./jobs/subscriptionOrderJob.js";
import { startEsewaBookingCleanupJob } from "./jobs/esewaBookingCleanupJob.js";
import { startCommissionSettlementJob } from "./jobs/commissionSettlementJob.js";
import { fileURLToPath } from "url";
import { dirname, join } from "path";
import fs from "fs";
import getLanIp from "./utils/lanIp.js";
import { initFirebase } from "./config/firebaseAdmin.js";
import { markOnline, markOffline, isOnline } from "./utils/onlineUsers.js";

dotenv.config();

const app = express();
const server = http.createServer(app);

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// ── CORS policy ───────────────────────────────────────────────
// Defined here rather than further down because Socket.IO (below) needs the
// same rules as the REST API — previously it had its own, different, and also
// wrong config.
//
// Scope note: CORS is a *browser* protection. The Android app sends no Origin
// header, and eSewa's return trip into /api/payments/... is a top-level
// navigation, not an XHR. Neither is affected by anything in this block.
const allowedOrigins = (process.env.CLIENT_URL || "")
    .split(",")
    .map((o) => o.trim().replace(/\/$/, ""))
    .filter(Boolean);

// Loopback / private-LAN origins, so the Admin dashboard can be developed
// against a running backend. Gated on NODE_ENV: a real production deploy
// should not be reachable from whatever a developer happens to be serving.
const DEV_ORIGIN = /^https?:\/\/(localhost|127\.0\.0\.1|10\.0\.2\.2|192\.168\.\d{1,3}\.\d{1,3})(:\d+)?$/;

// Rejections are logged once per origin, not once per request — a misconfigured
// CLIENT_URL would otherwise flood the log with identical lines.
const loggedRejections = new Set();

function corsOrigin(origin, callback) {
    // No Origin header: native app, curl, server-to-server, eSewa callback.
    // There is no browser here to protect, so CORS has no opinion.
    if (!origin) return callback(null, true);

    const normalized = origin.replace(/\/$/, "");
    if (allowedOrigins.includes(normalized)) return callback(null, true);
    if (process.env.NODE_ENV !== "production" && DEV_ORIGIN.test(normalized)) {
        return callback(null, true);
    }

    if (!loggedRejections.has(normalized)) {
        loggedRejections.add(normalized);
        console.warn(`⚠️  CORS rejected origin: ${normalized} — add it to CLIENT_URL if it is yours`);
    }
    // callback(null, false) omits Access-Control-Allow-Origin, so the browser
    // blocks the response. Deliberately NOT callback(new Error(...)), which
    // would return a 500 and make a config typo look like a server crash.
    return callback(null, false);
}

const io = new Server(server, {
    cors: {
        origin: corsOrigin,
        methods: ["GET", "POST"],
        credentials: true
    }
});

io.use((socket, next) => {
    const token = socket.handshake.auth.token;
    if (!token) {
        return next(new Error('Authentication required'));
    }
    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        socket.user = decoded;
        next();
    } catch (error) {
        next(new Error('Invalid token'));
    }
});

io.on("connection", (socket) => {
    console.log(`🔌 Socket connected: ${socket.id} (User: ${socket.user.id})`);
    socket.join(`user_${socket.user.id}`);

    // Presence: only broadcast on the FIRST connection for this user (refcount
    // was 0 before this call) — a second tab/device shouldn't re-announce "online".
    const wasOnline = isOnline(socket.user.id);
    markOnline(socket.user.id);
    if (!wasOnline) {
        io.emit("presenceChanged", { user_id: socket.user.id, is_online: true });
    }

 socket.on("joinChatRoom", async (conversationId) => {
     try {
         const [rows] = await db.promise().query(
             "SELECT customer_id, cook_id FROM conversations WHERE id = ?",
             [conversationId]
         );
         if (rows.length > 0) {
             const c = rows[0];
             if (c.customer_id === socket.user.id || c.cook_id === socket.user.id) {
                 socket.join(`chat_${conversationId}`);
                 console.log(`💬 Socket ${socket.id} joined chat_${conversationId}`);
             } else {
                 console.warn(`⚠️  Socket ${socket.id} denied access to chat_${conversationId}`);
             }
         }
     } catch (error) {
         console.error("Error joining chat room:", error);
     }
 });

 socket.on("leaveChatRoom", (conversationId) => {
     socket.leave(`chat_${conversationId}`);
 });

 socket.on("chatTyping", (data) => {
     if (data && data.conversation_id) {
         socket.to(`chat_${data.conversation_id}`).emit("chatTyping", {
             conversation_id: data.conversation_id,
             user_id: socket.user.id
         });
     }
 });

    socket.on("joinOrderRoom", async (orderId) => {
        try {
            const [order] = await db.promise().query(
                "SELECT customer_id, cook_id FROM orders WHERE id = ?",
                [orderId]
            );
            if (order.length > 0) {
                const o = order[0];
                if (o.customer_id === socket.user.id || o.cook_id === socket.user.id) {
                    socket.join(`order_${orderId}`);
                    console.log(`📦 Socket ${socket.id} joined order_${orderId}`);
                } else {
                    console.warn(`⚠️  Socket ${socket.id} denied access to order_${orderId}`);
                }
            }
        } catch (error) {
            console.error("Error joining order room:", error);
        }
    });

    socket.on("joinCookRoom", (cookId) => {
        if (socket.user.role === 'cook' && socket.user.id === parseInt(cookId)) {
            socket.join(`cook_${cookId}`);
            console.log(`👨‍🍳 Cook ${cookId} joined their room`);
        } else {
            console.warn(`⚠️  Socket ${socket.id} denied access to cook room ${cookId}`);
        }
    });

    socket.on("disconnect", () => {
        console.log(`🔌 Socket disconnected: ${socket.id}`);
        markOffline(socket.user.id);
        if (!isOnline(socket.user.id)) {
            io.emit("presenceChanged", { user_id: socket.user.id, is_online: false });
        }
    });
});

app.set("io", io);

if (allowedOrigins.length) {
    console.log(`🔒 CORS allowlist: ${allowedOrigins.join(", ")}`);
} else {
    console.warn(
        "⚠️  CORS: CLIENT_URL is empty — browser clients will be blocked. " +
        "The Android app is unaffected (it sends no Origin header)."
    );
}

app.use(cors({
    origin: corsOrigin,
    methods: ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization"],
    credentials: true
}));

app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true, limit: "10mb" }));
app.use("/uploads", express.static(join(__dirname, "uploads")));

app.use((req, _res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

app.get("/", (_req, res) => {
    res.json({
        status: "success",
        message: "TiffinCraft API is running ✓",
        version: "1.0.0",
        timestamp: new Date().toISOString()
    });
});

// Deliberately does NOT touch the database. An external uptime monitor hits
// this every 5 minutes to keep the free-tier host awake, and TiDB Cloud
// Serverless is billed per Request Unit — a DB round-trip per ping would burn
// quota forever to answer a question ("is the process up?") that needs no DB.
// Use /api/health/db when you actually want to check the database.
app.get("/api/health", (_req, res) => {
    res.status(200).json({
        status: "ok",
        uptime_seconds: Math.floor(process.uptime()),
        timestamp: new Date().toISOString()
    });
});

// Deeper check: verifies a pooled connection can actually be acquired.
// Not for the keep-alive pinger — for humans and deploy smoke tests.
app.get("/api/health/db", (_req, res) => {
    db.getConnection((err, connection) => {
        if (err) {
            return res.status(503).json({
                status: "error",
                database: "disconnected",
                // err.message only — never err.stack or the pool config, which
                // would surface the host and user in a public response.
                error: err.message,
                timestamp: new Date().toISOString()
            });
        }
        connection.release();
        return res.json({
            status: "ok",
            database: "connected",
            timestamp: new Date().toISOString()
        });
    });
});

// ── Dynamic tunnel URL discovery ──────────────────────────────
// The Android app calls this (via the PC's stable LAN IP) at startup to fetch
// the CURRENT public tunnel URL. This means the tunnel URL can change/rotate
// freely (localtunnel restarts, PC reboot, etc.) without ever needing to
// rebuild or reinstall the APK — the app self-heals its BASE_URL at runtime.
const tunnelConfigPath = join(__dirname, "tunnel-config.json");
const LAN_IP = getLanIp();

app.get("/api/config", (_req, res) => {
    let tunnelConfig = null;
    try {
        if (fs.existsSync(tunnelConfigPath)) {
            tunnelConfig = JSON.parse(fs.readFileSync(tunnelConfigPath, "utf-8"));
        }
    } catch (e) {
        console.warn("⚠️  Could not read tunnel-config.json:", e.message);
    }

    // Always include the LAN fallback so the app can use it directly
    // when on the same WiFi network, even if the tunnel is down.
    res.json({
        tunnel: tunnelConfig, // { baseUrl, serverUrl, updatedAt } or null if tunnel never started
        lan: {
            baseUrl: `http://${LAN_IP}:${PORT}/api/`,
            serverUrl: `http://${LAN_IP}:${PORT}`
        },
        timestamp: new Date().toISOString()
    });
});

app.use("/api/auth", authRoutes);
app.use("/api/cook", cookRoutes);
app.use("/api/meals", mealRoutes);
app.use("/api/orders", orderRoutes);
app.use("/api/reviews", reviewRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api/customer", customerDashboardRoutes);
app.use("/api/notifications", notificationRoutes);
app.use("/api/favorites", favoritesRoutes);
app.use("/api/cart", cartRoutes);
app.use("/api/upload", uploadRoutes);
app.use("/api/chat", chatRoutes);
app.use("/api/payments", paymentRoutes);
app.use("/api/payments", esewaEpayRoutes);
app.use("/api/refunds", refundRoutes);
app.use("/api/commission", commissionRoutes);
app.use("/api/referrals", referralRoutes);
app.use("/api/subscriptions", subscriptionRoutes);
app.use("/api/subscription-plans", subscriptionPlanRoutes);
app.use("/api/combos", comboRoutes);
app.use("/api/custom-meals", customMealRoutes);

app.use((_req, res) => {
    res.status(404).json({ message: "Route not found." });
});

app.use((err, _req, res, _next) => {
    console.error(`[ERROR] ${err.stack || err.message}`);
    res.status(err.status || 500).json({
        message: err.message || "Internal server error."
    });
});

const PORT = process.env.PORT || 5000;

// ── Environment preflight ─────────────────────────────────────
// Follows the same "fail loud at boot, not silently at the first real payment"
// principle already used in esewaClient.js. On a hosted deploy the only thing
// you get is the build log, so every missing variable has to announce itself
// there rather than surfacing as a confusing 500 an hour later.
(function preflightEnv() {
    const isProd = process.env.NODE_ENV === "production";

    // Without these the process cannot function correctly at all.
    const fatal = ["DB_HOST", "DB_USER", "DB_NAME", "JWT_SECRET"].filter((k) => !process.env[k]);
    if (fatal.length) {
        console.error(`❌ Missing required environment variable(s): ${fatal.join(", ")}`);
        console.error("   Set them in your host's dashboard (or .env locally) and redeploy.");
        process.exit(1);
    }

    if (!isProd) return;

    // Production-only. Each of these degrades or breaks a feature rather than
    // stopping boot, so they warn instead of exiting — but they are loud,
    // because NODE_ENV=production disables every sandbox/dev fallback.
    const checks = [
        ["PUBLIC_BASE_URL", "eSewa redirect/callback URLs will fall back to an unreachable LAN address — payments cannot complete"],
        ["ESEWA_EPAY_PRODUCT_CODE", "ePay v2 sandbox defaults are disabled in production — subscription payment forms would be signed with null"],
        ["ESEWA_EPAY_SECRET_KEY", "ePay v2 signature key missing — eSewa will reject every payment form"],
        ["ESEWA_EPAY_FORM_URL", "ePay v2 form endpoint missing — no default exists in production"],
        ["ESEWA_EPAY_STATUS_URL", "ePay v2 status endpoint missing — server-side payment re-verification cannot run"],
        ["CLIENT_URL", "admin dashboard origin unset — CORS and emailed links fall back to defaults"],
        ["SMTP_HOST", "transactional email (verification, approvals) will not send"]
    ];
    const missing = checks.filter(([k]) => !process.env[k]);
    if (missing.length) {
        console.warn(`\n⚠️  NODE_ENV=production but ${missing.length} variable(s) are unset:`);
        missing.forEach(([k, why]) => console.warn(`   • ${k} — ${why}`));
        console.warn("");
    }

    if (!process.env.FIREBASE_SERVICE_ACCOUNT_JSON
        && !process.env.FIREBASE_SERVICE_ACCOUNT_PATH
        && !process.env.FIREBASE_PROJECT_ID) {
        console.warn("⚠️  No Firebase credentials — push notifications are disabled (the API still works).");
    }
})();

db.getConnection((err, connection) => {
    if (err) {
        console.error("❌ Database connection failed:", err.message);
        process.exit(1);
    }
    connection.release();
    console.log("✅ Database connected");

    // Initialize Firebase Admin SDK for FCM push notifications
    initFirebase();

    transporter.verify((smtpErr) => {
        if (smtpErr) {
            console.warn(`⚠️  SMTP not ready: ${smtpErr.message}`);
        } else {
            console.log(`✅ SMTP ready — ${process.env.SMTP_HOST}`);
        }
    });

    // Start subscription auto-order cron job (runs daily at 06:00)
    startSubscriptionJob();

    // Auto-cancels stale eSewa BOOKED payments (every 5 min)
    startEsewaBookingCleanupJob();

    // Generates monthly commission settlement dues for cooks (1st of month)
    startCommissionSettlementJob();

    server.listen(PORT, "0.0.0.0", () => {
        console.log(`\n✅ Server running on port ${PORT}`);
        if (process.env.NODE_ENV === "production") {
            // The LAN/emulator addresses below are meaningless on a hosted
            // box; print what an operator can actually reach instead.
            const base = process.env.PUBLIC_BASE_URL || "(PUBLIC_BASE_URL not set)";
            console.log(`🌍 Public base:      ${base}`);
            console.log(`🔍 Health check:     ${base}/api/health`);
            console.log(`🩺 DB health:        ${base}/api/health/db`);
        } else {
            console.log(`🌐 Local:            http://localhost:${PORT}`);
            console.log(`📱 Android Emulator: http://10.0.2.2:${PORT}`);
            console.log(`🔍 Health check:     http://localhost:${PORT}/api/health`);
        }
        console.log(`🔌 Socket.IO:        ready\n`);
    });
});

// ── Graceful Shutdown ─────────────────────────────────────
const shutdown = (signal) => {
    console.log(`\n${signal} received — shutting down gracefully`);
    server.close(() => {
        db.end(() => {
            console.log("✅ DB pool closed. Exiting.");
            process.exit(0);
        });
    });
};

process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));