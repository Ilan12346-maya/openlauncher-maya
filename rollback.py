import os
import shutil
import re
import sys
import time
from datetime import datetime

def get_backup_root():
    source_dir = os.getcwd()
    return os.path.abspath(os.path.join(source_dir, '..', 'backup', 'openlauncher'))

def natural_sort_key(s):
    return [int(text) if text.isdigit() else text.lower()
            for text in re.split('([0-9]+)', s)]

def get_creation_time(path):
    t = os.path.getmtime(path)
    return datetime.fromtimestamp(t).strftime('%Y-%m-%d %H:%M:%S')

def get_current_version_info():
    info = {'versionCode': 0, 'versionName': "0.0.0"}
    try:
        if os.path.exists('app/build.gradle'):
            with open('app/build.gradle', 'r') as f:
                content = f.read()
                code_match = re.search(r'versionCode\s+(\d+)', content)
                name_match = re.search(r'versionName\s+"([\d\.]+)"', content)
                if code_match:
                    info['versionCode'] = int(code_match.group(1))
                if name_match:
                    info['versionName'] = name_match.group(1)
    except Exception as e:
        print(f"Warning: Could not read current version info: {e}")
    return info

def update_version_in_gradle(new_code):
    try:
        if os.path.exists('app/build.gradle'):
            with open('app/build.gradle', 'r') as f:
                content = f.read()
            
            # Update versionCode
            new_content = re.sub(r'versionCode\s+\d+', f'versionCode {new_code}', content)
            
            with open('app/build.gradle', 'w') as f:
                f.write(new_content)
            print(f"Updated versionCode to {new_code} in app/build.gradle")
    except Exception as e:
        print(f"Error updating app/build.gradle: {e}")

def list_files_in_backup(backup_path):
    affected = []
    for root, _, filenames in os.walk(backup_path):
        for f in filenames:
            rel = os.path.relpath(os.path.join(root, f), backup_path)
            affected.append(rel)
    return affected

def do_restore(backup_sequence, target_version_code):
    source_dir = os.getcwd()
    backup_root = get_backup_root()
    
    # Files to never delete or overwrite during rollback
    exclude = {'.git', 'bin', 'obj', '.gradle', '.idea', 'backup.py', 'rollback.py', 'build', 'app/build', 'gradlew', 'gradlew.bat', 'gradle'}
    
    print("\n[!] Starting Rollback...")
    
    # 1. Clean current directory
    print("Cleaning current project files...")
    for item in os.listdir(source_dir):
        if item in exclude:
            continue
        item_path = os.path.join(source_dir, item)
        try:
            if os.path.isfile(item_path):
                os.remove(item_path)
            else:
                shutil.rmtree(item_path)
        except Exception as e:
            print(f"Warning: Could not remove {item}: {e}")

    # 2. Restore in sequence
    for b_name in backup_sequence:
        b_path = os.path.join(backup_root, b_name)
        print(f"Applying backup layer: {b_name}...")
        for root, _, files in os.walk(b_path):
            for file in files:
                src_path = os.path.join(root, file)
                rel_path = os.path.relpath(src_path, b_path)
                
                if rel_path == 'rollback.py' or rel_path == 'backup.py':
                    continue
                    
                dst_path = os.path.join(source_dir, rel_path)
                os.makedirs(os.path.dirname(dst_path), exist_ok=True)
                shutil.copy2(src_path, dst_path)
    
    # 3. Post-restore: Increment version code to prevent downgrade error
    update_version_in_gradle(target_version_code)
    
    print("\n[OK] Rollback successful!")

def get_versions(backup_root):
    all_dirs = os.listdir(backup_root)
    versions = {}
    for d in all_dirs:
        match = re.search(r'(v[\d\.]+)', d)
        if match:
            v = match.group(1)
            if v not in versions: versions[v] = []
            versions[v].append(d)
    
    # Sort each version's backups naturally
    for v in versions:
        versions[v].sort(key=natural_sort_key)
        
    return versions

def main():
    backup_root = get_backup_root()
    if not os.path.exists(backup_root):
        print(f"Error: No backup directory found at {backup_root}")
        return

    # Store current version info before rollback
    current_info = get_current_version_info()
    target_code = current_info['versionCode'] + 1

    versions = get_versions(backup_root)
    v_list = sorted(versions.keys(), key=natural_sort_key, reverse=True)

    # CLI ARGUMENT HANDLING
    if len(sys.argv) > 1:
        if sys.argv[1] == "-a":
            if not v_list:
                print("No backups available.")
                return
            latest_v = v_list[0]
            backups = versions[latest_v]
            print(f"Automatic Rollback to: {backups[-1]}")
            do_restore(backups, target_code)
            return

        if sys.argv[1] == "--list":
            print("\n--- OpenLauncher Backup History ---")
            for v in v_list:
                print(f"\nVersion: {v}")
                backups = versions[v]
                for i, b_name in enumerate(backups):
                    b_path = os.path.join(backup_root, b_name)
                    c_time = get_creation_time(b_path)
                    files = list_files_in_backup(b_path)
                    print(f"  [{i+1}] {b_name} | {c_time} | ({len(files)} files)")
            return

        if sys.argv[1] == "--target" and len(sys.argv) > 2:
            target_name = sys.argv[2]
            for v, backups in versions.items():
                if target_name in backups:
                    idx = backups.index(target_name)
                    do_restore(backups[:idx+1], target_code)
                    return
            print(f"Error: Target '{target_name}' not found.")
            return

    # INTERACTIVE MODE
    print("--- OpenLauncher Rollback System ---")
    print(f"Current version detected: {current_info['versionName']} (Code: {current_info['versionCode']})")
    print(f"Rollback will increment next build to Code: {target_code}")
    print("\n[1] Rollback to the last build")
    print("[2] Rollback to a specific build")
    print("[q] Exit")
    
    try:
        cmd = input("\nSelect option: ").strip().lower()
    except EOFError:
        print("\nNon-interactive mode detected. Use -a, --list or --target [name]")
        return

    if cmd == '1':
        if not v_list:
            print("No backups available.")
            return
        latest_v = v_list[0]
        backups = versions[latest_v]
        target_name = backups[-1]
        
        confirm = input(f"Rollback to last state '{target_name}'? (y/n): ")
        if confirm.lower() == 'y':
            do_restore(backups, target_code)

    elif cmd == '2':
        print("\nAvailable Versions:")
        for i, v in enumerate(v_list):
            print(f"[{i+1}] {v}")
        
        try:
            v_idx_input = input("\nSelect version to explore: ").strip()
            if v_idx_input.lower() == 'q': return
            v_idx = int(v_idx_input) - 1
            selected_version = v_list[v_idx]
        except:
            return

        backups = versions[selected_version]
        
        print(f"\nBuild history for {selected_version}:")
        for i, b_name in enumerate(backups):
            b_path = os.path.join(backup_root, b_name)
            c_time = get_creation_time(b_path)
            files = list_files_in_backup(b_path)
            print(f"[{i+1}] {b_name} | {c_time} | ({len(files)} files changed)")
        
        try:
            target_idx_input = input("\nSelect build number to rollback to (or 'q' to cancel): ").strip()
            if target_idx_input.lower() == 'q': return
            target_idx = int(target_idx_input)
            sequence = backups[:target_idx]
            confirm = input(f"\nRollback project to state '{backups[target_idx-1]}'? (y/n): ")
            if confirm.lower() == 'y':
                do_restore(sequence, target_code)
        except Exception as e:
            print(f"Error: {e}")

if __name__ == "__main__":
    main()