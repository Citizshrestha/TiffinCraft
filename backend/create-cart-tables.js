import mysql from 'mysql2/promise';
import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

dotenv.config();

async function createCartTables() {
    let connection;
    
    try {
        console.log('Connecting to database...');
        
        connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'tiffincraft',
            multipleStatements: true
        });

        console.log('Connected successfully!');

        // Read the SQL file
        const sqlFilePath = join(__dirname, 'database', 'create_cart_favorites_tables.sql');
        const sqlScript = fs.readFileSync(sqlFilePath, 'utf8');

        console.log('Executing migration script...');
        
        // Execute the SQL script
        await connection.query(sqlScript);

        console.log('✓ Cart and Favorites tables created successfully!');

        // Verify tables were created
        const [tables] = await connection.query(`
            SELECT TABLE_NAME 
            FROM information_schema.TABLES 
            WHERE TABLE_SCHEMA = ? 
            AND TABLE_NAME IN ('cart', 'favorites')
        `, [process.env.DB_NAME || 'tiffincraft']);

        console.log('\nCreated tables:');
        tables.forEach(table => {
            console.log(`  - ${table.TABLE_NAME}`);
        });

    } catch (error) {
        console.error('Error creating cart tables:', error.message);
        process.exit(1);
    } finally {
        if (connection) {
            await connection.end();
            console.log('\nDatabase connection closed.');
        }
    }
}

createCartTables();
