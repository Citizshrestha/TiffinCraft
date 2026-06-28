import db from "../config/db.js";

/**
 * Seed Data Script for TiffinCraft Dashboard Testing
 *
 * This script populates the database with sample orders and reviews
 * to test the cook dashboard functionality.
 *
 * Run: node backend/database/seedData.js
 */

async function seedDashboardData() {
    try {
        console.log("🌱 Starting database seeding...\n");

        // Get the first cook user
        const [cooks] = await db.promise().query(
            `SELECT u.id as cook_id, u.full_name
             FROM users u
             JOIN cook_profiles cp ON u.id = cp.user_id
             WHERE u.role = 'cook'
             LIMIT 1`
        );

        if (cooks.length === 0) {
            console.log("❌ No cook found in database. Please register a cook first.");
            process.exit(1);
        }

        const cookId = cooks[0].cook_id;
        const cookName = cooks[0].full_name;
        console.log(`✅ Found cook: ${cookName} (ID: ${cookId})`);

        // Get or create a customer
        let [customers] = await db.promise().query(
            "SELECT id FROM users WHERE role = 'customer' LIMIT 1"
        );

        let customerId;
        if (customers.length === 0) {
            console.log("📝 Creating test customer...");
            const [result] = await db.promise().query(
                `INSERT INTO users (full_name, email, phone, password_hash, role)
                 VALUES ('Test Customer', 'customer@test.com', '9999999999', '$2a$10$dummy', 'customer')`
            );
            customerId = result.insertId;
            console.log(`✅ Created test customer (ID: ${customerId})`);
        } else {
            customerId = customers[0].id;
            console.log(`✅ Using existing customer (ID: ${customerId})`);
        }

        // Get cook's meals
        const [meals] = await db.promise().query(
            "SELECT id, name, price FROM meals WHERE cook_id = ? LIMIT 3",
            [cookId]
        );

        if (meals.length === 0) {
            console.log("❌ No meals found for this cook. Please add meals first.");
            process.exit(1);
        }

        console.log(`✅ Found ${meals.length} meals for the cook\n`);

        // Clear existing test data for this cook
        console.log("🧹 Cleaning up old test data...");
        await db.promise().query("DELETE FROM reviews WHERE cook_id = ?", [cookId]);
        await db.promise().query("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE cook_id = ?)", [cookId]);
        await db.promise().query("DELETE FROM orders WHERE cook_id = ?", [cookId]);
        console.log("✅ Cleaned up old data\n");

        // Seed today's orders
        console.log("📦 Creating today's orders...");
        const todayOrdersCount = 18;
        const todayOrderIds = [];

        for (let i = 0; i < todayOrdersCount; i++) {
            const meal = meals[i % meals.length];
            const quantity = Math.floor(Math.random() * 3) + 1;
            const totalAmount = meal.price * quantity;
            const statuses = ['confirmed', 'preparing', 'ready', 'delivered'];
            const status = statuses[Math.floor(Math.random() * statuses.length)];

            const [orderResult] = await db.promise().query(
                `INSERT INTO orders (customer_id, cook_id, total_amount, status, delivery_address, created_at)
                 VALUES (?, ?, ?, ?, 'Test Address, Kathmandu', NOW())`,
                [customerId, cookId, totalAmount, status]
            );

            const orderId = orderResult.insertId;
            todayOrderIds.push(orderId);

            // Add order items
            await db.promise().query(
                `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time)
                 VALUES (?, ?, ?, ?)`,
                [orderId, meal.id, quantity, meal.price]
            );
        }
        console.log(`✅ Created ${todayOrdersCount} orders for today`);

        // Seed yesterday's orders (for comparison)
        console.log("📦 Creating yesterday's orders...");
        const yesterdayOrdersCount = 16;

        for (let i = 0; i < yesterdayOrdersCount; i++) {
            const meal = meals[i % meals.length];
            const quantity = Math.floor(Math.random() * 3) + 1;
            const totalAmount = meal.price * quantity;
            const status = 'delivered';

            const [orderResult] = await db.promise().query(
                `INSERT INTO orders (customer_id, cook_id, total_amount, status, delivery_address, created_at)
                 VALUES (?, ?, ?, ?, 'Test Address, Kathmandu', DATE_SUB(NOW(), INTERVAL 1 DAY))`,
                [customerId, cookId, totalAmount, status]
            );
        }
        console.log(`✅ Created ${yesterdayOrdersCount} orders for yesterday`);

        // Seed last week's orders (for active orders comparison)
        console.log("📦 Creating last week's orders...");
        const lastWeekOrdersCount = 30;

        for (let i = 0; i < lastWeekOrdersCount; i++) {
            const meal = meals[i % meals.length];
            const quantity = Math.floor(Math.random() * 3) + 1;
            const totalAmount = meal.price * quantity;
            const daysAgo = Math.floor(Math.random() * 7) + 2; // 2-8 days ago

            const [orderResult] = await db.promise().query(
                `INSERT INTO orders (customer_id, cook_id, total_amount, status, delivery_address, created_at)
                 VALUES (?, ?, ?, 'delivered', 'Test Address, Kathmandu', DATE_SUB(NOW(), INTERVAL ? DAY))`,
                [customerId, cookId, totalAmount, daysAgo]
            );
        }
        console.log(`✅ Created ${lastWeekOrdersCount} orders for last week\n`);

        // Seed reviews
        console.log("⭐ Creating reviews...");
        const reviewsCount = 120;
        const ratings = [5, 5, 5, 4, 4, 4, 4, 3, 5, 4]; // Biased toward 4-5 stars
        const comments = [
            "Delicious food! Will order again.",
            "Great taste and on-time delivery.",
            "Amazing authentic flavors!",
            "Food was good but could use more spice.",
            "Excellent quality and portion size.",
            "Very satisfied with the meal.",
            "Tasty and well-packaged.",
            "Good food, friendly service.",
            "Highly recommended!",
            "Perfect comfort food."
        ];

        for (let i = 0; i < reviewsCount; i++) {
            // Create an old delivered order for the review
            const meal = meals[i % meals.length];
            const totalAmount = meal.price * 2;
            const daysAgo = Math.floor(Math.random() * 60) + 1; // 1-60 days ago

            const [orderResult] = await db.promise().query(
                `INSERT INTO orders (customer_id, cook_id, total_amount, status, delivery_address, created_at)
                 VALUES (?, ?, ?, 'delivered', 'Test Address, Kathmandu', DATE_SUB(NOW(), INTERVAL ? DAY))`,
                [customerId, cookId, totalAmount, daysAgo]
            );

            const orderId = orderResult.insertId;

            // Add review for this order
            const rating = ratings[Math.floor(Math.random() * ratings.length)];
            const comment = comments[Math.floor(Math.random() * comments.length)];

            await db.promise().query(
                `INSERT INTO reviews (order_id, customer_id, cook_id, rating, comment, created_at)
                 VALUES (?, ?, ?, ?, ?, DATE_SUB(NOW(), INTERVAL ? DAY))`,
                [orderId, customerId, cookId, rating, comment, daysAgo]
            );
        }
        console.log(`✅ Created ${reviewsCount} reviews\n`);

        // Display summary
        console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        console.log("📊 SEEDING SUMMARY");
        console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        console.log(`Cook: ${cookName} (ID: ${cookId})`);
        console.log(`Today's Orders: ${todayOrdersCount}`);
        console.log(`Yesterday's Orders: ${yesterdayOrdersCount}`);
        console.log(`Last Week's Orders: ${lastWeekOrdersCount}`);
        console.log(`Total Reviews: ${reviewsCount}`);
        console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        console.log("✅ Database seeding completed successfully!");
        console.log("🚀 You can now test the dashboard with real data.\n");

        process.exit(0);

    } catch (error) {
        console.error("❌ Error seeding data:", error);
        process.exit(1);
    }
}

// Run the seeder
seedDashboardData();
