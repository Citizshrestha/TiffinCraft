import db from './config/db.js';

async function seedOrdersForCook(conn, cook, customerIds, addresses) {
    let [meals] = await conn.query(
        `SELECT id, name, price FROM meals WHERE cook_id = ? LIMIT 5`,
        [cook.id]
    );

    if (meals.length === 0) {
        console.log(`⚠️  No meals for cook ${cook.full_name}. Creating demo meals...`);
        const demoMeals = [
            { name: 'Paneer Thali', description: 'Paneer curry, dal, rice, roti', price: 120, category: 'Lunch Thali' },
            { name: 'Veg Thali', description: 'Mix veg, dal fry, jeera rice, roti', price: 100, category: 'Lunch Thali' },
            { name: 'Rajma Chawal', description: 'Rajma curry with steamed rice', price: 90, category: 'Rice Bowl' },
            { name: 'Dal Makhani Set', description: 'Dal makhani, naan, salad', price: 110, category: 'Dinner Thali' },
            { name: 'Chole Bhature', description: 'Spicy chole with two bhature', price: 80, category: 'Snacks' },
        ];
        const insertedIds = [];
        for (const m of demoMeals) {
            const [r] = await conn.query(
                `INSERT INTO meals (cook_id, name, description, price, category, is_available, is_vegetarian, created_at)
                 VALUES (?, ?, ?, ?, ?, 1, 1, NOW())`,
                [cook.id, m.name, m.description, m.price, m.category]
            );
            insertedIds.push({ id: r.insertId, name: m.name, price: m.price });
        }
        meals = insertedIds;
    }

    await conn.query(
        `DELETE oi FROM order_items oi
         INNER JOIN orders o ON oi.order_id = o.id
         WHERE o.cook_id = ? AND o.customer_id IN (?)`,
        [cook.id, customerIds]
    );
    await conn.query(
        `DELETE FROM orders WHERE cook_id = ? AND customer_id IN (?)`,
        [cook.id, customerIds]
    );

    const dummyOrders = [
        { customer: 0, meal: 0,               qty: 2, status: 'pending',    days_ago: 0, address: 0 },
        { customer: 1, meal: 1 % meals.length, qty: 1, status: 'confirmed',  days_ago: 0, address: 1 },
        { customer: 2, meal: 2 % meals.length, qty: 3, status: 'preparing', days_ago: 0, address: 2 },
        { customer: 0, meal: 0,               qty: 1, status: 'ready',       days_ago: 1, address: 3 },
        { customer: 1, meal: 3 % meals.length, qty: 2, status: 'delivered', days_ago: 1, address: 4 },
        { customer: 2, meal: 4 % meals.length, qty: 1, status: 'delivered', days_ago: 2, address: 0 },
        { customer: 0, meal: 1 % meals.length, qty: 2, status: 'delivered', days_ago: 3, address: 1 },
        { customer: 1, meal: 0,               qty: 1, status: 'cancelled',  days_ago: 2, address: 2 },
        { customer: 2, meal: 1 % meals.length, qty: 2, status: 'delivered', days_ago: 0, address: 3 },
        { customer: 0, meal: 2 % meals.length, qty: 1, status: 'delivered', days_ago: 4, address: 4 },
        { customer: 1, meal: 4 % meals.length, qty: 2, status: 'delivered', days_ago: 5, address: 0 },
        { customer: 2, meal: 3 % meals.length, qty: 3, status: 'delivered', days_ago: 6, address: 1 },
    ];

    let insertedCount = 0;
    for (const o of dummyOrders) {
        const meal   = meals[o.meal];
        const custId = customerIds[o.customer];
        const total  = meal.price * o.qty;

        const created = new Date();
        created.setDate(created.getDate() - o.days_ago);
        created.setHours(8 + Math.floor(Math.random() * 12));
        created.setMinutes(Math.floor(Math.random() * 60));
        created.setSeconds(0);

        const [res] = await conn.query(
            `INSERT INTO orders
             (customer_id, cook_id, total_amount, delivery_address, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [custId, cook.id, total, addresses[o.address], o.status, created, created]
        );
        await conn.query(
            `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time)
             VALUES (?, ?, ?, ?)`,
            [res.insertId, meal.id, o.qty, meal.price]
        );
        insertedCount++;
    }

    return insertedCount;
}

async function seedOrders() {
    const conn = db.promise();

    try {
        const [cooks] = await conn.query(
            `SELECT u.id, u.full_name, u.email FROM users u WHERE u.role = 'cook'`
        );
        if (cooks.length === 0) {
            console.error('❌ No cook user found. Please register a cook first.');
            process.exit(1);
        }
        console.log(`✅ Found ${cooks.length} cook(s)`);

        const [customers] = await conn.query(
            `SELECT id, full_name FROM users WHERE role = 'customer' LIMIT 3`
        );

        let customerIds;
        if (customers.length === 0) {
            console.log('⚠️  No customers found. Creating dummy customers...');
            const demoCustomers = [
                ['Priya Sharma', 'priya.demo@tiffincraft.com', '9800000011'],
                ['Rahul Verma',  'rahul.demo@tiffincraft.com',  '9800000012'],
                ['Sneha Gupta',  'sneha.demo@tiffincraft.com',  '9800000013'],
            ];
            customerIds = [];
            for (const [name, email, phone] of demoCustomers) {
                await conn.query(
                    `INSERT IGNORE INTO users (full_name, email, phone, role, is_verified, password_hash, created_at)
                     VALUES (?, ?, ?, 'customer', 1, '$2b$10$dummyhashfortestonly', NOW())`,
                    [name, email, phone]
                );
                const [[u]] = await conn.query(`SELECT id FROM users WHERE email = ?`, [email]);
                customerIds.push(u.id);
            }
        } else {
            customerIds = customers.map(c => c.id);
            while (customerIds.length < 3) customerIds.push(customerIds[0]);
        }
        console.log(`✅ Using customer IDs: ${customerIds.join(', ')}`);

        const addresses = [
            'Baneshwor, Kathmandu, Nepal',
            'Lalitpur, Patan, Nepal',
            'Bhaktapur, Nepal',
            'Thamel, Kathmandu, Nepal',
            'Boudha, Kathmandu, Nepal',
        ];

        let totalSeeded = 0;
        for (const cook of cooks) {
            const count = await seedOrdersForCook(conn, cook, customerIds, addresses);
            console.log(`✅ Seeded ${count} orders for cook "${cook.full_name}" (ID: ${cook.id}, ${cook.email})`);
            totalSeeded += count;
        }

        console.log(`🎉 Done! Seeded ${totalSeeded} orders total. Rebuild the app and open Manage Orders.`);

    } catch (err) {
        console.error('❌ Error seeding orders:', err.message);
        console.error(err);
        process.exit(1);
    } finally {
        db.end();
    }
}

seedOrders();
