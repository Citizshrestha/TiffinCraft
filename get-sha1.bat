@echo off
echo Getting SHA-1 fingerprint for debug keystore...
echo.

cd "%USERPROFILE%\.android"

if not exist debug.keystore (
    echo Debug keystore not found!
    echo Run your Android app at least once to generate it.
    pause
    exit /b
)

keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android | findstr SHA1

echo.
echo Copy the SHA1 value above and paste it in Google Cloud Console
pause
