import mysql from 'mysql2/promise';
import dotenv from 'dotenv';

dotenv.config();

async function checkAndSeedNotifications() {
    let connection;
    
    try {
        console.log('🔌 Connecting to database...');
        
        connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'tiffincraft'
        });

        console.log('✅ Connected successfully!\n');

        // Check if users exist
        console.log('👥 Checking users...');
        const [users] = await connection.query('SELECT id, full_name, role FROM users LIMIT 5');
        
        if (users.length === 0) {
            console.log('❌ No users found in database. Please create users first.');
            process.exit(1);
        }

        console.log(`Found ${users.length} users:`);
        users.forEach(user => {
            console.log(`  - User ID ${user.id}: ${user.full_name} (${user.role})`);
        });

        // Find customer users
        const customers = users.filter(u => u.role === 'customer');
        const cooks = users.filter(u => u.role === 'cook');

        if (customers.length === 0) {
            console.log('⚠️  No customer users found. Creating notifications for first user anyway...');
        }

        // Check existing notifications
        console.log('\n🔔 Checking existing notifications...');
        const [existingNotifications] = await connection.query(
            'SELECT COUNT(*) as count FROM notifications'
        );

        console.log(`Found ${existingNotifications[0].count} existing notifications`);

        // Get notification counts per user
        const [userNotifications] = await connection.query(`
            SELECT user_id, COUNT(*) as count 
            FROM notifications 
            GROUP BY user_id
        `);

        if (userNotifications.length > 0) {
            console.log('\nNotifications per user:');
            userNotifications.forEach(un => {
                const user = users.find(u => u.id === un.user_id);
                console.log(`  - User ${un.user_id} (${user?.full_name || 'Unknown'}): ${un.count} notifications`);
            });
        }

        // Seed notifications for each customer
        console.log('\n📝 Seeding notifications...');

        for (const customer of customers.length > 0 ? customers : [users[0]]) {
            const userId = customer.id;
            const userName = customer.full_name;

            console.log(`\n🎯 Creating notifications for ${userName} (ID: ${userId})...`);

            // Check if user already has notifications
            const [userNotifCount] = await connection.query(
                'SELECT COUNT(*) as count FROM notifications WHERE user_id = ?',
                [userId]
            );

            if (userNotifCount[0].count >= 5) {
                console.log(`  ⏭️  User already has ${userNotifCount[0].count} notifications. Skipping...`);
                continue;
            }

            // Seed diverse notifications
            const notifications = [
                {
                    title: '🎉 Welcome to TiffinCraft!',
                    message: 'Start exploring delicious homemade meals from local cooks in your area.',
                    type: 'system',
                    is_read: false
                },
                {
                    title: '🍛 Order Delivered Successfully',
                    message: 'Your order from Anita\'s Kitchen has been delivered. Enjoy your meal!',
                    type: 'order',
                    is_read: false
                },
                {
                    title: '🎁 Special Offer - 20% Off!',
                    message: 'Get 20% off on your next order! Use code WELCOME20 at checkout.',
                    type: 'promo',
                    is_read: false
                },
                {
                    title: '👨‍🍳 New Cook Available',
                    message: 'Chef Ramesh is now available in your area. Check out their authentic South Indian cuisine!',
                    type: 'cook',
                    is_read: true
                },
                {
                    title: '⏰ Order Ready for Pickup',
                    message: 'Your order #1234 is ready. It will be delivered in 15 minutes.',
                    type: 'order',
                    is_read: false
                },
                {
                    title: '💝 Rate Your Recent Order',
                    message: 'How was your meal from Priya\'s Kitchen? Share your feedback and help others discover great food!',
                    type: 'system',
                    is_read: false
                },
                {
                    title: '🔥 Today\'s Special Menu',
                    message: 'Check out today\'s special: Paneer Butter Masala Thali at ₹150 only!',
                    type: 'promo',
                    is_read: true
                },
                {
                    title: '📦 Order Confirmed',
                    message: 'Your order has been confirmed and is being prepared. Estimated delivery: 45 minutes.',
                    type: 'order',
                    is_read: false
                }
            ];

            let insertedCount = 0;

            for (const notif of notifications) {
                try {
                    await connection.query(
                        `INSERT INTO notifications (user_id, title, message, type, is_read, order_id, created_at) 
                         VALUES (?, ?, ?, ?, ?, ?, NOW() - INTERVAL ? HOUR)`,
                        [
                            userId, 
                            notif.title, 
                            notif.message, 
                            notif.type, 
                            notif.is_read,
                            null,
                            Math.floor(Math.random() * 24) // Random time in last 24 hours
                        ]
                    );
                    insertedCount++;
                } catch (err) {
                    console.log(`  ⚠️  Skipped duplicate: ${notif.title}`);
                }
            }

            console.log(`  ✅ Created ${insertedCount} new notifications for ${userName}`);
        }

        // Final summary
        console.log('\n📊 Final Statistics:');
        const [finalCount] = await connection.query('SELECT COUNT(*) as total FROM notifications');
        const [unreadCount] = await connection.query('SELECT COUNT(*) as unread FROM notifications WHERE is_read = FALSE');
        
        console.log(`  Total notifications: ${finalCount[0].total}`);
        console.log(`  Unread notifications: ${unreadCount[0].unread}`);

        // Show notifications per user
        const [finalUserNotifs] = await connection.query(`
            SELECT 
                u.id, 
                u.full_name, 
                u.role,
                COUNT(n.id) as notification_count,
                SUM(CASE WHEN n.is_read = FALSE THEN 1 ELSE 0 END) as unread_count
            FROM users u
            LEFT JOIN notifications n ON u.id = n.user_id
            GROUP BY u.id, u.full_name, u.role
            HAVING notification_count > 0
        `);

        console.log('\n📱 Notifications by User:');
        finalUserNotifs.forEach(un => {
            console.log(`  - ${un.full_name} (${un.role}): ${un.notification_count} total, ${un.unread_count} unread`);
        });

        console.log('\n✅ Notification seeding completed successfully!');
        console.log('\n💡 Test API endpoints:');
        console.log('   GET /api/notifications - Get all notifications');
        console.log('   GET /api/notifications/unread-count - Get unread count');
        console.log('   GET /api/customer/dashboard - Get dashboard with notifications');

    } catch (error) {
        console.error('\n❌ Error:', error.message);
        console.error(error);
        process.exit(1);
    } finally {
        if (connection) {
            await connection.end();
            console.log('\n🔌 Database connection closed.');
        }
    }
}

checkAndSeedNotifications();
