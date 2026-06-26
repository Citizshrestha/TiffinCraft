#!/bin/bash

echo "========================================"
echo "TiffinCraft Complete Setup and Start"
echo "========================================"
echo ""

echo "Step 1: Checking Node.js..."
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed!"
    echo "Please install Node.js from https://nodejs.org/"
    exit 1
fi
echo "✓ Node.js is installed"
echo ""

echo "Step 2: Installing backend dependencies..."
cd backend
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to install dependencies"
        exit 1
    fi
else
    echo "✓ Dependencies already installed"
fi
echo ""

echo "Step 3: Database setup..."
echo ""
echo "⚠️  IMPORTANT: Database Setup Required!"
echo ""
echo "Please complete database setup:"
echo "1. Start MySQL service"
echo "2. Import database-setup-complete.sql"
echo ""
read -p "Press Enter after database is setup..."

echo ""
echo "Step 4: Starting backend server..."
echo ""
echo "Server will start on http://localhost:5000"
echo "Press Ctrl+C to stop the server"
echo ""
echo "========================================"
echo ""

npm run dev
