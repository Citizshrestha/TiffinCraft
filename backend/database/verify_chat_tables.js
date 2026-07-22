import dotenv from 'dotenv';
import mysql from 'mysql2/promise';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: path.join(__dirname, '..', '.env') });

async function verifyTables() {
    let connection;

    try {
        connection = await mysql.createConnection({
            host: process.env.DB_HOST || 'localhost',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'tiffincraft'
        });

        console.log('📋 Verifying chat tables structure:\n');

        // Check conversations table
        const [conversationsDesc] = await connection.query('DESCRIBE conversations');
        console.log('✅ CONVERSATIONS TABLE:');
        console.log('------------------------');
        conversationsDesc.forEach(col => {
            console.log(`  ${col.Field.padEnd(20)} ${col.Type.padEnd(25)} ${col.Key ? `[${col.Key}]` : ''}`);
        });

        // Check chat_messages table
        const [messagesDesc] = await connection.query('DESCRIBE chat_messages');
        console.log('\n✅ CHAT_MESSAGES TABLE:');
        console.log('------------------------');
        messagesDesc.forEach(col => {
            console.log(`  ${col.Field.padEnd(20)} ${col.Type.padEnd(25)} ${col.Key ? `[${col.Key}]` : ''}`);
        });

        // Check foreign keys
        const [fks] = await connection.query(`
            SELECT 
                CONSTRAINT_NAME,
                TABLE_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = ?
            AND TABLE_NAME IN ('conversations', 'chat_messages')
            AND REFERENCED_TABLE_NAME IS NOT NULL
        `, [process.env.DB_NAME || 'tiffincraft']);

        console.log('\n✅ FOREIGN KEY CONSTRAINTS:');
        console.log('----------------------------');
        fks.forEach(fk => {
            console.log(`  ${fk.TABLE_NAME}.${fk.COLUMN_NAME} → ${fk.REFERENCED_TABLE_NAME}.${fk.REFERENCED_COLUMN_NAME}`);
        });

        console.log('\n✅ Chat system tables are ready!');

    } catch (error) {
        console.error('❌ Verification failed:', error.message);
    } finally {
        if (connection) await connection.end();
    }
}

verifyTables();
