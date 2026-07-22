import db from './config/db.js';

async function alterTable() {
    try {
        await db.promise().query(
            "ALTER TABLE users MODIFY COLUMN role ENUM('customer', 'cook', 'admin') NOT NULL DEFAULT 'customer'"
        );
        console.log("Successfully altered users table");
    } catch (error) {
        console.error("Failed to alter users table:", error);
    } finally {
        process.exit(0);
    }
}

alterTable();
