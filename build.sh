#!/bin/bash
set -ex

# Project Config
PACKAGE_NAME="com.benny.openlauncher.debug"
ACTIVITY_NAME="com.benny.openlauncher.activity.OnBoardActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
FINAL_APK="openlauncher_debug.apk"
ADB="/data/data/com.termux/files/usr/bin/adb"
GRADLE_FILE="app/build.gradle"

echo "[1/5] Backing up project..."
python3 backup.py

echo "[2/5] Updating build number (versionCode)..."
# Increment versionCode in app/build.gradle
# Pattern: versionCode 208
if grep -q "versionCode" "$GRADLE_FILE"; then
    perl -i -pe 's/versionCode\s+(\d+)/"versionCode " . ($1+1)/e' "$GRADLE_FILE"
    NEW_VC=$(grep -oP 'versionCode\s+\K[0-9]+' "$GRADLE_FILE")
    echo "New versionCode: $NEW_VC"
else
    echo "Error: versionCode not found in $GRADLE_FILE"
    exit 1
fi

echo "[3/5] Building with Gradle..."
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
cp "$APK_PATH" "$FINAL_APK"

echo "[4/5] Installing APK..."
if command -v $ADB &> /dev/null; then
    echo "Installing: $FINAL_APK"
    $ADB install -r "$FINAL_APK"
    
    echo "[5/5] Launching Application..."
    $ADB shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
else
    echo "ADB not found. Skipping install."
fi

echo "Done!"