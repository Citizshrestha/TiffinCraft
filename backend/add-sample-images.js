import db from "./config/db.js";
import { promises as fs } from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { dirname } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

async function addSampleImages() {
    try {
        console.log("=== Adding Sample Meal Images ===\n");
        
        // Ensure uploads/meals directory exists
        const mealsDir = path.join(__dirname, "uploads", "meals");
        try {
            await fs.mkdir(mealsDir, { recursive: true });
            console.log("✅ uploads/meals directory ready\n");
        } catch (err) {
            console.log("⚠️  Directory already exists\n");
        }
        
        // Get all meals without images
        const [meals] = await db.promise().query(
            "SELECT id, name, category FROM meals WHERE image_url IS NULL"
        );
        
        if (meals.length === 0) {
            console.log("✅ All meals already have images!");
            process.exit(0);
        }
        
        console.log(`Found ${meals.length} meals without images:\n`);
        
        // Sample image URLs (using placeholder service)
        const placeholderImages = {
            'Breakfast': 'https://via.placeholder.com/400x300/4CAF50/FFFFFF?text=Breakfast',
            'Lunch Thali': 'https://via.placeholder.com/400x300/FF9800/FFFFFF?text=Lunch+Thali',
            'Dinner Thali': 'https://via.placeholder.com/400x300/2196F3/FFFFFF?text=Dinner+Thali',
            'Snacks': 'https://via.placeholder.com/400x300/E91E63/FFFFFF?text=Snacks',
            'Dessert': 'https://via.placeholder.com/400x300/9C27B0/FFFFFF?text=Dessert',
            'Beverages': 'https://via.placeholder.com/400x300/00BCD4/FFFFFF?text=Beverages'
        };
        
        // For this example, we'll just set relative URLs that point to placeholders
        // In production, you'd download actual images
        
        for (const meal of meals) {
            // Generate a placeholder image path
            const category = meal.category || 'Breakfast';
            const imagePath = `/uploads/meals/placeholder-${meal.id}.jpg`;
            
            // Update database
            await db.promise().query(
                "UPDATE meals SET image_url = ? WHERE id = ?",
                [imagePath, meal.id]
            );
            
            console.log(`✅ Updated meal "${meal.name}" (ID: ${meal.id})`);
            console.log(`   Image URL: ${imagePath}`);
        }
        
        console.log(`\n✅ Updated ${meals.length} meals with image URLs`);
        console.log("\n⚠️  NOTE: These are placeholder URLs.");
        console.log("To add real images:");
        console.log("1. Use the 'Add Meal' feature in the app and upload images");
        console.log("2. Or manually copy images to backend/uploads/meals/");
        console.log("3. Then update the database image_url column");
        
        process.exit(0);
        
    } catch (error) {
        console.error("❌ Error:", error.message);
        console.error(error);
        process.exit(1);
    }
}

addSampleImages();
