/**
 * Copies the local MySQL `tiffincraft` database (schema + data) into TiDB Cloud.
 *
 * Why this exists instead of `mysqldump | mysql`: the only mysqldump/mysql
 * binaries on this machine come from XAMPP (MariaDB 10.4), which cannot
 * authenticate against MySQL 8's caching_sha2_password, and the real MySQL 8.0
 * install here ships mysqld.exe only — no client tools. mysql2 speaks both
 * caching_sha2_password (local) and TLS (TiDB), so it can do the whole job.
 *
 * SAFETY: the source connection issues nothing but SELECT / SHOW. Your local
 * database is never modified, so a failed or repeated run cannot damage it —
 * worst case you re-run this.
 *
 * Usage (PowerShell / bash):
 *   LOCAL_DB_PASSWORD='yourpass' node scripts/migrate_local_to_tidb.js --dry-run
 *   LOCAL_DB_PASSWORD='yourpass' node scripts/migrate_local_to_tidb.js
 *
 * Flags:
 *   --dry-run      Report what would be copied; write nothing.
 *   --schema-only  Create tables/views but copy no rows.
 *   --overwrite    Replace target tables that already exist (DROP + recreate).
 *                  Without it, a non-empty target table aborts the run.
 *
 * Source overrides (all optional): LOCAL_DB_HOST, LOCAL_DB_PORT, LOCAL_DB_USER,
 * LOCAL_DB_NAME. Target is read from .env (the TiDB values).
 */
import "dotenv/config";
import fs from "fs";
import path from "path";
import mysql from "mysql2/promise";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const args = new Set(process.argv.slice(2));
const DRY_RUN = args.has("--dry-run");
const SCHEMA_ONLY = args.has("--schema-only");
const OVERWRITE = args.has("--overwrite");

const BATCH = 300;

const SOURCE = {
    host: process.env.LOCAL_DB_HOST || "127.0.0.1",
    port: Number(process.env.LOCAL_DB_PORT || 3306),
    user: process.env.LOCAL_DB_USER || "root",
    password: process.env.LOCAL_DB_PASSWORD ?? "",
    database: process.env.LOCAL_DB_NAME || "tiffincraft",
    // Dates as strings: stops JS Date round-tripping from shifting values by
    // the local UTC offset, which would silently move every delivery date.
    dateStrings: true,
    supportBigNumbers: true,
    bigNumberStrings: true,
    connectTimeout: 15000
};

function targetConfig() {
    const cfg = {
        host: process.env.DB_HOST,
        port: Number(process.env.DB_PORT || 4000),
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        dateStrings: true,
        supportBigNumbers: true,
        bigNumberStrings: true,
        connectTimeout: 20000,
        multipleStatements: false
    };
    if (process.env.DB_USE_SSL === "true") {
        cfg.ssl = {
            ca: fs.readFileSync(path.join(__dirname, "../certs/isrgrootx1.pem")),
            minVersion: "TLSv1.2"
        };
    }
    return cfg;
}

/**
 * TiDB parses almost all of MySQL 8's DDL, but not these. Strip them rather
 * than fail the whole table — and tell the user exactly what was dropped so
 * nothing silently goes missing.
 */
function sanitizeDdl(ddl, tableName, notes) {
    let out = ddl;

    // FULLTEXT / SPATIAL indexes: unsupported by TiDB.
    const dropped = [];
    out = out.replace(/^\s*(FULLTEXT|SPATIAL)\s+(KEY|INDEX)\s+[^\n]*?,?\s*$/gim, (m) => {
        dropped.push(m.trim().replace(/,$/, ""));
        return "";
    });
    if (dropped.length) {
        notes.push(`${tableName}: dropped unsupported index(es) → ${dropped.join(" | ")}`);
    }

    // Clean up any dangling comma left behind before the closing paren.
    out = out.replace(/,(\s*)\)/g, "$1)");
    // Blank lines from the removals above.
    out = out.replace(/\n\s*\n/g, "\n");

    return out;
}

function encodeValue(v) {
    if (v === null || v === undefined) return null;
    if (Buffer.isBuffer(v)) return v;
    if (typeof v === "object") return JSON.stringify(v); // JSON columns
    return v;
}

