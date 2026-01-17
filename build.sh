#!/bin/bash

# --- OpenLauncher Build Script (Maya Edition) ---

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Config
PACKAGE_NAME="com.benny.openlauncher.debug"
ACTIVITY_NAME="com.benny.openlauncher.activity.OnBoardActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
FINAL_APK="openlauncher_debug.apk"
ADB="/data/data/com.termux/files/usr/bin/adb"
GRADLE_FILE="app/build.gradle"
BUILD_INDEX="build.index"

function header() {
    echo -e "\n${BLUE}${BOLD}=== $1 ===${NC}"
}

function success() {
    echo -e "${GREEN}[OK] $1${NC}"
}

function info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

function error_exit() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}

# Start Build Process
echo -e "${BOLD}Starting OpenLauncher Build Process${NC}"

# [1/5] Backup
header "[1/5] Backing up project"
rm -f LAST_BACKUP_NAME
if python3 backup.py; then
    BACKUP_NAME=$(cat LAST_BACKUP_NAME 2>/dev/null || echo "")
    success "Project backed up successfully."
else
    echo -e "${YELLOW}[WARN] Backup failed, but continuing...${NC}"
fi

# [2/5] Versioning
header "[2/5] Updating Build Version"
if grep -q "versionCode" "$GRADLE_FILE"; then
    perl -i -pe 's/versionCode\s+(\d+)/"versionCode " . ($1+1)/e' "$GRADLE_FILE"
    NEW_VC=$(grep -oP 'versionCode\s+\K[0-9]+' "$GRADLE_FILE")
    BASE_VERSION=$(grep -oP 'versionName\s+"v?\K[\d\.]+' "$GRADLE_FILE" | head -1)
    NEW_VERSION_NAME="v${BASE_VERSION}-cv${NEW_VC}"
    sed -i "s/versionName\s\+\".*\"/versionName \"$NEW_VERSION_NAME\"/" "$GRADLE_FILE"
    success "Updated to versionCode: ${BOLD}$NEW_VC${NC} (Version: $NEW_VERSION_NAME)"
else
    error_exit "versionCode not found in $GRADLE_FILE"
fi

# [3/5] Compilation
header "[3/5] Compiling with Gradle"
info "Building debug APK..."
if ./gradlew assembleDebug -q -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2; then
    cp "$APK_PATH" "$FINAL_APK" || error_exit "Could not copy APK to destination"
    success "Build successful: ${BOLD}$FINAL_APK${NC}"
    # Log status for the specific backup name (crucial for rollback colors)
    if [ ! -z "$BACKUP_NAME" ]; then
        echo "$BACKUP_NAME:SUCCESS" >> "$BUILD_INDEX"
    fi
else
    if [ ! -z "$BACKUP_NAME" ]; then
        echo "$BACKUP_NAME:FAIL" >> "$BUILD_INDEX"
    fi
    error_exit "Gradle build failed"
fi
rm -f LAST_BACKUP_NAME

# [4/5] Installation
header "[4/5] Installing on Device"
if command -v $ADB &> /dev/null; then
    info "Transferring APK to device..."
    if $ADB install -r "$FINAL_APK" > /dev/null; then
        success "APK installed successfully."
    else
        error_exit "Installation failed"
    fi
else
    info "ADB not found. Skipping installation."
fi

# [5/5] Launching
if command -v $ADB &> /dev/null; then
    header "[5/5] Launching Application"
    $ADB shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME" > /dev/null
    success "OpenLauncher is now running!"
fi

echo -e "\n${GREEN}${BOLD}All tasks completed successfully!${NC}\n"