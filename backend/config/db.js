import mysql from "mysql2";
import dotenv from "dotenv";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

dotenv.config();

// __dirname doesn't exist in ES Modules by default — recreate it
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// TiDB's public endpoint requires TLS. We read the CA cert file
// and pass it into the SSL config below.
// DB_USE_SSL lets you toggle this off for local MySQL during
// development, and on for TiDB in production.
const useSSL = process.env.DB_USE_SSL === "true";

const poolConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT,
    connectionLimit: 10, // good practice for pooled connections in production
};

if (useSSL) {
    poolConfig.ssl = { minVersion: "TLSv1.2" };

    // TiDB Cloud's gateway certificate chains to ISRG Root X1, which Node
    // already trusts through its bundled CA store — so the explicit file is
    // belt-and-braces, not a requirement. It is read defensively because a
    // MISSING file must never take the whole server down at boot: on a fresh
    // deploy (Render, Docker, a clean clone) certs/ may simply not be there,
    // and the old unconditional readFileSync threw before Express ever started.
    const caPath = path.join(__dirname, "../certs/isrgrootx1.pem");
    try {
        poolConfig.ssl.ca = fs.readFileSync(caPath);
    } catch (err) {
        console.warn(
            `⚠️  CA file not readable at ${caPath} (${err.code}) — ` +
            "falling back to Node's bundled root CAs. TLS is still enforced."
        );
    }
}

const db = mysql.createPool(poolConfig);

db.getConnection((err, connection) => {
    if (err) {
        console.error(`Database Connection Failed: ${err.message || err}`);
        return;
    }

    console.log(`MySQL Connected Successfully ${useSSL ? "(TiDB, SSL)" : "(Local)"}`);
    connection.release();
});

export default db;
