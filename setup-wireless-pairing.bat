@echo off
echo ========================================
echo TiffinCraft - Wireless Pairing Setup (Android 11+)
echo ========================================
echo.

echo STEP-BY-STEP INSTRUCTIONS:
echo.
echo ON YOUR ANDROID DEVICE:
echo 1. Go to Settings ^> Developer Options
echo 2. Enable "Wireless debugging"
echo 3. Tap on "Wireless debugging"
echo 4. Tap "Pair device with pairing code"
echo 5. Note the pairing code and IP address:port shown
echo.
echo ON YOUR COMPUTER:
echo 6. Enter the pairing command below with YOUR device details
echo    Format: adb pair IP_ADDRESS:PORT
echo    Example: adb pair 192.168.1.100:45678
echo.
echo 7. When prompted, enter the 6-digit pairing code from your device
echo.
echo 8. After successful pairing, connect using:
echo    adb connect IP_ADDRESS:PORT
echo    (Use the main IP:PORT shown in Wireless debugging, not the pairing port)
echo.
echo ========================================
echo TROUBLESHOOTING:
echo - Make sure your computer and phone are on the same WiFi network
echo - Disable and re-enable Wireless debugging on phone
echo - Restart ADB: adb kill-server then adb start-server
echo - Check Windows Firewall isn't blocking ADB
echo ========================================
echo.

echo Checking ADB version...
adb version
echo.

echo Current ADB devices:
adb devices
echo.

pause
