import db from './config/db.js';

async function createAdminRecordsTable() {
    try {
        await db.promise().query(`
            CREATE TABLE IF NOT EXISTS admin_records (
                id INT PRIMARY KEY AUTO_INCREMENT,
                admin_id INT NOT NULL,
                action_type VARCHAR(100) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        console.log('admin_records table created successfully.');
    } catch (error) {
        console.error('Failed to create admin_records table:', error.message);
    } finally {
        process.exit(0);
    }
}

createAdminRecordsTable();
