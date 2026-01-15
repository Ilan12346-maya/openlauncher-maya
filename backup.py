import os
import shutil
import re
import hashlib

def get_version():
    try:
        with open('app/build.gradle', 'r') as f:
            content = f.read()
            match = re.search(r'versionName\s+"([\d\.]+)"', content)
            if match:
                return f"v{match.group(1)}"
    except Exception as e:
        print(f"Error reading version: {e}")
    return "v0.0.0"

def get_file_hash(filepath):
    hasher = hashlib.md5()
    try:
        with open(filepath, 'rb') as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hasher.update(chunk)
        return hasher.hexdigest()
    except:
        return None

def backup():
    version = get_version()
    source_dir = os.getcwd()
    # Organized folder: ../backup/openlauncher
    backup_root = os.path.abspath(os.path.join(source_dir, '..', 'backup', 'openlauncher'))
    
    if not os.path.exists(backup_root):
        os.makedirs(backup_root)

    # Find the latest backup of this version to compare against
    prefix = f'openlauncher_{version}'
    existing_backups = sorted([d for d in os.listdir(backup_root) if d.startswith(prefix)])
    latest_backup = os.path.join(backup_root, existing_backups[-1]) if existing_backups else None

    # Determine target directory name
    if not existing_backups:
        target_name = f'{prefix}_full'
    else:
        count = len(existing_backups)
        target_name = f'{prefix}_inc_{count}'

    target_dir = os.path.join(backup_root, target_name)
    
    exclude = {'.git', 'bin', 'obj', '.gradle', '.idea', 'backup.py', 'rollback.py', 'build', 'app/build', 'gradlew', 'gradlew.bat', 'gradle'}
    changed_files = []

    print(f"Backing up OpenLauncher {version}...")
    if latest_backup:
        print(f"Comparing against latest backup: {latest_backup}")
    else:
        print("No previous backup for this version. Performing full backup.")

    # Collect files to backup
    for root, dirs, files in os.walk(source_dir):
        # Filter directories
        dirs[:] = [d for d in dirs if d not in exclude]
        
        for file in files:
            if file.endswith('.log') or file in exclude:
                continue
                
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, source_dir)
            
            should_copy = False
            if not existing_backups:
                should_copy = True
            else:
                # Find the most recent version of this file in any previous backup
                found_in_any = False
                for prev_backup_name in reversed(existing_backups):
                    prev_file_path = os.path.join(backup_root, prev_backup_name, rel_path)
                    if os.path.exists(prev_file_path):
                        found_in_any = True
                        if get_file_hash(file_path) != get_file_hash(prev_file_path):
                            should_copy = True
                        break # Found the latest version, no need to look further
                
                if not found_in_any:
                    should_copy = True
            
            if should_copy:
                changed_files.append((file_path, rel_path))

    if not changed_files:
        print("No changes detected since last backup. Skipping.")
        return

    print(f"Found {len(changed_files)} files to copy.")
    os.makedirs(target_dir, exist_ok=True)
    
    for src_path, rel_path in changed_files:
        dst_path = os.path.join(target_dir, rel_path)
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        shutil.copy2(src_path, dst_path)

    print(f"Backup successful: {target_dir}")

if __name__ == "__main__":
    backup()