async function main() {
    if (!SOURCE.password) {
        console.error("❌ LOCAL_DB_PASSWORD is not set.\n" +
            "   Run:  LOCAL_DB_PASSWORD='your-local-mysql-password' node scripts/migrate_local_to_tidb.js --dry-run");
        process.exit(1);
    }
    if (!process.env.DB_HOST || !process.env.DB_NAME) {
        console.error("❌ Target DB_HOST/DB_NAME missing from .env.");
        process.exit(1);
    }

    console.log(`SOURCE : ${SOURCE.user}@${SOURCE.host}:${SOURCE.port}/${SOURCE.database}  (read-only)`);
    console.log(`TARGET : ${process.env.DB_USER}@${process.env.DB_HOST}:${process.env.DB_PORT}/${process.env.DB_NAME}`);
    console.log(`MODE   : ${DRY_RUN ? "DRY RUN (no writes)" : SCHEMA_ONLY ? "schema only" : "schema + data"}` +
        `${OVERWRITE ? ", overwrite existing" : ""}\n`);

    const src = await mysql.createConnection(SOURCE);
    const dst = await mysql.createConnection(targetConfig());

    const notes = [];
    const summary = [];

    try {
        // ── Inventory ────────────────────────────────────────────────────
        const [objects] = await src.query("SHOW FULL TABLES");
        const nameKey = Object.keys(objects[0] || {})[0];
        const tables = objects.filter((r) => r.Table_type === "BASE TABLE").map((r) => r[nameKey]);
        const views = objects.filter((r) => r.Table_type === "VIEW").map((r) => r[nameKey]);

        // TiDB supports none of these. Report so they aren't lost silently.
        const [triggers] = await src.query("SHOW TRIGGERS");
        const [routines] = await src.query(
            "SELECT ROUTINE_NAME, ROUTINE_TYPE FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = ?",
            [SOURCE.database]
        );
        if (triggers.length) notes.push(`⚠️  ${triggers.length} trigger(s) NOT migrated — TiDB does not support triggers: ${triggers.map((t) => t.Trigger).join(", ")}`);
        if (routines.length) notes.push(`⚠️  ${routines.length} stored routine(s) NOT migrated — TiDB does not support them: ${routines.map((r) => `${r.ROUTINE_NAME} (${r.ROUTINE_TYPE})`).join(", ")}`);

        console.log(`Found ${tables.length} table(s), ${views.length} view(s).\n`);

        // ── Guard against clobbering a populated target ──────────────────
        if (!DRY_RUN && !OVERWRITE) {
            const clashes = [];
            for (const t of tables) {
                const [[exists]] = await dst.query(
                    "SELECT COUNT(*) c FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
                    [process.env.DB_NAME, t]
                );
                if (exists.c > 0) {
                    const [[n]] = await dst.query(`SELECT COUNT(*) c FROM \`${t}\``);
                    if (n.c > 0) clashes.push(`${t} (${n.c} rows)`);
                }
            }
            if (clashes.length) {
                console.error("❌ Target already has data in: " + clashes.join(", ") +
                    "\n   Re-run with --overwrite to replace those tables, or drop them first.");
                process.exit(1);
            }
        }

        // FK checks off for the whole load, so table creation and insert order
        // don't have to be topologically sorted.
        if (!DRY_RUN) await dst.query("SET foreign_key_checks = 0");

        // ── Schema + data, table by table ────────────────────────────────
        for (const table of tables) {
            const [[srcCount]] = await src.query(`SELECT COUNT(*) c FROM \`${table}\``);
            // Number(): bigNumberStrings makes COUNT(*) a string, which would
            // make the row-count check below a string comparison and the totals
            // string concatenation.
            const srcRows = Number(srcCount.c);
            const [[createRow]] = await src.query(`SHOW CREATE TABLE \`${table}\``);
            const rawDdl = createRow["Create Table"];
            const ddl = sanitizeDdl(rawDdl, table, notes);

            if (DRY_RUN) {
                console.log(`  ${table.padEnd(34)} would copy ${String(srcRows).padStart(7)} row(s)`);
                summary.push({ table, srcRows, dstRows: null });
                continue;
            }

            if (OVERWRITE) await dst.query(`DROP TABLE IF EXISTS \`${table}\``);
            try {
                await dst.query(ddl);
            } catch (err) {
                console.error(`❌ CREATE TABLE \`${table}\` failed: ${err.code} ${err.message}`);
                console.error("   DDL was:\n" + ddl + "\n");
                throw err;
            }

            let copied = 0;
            if (!SCHEMA_ONLY && srcRows > 0) {
                const [cols] = await src.query(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND (EXTRA IS NULL OR EXTRA NOT LIKE '%GENERATED%') ORDER BY ORDINAL_POSITION",
                    [SOURCE.database, table]
                );
                const colNames = cols.map((c) => c.COLUMN_NAME);
                const colList = colNames.map((c) => `\`${c}\``).join(", ");

                // Stable order so LIMIT/OFFSET paging can't skip or repeat rows.
                const [pk] = await src.query(
                    "SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? " +
                    "AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY' ORDER BY ORDINAL_POSITION",
                    [SOURCE.database, table]
                );
                const orderBy = pk.length
                    ? "ORDER BY " + pk.map((c) => `\`${c.COLUMN_NAME}\``).join(", ")
                    : "";

                for (let offset = 0; offset < srcRows; offset += BATCH) {
                    const [rows] = await src.query(
                        `SELECT ${colList} FROM \`${table}\` ${orderBy} LIMIT ${BATCH} OFFSET ${offset}`
                    );
                    if (!rows.length) break;
                    const values = rows.map((r) => colNames.map((c) => encodeValue(r[c])));
                    await dst.query(`INSERT INTO \`${table}\` (${colList}) VALUES ?`, [values]);
                    copied += rows.length;
                }

                // Advance the auto-increment counter past the ids we inserted
                // explicitly, so the app's next INSERT can't collide.
                const [autoRows] = await src.query(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND EXTRA LIKE '%auto_increment%' LIMIT 1",
                    [SOURCE.database, table]
                );
                const autoCol = autoRows[0]?.COLUMN_NAME;
                if (autoCol) {
                    const [[m]] = await dst.query(`SELECT COALESCE(MAX(\`${autoCol}\`), 0) + 1 nxt FROM \`${table}\``);
                    try {
                        await dst.query(`ALTER TABLE \`${table}\` AUTO_INCREMENT = ${Number(m.nxt)}`);
                    } catch (err) {
                        notes.push(`${table}: could not set AUTO_INCREMENT (${err.code}) — TiDB allocates in ranges, usually harmless.`);
                    }
                }
            }

            const [[dstCount]] = await dst.query(`SELECT COUNT(*) c FROM \`${table}\``);
            const dstRows = Number(dstCount.c);
            const ok = SCHEMA_ONLY || srcRows === dstRows;
            console.log(`  ${ok ? "✅" : "❌"} ${table.padEnd(34)} ${String(copied).padStart(7)} copied  → target has ${dstRows} / source ${srcRows}`);
            summary.push({ table, srcRows, dstRows });
        }

        // ── Views last: they can reference any table ─────────────────────
        for (const view of views) {
            if (DRY_RUN) { console.log(`  (view) ${view} would be recreated`); continue; }
            const [[row]] = await src.query(`SHOW CREATE VIEW \`${view}\``);
            let ddl = row["Create View"];
            // Strip DEFINER: the local user doesn't exist on TiDB.
            ddl = ddl.replace(/DEFINER=`[^`]*`@`[^`]*`\s*/i, "").replace(/SQL SECURITY DEFINER\s*/i, "");
            try {
                await dst.query(`DROP VIEW IF EXISTS \`${view}\``);
                await dst.query(ddl);
                console.log(`  ✅ (view) ${view}`);
            } catch (err) {
                notes.push(`view ${view}: NOT created (${err.code} ${err.message})`);
                console.log(`  ❌ (view) ${view} — ${err.code}`);
            }
        }

        if (!DRY_RUN) await dst.query("SET foreign_key_checks = 1");

        // ── Verdict ──────────────────────────────────────────────────────
        console.log("");
        if (notes.length) {
            console.log("Notes:");
            notes.forEach((n) => console.log("  • " + n));
            console.log("");
        }

        if (DRY_RUN) {
            const total = summary.reduce((a, s) => a + s.srcRows, 0);
            console.log(`DRY RUN complete — ${summary.length} table(s), ${total} row(s) would be copied. Nothing was written.`);
            process.exit(0);
        }

        const bad = summary.filter((s) => !SCHEMA_ONLY && s.srcRows !== s.dstRows);
        if (bad.length) {
            console.error("❌ Row counts do not match for: " + bad.map((b) => `${b.table} (${b.dstRows}/${b.srcRows})`).join(", "));
            process.exit(1);
        }
        console.log(`✅ Migration verified — ${summary.length} table(s), ` +
            `${summary.reduce((a, s) => a + s.dstRows, 0)} row(s) present on TiDB and matching local.`);
        process.exit(0);
    } finally {
        await src.end().catch(() => {});
        await dst.end().catch(() => {});
    }
}

main().catch((err) => {
    console.error("\n❌ Migration aborted:", err.code || "", err.message);
    console.error("   Your local database was not modified.");
    process.exit(1);
});
