import db from '../config/db.js';

async function fixDatabase() {
    try {
        console.log('🔄 Fixing database schema...');

        await db.promise().query('DROP TABLE IF EXISTS cook_profiles');
        await db.promise().query('DROP TABLE IF EXISTS users');
        console.log('✅ Old tables dropped');

        await db.promise().query(`
            CREATE TABLE users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                full_name VARCHAR(100) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                phone VARCHAR(20) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role ENUM('customer', 'cook') NOT NULL,
                is_active BOOLEAN DEFAULT TRUE,
                profile_image VARCHAR(500),
                address TEXT,
                latitude DECIMAL(10, 8),
                longitude DECIMAL(11, 8),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_email (email),
                INDEX idx_phone (phone),
                INDEX idx_role (role)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        console.log('✅ Users table created with correct schema');

        await db.promise().query(`
            CREATE TABLE cook_profiles (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                bio TEXT,
                specialties TEXT,
                years_experience INT,
                rating DECIMAL(3, 2) DEFAULT 0.00,
                total_orders INT DEFAULT 0,
                is_verified BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY unique_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        console.log('✅ Cook profiles table created');

        const [columns] = await db.promise().query('DESCRIBE users');
        console.log('\n📋 Users table structure:');
        columns.forEach(col => {
            console.log(`  - ${col.Field}: ${col.Type}`);
        });

        console.log('\n✅ Database schema fixed successfully!');
        process.exit(0);
    } catch (error) {
        console.error('❌ Error fixing database:', error.message);
        process.exit(1);
    }
}

fixDatabase();
