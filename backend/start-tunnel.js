import localtunnel from "localtunnel";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Path to RetrofitClient.java (relative to backend folder)
const retrofitClientPath = path.join(
    __dirname,
    "../frontend/app/src/main/java/com/tiffincraft/app/api/RetrofitClient.java"
);

const PORT = 5000;

console.log(`🚀 Opening tunnel on port ${PORT}...`);

async function startTunnel() {
    let tunnel;
    try {
        tunnel = await localtunnel({ port: PORT });
    } catch (err) {
        console.error("❌ Failed to create tunnel:", err.message);
        process.exit(1);
    }

    const publicUrl = tunnel.url;
    console.log(`✅ Tunnel active! URL: ${publicUrl}`);

    updateRetrofitClient(publicUrl);

    // Keep-alive: log when tunnel sends errors
    tunnel.on("error", (err) => {
        console.error("⚠️  Tunnel error:", err.message);
    });

    tunnel.on("close", () => {
        console.warn("⚠️  Tunnel closed. Reconnecting in 3 seconds...");
        setTimeout(startTunnel, 3000);
    });

    // Prevent the node process from exiting
    console.log("⚠️  Do NOT close this terminal — keep it open during your showcase!\n");
}

function updateRetrofitClient(newUrl) {
    if (!fs.existsSync(retrofitClientPath)) {
        console.error(`❌ Cannot find RetrofitClient.java at:\n   ${retrofitClientPath}`);
        return;
    }

    try {
        let content = fs.readFileSync(retrofitClientPath, "utf-8");

        // Update SERVER_URL
        content = content.replace(
            /public static final String SERVER_URL = ".*?";/,
            `public static final String SERVER_URL = "${newUrl}";`
        );

        // Update BASE_URL
        content = content.replace(
            /public static final String BASE_URL = ".*?";/,
            `public static final String BASE_URL = "${newUrl}/api/";`
        );

        fs.writeFileSync(retrofitClientPath, content, "utf-8");

        console.log(`🎉 RetrofitClient.java updated!`);
        console.log(`📱 Now REBUILD the app in Android Studio (green Run button).`);
        console.log(`   Server URL: ${newUrl}`);
        console.log(`   API URL:    ${newUrl}/api/\n`);
    } catch (err) {
        console.error("❌ Failed to update RetrofitClient.java:", err.message);
    }
}

startTunnel();
