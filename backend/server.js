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
import { fileURLToPath } from "url";
import { dirname, join } from "path";

dotenv.config();

const app = express();
const server = http.createServer(app);

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const io = new Server(server, {
    cors: {
        origin: process.env.CLIENT_URL || "*",
        methods: ["GET", "POST"]
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
    });
});

app.set("io", io);

const allowedOrigins = (process.env.CLIENT_URL || '').split(',').filter(Boolean);

if (allowedOrigins.length === 0) {
    console.warn("⚠️  CLIENT_URL not configured. CORS will reject all origins.");
}

app.use(cors({
    origin: (origin, callback) => {
        if (!origin || allowedOrigins.includes(origin)) {
            callback(null, true);
        } else {
            callback(new Error('CORS policy violation'));
        }
    },
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

app.get("/api/health", (_req, res) => {
    db.getConnection((err, connection) => {
        if (err) {
            return res.status(503).json({
                status: "error",
                database: "disconnected",
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

app.use("/api/auth", authRoutes);
app.use("/api/cook", cookRoutes);
app.use("/api/meals", mealRoutes);
app.use("/api/orders", orderRoutes);
app.use("/api/reviews", reviewRoutes);
app.use("/api/admin", adminRoutes);

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

db.getConnection((err, connection) => {
    if (err) {
        console.error("❌ Database connection failed:", err.message);
        process.exit(1);
    }
    connection.release();
    console.log("✅ Database connected");

    transporter.verify((smtpErr) => {
        if (smtpErr) {
            console.warn(`⚠️  SMTP not ready: ${smtpErr.message}`);
        } else {
            console.log(`✅ SMTP ready — ${process.env.SMTP_HOST}`);
        }
    });

    server.listen(PORT, "0.0.0.0", () => {
        console.log(`\n✅ Server running on port ${PORT}`);
        console.log(`🌐 Local:            http://localhost:${PORT}`);
        console.log(`📱 Android Emulator: http://10.0.2.2:${PORT}`);
        console.log(`🔍 Health check:     http://localhost:${PORT}/api/health`);
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