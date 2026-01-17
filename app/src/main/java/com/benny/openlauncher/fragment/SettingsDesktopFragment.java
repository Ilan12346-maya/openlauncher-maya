package com.benny.openlauncher.fragment;

import android.os.Bundle;
import androidx.preference.Preference;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.util.LauncherAction;
import net.gsantner.opoc.util.ContextUtils;

public class SettingsDesktopFragment extends SettingsBaseFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_desktop);
    }
}
