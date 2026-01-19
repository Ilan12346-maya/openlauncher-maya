package com.benny.openlauncher.manager;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;

import com.benny.openlauncher.receivers.AppUpdateReceiver;
import com.benny.openlauncher.receivers.ShortcutReceiver;

public class LauncherReceiverManager {
    private static final IntentFilter _appUpdateIntentFilter = new IntentFilter();
    private static final IntentFilter _shortcutIntentFilter = new IntentFilter();
    
    private AppUpdateReceiver _appUpdateReceiver;
    private ShortcutReceiver _shortcutReceiver;
    private final Context _context;

    static {
        _appUpdateIntentFilter.addDataScheme("package");
        _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        _shortcutIntentFilter.addAction("com.android.launcher.action.INSTALL_SHORTCUT");
    }

    public LauncherReceiverManager(Context context) {
        _context = context;
    }

    public void registerReceivers() {
        _appUpdateReceiver = new AppUpdateReceiver();
        _shortcutReceiver = new ShortcutReceiver();

        // Register all receivers
        ContextCompat.registerReceiver(_context, _appUpdateReceiver, _appUpdateIntentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(_context, _shortcutReceiver, _shortcutIntentFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    public void unregisterReceivers() {
        if (_appUpdateReceiver != null) {
            _context.unregisterReceiver(_appUpdateReceiver);
            _appUpdateReceiver = null;
        }
        if (_shortcutReceiver != null) {
            _context.unregisterReceiver(_shortcutReceiver);
            _shortcutReceiver = null;
        }
    }
}
