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

    public interface BackupListener {
        void onLog(String message);
        void onProgress(int progress, int max);
        void onCompleted(boolean success);
    }

    public static void createBackup(Context context, OutputStream outputStream, BackupListener listener) {
        if (listener != null) listener.onLog("Backup-Vorgang gestartet...");
        try {
            ZipOutputStream zos = new ZipOutputStream(outputStream);

            // 1. Backup Database
            if (listener != null) listener.onLog("Sichere Datenbank...");
            File dbFile = context.getDatabasePath("home.db");
            if (dbFile.exists()) {
                addToZip(zos, dbFile, "home.db");
                if (listener != null) listener.onLog("Datenbank 'home.db' gesichert.");
            } else {
                if (listener != null) listener.onLog("Warnung: Datenbankdatei nicht gefunden.");
            }

            // 2. Backup Icons
            if (listener != null) listener.onLog("Sichere Icons...");
            File iconsDir = new File(context.getFilesDir() + "/icons/");
            if (iconsDir.exists() && iconsDir.isDirectory()) {
                File[] icons = iconsDir.listFiles();
                if (icons != null) {
                    for (File icon : icons) {
                        addToZip(zos, icon, "icons/" + icon.getName());
                    }
                    if (listener != null) listener.onLog(icons.length + " Icons gesichert.");
                }
            } else {
                if (listener != null) listener.onLog("Keine benutzerdefinierten Icons zum Sichern gefunden.");
            }

            // 3. Backup SharedPreferences
            if (listener != null) listener.onLog("Sichere Einstellungen...");
            File prefsDir = new File(context.getDataDir(), "shared_prefs");
            if (prefsDir.exists() && prefsDir.isDirectory()) {
                File[] prefs = prefsDir.listFiles();
                if (prefs != null) {
                    for (File pref : prefs) {
                        if (pref.getName().endsWith(".xml")) {
                            addToZip(zos, pref, "prefs/" + pref.getName());
                            if (listener != null) listener.onLog("Einstellung '" + pref.getName() + "' gesichert.");
                        }
                    }
                }
            }

            zos.close();
            if (listener != null) {
                listener.onLog("Backup-Vorgang erfolgreich abgeschlossen.");
                listener.onCompleted(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onLog("Fehler beim Backup: " + e.getMessage());
                listener.onCompleted(false);
            }
        }
    }

    public static void createBackup(Context context, OutputStream outputStream) {
        createBackup(context, outputStream, null);
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

    public static void restoreBackup(Context context, InputStream inputStream, BackupListener listener) {
        if (listener != null) listener.onLog("Wiederherstellung gestartet...");
        try {
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry ze = zis.getNextEntry();
            
            byte[] buffer = new byte[1024];

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = null;

                if (listener != null) listener.onLog("Stelle '" + fileName + "' wieder her...");

                if (fileName.equals("home.db")) {
                    newFile = context.getDatabasePath("home.db");
                } else if (fileName.startsWith("icons/")) {
                    File iconsDir = new File(context.getFilesDir() + "/icons/");
                    if (!iconsDir.exists()) iconsDir.mkdirs();
                    newFile = new File(context.getFilesDir(), fileName);
                } else if (fileName.startsWith("prefs/")) {
                    String prefName = fileName.substring("prefs/".length());
                    newFile = new File(context.getDataDir(), "shared_prefs/" + prefName);
                } else if (fileName.equals("prefs_app.xml")) {
                    // Legacy support
                    newFile = new File(context.getDataDir(), "shared_prefs/app.xml");
                }

                if (newFile != null) {
                    File parent = newFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                    
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    if (listener != null) listener.onLog("Datei '" + fileName + "' erfolgreich wiederhergestellt.");
                } else {
                    if (listener != null) listener.onLog("Überspringe unbekannte Datei: " + fileName);
                }
                
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.close();

            // After restoration, we should check if all apps in the database are still installed
            if (listener != null) listener.onLog("Überprüfe installierte Apps...");
            DatabaseHelper db = Setup.dataManager();
            List<Item> allItems = db.getAllItems();
            int missingApps = 0;
            for (Item item : allItems) {
                if (item.getType() == Item.Type.APP) {
                    if (item.getIntent() != null && item.getIntent().getComponent() != null) {
                        String packageName = item.getIntent().getComponent().getPackageName();
                        if (!Tool.isPackageInstalled(packageName, context.getPackageManager())) {
                            if (listener != null) listener.onLog("Warnung: App '" + item.getLabel() + "' (" + packageName + ") ist nicht installiert.");
                            missingApps++;
                        }
                    }
                }
            }
            if (missingApps > 0) {
                if (listener != null) listener.onLog(missingApps + " Apps aus dem Backup sind aktuell nicht installiert.");
            } else {
                if (listener != null) listener.onLog("Alle gesicherten Apps sind installiert.");
            }

            if (listener != null) {
                listener.onLog("Wiederherstellung erfolgreich abgeschlossen.");
                listener.onCompleted(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onLog("Fehler bei der Wiederherstellung: " + e.getMessage());
                listener.onCompleted(false);
            }
        }
    }

    public static void restoreBackup(Context context, InputStream inputStream) {
        restoreBackup(context, inputStream, null);
    }
}