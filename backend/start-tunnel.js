import localtunnel from "localtunnel";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import https from "https";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Path to the tunnel config file the running backend serves at GET /api/config.
// The Android app polls this (via the PC's LAN IP) to auto-discover the current
// tunnel URL WITHOUT needing an APK rebuild every time the tunnel restarts.
const tunnelConfigPath = path.join(__dirname, "tunnel-config.json");

const PORT = 5000;
let currentTunnel = null;
let reconnectAttempts = 0;
let keepAliveInterval = null;

console.log(`🚀 Opening tunnel on port ${PORT}...`);

async function startTunnel() {
    // Clean up existing tunnel
    if (currentTunnel) {
        try {
            currentTunnel.close();
        } catch (e) {
            // Ignore cleanup errors
        }
    }

    // Clear existing keep-alive
    if (keepAliveInterval) {
        clearInterval(keepAliveInterval);
    }

    try {
        currentTunnel = await localtunnel({ 
            port: PORT,
            subdomain: undefined // Let localtunnel assign a random subdomain
        });
        
        reconnectAttempts = 0; // Reset counter on success

        const publicUrl = currentTunnel.url;
        console.log(`✅ Tunnel active! URL: ${publicUrl}`);
        console.log(`   Time: ${new Date().toLocaleTimeString()}`);

        updateTunnelConfig(publicUrl);

        // Set up keep-alive ping every 30 seconds
        keepAliveInterval = setInterval(() => {
            if (currentTunnel && currentTunnel.url) {
                https.get(currentTunnel.url, (res) => {
                    // Just checking connection, ignore response
                }).on('error', (err) => {
                    console.log(`⚡ Keep-alive ping failed: ${err.message}`);
                });
            }
        }, 30000);

        // Handle tunnel errors
        currentTunnel.on("error", (err) => {
            console.error(`⚠️  Tunnel error (${new Date().toLocaleTimeString()}): ${err.message}`);
            reconnect();
        });

        currentTunnel.on("close", () => {
            console.warn(`⚠️  Tunnel closed (${new Date().toLocaleTimeString()})`);
            reconnect();
        });

        // The Android app polls GET /api/config (see ServerConfig.java) and picks
        // this URL up automatically — no APK rebuild needed.
        console.log("⚠️  Do NOT close this terminal — keep it open during your showcase!\n");

    } catch (err) {
        console.error(`❌ Failed to create tunnel: ${err.message}`);
        reconnect();
    }
}

function reconnect() {
    reconnectAttempts++;
    const delay = Math.min(5000 * reconnectAttempts, 30000); // Max 30 seconds
    
    console.log(`🔄 Reconnecting in ${delay/1000} seconds... (attempt ${reconnectAttempts})`);
    
    setTimeout(() => {
        startTunnel();
    }, delay);
}

function updateTunnelConfig(newUrl) {
    try {
        const config = {
            baseUrl: `${newUrl}/api/`,
            serverUrl: newUrl,
            updatedAt: new Date().toISOString()
        };
        fs.writeFileSync(tunnelConfigPath, JSON.stringify(config, null, 2), "utf-8");
        console.log(`📝 tunnel-config.json updated (served at /api/config)`);
    } catch (err) {
        console.error("❌ Failed to write tunnel-config.json:", err.message);
    }
}

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\n\n🛑 Shutting down tunnel gracefully...');
    if (keepAliveInterval) {
        clearInterval(keepAliveInterval);
    }
    if (currentTunnel) {
        currentTunnel.close();
    }
    process.exit(0);
});

process.on('SIGTERM', () => {
    console.log('\n\n🛑 Shutting down tunnel gracefully...');
    if (keepAliveInterval) {
        clearInterval(keepAliveInterval);
    }
    if (currentTunnel) {
        currentTunnel.close();
    }
    process.exit(0);
});

startTunnel();
