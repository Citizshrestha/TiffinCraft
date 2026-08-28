/**
 * Reset Test Data Script (Auto-confirm)
 * 
 * Resets all subscription, order, payment, and earnings data
 * while keeping users, cook profiles, and meals intact
 * 
 * WARNING: This runs automatically without confirmation!
 */

import mysql from 'mysql2/promise';
import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { readFileSync } from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Load environment variables
dotenv.config({ path: join(__dirname, '..', '.env') });

async function resetTestData() {
    console.log('\n========================================');
    console.log('  TiffinCraft Test Data Reset');
    console.log('========================================\n');

    // Database configuration
    const dbConfig = {
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 3306,
        user: process.env.DB_USER || 'root',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'tiffincraft',
        multipleStatements: true
    };

    console.log('Database Configuration:');
    console.log(`  Host: ${dbConfig.host}`);
    console.log(`  Port: ${dbConfig.port}`);
    console.log(`  User: ${dbConfig.user}`);
    console.log(`  Database: ${dbConfig.database}\n`);

    console.log('\x1b[33m%s\x1b[0m', 'Resetting test data automatically...\n');

    let connection;
    try {
        // Read SQL file
        const sqlFile = join(__dirname, 'reset_test_data.sql');
        const sql = readFileSync(sqlFile, 'utf8');

        // Connect to database
        console.log('Connecting to database...');
        connection = await mysql.createConnection(dbConfig);
        console.log('\x1b[32m%s\x1b[0m', '✓ Connected to database\n');

        // Execute reset script
        console.log('Executing reset script...');
        await connection.query(sql);
        console.log('\x1b[32m%s\x1b[0m', '✓ Reset script executed\n');

        // Verify reset
        console.log('Verifying reset...');
        const [results] = await connection.query(`
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

        process.exit(0);

    } catch (error) {
        console.error('\n\x1b[31m%s\x1b[0m', 'Error during reset:');
        console.error(error.message);
        console.log('\n\x1b[33m%s\x1b[0m', 'Troubleshooting:');
        console.log('  1. Verify database credentials in backend/.env');
        console.log('  2. Check if MySQL service is running');
        console.log('  3. Ensure the database exists\n');
        process.exit(1);
    } finally {
        if (connection) {
            await connection.end();
        }
    }
}

// Run the reset
resetTestData().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});
