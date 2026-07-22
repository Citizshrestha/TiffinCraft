import dotenv from 'dotenv';
import mysql from 'mysql2/promise';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: path.join(__dirname, '..', '.env') });

async function runChatMigration() {
    let connection;

    try {
        console.log('🔄 Connecting to database...');
        
        connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'tiffincraft',
            multipleStatements: true
        });

        console.log('✅ Connected to database');

        // Read the migration SQL file
        const sqlFilePath = path.join(__dirname, 'migration_chat_tables.sql');
        const sqlContent = fs.readFileSync(sqlFilePath, 'utf8');

        console.log('🔄 Running chat tables migration...');

        // Execute the migration
        await connection.query(sqlContent);

        console.log('✅ Chat tables migration completed successfully!');
        console.log('\n📊 Tables created:');
        console.log('   - conversations (customer <-> cook pairs)');
        console.log('   - chat_messages (text messages and call logs)');

        // Verify tables were created
        const [tables] = await connection.query(`
            SELECT TABLE_NAME 
            FROM information_schema.TABLES 
            WHERE TABLE_SCHEMA = ? 
            AND TABLE_NAME IN ('conversations', 'chat_messages')
        `, [process.env.DB_NAME || 'tiffincraft']);

        console.log('\n✅ Verified tables exist:');
        tables.forEach(table => {
            console.log(`   ✓ ${table.TABLE_NAME}`);
        });

    } catch (error) {
        console.error('❌ Migration failed:', error.message);
        console.error('Error details:', error);
        process.exit(1);
    } finally {
        if (connection) {
            await connection.end();
            console.log('\n✅ Database connection closed');
        }
    }
}

// Run the migration
runChatMigration();
