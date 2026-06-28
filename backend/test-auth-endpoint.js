import jwt from "jsonwebtoken";
import dotenv from "dotenv";
import http from "http";

dotenv.config();

function makeRequest(url, token) {
    return new Promise((resolve, reject) => {
        const urlObj = new URL(url);
        
        const options = {
            hostname: urlObj.hostname,
            port: urlObj.port,
            path: urlObj.pathname,
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        };
        
        const req = http.request(options, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                resolve({ status: res.statusCode, data: data });
            });
        });
        
        req.on('error', (error) => {
            reject(error);
        });
        
        req.end();
    });
}

async function testAuthEndpoint() {
    try {
        console.log("=== Testing Authenticated Endpoints ===\n");
        
        // Create a test token for cook user (ID 3)
        const testPayload = {
            id: 3,
            email: "citizshresthaa@gmail.com",
            role: "cook"
        };
        
        const token = jwt.sign(testPayload, process.env.JWT_SECRET, { expiresIn: "24h" });
        
        console.log("1. Generated test token for cook ID 3");
        console.log("Token (first 50 chars):", token.substring(0, 50) + "...");
        console.log("\n📋 COPY THIS TOKEN TO TEST IN YOUR APP:");
        console.log(token);
        console.log();
        
        // Test /api/meals/my endpoint
        console.log("\n2. Testing GET /api/meals/my...");
        const mealsResponse = await makeRequest("http://192.168.100.115:5000/api/meals/my", token);
        console.log("Status:", mealsResponse.status);
        
        if (mealsResponse.status === 200) {
            const mealsData = JSON.parse(mealsResponse.data);
            console.log("✅ SUCCESS!");
            console.log("Meals found:", mealsData.meals ? mealsData.meals.length : 0);
            if (mealsData.meals && mealsData.meals.length > 0) {
                console.log("First meal:", mealsData.meals[0].name);
            }
        } else {
            console.log("❌ FAILED!");
            console.log("Response:", mealsResponse.data);
        }
        
        // Test /api/cook/dashboard endpoint
        console.log("\n3. Testing GET /api/cook/dashboard...");
        const dashboardResponse = await makeRequest("http://192.168.100.115:5000/api/cook/dashboard", token);
        console.log("Status:", dashboardResponse.status);
        
        if (dashboardResponse.status === 200) {
            const dashboardData = JSON.parse(dashboardResponse.data);
            console.log("✅ SUCCESS!");
            console.log("Dashboard data:");
            if (dashboardData.dashboard) {
                console.log("  - Today's orders:", dashboardData.dashboard.today_orders?.count || 0);
                console.log("  - Today's earnings: ₹" + (dashboardData.dashboard.today_earnings?.amount || 0));
                console.log("  - Active orders:", dashboardData.dashboard.active_orders?.count || 0);
                console.log("  - Average rating:", dashboardData.dashboard.average_rating?.rating || 0);
            }
        } else {
            console.log("❌ FAILED!");
            console.log("Response:", dashboardResponse.data);
        }
        
        console.log("\n=== Test Complete ===");
        console.log("\n✅ If both endpoints returned data, the backend is working correctly.");
        console.log("⚠️  The issue is in the Android app. Check:");
        console.log("   1. Is the token being sent correctly?");
        console.log("   2. Is the network reachable from the Android device?");
        console.log("   3. Check Logcat for errors");
        
    } catch (error) {
        console.error("\n❌ Error:", error.message);
        console.error(error);
    }
}

testAuthEndpoint();
