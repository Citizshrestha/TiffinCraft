#!/usr/bin/env pwsh
# TiffinCraft Android Build and Run Script

param(
    [switch]$BuildOnly,
    [switch]$InstallOnly
)

Write-Host "`n🚀 TiffinCraft Android Builder" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

# Check if Android SDK is available
$adbPath = "C:\Android\platform-tools\adb.exe"
$hasSDK = Test-Path $adbPath

if (-not $BuildOnly -and -not $hasSDK) {
    Write-Host "⚠️  Android SDK not found at C:\Android" -ForegroundColor Yellow
    Write-Host "   Please install Android SDK first. See VS_CODE_ANDROID_SETUP.md" -ForegroundColor Yellow
    Write-Host "   Running in build-only mode...`n" -ForegroundColor Yellow
    $BuildOnly = $true
}

# Build APK
if (-not $InstallOnly) {
    Write-Host "📦 Building APK..." -ForegroundColor Green
    .\gradlew.bat assembleDebug
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`n❌ Build failed!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "`n✅ Build successful!" -ForegroundColor Green
    Write-Host "   APK: app\build\outputs\apk\debug\app-debug.apk`n" -ForegroundColor Gray
}

# Deploy to device
if (-not $BuildOnly -and $hasSDK) {
    Write-Host "📱 Checking for connected devices..." -ForegroundColor Green
    $devices = & $adbPath devices
    
    $connectedDevices = $devices | Select-String -Pattern "device$"
    
    if ($connectedDevices) {
        Write-Host "   Found device(s)!`n" -ForegroundColor Green
        
        Write-Host "📲 Installing APK..." -ForegroundColor Green
        & $adbPath install -r app\build\outputs\apk\debug\app-debug.apk
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n🚀 Launching app..." -ForegroundColor Green
            & $adbPath shell am start -n com.tiffincraft.app/.activities.SplashActivity
            
            Write-Host "`n✅ App launched successfully!" -ForegroundColor Green
            Write-Host "`n📊 To view logs, run:" -ForegroundColor Cyan
            Write-Host "   $adbPath logcat | Select-String 'TiffinCraft'`n" -ForegroundColor Gray
        } else {
            Write-Host "`n❌ Installation failed!" -ForegroundColor Red
        }
    } else {
        Write-Host "`n⚠️  No device connected!" -ForegroundColor Yellow
        Write-Host "   Options:" -ForegroundColor Yellow
        Write-Host "   1. Connect Android phone via USB (enable USB debugging)" -ForegroundColor Yellow
        Write-Host "   2. Start an emulator: C:\Android\emulator\emulator.exe -avd YOUR_AVD_NAME" -ForegroundColor Yellow
        Write-Host "   3. Transfer APK manually: app\build\outputs\apk\debug\app-debug.apk`n" -ForegroundColor Yellow
    }
}

if ($BuildOnly) {
    Write-Host "ℹ️  Build complete. Transfer the APK to your device manually." -ForegroundColor Cyan
    Write-Host "   APK location: app\build\outputs\apk\debug\app-debug.apk`n" -ForegroundColor Gray
}

Write-Host "✨ Done!`n" -ForegroundColor Green
