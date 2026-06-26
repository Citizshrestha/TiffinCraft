#!/bin/bash
echo "Getting SHA-1 fingerprint for debug keystore..."
echo ""

KEYSTORE="$HOME/.android/debug.keystore"

if [ ! -f "$KEYSTORE" ]; then
    echo "Debug keystore not found!"
    echo "Run your Android app at least once to generate it."
    exit 1
fi

keytool -list -v -keystore "$KEYSTORE" -alias androiddebugkey -storepass android -keypass android | grep SHA1

echo ""
echo "Copy the SHA1 value above and paste it in Google Cloud Console"
