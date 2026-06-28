import db from "../config/db.js";
import { readFileSync } from "fs";
import { fileURLToPath } from "url";
import { dirname, join } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const sqlFile = join(__dirname, "dashboard_tables.sql");
const sql = readFileSync(sqlFile, "utf-8");

// Split by semicolons and execute each statement
const statements = sql
  .split(";")
  .map((stmt) => stmt.trim())
  .filter((stmt) => stmt.length > 0);

console.log(`Executing ${statements.length} SQL statements...\n`);

async function runMigration() {
  try {
    for (let i = 0; i < statements.length; i++) {
      const stmt = statements[i];
      console.log(`[${i + 1}/${statements.length}] Executing...`);
      await db.promise().query(stmt);
      console.log(`✅ Success\n`);
    }
    console.log("✅ Migration completed successfully!");
    process.exit(0);
  } catch (error) {
    console.error("❌ Migration failed:", error.message);
    process.exit(1);
  }
}

runMigration();
