@echo off
echo ========================================
echo TiffinCraft - WiFi Wireless Debugging Setup
echo ========================================
echo.

echo Step 1: Checking ADB connection...
adb devices
echo.

echo Step 2: Restarting ADB server...
adb kill-server
adb start-server
echo.

echo Step 3: Setting up TCP/IP on port 5555...
adb tcpip 5555
echo.

echo Step 4: Getting device IP address...
echo Please note your device IP address from the output below:
adb shell ip addr show wlan0
echo.

echo ========================================
echo INSTRUCTIONS:
echo 1. Note your device IP address from above (usually starts with 192.168.x.x)
echo 2. Disconnect USB cable
echo 3. Run: adb connect YOUR_DEVICE_IP:5555
echo 4. Example: adb connect 192.168.1.100:5555
echo ========================================
echo.

pause
