package com.benny.openlauncher.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.benny.openlauncher.model.App;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HistoryManager {
    private static final String PREF_RECENT_APPS = "pref_recent_apps";
    private static final String PREF_RECENT_SEARCHES = "pref_recent_searches";
    private static final String DELIMITER = ";;;";
    private static final String ITEM_DELIMITER = ":::";

    private static HistoryManager _instance;
    private final Context _context;

    private HistoryManager(Context context) {
        _context = context;
    }

    public static HistoryManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new HistoryManager(context);
        }
        return _instance;
    }

    // --- Recent Apps ---

    public void addRecentApp(App app) {
        if (app == null) return;
        List<String> saved = loadList(PREF_RECENT_APPS);
        String key = app._packageName + ITEM_DELIMITER + app._className;

        // Remove if exists (to move to top)
        saved.remove(key);
        // Add to front
        saved.add(0, key);

        // Limit size (default 2 rows, let's say max 16 to be safe, UI determines display count)
        int max = AppSettings.get().getDrawerColumnCount() * 2;
        if (max < 4) max = 8; // fallback
        while (saved.size() > max) {
            saved.remove(saved.size() - 1);
        }

        saveList(PREF_RECENT_APPS, saved);
    }

    public List<App> getRecentApps(int maxCount) {
        List<String> saved = loadList(PREF_RECENT_APPS);
        List<App> apps = new ArrayList<>();
        AppManager appManager = AppManager.getInstance(_context);

        for (String key : saved) {
            if (apps.size() >= maxCount) break;
            String[] parts = key.split(ITEM_DELIMITER);
            if (parts.length == 2) {
                // Find app in AppManager
                // We iterate AppManager's list which is cached
                for (App app : appManager.getApps()) {
                    if (app._packageName.equals(parts[0]) && app._className.equals(parts[1])) {
                        apps.add(app);
                        break;
                    }
                }
            }
        }
        return apps;
    }

    // --- Recent Searches ---

    public void addRecentSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        List<String> saved = loadList(PREF_RECENT_SEARCHES);
        
        saved.remove(query);
        saved.add(0, query);

        // Limit to 8
        while (saved.size() > 8) {
            saved.remove(saved.size() - 1);
        }

        saveList(PREF_RECENT_SEARCHES, saved);
    }

    public List<String> getRecentSearches() {
        return loadList(PREF_RECENT_SEARCHES);
    }

    // --- Persistence Helpers ---

    private List<String> loadList(String prefKey) {
        SharedPreferences prefs = _context.getSharedPreferences("openlauncher_history", Context.MODE_PRIVATE);
        String raw = prefs.getString(prefKey, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(DELIMITER)));
    }

    private void saveList(String prefKey, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(DELIMITER);
            }
        }
        SharedPreferences prefs = _context.getSharedPreferences("openlauncher_history", Context.MODE_PRIVATE);
        prefs.edit().putString(prefKey, sb.toString()).apply();
    }
}
