import db from './config/db.js';

const customerNotifications = [
    {
        title: "Welcome to TiffinCraft!",
        message: "We're glad to have you here. Explore our delicious home-cooked meals.",
        type: "system"
    },
    {
        title: "Special Offer 🎉",
        message: "Get 20% off on your first order. Use code FIRST20 at checkout.",
        type: "promo"
    },
    {
        title: "Order Status Update",
        message: "Your recent order has been confirmed and is being prepared by the cook.",
        type: "order_status"
    }
];

const cookNotifications = [
    {
        title: "Welcome to TiffinCraft Kitchen!",
        message: "We're glad to have you here. Start adding your delicious meals.",
        type: "system"
    },
    {
        title: "New Review Received ⭐",
        message: "A customer left a 5-star review on your Special Thali!",
        type: "review"
    },
    {
        title: "Weekly Payout Processed",
        message: "Your earnings for this week have been successfully transferred to your bank account.",
        type: "system"
    }
];

// Fetch all users to seed notifications for them
db.query('SELECT id, role FROM users', (err, users) => {
    if (err) {
        console.error('Error fetching users:', err);
        process.exit(1);
    }

    if (users.length === 0) {
        console.log('No users found in database to seed notifications for.');
        process.exit(0);
    }

    let insertedCount = 0;
    // We assume 3 notifications per user
    const totalToInsert = users.length * 3;

    users.forEach(user => {
        const notifications = user.role === 'cook' ? cookNotifications : customerNotifications;
        
        notifications.forEach(notification => {
            const sql = 'INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)';
            db.query(sql, [user.id, notification.title, notification.message, notification.type], (insertErr) => {
                if (insertErr) {
                    console.error('Error inserting notification for user', user.id, ':', insertErr);
                }
                insertedCount++;
                if (insertedCount === totalToInsert) {
                    console.log(`Successfully seeded ${insertedCount} dummy notifications across ${users.length} users.`);
                    process.exit(0);
                }
            });
        });
    });
});
