/**
 * Reset Test Data Script - Direct using db config
 */

import mysql from 'mysql2';
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load .env from backend directory
dotenv.config({ path: path.join(__dirname, '..', '.env') });

async function resetTestData() {
    console.log('\n========================================');
    console.log('  TiffinCraft Test Data Reset');
    console.log('========================================\n');

    console.log('\x1b[33m%s\x1b[0m', 'WARNING: Resetting all test data...\n');

    // Setup database connection with promise
    const useSSL = process.env.DB_USE_SSL === "true";
    const poolConfig = {
        host: process.env.DB_HOST || 'localhost',
        user: process.env.DB_USER || 'root',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'tiffincraft',
        port: parseInt(process.env.DB_PORT || '3306'),
        connectionLimit: 10,
    };

    console.log(`DB Config: ${poolConfig.host}:${poolConfig.port} / ${poolConfig.database}\n`);

    if (useSSL) {
        poolConfig.ssl = { minVersion: "TLSv1.2" };
        const caPath = path.join(__dirname, "../certs/isrgrootx1.pem");
        try {
            poolConfig.ssl.ca = fs.readFileSync(caPath);
        } catch (err) {
            console.warn(`⚠️  CA file not readable - using Node's bundled CAs`);
        }
    }

    const pool = mysql.createPool(poolConfig).promise();

    try {
        console.log('Connected to database\n');

        // Disable foreign key checks
        await pool.query('SET FOREIGN_KEY_CHECKS = 0');
        console.log('✓ Disabled foreign key checks\n');

        // Reset subscription data
        console.log('Resetting subscription data...');
        await pool.query('TRUNCATE TABLE subscription_daily_log');
        await pool.query('TRUNCATE TABLE subscription_payment_events');
        await pool.query('TRUNCATE TABLE subscriptions');
        await pool.query('TRUNCATE TABLE subscription_plan_items');
        await pool.query('TRUNCATE TABLE subscription_plans');
        console.log('\x1b[32m%s\x1b[0m', '✓ Subscription data cleared\n');

        // Reset order data
        console.log('Resetting order data...');
        await pool.query('TRUNCATE TABLE order_items');
        await pool.query('TRUNCATE TABLE orders');
        console.log('\x1b[32m%s\x1b[0m', '✓ Order data cleared\n');

        // Reset cart data
        console.log('Resetting cart data...');
        await pool.query('TRUNCATE TABLE cart_items');
        await pool.query('TRUNCATE TABLE cart');
        console.log('\x1b[32m%s\x1b[0m', '✓ Cart data cleared\n');

        // Reset payment & financial data
        console.log('Resetting payment data...');
        await pool.query('TRUNCATE TABLE payments');
        await pool.query('TRUNCATE TABLE refund_requests');
        await pool.query('TRUNCATE TABLE commission_settlements');
        console.log('\x1b[32m%s\x1b[0m', '✓ Payment data cleared\n');

        // Reset engagement data
        console.log('Resetting engagement data...');
        await pool.query('TRUNCATE TABLE reviews');
        await pool.query('TRUNCATE TABLE favorites');
        await pool.query('TRUNCATE TABLE notifications');
        await pool.query('TRUNCATE TABLE chat_messages');
        await pool.query('TRUNCATE TABLE conversations');
        console.log('\x1b[32m%s\x1b[0m', '✓ Engagement data cleared\n');

        // Reset custom requests & deals
        console.log('Resetting custom requests...');
        await pool.query('TRUNCATE TABLE custom_meal_requests');
        await pool.query('TRUNCATE TABLE combo_deal_items');
        await pool.query('TRUNCATE TABLE combo_deals');
        console.log('\x1b[32m%s\x1b[0m', '✓ Custom requests cleared\n');

        // Reset referrals
        console.log('Resetting referrals...');
        await pool.query('TRUNCATE TABLE referrals');
        console.log('\x1b[32m%s\x1b[0m', '✓ Referrals cleared\n');

        // Re-enable foreign key checks
        await pool.query('SET FOREIGN_KEY_CHECKS = 1');
        console.log('✓ Re-enabled foreign key checks\n');

        // Verify reset
        console.log('Verifying reset...');
        const [results] = await pool.query(`
            SELECT 'Subscriptions' AS table_name, COUNT(*) AS count FROM subscriptions
            UNION ALL
            SELECT 'Orders', COUNT(*) FROM orders
            UNION ALL
            SELECT 'Payments', COUNT(*) FROM payments
            UNION ALL
            SELECT 'Commission Settlements', COUNT(*) FROM commission_settlements
            UNION ALL
            SELECT 'Reviews', COUNT(*) FROM reviews
            UNION ALL
            SELECT 'Notifications', COUNT(*) FROM notifications
            UNION ALL
            SELECT 'Chat Messages', COUNT(*) FROM chat_messages
            UNION ALL
            SELECT 'Cart Items', COUNT(*) FROM cart_items
            UNION ALL
            SELECT '---' AS table_name, NULL AS count
            UNION ALL
            SELECT 'Users (preserved)', COUNT(*) FROM users
            UNION ALL
            SELECT 'Cook Profiles (preserved)', COUNT(*) FROM cook_profiles
            UNION ALL
            SELECT 'Meals (preserved)', COUNT(*) FROM meals
        `);

        console.log('\nDatabase Status:');
        console.log('─────────────────────────────────────────');
        results.forEach(row => {
            if (row.table_name === '---') {
                console.log('─────────────────────────────────────────');
            } else if (row.table_name.includes('preserved')) {
                console.log(`\x1b[32m${row.table_name.padEnd(35)}: ${row.count}\x1b[0m`);
            } else {
                console.log(`${row.table_name.padEnd(35)}: ${row.count}`);
            }
        });
        console.log('─────────────────────────────────────────\n');

        console.log('\x1b[32m%s\x1b[0m', '========================================');
        console.log('\x1b[32m%s\x1b[0m', '  Reset Completed Successfully!');
        console.log('\x1b[32m%s\x1b[0m', '========================================\n');
        console.log('\x1b[32m%s\x1b[0m', '✓ All test data has been cleared');
        console.log('\x1b[32m%s\x1b[0m', '✓ Users, cook profiles, and meals preserved');
        console.log('\x1b[32m%s\x1b[0m', '✓ System ready for fresh testing\n');

        await pool.end();
        process.exit(0);

    } catch (error) {
        console.error('\n\x1b[31m%s\x1b[0m', 'Error during reset:');
        console.error(error.message);
        console.log('\n\x1b[33m%s\x1b[0m', 'Troubleshooting:');
        console.log('  1. Verify database connection');
        console.log('  2. Check if database service is running');
        console.log('  3. Ensure tables exist\n');
        await pool.end();
        process.exit(1);
    }
}

// Run the reset
resetTestData().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});
