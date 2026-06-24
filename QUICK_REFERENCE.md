# TiffinCraft - Quick Reference Card

## 🚀 One-Line Commands

### Build APK (VS Code)
Press: `Ctrl+Shift+B`

### Build APK (Terminal)
```powershell
cd frontend; .\gradlew.bat assembleDebug
```

### Start Backend
```powershell
cd backend; npm run dev
```

### APK Location
```
frontend\app\build\outputs\apk\debug\app-debug.apk
```

---

## 📱 Install APK (Easiest Way)

1. Copy `app-debug.apk` to phone
2. Open file on phone
3. Tap "Install"
4. Done!

---

## 🔗 Important URLs

| Service | URL |
|---------|-----|
| Backend | http://localhost:5000 |
| Health Check | http://localhost:5000/api/health |
| For Android Emulator | http://10.0.2.2:5000 |
| For Physical Device | http://YOUR_IP:5000 |

Find your IP: `ipconfig` (look for IPv4 Address)

---

## ✅ Current Status

- ✅ MySQL Database: Running (port 3306)
- ✅ Backend Server: Running (port 5000)
- ✅ Android APK: Built successfully
- 📱 Next: Install APK on phone!

---

## 📝 Quick Checks

**Is backend running?**
```powershell
curl http://localhost:5000/api/health
```

**Is database running?**
```powershell
C:\xampp\mysql\bin\mysql.exe -uroot -e "USE tiffincraft; SHOW TABLES;"
```

**Java version?**
```powershell
java --version
```
Should show: OpenJDK 21

---

## 🛠️ VS Code Tasks

`Ctrl+Shift+P` → `Tasks: Run Task` →

- Build Android APK
- Clean and Build Android APK  
- Start Backend Server
- Install APK to Device (ADB)

---

## 🔧 If Something Breaks

**Backend won't start:**
- Check if port 5000 is in use: `netstat -ano | findstr :5000`
- Kill process and restart

**Build fails:**
```powershell
cd frontend
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

**MySQL not running:**
- Start XAMPP Control Panel
- Start MySQL service

---

## 📚 Documentation Files

- `README_VSCODE.md` - Full VS Code guide
- `INSTALL_APK.md` - APK installation guide
- `VS_CODE_ANDROID_SETUP.md` - Android SDK setup
- `RUNNING_INSTRUCTIONS.md` - General running instructions

---

**Need help? Check the documentation files above! 📖**
