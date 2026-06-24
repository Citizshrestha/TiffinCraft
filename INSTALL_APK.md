# 📱 TiffinCraft APK Installation Guide

## ✅ Build Successful!

**APK Location:** `C:\Users\ASUS\TiffinCraft\frontend\app\build\outputs\apk\debug\app-debug.apk`  
**Size:** ~17 MB  
**Build Date:** Just now!

---

## Installation Methods

### Method 1: USB Installation (Recommended)

1. **Enable USB Debugging on your Android phone:**
   - Go to `Settings` → `About Phone`
   - Tap `Build Number` 7 times (Developer mode activated)
   - Go back to `Settings` → `Developer Options`
   - Enable `USB Debugging`

2. **Connect phone to PC via USB**

3. **Install using ADB (if you have Android SDK):**
   ```powershell
   cd C:\Users\ASUS\TiffinCraft\frontend
   C:\android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Method 2: Direct Transfer (Easiest - No SDK Required)

1. **Copy APK to your phone:**
   - Connect phone via USB
   - Copy `app-debug.apk` from:
     `C:\Users\ASUS\TiffinCraft\frontend\app\build\outputs\apk\debug\app-debug.apk`
   - Paste to phone's `Downloads` folder

2. **Install on phone:**
   - Open `Files` or `My Files` app on your phone
   - Go to `Downloads` folder
   - Tap on `app-debug.apk`
   - Tap `Install` (you may need to allow "Install from Unknown Sources")
   - Once installed, tap `Open`

### Method 3: Cloud Transfer

1. **Upload APK to cloud:**
   - Upload to Google Drive / Dropbox / OneDrive
   - Or email it to yourself

2. **Download on phone:**
   - Open link on your phone
   - Download the APK
   - Install as described in Method 2

### Method 4: Share via Bluetooth/Nearby Share

1. Right-click `app-debug.apk`
2. Select `Send to` → `Bluetooth device` or `Nearby Share`
3. Select your phone
4. Accept transfer on phone
5. Install the APK

---

## Important Configuration

**Before running the app**, make sure:

### Backend Server is Running
```powershell
cd C:\Users\ASUS\TiffinCraft\backend
npm run dev
```

### Find Your PC's IP Address
```powershell
ipconfig
```
Look for `IPv4 Address` (e.g., 192.168.1.100)

### Update App to Use Your IP

If you're testing on a physical device (not emulator), the app needs to connect to your PC's IP instead of localhost.

**Option A: Using Same WiFi Network**
- Make sure your phone and PC are on the same WiFi
- Backend should be accessible at: `http://YOUR_IP:5000`
- Example: `http://192.168.1.100:5000`

**Option B: Using USB with Port Forwarding**
If you have ADB installed:
```powershell
C:\android\platform-tools\adb.exe reverse tcp:5000 tcp:5000
```
This makes `http://localhost:5000` on the phone point to your PC.

---

## Quick Build Commands (for VS Code)

### Rebuild APK
```powershell
cd C:\Users\ASUS\TiffinCraft\frontend
.\gradlew.bat assembleDebug
```

### Clean and Rebuild
```powershell
cd C:\Users\ASUS\TiffinCraft\frontend
.\gradlew.bat clean assembleDebug
```

### View Build Output
APK is always created at: `app\build\outputs\apk\debug\app-debug.apk`

---

## Troubleshooting

### "App not installed" error
- Enable "Install from Unknown Sources" in phone settings
- Or try: Settings → Apps → Special Access → Install Unknown Apps → Enable for Files/Chrome

### Can't connect to backend
- Check backend is running: `curl http://localhost:5000/api/health`
- Check firewall isn't blocking port 5000
- On physical device: Use your PC's IP, not localhost
- Make sure phone and PC are on same network

### App crashes on startup
- Check Android logs: `C:\android\platform-tools\adb.exe logcat`
- Make sure minimum Android version is met (API 26 / Android 8.0+)

---

## Summary

✅ **Backend:** Running on http://localhost:5000  
✅ **Database:** MySQL running with tiffincraft database  
✅ **Android APK:** Built successfully at `app\build\outputs\apk\debug\app-debug.apk`  

**Next Step:** Transfer APK to your phone and install it! 🚀
