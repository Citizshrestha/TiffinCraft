import jwt from "jsonwebtoken";
import dotenv from "dotenv";

dotenv.config();

// This script helps debug JWT tokens and API calls
// Usage: node debug-api-call.js <token>

const token = process.argv[2];

if (!token) {
    console.log("Usage: node debug-api-call.js <your-jwt-token>");
    console.log("\nExample:");
    console.log("node debug-api-call.js eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
    process.exit(1);
}

try {
    console.log("=== JWT Token Debugger ===\n");
    
    console.log("1. Decoding token...");
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    
    console.log("✅ Token is VALID\n");
    console.log("Token payload:");
    console.log(JSON.stringify(decoded, null, 2));
    console.log();
    
    console.log("User Details:");
    console.log("  - User ID:", decoded.id);
    console.log("  - Email:", decoded.email);
    console.log("  - Role:", decoded.role);
    console.log();
    
    // Check expiration
    const now = Math.floor(Date.now() / 1000);
    if (decoded.exp) {
        const expiresIn = decoded.exp - now;
        if (expiresIn > 0) {
            console.log("  - Expires in:", Math.floor(expiresIn / 3600), "hours");
        } else {
            console.log("  - ❌ Token EXPIRED");
        }
    }
    
    console.log("\nTo test API call:");
    console.log(`curl -H "Authorization: Bearer ${token}" http://localhost:5000/api/meals/my`);
    
} catch (error) {
    console.log("❌ Token is INVALID");
    console.log("Error:", error.message);
    
    if (error.name === "TokenExpiredError") {
        console.log("\n⚠️  Token has expired. Please login again to get a fresh token.");
    } else if (error.name === "JsonWebTokenError") {
        console.log("\n⚠️  Token is malformed or signed with wrong secret.");
    }
}
