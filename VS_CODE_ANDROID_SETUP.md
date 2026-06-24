# Running Android App from VS Code (Without Android Studio)

## Prerequisites Setup

### Step 1: Install Android SDK Command-Line Tools

1. **Download SDK Command Line Tools**
   - Go to: https://developer.android.com/studio#command-line-tools-only
   - Download "Command line tools only" for Windows
   - Extract to: `C:\Android\cmdline-tools\latest`

2. **Set Environment Variables**
   ```powershell
   # Run in PowerShell as Administrator
   [System.Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Android', 'User')
   [System.Environment]::SetEnvironmentVariable('Path', $env:Path + ';C:\Android\cmdline-tools\latest\bin;C:\Android\platform-tools', 'User')
   ```

3. **Install Required SDK Components**
   ```powershell
   # Restart your terminal first to load new environment variables
   cd C:\Android\cmdline-tools\latest\bin
   
   # Accept licenses
   .\sdkmanager.bat --licenses
   
   # Install required packages
   .\sdkmanager.bat "platform-tools" "platforms;android-33" "build-tools;33.0.0" "emulator" "system-images;android-33;google_apis;x86_64"
   ```

### Step 2: Update local.properties

Update `c:\Users\ASUS\TiffinCraft\frontend\local.properties`:
```properties
sdk.dir=C\:\\Android
```

---

## Running the App

### Option A: Using Gradle Commands in VS Code Terminal

1. **Build the APK**
   ```powershell
   cd C:\Users\ASUS\TiffinCraft\frontend
   .\gradlew.bat assembleDebug
   ```
   APK will be created at: `app\build\outputs\apk\debug\app-debug.apk`

2. **Connect Physical Device**
   - Enable USB Debugging on your phone
   - Connect via USB
   - Verify connection:
     ```powershell
     C:\Android\platform-tools\adb.exe devices
     ```

3. **Install and Run**
   ```powershell
   # Install APK
   C:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
   
   # Launch app
   C:\Android\platform-tools\adb.exe shell am start -n com.tiffincraft.app/.activities.SplashActivity
   ```

### Option B: Using Emulator from Command Line

1. **Create AVD (Android Virtual Device)**
   ```powershell
   C:\Android\cmdline-tools\latest\bin\avdmanager.bat create avd -n TiffinCraft_AVD -k "system-images;android-33;google_apis;x86_64" -d pixel_5
   ```

2. **Start Emulator**
   ```powershell
   C:\Android\emulator\emulator.exe -avd TiffinCraft_AVD
   ```

3. **Build and Install**
   ```powershell
   cd C:\Users\ASUS\TiffinCraft\frontend
   .\gradlew.bat installDebug
   ```

### Option C: VS Code Extension

1. **Install VS Code Extension**
   - Install "Android iOS Emulator" extension
   - Or "Gradle for Java" extension

2. **Configure in VS Code**
   - Press `Ctrl+Shift+P`
   - Type "Android: Select Device"
   - Build and run using extension commands

---

## Quick Build & Deploy Script

Create a PowerShell script for one-command deployment:

**File: `c:\Users\ASUS\TiffinCraft\frontend\build-and-run.ps1`**
```powershell
# Build the APK
Write-Host "Building APK..." -ForegroundColor Green
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    
    # Check if device is connected
    $devices = & C:\Android\platform-tools\adb.exe devices
    
    if ($devices -match "device$") {
        Write-Host "Installing on device..." -ForegroundColor Green
        & C:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
        
        Write-Host "Launching app..." -ForegroundColor Green
        & C:\Android\platform-tools\adb.exe shell am start -n com.tiffincraft.app/.activities.SplashActivity
        
        Write-Host "App launched successfully!" -ForegroundColor Green
    } else {
        Write-Host "No device connected. Please connect a device or start an emulator." -ForegroundColor Yellow
    }
} else {
    Write-Host "Build failed!" -ForegroundColor Red
}
```

**Run it:**
```powershell
cd C:\Users\ASUS\TiffinCraft\frontend
.\build-and-run.ps1
```

---

## Recommended VS Code Extensions

1. **Extension Pack for Java** (Microsoft)
2. **Gradle for Java** (Microsoft)
3. **Android iOS Emulator** (Didin Komarudin)
4. **XML Tools** (Josh Johnson)

---

## Debugging

### View Logs
```powershell
C:\Android\platform-tools\adb.exe logcat | Select-String "TiffinCraft"
```

### Clear App Data
```powershell
C:\Android\platform-tools\adb.exe shell pm clear com.tiffincraft.app
```

### Uninstall App
```powershell
C:\Android\platform-tools\adb.exe uninstall com.tiffincraft.app
```

---

## Alternative: Simpler Approach

If SDK installation is too complex, you have two easier options:

### 1. Use Expo/React Native Instead
Convert your app to React Native which is easier to run from VS Code.

### 2. Install Android Studio Anyway
Even if you code in VS Code, Android Studio provides:
- One-click SDK installation
- Better emulator management
- Easier device debugging
- You can still code in VS Code and just use Android Studio to run

---

## Current Limitations Without Android Studio

❌ No visual layout editor  
❌ Manual emulator management  
❌ Command-line only debugging  
✅ Full Gradle build support  
✅ Code editing works perfectly  
✅ Can deploy to physical devices  

**Recommendation**: Install Android Studio for device management, but continue coding in VS Code if you prefer it!
