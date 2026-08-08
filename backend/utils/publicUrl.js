import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import getLanIp from "./lanIp.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
// Same file server.js's /api/config reads — reused here so eSewa's
// callback_url tracks whatever tunnel URL is currently live, without a
// separate env var to keep in sync during local dev.
const tunnelConfigPath = path.join(__dirname, "..", "tunnel-config.json");

/**
 * The base URL eSewa's servers must be able to reach (HTTPS, publicly
 * routable) for callback_url/redirect_url. Resolution order:
 *   1. PUBLIC_BASE_URL env var — set this in production behind a real domain.
 *   2. The current localtunnel URL from tunnel-config.json (local dev).
 * Returns null if neither is available — callers must handle that (eSewa
 * booking simply can't succeed without a reachable callback_url).
 */
export function getPublicBaseUrl() {
    if (process.env.PUBLIC_BASE_URL) {
        return process.env.PUBLIC_BASE_URL.replace(/\/$/, "");
    }
    try {
        if (fs.existsSync(tunnelConfigPath)) {
            const config = JSON.parse(fs.readFileSync(tunnelConfigPath, "utf-8"));
            if (config?.serverUrl) return config.serverUrl.replace(/\/$/, "");
        }
    } catch {
        // fall through to null
    }
    return null;
}

/**
 * Base URL for BROWSER-REDIRECT targets (eSewa ePay v2's success_url /
 * failure_url). Unlike getPublicBaseUrl()'s server-to-server callbacks,
 * these are loaded by the WebView on the user's own phone — so in local dev
 * the LAN address works and is far more reliable than a localtunnel URL
 * that dies whenever the tunnel process stops (which strands the user on a
 * "Tunnel Unavailable" page right after they've paid).
 * Production still uses the real domain via PUBLIC_BASE_URL.
 */
export function getRedirectBaseUrl() {
    if (process.env.PUBLIC_BASE_URL) {
        return process.env.PUBLIC_BASE_URL.replace(/\/$/, "");
    }
    const port = process.env.PORT || 5000;
    return `http://${getLanIp()}:${port}`;
}
