/**
 * Adds is_deleted column to chat_messages if missing.
 * Usage: node database/run_chat_delete_migration.js
 */
import db from "../config/db.js";

async function run() {
  try {
    const [cols] = await db.promise().query(
      `SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'chat_messages'
         AND COLUMN_NAME = 'is_deleted'`
    );

    if (cols.length > 0) {
      console.log("is_deleted already exists on chat_messages — nothing to do.");
      process.exit(0);
      return;
    }

    await db.promise().query(
      `ALTER TABLE chat_messages
       ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER content`
    );
    console.log("Migration OK: added chat_messages.is_deleted");
    process.exit(0);
  } catch (err) {
    console.error("Migration failed:", err.message);
    process.exit(1);
  }
}

run();
