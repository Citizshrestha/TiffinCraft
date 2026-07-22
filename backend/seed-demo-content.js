import db from './config/db.js';

// Seeds demo orders + favorites for the customer test account used during
// manual QA (customer.test@tiffincraft.com), so the redesigned Menu/Orders/
// Favorites screens have real DB-backed content instead of empty states.
// Safe to re-run: it skips seeding orders/favorites the account already has.

const TEST_CUSTOMER_EMAIL = 'customer.test@tiffincraft.com';

async function seedDemoContent() {
    const conn = db.promise();

    try {
        const [[customer]] = await conn.query(
            `SELECT id, full_name FROM users WHERE email = ? AND role = 'customer'`,
            [TEST_CUSTOMER_EMAIL]
        );

        if (!customer) {
            console.error(`❌ No customer found with email ${TEST_CUSTOMER_EMAIL}. Register that account first.`);
            process.exit(1);
        }
        console.log(`✅ Seeding for customer "${customer.full_name}" (ID: ${customer.id})`);

        const [cooks] = await conn.query(
            `SELECT u.id, u.full_name, cp.kitchen_name
             FROM users u JOIN cook_profiles cp ON cp.user_id = u.id
             WHERE u.role = 'cook' AND u.is_active = TRUE AND cp.is_approved = TRUE
             ORDER BY u.id LIMIT 4`
        );
        if (cooks.length === 0) {
            console.error('❌ No approved cooks found.');
            process.exit(1);
        }

        // ---------------- Favorites ----------------
        const [existingFavs] = await conn.query(
            `SELECT cook_id FROM favorites WHERE customer_id = ?`, [customer.id]
        );
        if (existingFavs.length === 0) {
            const favCooks = cooks.slice(0, Math.min(3, cooks.length));
            for (const cook of favCooks) {
                await conn.query(
                    `INSERT INTO favorites (customer_id, cook_id) VALUES (?, ?)
                     ON DUPLICATE KEY UPDATE created_at = created_at`,
                    [customer.id, cook.id]
                );
            }
            console.log(`✅ Seeded ${favCooks.length} favorite cook(s).`);
        } else {
            console.log(`↷ Customer already has ${existingFavs.length} favorite(s), skipping.`);
        }

        // ---------------- Orders ----------------
        const [existingOrders] = await conn.query(
            `SELECT id FROM orders WHERE customer_id = ?`, [customer.id]
        );
        if (existingOrders.length > 0) {
            console.log(`↷ Customer already has ${existingOrders.length} order(s), skipping order seed.`);
            return;
        }

        const addresses = [
            'Continental Road, Gaindakot-2',
            'Baneshwor, Kathmandu, Nepal',
            'Thamel, Kathmandu, Nepal',
        ];

        const orderPlans = [
            { cookIdx: 0, status: 'delivered', daysAgo: 5, itemCounts: [2, 1] },
            { cookIdx: 1, status: 'delivered', daysAgo: 3, itemCounts: [1] },
            { cookIdx: 0, status: 'cancelled', daysAgo: 2, itemCounts: [1] },
            { cookIdx: 2 % cooks.length, status: 'preparing', daysAgo: 0, itemCounts: [1, 1] },
            { cookIdx: 1, status: 'pending', daysAgo: 0, itemCounts: [2] },
        ];

        let seededOrders = 0;
        for (const plan of orderPlans) {
            const cook = cooks[plan.cookIdx];
            const [meals] = await conn.query(
                `SELECT id, name, price FROM meals WHERE cook_id = ? AND is_available = TRUE LIMIT ?`,
                [cook.id, Math.max(plan.itemCounts.length, 3)]
            );
            if (meals.length === 0) {
                console.log(`⚠️  Cook ${cook.full_name} has no meals, skipping this order.`);
                continue;
            }

            const created = new Date();
            created.setDate(created.getDate() - plan.daysAgo);
            created.setHours(12, 30, 0, 0);

            let total = 0;
            const items = plan.itemCounts.map((qty, i) => {
                const meal = meals[i % meals.length];
                total += meal.price * qty;
                return { meal, qty };
            });

            const [orderResult] = await conn.query(
                `INSERT INTO orders
                    (customer_id, cook_id, total_amount, delivery_address, status,
                     payment_method, payment_status, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, 'cod', ?, ?, ?)`,
                [
                    customer.id, cook.id, total,
                    addresses[seededOrders % addresses.length], plan.status,
                    plan.status === 'delivered' ? 'paid' : 'pending',
                    created, created
                ]
            );

            for (const { meal, qty } of items) {
                await conn.query(
                    `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time)
                     VALUES (?, ?, ?, ?)`,
                    [orderResult.insertId, meal.id, qty, meal.price]
                );
            }
            seededOrders++;
        }

        console.log(`✅ Seeded ${seededOrders} order(s) for "${customer.full_name}".`);
        console.log('🎉 Done. Reopen Menu / Orders / Favorites in the app to see real data.');

    } catch (err) {
        console.error('❌ Error seeding demo content:', err.message);
        console.error(err);
        process.exit(1);
    } finally {
        db.end();
    }
}

seedDemoContent();
