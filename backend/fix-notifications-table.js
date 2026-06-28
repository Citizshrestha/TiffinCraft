import mysql from 'mysql2/promise';
import dotenv from 'dotenv';

dotenv.config();

async function fixNotificationsTable() {
    let connection;
    
    try {
        console.log('Connecting to database...');
        
        connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'tiffincraft'
        });

        console.log('Connected successfully!');

        // Check current table structure
        console.log('\nChecking notifications table structure...');
        const [columns] = await connection.query('DESCRIBE notifications');
        
        console.log('Current columns:');
        columns.forEach(col => {
            console.log(`  - ${col.Field} (${col.Type})`);
        });

        // Check if order_id column exists
        const hasOrderId = columns.some(col => col.Field === 'order_id');

        if (!hasOrderId) {
            console.log('\n⚠️  order_id column is missing. Adding it...');
            
            await connection.query(`
                ALTER TABLE notifications 
                ADD COLUMN order_id INT NULL AFTER is_read,
                ADD CONSTRAINT fk_notifications_order 
                FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
            `);

            console.log('✅ order_id column added successfully!');
        } else {
            console.log('\n✅ order_id column already exists!');
        }

        // Verify the fix
        console.log('\nFinal table structure:');
        const [finalColumns] = await connection.query('DESCRIBE notifications');
        finalColumns.forEach(col => {
            console.log(`  - ${col.Field} (${col.Type}) ${col.Null} ${col.Key} ${col.Default || ''}`);
        });

        console.log('\n✅ Notifications table is now correct!');

    } catch (error) {
        console.error('❌ Error:', error.message);
        process.exit(1);
    } finally {
        if (connection) {
            await connection.end();
            console.log('\nDatabase connection closed.');
        }
    }
}

fixNotificationsTable();
