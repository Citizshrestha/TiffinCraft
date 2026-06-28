import db from "./config/db.js";

async function testMeals() {
    try {
        console.log("=== Testing Meals API ===\n");
        
        // Test 1: Check database connection
        console.log("1. Testing database connection...");
        const [rows] = await db.promise().query("SELECT 1 as test");
        console.log("✅ Database connected\n");
        
        // Test 2: Get all meals
        console.log("2. Fetching all meals from database...");
        const [allMeals] = await db.promise().query("SELECT * FROM meals");
        console.log(`✅ Found ${allMeals.length} meals in database\n`);
        
        if (allMeals.length > 0) {
            console.log("Sample meal data:");
            console.log(JSON.stringify(allMeals[0], null, 2));
            console.log();
        }
        
        // Test 3: Get meals by cook_id
        if (allMeals.length > 0) {
            const cookId = allMeals[0].cook_id;
            console.log(`3. Fetching meals for cook_id: ${cookId}...`);
            const [cookMeals] = await db.promise().query(
                "SELECT * FROM meals WHERE cook_id = ?",
                [cookId]
            );
            console.log(`✅ Found ${cookMeals.length} meals for cook ${cookId}\n`);
        }
        
        // Test 4: Check users table
        console.log("4. Checking users table...");
        const [users] = await db.promise().query("SELECT id, email, role FROM users WHERE role = 'cook'");
        console.log(`✅ Found ${users.length} cook users\n`);
        
        if (users.length > 0) {
            console.log("Cook users:");
            users.forEach(user => {
                console.log(`  - ID: ${user.id}, Email: ${user.email}, Role: ${user.role}`);
            });
            console.log();
        }
        
        process.exit(0);
        
    } catch (error) {
        console.error("❌ Error:", error.message);
        console.error(error);
        process.exit(1);
    }
}

testMeals();
