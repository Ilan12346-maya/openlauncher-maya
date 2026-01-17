package com.benny.openlauncher.util;

import android.content.Context;
import android.util.Log;

import com.benny.openlauncher.AppObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static final String LOG_FILE_NAME = "openlauncher_debug.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    
    public static boolean isEnabled() {
        return AppSettings.get().getDebugMode();
    }

    public static void log(Object source, String message) {
        if (!isEnabled()) {
            return;
        }
        
        String sourceName = source instanceof String ? (String) source : source.getClass().getSimpleName();
        String logEntry = String.format("%s [%s]: %s", DATE_FORMAT.format(new Date()), sourceName, message);
        
        Log.d("OpenLauncherDebug", logEntry);
        writeToFile(logEntry);
    }

    public static void error(Object source, String message, Throwable throwable) {
        if (!isEnabled()) {
            return;
        }
        String sourceName = source instanceof String ? (String) source : source.getClass().getSimpleName();
        String logEntry = String.format("%s [%s] ERROR: %s", DATE_FORMAT.format(new Date()), sourceName, message);
        if (throwable != null) {
            logEntry += "\n" + Log.getStackTraceString(throwable);
        }

        Log.e("OpenLauncherDebug", logEntry);
        writeToFile(logEntry);
    }

    private static synchronized void writeToFile(String logEntry) {
        Context context = AppObject.get();
        if (context == null) return;

        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            Log.e("Logger", "Failed to write log to file", e);
        }
    }

    public static String getLogContent() {
        Context context = AppObject.get();
        if (context == null) return "";

        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        if (!logFile.exists()) return "No logs found.";

        StringBuilder content = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Error reading logs: " + e.getMessage();
        }
        return content.toString();
    }

    public static void clearLogs() {
        Context context = AppObject.get();
        if (context == null) return;

        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        if (logFile.exists()) {
            logFile.delete();
        }
    }
}
