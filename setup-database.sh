#!/bin/bash

echo "========================================"
echo "TiffinCraft Database Setup"
echo "========================================"
echo ""

echo "Checking MySQL connection..."
if ! command -v mysql &> /dev/null; then
    echo "ERROR: MySQL is not installed or not in PATH"
    echo "Please install MySQL Server"
    exit 1
fi

echo ""
echo "Creating database and importing schema..."
echo ""

mysql -u root -pDRSGAMING123 -e "DROP DATABASE IF EXISTS tiffincraft; CREATE DATABASE tiffincraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Could not connect to MySQL"
    echo "Please check:"
    echo "1. MySQL service is running"
    echo "2. Password is correct (DRSGAMING123)"
    echo "3. Root user has proper permissions"
    echo ""
    exit 1
fi

echo "Database created successfully!"
echo ""

echo "Importing complete schema..."
mysql -u root -pDRSGAMING123 tiffincraft < backend/database/complete_schema.sql 2>/dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: Could not import schema"
    exit 1
fi

echo ""
echo "Applying schema fixes..."
mysql -u root -pDRSGAMING123 tiffincraft < backend/database/migration_fix_schema_complete.sql 2>/dev/null
if [ $? -ne 0 ]; then
    echo "WARNING: Schema fixes could not be applied (this is normal if already applied)"
fi

echo ""
echo "========================================"
echo "Database setup completed successfully!"
echo "========================================"
echo ""
echo "Verifying tables..."
mysql -u root -pDRSGAMING123 tiffincraft -e "SHOW TABLES;"

echo ""
echo "Database is ready!"
