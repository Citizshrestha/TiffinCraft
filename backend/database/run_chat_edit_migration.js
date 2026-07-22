/**
 * Adds edited_at column to chat_messages if missing.
 * Usage: node database/run_chat_edit_migration.js
 */
import db from "../config/db.js";

async function run() {
  try {
    const [cols] = await db.promise().query(
      `SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'chat_messages'
         AND COLUMN_NAME = 'edited_at'`
    );

    if (cols.length > 0) {
      console.log("edited_at already exists on chat_messages — nothing to do.");
      process.exit(0);
      return;
    }

    await db.promise().query(
      `ALTER TABLE chat_messages
       ADD COLUMN edited_at TIMESTAMP NULL DEFAULT NULL AFTER created_at`
    );
    console.log("Migration OK: added chat_messages.edited_at");
    process.exit(0);
  } catch (err) {
    console.error("Migration failed:", err.message);
    process.exit(1);
  }
}

run();
