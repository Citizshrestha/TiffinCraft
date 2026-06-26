# 🎉 TiffinCraft - Running from VS Code (Without Android Studio)

## ✅ Setup Complete!

Your project is now configured to build Android APKs directly from VS Code!

---

## 🚀 Quick Start Guide

### 1. Build Android APK in VS Code

**Method A: Using VS Code Tasks (Recommended)**
1. Press `Ctrl+Shift+P`
2. Type: `Tasks: Run Task`
3. Select: `Build Android APK`
4. Wait for build to complete
5. APK will be at: `frontend/app/build/outputs/apk/debug/app-debug.apk`

**Method B: Using Terminal**
```powershell
cd frontend
.\gradlew.bat assembleDebug
```

### 2. Install APK on Your Phone

**Easiest Way (No SDK needed):**
1. Copy `frontend/app/build/outputs/apk/debug/app-debug.apk`
2. Transfer to your phone (USB/Bluetooth/Email/Cloud)
3. Open the file on your phone
4. Tap "Install"
5. Done! 🎉

**With ADB (if you have Android SDK):**
1. Connect phone via USB (enable USB debugging)
2. Run VS Code task: `Install APK to Device (ADB)`
3. Or use terminal: `C:\android\platform-tools\adb.exe install -r frontend\app\build\outputs\apk\debug\app-debug.apk`

### 3. Start Backend Server

**Method A: Using VS Code Tasks**
1. Press `Ctrl+Shift+P`
2. Type: `Tasks: Run Task`
3. Select: `Start Backend Server`

**Method B: Using Terminal**
```powershell
cd backend
npm run dev
```

---

## 📋 Available VS Code Tasks

Press `Ctrl+Shift+P` → `Tasks: Run Task` → Select:

| Task | Description |
|------|-------------|
| **Build Android APK** | Build debug APK (default build task: `Ctrl+Shift+B`) |
| **Clean and Build Android APK** | Clean and rebuild APK |
| **Start Backend Server** | Start Node.js backend server |
| **Install APK to Device (ADB)** | Install APK via ADB (requires Android SDK) |
| **Build and Install APK** | Build and install in one go |

---

## 🛠️ Current Setup

### Backend (Node.js + Express)
- **Status:** ✅ Running on port 5000
- **URL:** http://localhost:5000
- **Database:** MySQL (XAMPP) - tiffincraft database
- **Start Command:** `cd backend && npm run dev`

### Frontend (Android)
- **Build Tool:** Gradle
- **Java Version:** OpenJDK 21
- **Min SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 14 (API 36)
- **Build Command:** `cd frontend && .\gradlew.bat assembleDebug`

### APK Output Location
```
frontend\app\build\outputs\apk\debug\app-debug.apk
```

---

## 🔧 Configuration Files Updated

✅ `frontend/gradle.properties` - Java home configured  
✅ `frontend/local.properties` - Android SDK path  
✅ `frontend/app/build.gradle.kts` - Java 21 compatibility  
✅ `.vscode/tasks.json` - Build tasks configured  

---

## 📱 Testing the App

### On Physical Device (Same WiFi Network)

1. **Find your PC's IP:**
   ```powershell
   ipconfig
   ```
   Look for IPv4 Address (e.g., 192.168.1.100)

2. **Update app configuration** (if needed):
   - Backend should be accessible at: `http://YOUR_IP:5000`
   - Make sure firewall allows connections on port 5000

3. **Install and run the APK**

### With ADB Port Forwarding

If you have ADB installed:
```powershell
C:\android\platform-tools\adb.exe reverse tcp:5000 tcp:5000
```
This makes `localhost:5000` on the phone point to your PC.

---

## 🎯 Development Workflow

### Typical Development Cycle:

1. **Code in VS Code** (edit Java/Kotlin files)
2. **Build APK:** Press `Ctrl+Shift+B` (or run task)
3. **Transfer APK to phone** (USB/Cloud/Bluetooth)
4. **Install and test** on phone
5. **Make changes and repeat**

### Making Backend Changes:

1. Edit backend files in VS Code
2. Backend auto-reloads (nodemon)
3. Test API: `curl http://localhost:5000/api/health`

### Making Frontend Changes:

1. Edit Android files
2. Rebuild APK: `Ctrl+Shift+B`
3. Reinstall on device

---

## 📚 Useful Commands

### Backend Commands
```powershell
cd backend

# Start server
npm run dev

# Check health
curl http://localhost:5000/api/health

# View logs
# (logs appear in terminal)
```

### Frontend Commands
```powershell
cd frontend

# Build APK
.\gradlew.bat assembleDebug

# Clean build
.\gradlew.bat clean assembleDebug

# View dependencies
.\gradlew.bat dependencies

# View all tasks
.\gradlew.bat tasks
```

### ADB Commands (if you have Android SDK)
```powershell
# Check connected devices
C:\android\platform-tools\adb.exe devices

# Install APK
C:\android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

# View logs
C:\android\platform-tools\adb.exe logcat | Select-String "TiffinCraft"

# Port forwarding
C:\android\platform-tools\adb.exe reverse tcp:5000 tcp:5000

# Uninstall app
C:\android\platform-tools\adb.exe uninstall com.tiffincraft.app
```

---

## 🐛 Troubleshooting

### Build Fails
- Make sure Java 21 is installed: `java --version`
- Check gradle.properties has correct Java path
- Try clean build: `.\gradlew.bat clean assembleDebug`

### App Won't Install
- Enable "Install from Unknown Sources" on your phone
- Try uninstalling old version first
- Make sure Android version is 8.0 or higher

### Can't Connect to Backend
- Check backend is running: `curl http://localhost:5000`
- On physical device: use PC's IP, not localhost
- Check firewall settings
- Make sure phone and PC are on same WiFi

### "Not on classpath" Warning in VS Code
- This is just a warning, ignore it
- The APK builds fine from command line
- Or install Android extension pack for VS Code

---

## 🎓 Next Steps

1. ✅ Backend is running
2. ✅ Database is set up
3. ✅ APK builds successfully
4. 📱 **Install APK on your phone**
5. 🧪 **Test the app!**

---

## 📖 Additional Resources

- **Full Setup Guide:** `VS_CODE_ANDROID_SETUP.md`
- **APK Installation Guide:** `INSTALL_APK.md`
- **Running Instructions:** `RUNNING_INSTRUCTIONS.md`

---

## ✨ Summary

You can now:
- ✅ Build Android APKs from VS Code (no Android Studio needed)
- ✅ Use VS Code tasks for quick builds (`Ctrl+Shift+B`)
- ✅ Run backend server from VS Code
- ✅ Install APKs on your phone
- ✅ Develop in your favorite editor!

**Happy Coding! 🚀**
