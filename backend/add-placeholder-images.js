import db from "./config/db.js";

async function addPlaceholderImages() {
    try {
        console.log("=== Adding Placeholder Images to Meals ===\n");
        
        // Get all meals without images
        const [meals] = await db.promise().query(
            "SELECT id, name, category FROM meals WHERE image_url IS NULL ORDER BY id"
        );
        
        if (meals.length === 0) {
            console.log("✅ All meals already have images!");
            process.exit(0);
        }
        
        console.log(`Found ${meals.length} meals without images:\n`);
        
        // Update each meal with a placeholder path
        for (const meal of meals) {
            const imagePath = `/uploads/meals/placeholder-${meal.id}.jpg`;
            
            await db.promise().query(
                "UPDATE meals SET image_url = ? WHERE id = ?",
                [imagePath, meal.id]
            );
            
            console.log(`✅ Meal "${meal.name}" (ID: ${meal.id})`);
            console.log(`   Category: ${meal.category || 'None'}`);
            console.log(`   Image URL: ${imagePath}\n`);
        }
        
        console.log(`\n✅ Updated ${meals.length} meals with placeholder URLs`);
        console.log("\n⚠️  NOTE:");
        console.log("These are placeholder URLs - actual image files don't exist yet.");
        console.log("The app will show default placeholders for these meals.");
        console.log("\nTo see REAL images:");
        console.log("1. Restart backend: cd backend && npm start");
        console.log("2. Rebuild Android app");
        console.log("3. Add a NEW meal with a photo");
        console.log("4. The new meal will have a real uploaded image!");
        
        process.exit(0);
        
    } catch (error) {
        console.error("❌ Error:", error.message);
        console.error(error);
        process.exit(1);
    }
}

addPlaceholderImages();
