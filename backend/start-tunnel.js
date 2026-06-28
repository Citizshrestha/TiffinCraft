import { spawn } from "child_process";
import fs from "fs";
import path from "path";

// Path to RetrofitClient.java
const retrofitClientPath = path.join(
    process.cwd(), 
    "../frontend/app/src/main/java/com/tiffincraft/app/api/RetrofitClient.java"
);

console.log("🚀 Starting LocalTunnel on port 5000...");

// Run localtunnel using npx
const tunnel = spawn("npx", ["localtunnel", "--port", "5000"], { shell: true });

tunnel.stdout.on("data", (data) => {
    const output = data.toString();
    console.log(`[LocalTunnel] ${output.trim()}`);

    // Check if the output contains the URL
    if (output.includes("your url is:")) {
        const urlMatch = output.match(/https:\/\/[^\s]+/);
        if (urlMatch) {
            const publicUrl = urlMatch[0];
            console.log(`✅ Tunnel active! URL: ${publicUrl}`);
            
            // Automatically update RetrofitClient.java
            updateRetrofitClient(publicUrl);
        }
    }
});

tunnel.stderr.on("data", (data) => {
    console.error(`[LocalTunnel Error] ${data.toString().trim()}`);
});

tunnel.on("close", (code) => {
    console.log(`❌ LocalTunnel closed with code ${code}`);
});

function updateRetrofitClient(newUrl) {
    if (!fs.existsSync(retrofitClientPath)) {
        console.error(`❌ Could not find RetrofitClient.java at ${retrofitClientPath}`);
        return;
    }

    try {
        let content = fs.readFileSync(retrofitClientPath, "utf-8");
        
        // Update SERVER_URL
        content = content.replace(
            /public static final String SERVER_URL = ".*";/,
            `public static final String SERVER_URL = "${newUrl}";`
        );
        
        // Update BASE_URL
        content = content.replace(
            /public static final String BASE_URL = ".*\/api\/";/,
            `public static final String BASE_URL = "${newUrl}/api/";`
        );

        fs.writeFileSync(retrofitClientPath, content, "utf-8");
        console.log(`🎉 SUCCESS: Automatically updated RetrofitClient.java with the new URL!`);
        console.log(`📱 You can now build and run your Android app to connect via mobile data.`);
        console.log(`⚠️  Do not close this terminal window until your showcase is over.\n`);
    } catch (err) {
        console.error("❌ Failed to update RetrofitClient.java:", err);
    }
}
