import os
import shutil
import re
import sys
import datetime

# Color codes
GREEN = '\033[0;32m'
RED = '\033[0;31m'
NC = '\033[0m' # No Color
BOLD = '\033[1m'

def get_backup_root():
    source_dir = os.getcwd()
    return os.path.abspath(os.path.join(source_dir, '..', 'backup', 'openlauncher-maya'))

def load_build_index():
    index = {}
    if os.path.exists("build.index"):
        with open("build.index", "r") as f:
            for line in f:
                line = line.strip()
                if ":" in line:
                    version, status = line.split(":", 1)
                    index[version] = status
    return index

def get_inc_num(b_name):
    """Extract increment number from backup name. Full is 0."""
    match = re.search(r'_inc_(\d+)', b_name)
    if match:
        return int(match.group(1))
    return 0

def get_version_from_backup(b_name):
    """Extract base version from backup name, e.g., v0.9.6 from v0.9.6_inc_1."""
    match = re.split(r'_(inc|full)', b_name)
    if match:
        return match[0]
    return b_name

def list_files_in_backup(backup_path):
    affected = []
    for root, _, filenames in os.walk(backup_path):
        for f in filenames:
            rel = os.path.relpath(os.path.join(root, f), backup_path)
            affected.append(rel)
    return affected

def do_restore(backup_sequence):
    source_dir = os.getcwd()
    backup_root = get_backup_root()
    
    gradle_path = os.path.join(source_dir, 'app', 'build.gradle')
    current_vc = 0
    if os.path.exists(gradle_path):
        try:
            with open(gradle_path, 'r') as f:
                content = f.read()
                match = re.search(r'versionCode\s+(\d+)', content)
                if match:
                    current_vc = int(match.group(1))
                    print(f"Preserving current versionCode: {current_vc}")
        except Exception as e:
            print(f"Warning: Could not read current versionCode: {e}")

    # build.index is excluded to preserve history during rollback
    exclude = {'.git', 'build', '.gradle', '.idea', 'backup.py', 'rollback.py', 'debug.keystore', 'local.properties', 'build.index'}
    
    print("\n[!] Starting Rollback...")
    print("Cleaning current project files...")
    for item in os.listdir(source_dir):
        if item in exclude: continue
        item_path = os.path.join(source_dir, item)
        try:
            if os.path.isfile(item_path): os.remove(item_path)
            else: shutil.rmtree(item_path)
        except Exception as e: print(f"Warning: Could not remove {item}: {e}")

    for b_name in backup_sequence:
        b_path = os.path.join(backup_root, b_name)
        print(f"Applying backup layer: {b_name}...")
        for root, _, files in os.walk(b_path):
            for file in files:
                src_path = os.path.join(root, file)
                rel_path = os.path.relpath(src_path, b_path)
                if rel_path == 'rollback.py': continue
                dst_path = os.path.join(source_dir, rel_path)
                os.makedirs(os.path.dirname(dst_path), exist_ok=True)
                shutil.copy2(src_path, dst_path)
    
    if current_vc > 0:
        try:
            with open(gradle_path, 'r') as f: content = f.read()
            match = re.search(r'versionCode\s+(\d+)', content)
            if match:
                restored_vc = int(match.group(1))
                new_vc = max(restored_vc, current_vc + 1)
                print(f"Setting versionCode to {new_vc}...")
                new_content = re.sub(r'versionCode\s+\d+', f'versionCode {new_vc}', content)
                with open(gradle_path, 'w') as f: f.write(new_content)
        except Exception as e: print(f"Warning: Could not update versionCode: {e}")
    print("\n[OK] Rollback successful!")

def natural_key(string_):
    return [int(s) if s.isdigit() else s.lower() for s in re.split('([0-9]+)', string_)]

def get_versions(backup_root):
    if not os.path.exists(backup_root): return {}
    all_dirs = sorted(os.listdir(backup_root), key=natural_key)
    versions = {}
    for d in all_dirs:
        v = get_version_from_backup(d)
        if v not in versions: versions[v] = []
        versions[v].append(d)
    return versions

def main():
    backup_root = get_backup_root()
    versions = get_versions(backup_root)
    v_list = sorted(versions.keys(), reverse=True, key=natural_key)
    build_index = load_build_index()

    if len(sys.argv) > 1:
        if sys.argv[1] in ["-a", "--auto"]:
            if not v_list: return
            do_restore(versions[v_list[0]])
            return
        if sys.argv[1] == "--list":
            for v in v_list:
                print(f"\nVersion: {v}")
                for b_name in versions[v]:
                    color = NC
                    status = build_index.get(b_name, "UNKNOWN")
                    if status == "SUCCESS": color = GREEN
                    elif status == "FAIL": color = RED
                    print(f"  {color}{b_name}{NC}")
            return

    print("--- OpenLauncher Rollback System ---")
    print("[1] Rollback to the last build")
    print("[2] Rollback to a specific build")
    
    try:
        cmd = input("\nSelect option: ").strip().lower()
    except EOFError: return

    if cmd == '1':
        if v_list: do_restore(versions[v_list[0]])
    elif cmd == '2':
        for i, v in enumerate(v_list): print(f"[{i+1}] {v}")
        try:
            v_idx = int(input("\nSelect version: ")) - 1
            selected_version = v_list[v_idx]
        except:
            return
        
        backups = versions[selected_version]
        for b_name in reversed(backups):
            color = NC
            status = build_index.get(b_name, "UNKNOWN")
            if status == "SUCCESS": color = GREEN
            elif status == "FAIL": color = RED
            print(f"{color}[{get_inc_num(b_name)}] {b_name}{NC}")
        
        try:
            target_inc = int(input("\nSelect increment number: "))
            for idx, b in enumerate(backups):
                if get_inc_num(b) == target_inc:
                    do_restore(backups[:idx+1])
                    break
        except:
            return

if __name__ == "__main__":
    main()