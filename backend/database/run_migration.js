import db from '../config/db.js';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const migrationFile = join(__dirname, 'migrations', 'add_in_subscription_to_meals.sql');

async function runMigration() {
    try {
        console.log('📦 Running migration: add_in_subscription_to_meals.sql');
        
        const sql = readFileSync(migrationFile, 'utf8');
        const statements = sql
            .split(';')
            .map(s => s.trim())
            .filter(s => s.length > 0 && !s.startsWith('--'));

        for (const statement of statements) {
            if (statement.trim()) {
                console.log(`   Executing: ${statement.substring(0, 80)}...`);
                await db.promise().query(statement);
            }
        }

        console.log('✅ Migration completed successfully!');
        process.exit(0);
    } catch (error) {
        console.error('❌ Migration failed:', error.message);
        process.exit(1);
    }
}

runMigration();
