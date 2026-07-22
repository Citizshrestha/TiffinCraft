// One-off script to seed a real admin login for the Admin panel.
// Usage: npm run seed:admin
// Configure ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_PHONE / ADMIN_NAME in .env to override defaults.
import dotenv from "dotenv";
import bcrypt from "bcryptjs";
import db from "./config/db.js";

dotenv.config();

const ADMIN_NAME = process.env.ADMIN_NAME || "Admin User";
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || "admin@tiffincraft.com";
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "admin123";
const ADMIN_PHONE = process.env.ADMIN_PHONE || "9999999999";

async function seedAdmin() {
    try {
        const [existing] = await db.promise().query(
            "SELECT id FROM users WHERE email = ?", [ADMIN_EMAIL]
        );

        if (existing.length > 0) {
            console.log(`Admin user already exists: ${ADMIN_EMAIL}`);
            process.exit(0);
        }

        const password_hash = await bcrypt.hash(ADMIN_PASSWORD, 10);

        await db.promise().query(
            `INSERT INTO users (full_name, email, phone, password_hash, role, is_active, is_verified, auth_provider)
             VALUES (?, ?, ?, ?, 'admin', TRUE, TRUE, 'local')`,
            [ADMIN_NAME, ADMIN_EMAIL, ADMIN_PHONE, password_hash]
        );

        console.log(`Admin user created successfully: ${ADMIN_EMAIL} / ${ADMIN_PASSWORD}`);
        process.exit(0);
    } catch (error) {
        console.error("Failed to seed admin user:", error.message);
        process.exit(1);
    }
}

seedAdmin();
