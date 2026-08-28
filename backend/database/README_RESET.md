# Database Reset Scripts

This directory contains scripts to reset test data for fresh testing cycles.

## What Gets Reset

The reset scripts will **DELETE** the following data:
- ✗ All subscriptions and subscription plans
- ✗ All orders and order items  
- ✗ All payments and refund requests
- ✗ All commission settlements (earnings data)
- ✗ All reviews and favorites
- ✗ All notifications
- ✗ All chat conversations and messages
- ✗ All custom meal requests and combo deals
- ✗ All cart data
- ✗ All referrals

## What Gets Preserved

The following data is **PRESERVED**:
- ✓ Users (customers, cooks, admins)
- ✓ Cook profiles
- ✓ Meals
- ✓ Platform settings
- ✓ Most recent commission rate

## Usage

### Option 1: Node.js Script (Recommended)

```bash
# From project root
node backend/database/reset_test_data_direct.js
```

This script:
- Loads database credentials from `backend/.env`
- Connects to the database (works with both TiDB Cloud and local MySQL)
- Resets all test data
- Shows verification summary

### Option 2: PowerShell Script

```powershell
# From database directory
cd backend/database
.\run_reset.ps1
```

This script:
- Prompts for confirmation
- Loads credentials from backend/.env
- Executes reset_test_data.sql using MySQL CLI
- Requires MySQL client installed

### Option 3: Direct SQL Execution

```bash
# Using MySQL CLI
mysql -h <host> -P <port> -u <user> -p<password> <database> < backend/database/reset_test_data.sql
```

## Files

- **reset_test_data.sql** - SQL script with TRUNCATE statements
- **reset_test_data_direct.js** - Node.js script (auto-confirm, uses mysql2)
- **reset_test_data.js** - Node.js script (with confirmation prompt)
- **reset_test_data_auto.js** - Alternative auto-confirm version
- **run_reset.ps1** - PowerShell wrapper script

## Verification

After running the reset, the script will display a summary showing:
- Count of cleared tables (should all be 0)
- Count of preserved data (users, meals, profiles)

Example output:
```
Database Status:
─────────────────────────────────────────
Subscriptions                      : 0
Orders                             : 0
Payments                           : 0
Commission Settlements             : 0
Reviews                            : 0
Notifications                      : 0
Chat Messages                      : 0
Cart Items                         : 0
─────────────────────────────────────────
Users (preserved)                  : 7
Cook Profiles (preserved)          : 3
Meals (preserved)                  : 5
─────────────────────────────────────────
```

## Warning

⚠️ **This operation is irreversible!** All transactional data will be permanently deleted.

Only run this on:
- Development databases
- Test databases
- When explicitly resetting for a new test cycle

**Never run on production data!**
