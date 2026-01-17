package com.benny.openlauncher.util;

import android.content.Context;
import android.os.Build;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    public interface BackupListener {
        void onLog(String message);
        void onProgress(int progress, int max);
        void onCompleted(boolean success);
    }

    private static File getDataDir(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getDataDir();
        } else {
            return context.getFilesDir().getParentFile();
        }
    }

    public static void createBackup(Context context, OutputStream outputStream, BackupListener listener) {
        if (listener != null) listener.onLog("Full Backup started...");
        try {
            ZipOutputStream zos = new ZipOutputStream(outputStream);
            File dataDir = getDataDir(context);

            // 1. Databases (including WAL/SHM)
            if (listener != null) listener.onLog("Backing up databases...");
            File dbDir = context.getDatabasePath("home.db").getParentFile();
            if (dbDir != null && dbDir.exists()) {
                File[] dbFiles = dbDir.listFiles();
                if (dbFiles != null) {
                    for (File f : dbFiles) {
                        if (f.getName().startsWith("home.db")) {
                            addToZip(zos, f, "databases/" + f.getName());
                            if (listener != null) listener.onLog("Backed up: " + f.getName());
                        }
                    }
                }
            }

            // 2. Shared Preferences
            if (listener != null) listener.onLog("Backing up settings...");
            File prefsDir = new File(dataDir, "shared_prefs");
            if (prefsDir.exists()) {
                File[] prefs = prefsDir.listFiles();
                if (prefs != null) {
                    for (File f : prefs) {
                        if (f.getName().endsWith(".xml")) {
                            addToZip(zos, f, "shared_prefs/" + f.getName());
                        }
                    }
                }
            }

            // 3. Files (recursively, skipping cache)
            if (listener != null) listener.onLog("Backing up files...");
            File filesDir = context.getFilesDir();
            zipRecursively(zos, filesDir, "files", listener);

            zos.close();
            if (listener != null) {
                listener.onLog("Backup completed successfully.");
                listener.onCompleted(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onLog("Backup error: " + e.getMessage());
                listener.onCompleted(false);
            }
        }
    }

    private static void zipRecursively(ZipOutputStream zos, File source, String parentPath, BackupListener listener) throws IOException {
        if (!source.exists()) return;

        if (source.isDirectory()) {
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipRecursively(zos, file, parentPath + "/" + file.getName(), listener);
                }
            }
        } else {
            addToZip(zos, source, parentPath);
            if (listener != null) listener.onLog("Backed up: " + source.getName());
        }
    }

    public static void createBackup(Context context, OutputStream outputStream) {
        createBackup(context, outputStream, null);
    }

    private static void addToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[4096];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }

    public static void restoreBackup(Context context, InputStream inputStream, BackupListener listener) {
        if (listener != null) listener.onLog("Restoration started...");
        try {
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry ze;
            File dataDir = getDataDir(context);
            byte[] buffer = new byte[4096];

            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                File newFile = null;

                if (listener != null) listener.onLog("Restoring: " + fileName);

                // Determine destination
                if (fileName.startsWith("databases/")) {
                    File dbDir = context.getDatabasePath("home.db").getParentFile();
                    if (!dbDir.exists()) dbDir.mkdirs();
                    newFile = new File(dbDir, fileName.substring("databases/".length()));
                } 
                else if (fileName.startsWith("shared_prefs/")) {
                     File prefsDir = new File(dataDir, "shared_prefs");
                     if (!prefsDir.exists()) prefsDir.mkdirs();
                     newFile = new File(prefsDir, fileName.substring("shared_prefs/".length()));
                }
                else if (fileName.startsWith("files/")) {
                     File filesDir = context.getFilesDir();
                     newFile = new File(filesDir, fileName.substring("files/".length()));
                }
                // Backward compatibility
                else if (fileName.equals("home.db")) {
                     newFile = context.getDatabasePath("home.db");
                }
                else if (fileName.startsWith("icons/")) {
                     File iconsDir = new File(context.getFilesDir(), "icons");
                     newFile = new File(iconsDir, fileName.substring("icons/".length()));
                }
                else if (fileName.startsWith("prefs/")) {
                     File prefsDir = new File(dataDir, "shared_prefs");
                     newFile = new File(prefsDir, fileName.substring("prefs/".length()));
                }
                else if (fileName.equals("prefs_app.xml")) {
                     newFile = new File(dataDir, "shared_prefs/app.xml");
                }

                if (newFile != null) {
                    if (ze.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        File parent = newFile.getParentFile();
                        if (parent != null && !parent.exists()) parent.mkdirs();
                        
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                } else {
                    if (listener != null) listener.onLog("Skipped unknown file: " + fileName);
                }
                zis.closeEntry();
            }
            zis.close();

            // Verify
            verifyApps(context, listener);

            if (listener != null) {
                listener.onLog("Restoration completed successfully. Please restart.");
                listener.onCompleted(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onLog("Restoration error: " + e.getMessage());
                listener.onCompleted(false);
            }
        }
    }

    private static void verifyApps(Context context, BackupListener listener) {
        if (listener != null) listener.onLog("Verifying installed apps...");
        try {
            DatabaseHelper db = Setup.dataManager();
            List<Item> allItems = db.getAllItems();
            int missingApps = 0;
            for (Item item : allItems) {
                if (item.getType() == Item.Type.APP && item.getIntent() != null && item.getIntent().getComponent() != null) {
                    String packageName = item.getIntent().getComponent().getPackageName();
                    if (!Tool.isPackageInstalled(packageName, context.getPackageManager())) {
                        if (listener != null) listener.onLog("Warning: Missing app '" + item.getLabel() + "' (" + packageName + ")");
                        missingApps++;
                    }
                }
            }
            if (missingApps > 0) {
                if (listener != null) listener.onLog(missingApps + " apps are missing.");
            } else {
                if (listener != null) listener.onLog("All apps verified.");
            }
        } catch (Exception e) {
            // Ignore DB errors during verify
        }
    }

    public static void restoreBackup(Context context, InputStream inputStream) {
        restoreBackup(context, inputStream, null);
    }
}