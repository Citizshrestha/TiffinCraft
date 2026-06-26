# Backend Start Script

echo "Starting TiffinCraft Backend..."
echo ""
echo "Checking prerequisites..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js first."
    exit 1
fi

echo "✅ Node.js version: $(node --version)"

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed."
    exit 1
fi

echo "✅ npm version: $(npm --version)"

# Navigate to backend directory
cd backend

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo ""
    echo "📦 Installing dependencies..."
    npm install
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo ""
    echo "⚠️  .env file not found. Creating default .env file..."
    cat > .env << EOF
PORT=5000
MONGODB_URI=mongodb://localhost:27017/tiffincraft
JWT_SECRET=tiffincraft_secret_key_2024_change_in_production
NODE_ENV=development
EOF
    echo "✅ Created .env file"
fi

echo ""
echo "🚀 Starting server..."
echo ""

# Start the server
npm start
