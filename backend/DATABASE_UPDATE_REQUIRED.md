# ⚠️ IMPORTANT: Database Schema Update Required

## Issue
The current database schema is missing required tables and columns. This needs to be fixed before the backend API will work correctly.

## Missing Components
1. **cook_profiles table** is missing columns:
   - `kitchen_name`
   - `food_type`
   - `description`
   - `capacity_per_day`
   - `is_approved`

2. **meals table** doesn't exist

3. **orders, order_items, reviews tables** don't exist (needed for future functionality)

## How to Fix

### Option 1: Using MySQL Workbench (RECOMMENDED)

1. Open **MySQL Workbench**
2. Connect to your MySQL server
3. Open the file: `backend/database/migration_update_schema.sql`
4. Click the **Execute** button (⚡ icon) to run the entire script
5. Verify all tables are created successfully

### Option 2: Using MySQL Command Line

If you can access MySQL command line directly:

```bash
cd backend
mysql -u root -p tiffincraft < database/migration_update_schema.sql
```

Enter your MySQL password when prompted.

### Option 3: Copy and Paste in MySQL Client

1. Open your MySQL client (phpMyAdmin, DBeaver, etc.)
2. Select the `tiffincraft` database
3. Open `backend/database/migration_update_schema.sql`
4. Copy the entire content
5. Paste and execute in your MySQL client

## Verification

After running the migration, verify with these queries:

```sql
USE tiffincraft;

-- Check all tables exist
SHOW TABLES;

-- Should show: cook_profiles, meals, order_items, orders, reviews, users

-- Check cook_profiles structure
DESCRIBE cook_profiles;

-- Should include: kitchen_name, food_type, description, capacity_per_day, is_approved

-- Check meals table exists
DESCRIBE meals;
```

## After Migration

Once the migration is complete:

1. **Restart the backend server** (Ctrl+C and `npm start` or `npm run dev`)
2. **Test the API endpoints** using the test script or cURL commands
3. **Frontend should now work** with all API calls

## Testing After Migration

Run these commands to verify everything works:

```bash
# Test registration
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"full_name":"Test User","email":"test@example.com","phone":"1234567890","password":"test123","role":"cook"}'

# Test login
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Copy the token from login response and use it below
TOKEN="your_token_here"

# Test cook profile setup
curl -X POST http://localhost:5000/api/cook/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"kitchen_name":"Test Kitchen","food_type":"Indian","description":"Great food","capacity_per_day":50}'

# Test add meal
curl -X POST http://localhost:5000/api/meals \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Test Meal","description":"Delicious","price":100.00,"category":"Main Course"}'

# Test get meals (public - no auth needed)
curl http://localhost:5000/api/meals
```

All should return `"success": true` responses.

---

**DO NOT PROCEED** with testing until this migration is complete!
