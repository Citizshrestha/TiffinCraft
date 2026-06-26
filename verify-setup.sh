#!/bin/bash

# TiffinCraft Complete Verification Script
# This script verifies that everything is set up correctly

echo "========================================="
echo "TiffinCraft Setup Verification"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Node.js
echo "1. Checking Node.js installation..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓ Node.js installed: $NODE_VERSION${NC}"
else
    echo -e "${RED}✗ Node.js not found. Please install Node.js v14+${NC}"
fi
echo ""

# Check npm
echo "2. Checking npm installation..."
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo -e "${GREEN}✓ npm installed: $NPM_VERSION${NC}"
else
    echo -e "${RED}✗ npm not found${NC}"
fi
echo ""

# Check MySQL
echo "3. Checking MySQL installation..."
if command -v mysql &> /dev/null; then
    MYSQL_VERSION=$(mysql --version)
    echo -e "${GREEN}✓ MySQL installed: $MYSQL_VERSION${NC}"
else
    echo -e "${YELLOW}⚠ MySQL command not found (may still be installed)${NC}"
fi
echo ""

# Check backend dependencies
echo "4. Checking backend dependencies..."
if [ -f "backend/package.json" ]; then
    echo -e "${GREEN}✓ package.json found${NC}"
    if [ -d "backend/node_modules" ]; then
        echo -e "${GREEN}✓ node_modules found${NC}"
    else
        echo -e "${YELLOW}⚠ node_modules not found. Run: cd backend && npm install${NC}"
    fi
else
    echo -e "${RED}✗ backend/package.json not found${NC}"
fi
echo ""

# Check .env file
echo "5. Checking environment configuration..."
if [ -f "backend/.env" ]; then
    echo -e "${GREEN}✓ .env file found${NC}"

    # Check for required variables
    if grep -q "JWT_SECRET" backend/.env; then
        echo -e "${GREEN}✓ JWT_SECRET configured${NC}"
    else
        echo -e "${RED}✗ JWT_SECRET not found in .env${NC}"
    fi

    if grep -q "DB_NAME" backend/.env; then
        echo -e "${GREEN}✓ DB_NAME configured${NC}"
    else
        echo -e "${RED}✗ DB_NAME not found in .env${NC}"
    fi
else
    echo -e "${RED}✗ .env file not found${NC}"
fi
echo ""

# Check if server is running
echo "6. Checking if backend server is running..."
if curl -s http://localhost:5000/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Backend server is running${NC}"
    HEALTH_RESPONSE=$(curl -s http://localhost:5000/api/health)
    echo "   Response: $HEALTH_RESPONSE"
else
    echo -e "${YELLOW}⚠ Backend server not responding on port 5000${NC}"
    echo "   Start with: cd backend && npm run dev"
fi
echo ""

# Check database tables
echo "7. Checking database schema..."
echo "   Manual check required - Run in MySQL:"
echo "   USE tiffincraft;"
echo "   SHOW TABLES;"
echo ""
echo "   Expected tables:"
echo "   - users"
echo "   - cook_profiles"
echo "   - meals"
echo "   - orders"
echo "   - order_items"
echo "   - reviews"
echo ""

# Check frontend files
echo "8. Checking frontend files..."
if [ -d "frontend/app" ]; then
    echo -e "${GREEN}✓ Frontend app directory found${NC}"

    if [ -f "frontend/app/src/main/java/com/tiffincraft/app/api/ApiService.java" ]; then
        echo -e "${GREEN}✓ ApiService.java found${NC}"
    else
        echo -e "${RED}✗ ApiService.java not found${NC}"
    fi

    if [ -f "frontend/app/src/main/java/com/tiffincraft/app/api/RetrofitClient.java" ]; then
        echo -e "${GREEN}✓ RetrofitClient.java found${NC}"
    else
        echo -e "${RED}✗ RetrofitClient.java not found${NC}"
    fi
else
    echo -e "${RED}✗ Frontend app directory not found${NC}"
fi
echo ""

# Summary
echo "========================================="
echo "Verification Summary"
echo "========================================="
echo ""
echo "Next steps:"
echo ""
echo "1. If backend is not running:"
echo "   cd backend && npm run dev"
echo ""
echo "2. If database tables are missing:"
echo "   - Open MySQL Workbench"
echo "   - Run: backend/database/migration_update_schema.sql"
echo ""
echo "3. Test API endpoints:"
echo "   cd backend && ./test-api.sh"
echo ""
echo "4. Update frontend base URL in RetrofitClient.java"
echo ""
echo "5. Build and run Android app"
echo ""
echo "========================================="
echo ""
