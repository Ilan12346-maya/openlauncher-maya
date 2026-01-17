package com.benny.openlauncher.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import com.benny.openlauncher.R;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.Logger;
import com.benny.openlauncher.util.Tool;

import androidx.preference.SwitchPreference;
import com.benny.openlauncher.R;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.Logger;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.widget.LogViewPreference;

public class SettingsDebugFragment extends SettingsBaseFragment {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable updateLogRunnable = new Runnable() {
        @Override
        public void run() {
            LogViewPreference.updateLog();
            if (AppSettings.get().getDebugMode()) {
                handler.postDelayed(this, 2000);
            }
        }
    };

    private final Runnable disableLoggingRunnable = new Runnable() {
        @Override
        public void run() {
            AppSettings settings = AppSettings.get();
            settings.setDebugMode(false);
            SwitchPreference loggingPref = findPreference(getString(R.string.pref_key__debug_mode));
            if (loggingPref != null) {
                loggingPref.setChecked(false);
            }
            Logger.log(this, "Logging automatically disabled after 1 minute.");
            Tool.toast(getContext(), "Logging automatisch beendet.");
            handler.removeCallbacks(updateLogRunnable);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_debug);

        SwitchPreference loggingPref = findPreference(getString(R.string.pref_key__debug_mode));
        if (loggingPref != null) {
            loggingPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (boolean) newValue;
                    if (enabled) {
                        Logger.clearLogs();
                        Logger.log(this, "Logging enabled by user.");
                        handler.postDelayed(disableLoggingRunnable, 60 * 1000);
                        handler.post(updateLogRunnable);
                        Tool.toast(getContext(), "Logging für 1 Minute aktiviert.");
                    } else {
                        handler.removeCallbacks(disableLoggingRunnable);
                        handler.removeCallbacks(updateLogRunnable);
                        Logger.log(this, "Logging disabled by user.");
                    }
                    return true;
                }
            });
        }

        Preference copyPref = findPreference("pref_key__copy_logs");
        if (copyPref != null) {
            copyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String logs = Logger.getLogContent();
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("OpenLauncher Logs", logs);
                    clipboard.setPrimaryClip(clip);
                    Tool.toast(getContext(), R.string.toast_logs_copied);
                    return true;
                }
            });
        }

        Preference clearPref = findPreference("pref_key__clear_logs");
        if (clearPref != null) {
            clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Logger.clearLogs();
                    LogViewPreference.updateLog();
                    Tool.toast(getContext(), R.string.toast_logs_cleared);
                    return true;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AppSettings.get().getDebugMode()) {
            handler.post(updateLogRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateLogRunnable);
    }
}