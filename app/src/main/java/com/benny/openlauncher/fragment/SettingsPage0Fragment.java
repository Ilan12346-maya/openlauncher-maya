package com.benny.openlauncher.fragment;

import android.os.Bundle;
import androidx.preference.Preference;
import com.benny.openlauncher.R;
import com.benny.openlauncher.widget.Desktop;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.util.Tool;

public class SettingsPage0Fragment extends SettingsBaseFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_page0);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("pref_key__desktop_page_0_clear")) {
            if (HomeActivity._launcher != null) {
                Desktop desktop = HomeActivity._launcher.getDesktop();
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
