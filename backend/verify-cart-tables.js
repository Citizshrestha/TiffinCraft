import mysql from 'mysql2/promise';
import dotenv from 'dotenv';

dotenv.config();

async function verifyTables() {
    let connection;
    
    try {
        connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'tiffincraft'
        });

        const [cartRows] = await connection.query('SHOW TABLES LIKE "cart"');
        const [favRows] = await connection.query('SHOW TABLES LIKE "favorites"');
        
        console.log('Cart table exists:', cartRows.length > 0);
        console.log('Favorites table exists:', favRows.length > 0);
        
        if (cartRows.length > 0) {
            const [cartDesc] = await connection.query('DESCRIBE cart');
            console.log('\nCart table structure:');
            cartDesc.forEach(col => console.log(`  - ${col.Field} (${col.Type})`));
        }
        
    } catch (error) {
        console.error('Error:', error.message);
    } finally {
        if (connection) await connection.end();
    }
}

verifyTables();
