import os
import subprocess
import re
import sys
import time

# Configurations
GRADLE_FILE = 'app/build.gradle'
BACKUP_SCRIPT = 'backup.py'
PACKAGE_NAME = 'com.benny.openlauncher.debug'
MAIN_ACTIVITY = 'com.benny.openlauncher.activity.HomeActivity'
JAVA_HOME = r'C:\Program Files\Android\Android Studio\jbr'
ANDROID_HOME = r'C:\Users\anonm\AppData\Local\Android\sdk'
LOG_FILE = 'build_log.txt'

def log(message, level="INFO", terminal_only=False):
    timestamp = time.strftime("%H:%M:%S")
    formatted_msg = f"[{timestamp}] [{level}] {message}"
    print(formatted_msg)
    if not terminal_only:
        with open(LOG_FILE, 'a', encoding='utf-8') as f:
            f.write(formatted_msg + "\n")

def run_command_live(command, env=None):
    """Runs a command and streams the output to the terminal and log file."""
    log(f"Executing: {command}", "CMD")
    try:
        process = subprocess.Popen(
            command,
            shell=True,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            universal_newlines=True
        )
        
        full_output = []
        for line in process.stdout:
            print(line, end='') # Live terminal output
            full_output.append(line)
            with open(LOG_FILE, 'a', encoding='utf-8') as f:
                f.write(line)
        
        process.wait()
        return process.returncode == 0, "".join(full_output)
    except Exception as e:
        log(f"Exception: {e}", "ERROR")
        return False, str(e)

def increment_version_code():
    log("Incrementing versionCode...", "STEP")
    try:
        with open(GRADLE_FILE, 'r') as f:
            content = f.read()
        
        match = re.search(r'versionCode\s+(\d+)', content)
        if match:
            old_code = int(match.group(1))
            new_code = old_code + 1
            new_content = re.sub(r'versionCode\s+\d+', f'versionCode {new_code}', content)
            with open(GRADLE_FILE, 'w') as f:
                f.write(new_content)
            log(f"versionCode updated: {old_code} -> {new_code}")
            return True
        else:
            log("Could not find versionCode in build.gradle", "ERROR")
            return False
    except Exception as e:
        log(f"Error updating versionCode: {e}", "ERROR")
        return False

def run_backup():
    log("Running backup.py...", "STEP")
    success, _ = run_command_live("py backup.py")
    return success

def build_project():
    log("Building project with Gradle...", "STEP")
    env = os.environ.copy()
    env["JAVA_HOME"] = JAVA_HOME
    env["ANDROID_HOME"] = ANDROID_HOME
    env["PATH"] = os.path.join(JAVA_HOME, "bin") + os.pathsep + env["PATH"]
    
    success, _ = run_command_live("gradlew.bat assembleFlavorDefaultDebug", env=env)
    return success

def get_apk_path():
    base_path = "app/build/outputs/apk/flavorDefault/debug"
    if os.path.exists(base_path):
        for file in os.listdir(base_path):
            if file.endswith(".apk"):
                return os.path.join(base_path, file)
    return None

def ad_install_and_run():
    log("Checking ADB devices...", "STEP")
    success, devices_out = run_command_live("adb devices")
    if "device\n" not in devices_out and "\tdevice" not in devices_out:
        log("No ADB devices connected!", "ERROR")
        return False

    apk_path = get_apk_path()
    if not apk_path:
        log("APK not found!", "ERROR")
        return False

    log(f"Installing {apk_path}...", "STEP")
    success, _ = run_command_live(f"adb install -r \"{apk_path}\"",)
    if not success: return False

    log(f"Starting {PACKAGE_NAME}...", "STEP")
    success, _ = run_command_live(f"adb shell am start -n {PACKAGE_NAME}/{MAIN_ACTIVITY}")
    return success

def main():
    # Clear old log
    with open(LOG_FILE, 'w', encoding='utf-8') as f:
        f.write(f"--- OpenLauncher Build Log: {time.ctime()} ---\\n")

    log("=== OpenLauncher Automated Build System ===", "START")
    
    if not increment_version_code(): sys.exit(1)
    if not run_backup(): log("Backup warning: Some issues occurred", "WARNING")
    if not build_project(): 
        log("BUILD FAILED!", "ERROR")
        sys.exit(1)
    if not ad_install_and_run():
        log("Deployment failed!", "ERROR")
        sys.exit(1)
        
    log("=== Process Completed Successfully ===", "SUCCESS")
    
    # Clean up log file on success
    try:
        if os.path.exists(LOG_FILE):
            os.remove(LOG_FILE)
    except Exception as e:
        log(f"Could not remove log file: {e}", "WARNING")

if __name__ == "__main__":
    main()