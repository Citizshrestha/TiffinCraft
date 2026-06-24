# TiffinCraft Backend Setup Instructions

## Prerequisites
- MySQL Server installed and running
- Node.js (v14 or higher)
- npm

## Database Setup

### Option 1: Using MySQL Command Line
```bash
# Login to MySQL
mysql -u root -p

# Then run these commands:
CREATE DATABASE IF NOT EXISTS tiffincraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tiffincraft;

# Create users table
CREATE TABLE IF NOT EXISTS users (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

# Create cook_profiles table
CREATE TABLE IF NOT EXISTS cook_profiles (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Option 2: Copy-paste from setup.sql
Open `database/setup.sql` and copy-paste the SQL commands into your MySQL client.

## Backend Configuration

1. Update `.env` file with your MySQL credentials:
```
PORT=5000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password_here
DB_NAME=tiffincraft
DB_PORT=3306
JWT_SECRET=tiffincraft_secret_key_2026
```

2. Install dependencies:
```bash
npm install
```

3. Start the server:
```bash
npm run dev
```

The server should start on http://localhost:5000

## Testing the API

### Register User
```bash
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "full_name": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "password": "password123",
    "role": "customer"
  }'
```

### Login User
```bash
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

## Android App Configuration

Update the BASE_URL in `RetrofitClient.java`:
- For Android Emulator: `http://10.0.2.2:5000/api/`
- For physical device: `http://YOUR_COMPUTER_IP:5000/api/`

Make sure your computer's firewall allows connections on port 5000.
