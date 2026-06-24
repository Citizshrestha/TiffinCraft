# 🔧 Fix MySQL Issue - Manual Steps Required

## Problem
MySQL is failing to start due to corrupted Aria storage engine tables.

## ✅ Solution: Use XAMPP Control Panel

### Method 1: Start MySQL via XAMPP Control Panel (Recommended)

1. **Open XAMPP Control Panel**
   - Navigate to: `C:\xampp\`
   - Run: `xampp-control.exe` (as Administrator)

2. **Start MySQL**
   - Click the "Start" button next to MySQL
   - Wait for it to show "Running" in green

3. **Verify MySQL is Running**
   ```powershell
   netstat -ano | findstr :3306
   ```
   You should see something like:
   ```
   TCP    0.0.0.0:3306           0.0.0.0:0              LISTENING       XXXX
   ```

### Method 2: Repair MySQL Database (If Method 1 Fails)

1. **Stop all MySQL processes:**
   ```powershell
   taskkill /F /IM mysqld.exe
   ```

2. **Delete corrupted Aria log files:**
   ```powershell
   Remove-Item "C:\xampp\mysql\data\aria_log.*" -Force
   ```

3. **Start MySQL via XAMPP Control Panel** (Method 1 above)

### Method 3: Nuclear Option - Reinstall MySQL Data

⚠️ **WARNING: This will delete ALL your databases!**

If Methods 1 & 2 don't work:

1. Stop MySQL completely
2. Backup `C:\xampp\mysql\data\tiffincraft` folder (if it exists)
3. Delete `C:\xampp\mysql\data` folder
4. Reinstall XAMPP or restore default data folder
5. Recreate the database using:
   ```powershell
   Get-Content "C:\Users\ASUS\TiffinCraft\backend\database\complete_schema.sql" | C:\xampp\mysql\bin\mysql.exe -uroot
   ```

---

## ✅ Once MySQL is Running

1. **Verify database exists:**
   ```powershell
   C:\xampp\mysql\bin\mysql.exe -uroot -e "SHOW DATABASES;"
   ```

2. **Check tiffincraft database:**
   ```powershell
   C:\xampp\mysql\bin\mysql.exe -uroot -e "USE tiffincraft; SHOW TABLES;"
   ```

3. **Restart backend server:**
   ```powershell
   cd C:\Users\ASUS\TiffinCraft\backend
   npm run dev
   ```

---

## 🎯 Quick Check if Everything Works

Run this to test the full stack:

```powershell
# Check MySQL
C:\xampp\mysql\bin\mysql.exe -uroot -e "SELECT 1;"

# Check Backend
curl http://localhost:5000/api/health
```

If both work, you're good to go! ✅

---

## Alternative: Use Different Database

If MySQL continues to have issues, you can switch to SQLite (simpler for development):

1. Install SQLite package:
   ```powershell
   cd backend
   npm install sqlite3
   ```

2. Update `backend/config/db.js` to use SQLite instead of MySQL
3. SQLite requires no separate server process

Let me know if you need help with this approach!
