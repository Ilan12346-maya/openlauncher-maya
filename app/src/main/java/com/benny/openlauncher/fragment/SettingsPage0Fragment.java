package com.benny.openlauncher.fragment;

import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.preference.Preference;
import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.widget.Desktop;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.util.Tool;

public class SettingsPage0Fragment extends SettingsBaseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_page0);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("pref_key__desktop_page_0")) {
            Setup.appSettings().setAppRestartRequired(true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("pref_key__desktop_page_0_clear")) {
            if (HomeActivity.Companion.getLauncher() != null) {
                Desktop desktop = HomeActivity.Companion.getLauncher().getDesktop();
                if (desktop != null && desktop.getAdapter() instanceof Desktop.DesktopAdapter) {
                    ((Desktop.DesktopAdapter) desktop.getAdapter()).clearPage0();
                    Tool.toast(getContext(), "Seite 0 zurückgesetzt");
                }
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
