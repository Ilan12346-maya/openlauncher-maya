package com.benny.openlauncher.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HideAppsActivity;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.activity.MoreInfoActivity;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.LauncherAction;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DialogHelper;
import com.benny.openlauncher.widget.Desktop;

import net.gsantner.opoc.util.ContextUtils;
import java.util.Locale;

public class SettingsMasterFragment extends SettingsBaseFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_master);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        int key = new ContextUtils(getActivity()).getResId(ContextUtils.ResType.STRING, preference.getKey());
        if (key != 0) {
            switch (key) {
                case R.string.pref_key__cat_hide_apps:
                    Intent intent = new Intent(getActivity(), HideAppsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    return true;
                case R.string.pref_key__cat_about:
                    startActivity(new Intent(getActivity(), MoreInfoActivity.class));
                    return true;
                case R.string.pref_key__icon_pack:
                    DialogHelper.startPickIconPackIntent(getActivity());
                    return true;
            }
        }

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

    @Override
    public void updateSummaries() {
        Preference categoryDesktop = findPreference(getString(R.string.pref_key__cat_desktop));
        Preference categoryDock = findPreference(getString(R.string.pref_key__cat_dock));
        Preference categoryAppDrawer = findPreference(getString(R.string.pref_key__cat_app_drawer));
        Preference categoryAppearance = findPreference(getString(R.string.pref_key__cat_appearance));

        if (categoryDesktop != null) categoryDesktop.setSummary(String.format(Locale.ENGLISH, "%s: %d x %d", getString(R.string.pref_title__size), AppSettings.get().getDesktopColumnCount(), AppSettings.get().getDesktopRowCount()));
        if (categoryDock != null) categoryDock.setSummary(String.format(Locale.ENGLISH, "%s: %d x %d", getString(R.string.pref_title__size), AppSettings.get().getDockColumnCount(), AppSettings.get().getDockRowCount()));
        if (categoryAppearance != null) categoryAppearance.setSummary(String.format(Locale.ENGLISH, "%s: %ddp", getString(R.string.pref_title__icons), AppSettings.get().getIconSize()));

        if (categoryAppDrawer != null) {
            String style = AppSettings.get().getDrawerStyle() == 0 ? getString(R.string.vertical_scroll_drawer) : getString(R.string.horizontal_paged_drawer);
            categoryAppDrawer.setSummary(String.format("%s: %s", getString(R.string.pref_title__style), style));
        }
    }
}
