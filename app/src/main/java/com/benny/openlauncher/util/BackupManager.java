package com.benny.openlauncher.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    public static void createBackup(Context context, OutputStream outputStream) {
        try {
            ZipOutputStream zos = new ZipOutputStream(outputStream);

            // 1. Backup Database
            File dbFile = context.getDatabasePath("home.db");
            if (dbFile.exists()) {
                addToZip(zos, dbFile, "home.db");
            }

            // 2. Backup Icons
            File iconsDir = new File(context.getFilesDir() + "/icons/");
            if (iconsDir.exists() && iconsDir.isDirectory()) {
                File[] icons = iconsDir.listFiles();
                if (icons != null) {
                    for (File icon : icons) {
                        addToZip(zos, icon, "icons/" + icon.getName());
                    }
                }
            }

            // 3. Backup SharedPreferences
            // The name is "app" as seen in AppSettings.java
            File prefsFile = new File(context.getDataDir(), "shared_prefs/app.xml");
            if (prefsFile.exists()) {
                addToZip(zos, prefsFile, "prefs_app.xml");
            }

            zos.close();
            Tool.toast(context, "Backup erfolgreich erstellt (ZIP)");
        } catch (Exception e) {
            e.printStackTrace();
            Tool.toast(context, "Backup fehlgeschlagen: " + e.getMessage());
        }
    }

    private static void addToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }

    public static void restoreBackup(Context context, InputStream inputStream) {
        try {
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry ze = zis.getNextEntry();
            
            byte[] buffer = new byte[1024];

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = null;

                if (fileName.equals("home.db")) {
                    newFile = context.getDatabasePath("home.db");
                } else if (fileName.startsWith("icons/")) {
                    File iconsDir = new File(context.getFilesDir() + "/icons/");
                    if (!iconsDir.exists()) iconsDir.mkdirs();
                    newFile = new File(context.getFilesDir(), fileName);
                } else if (fileName.equals("prefs_app.xml")) {
                    newFile = new File(context.getDataDir(), "shared_prefs/app.xml");
                }

                if (newFile != null) {
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.close();

            Tool.toast(context, "Wiederherstellung erfolgreich. Launcher wird neu gestartet...");
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).recreate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Tool.toast(context, "Fehler beim Wiederherstellen: " + e.getMessage());
        }
    }
}