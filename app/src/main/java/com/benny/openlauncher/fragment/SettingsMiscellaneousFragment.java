package com.benny.openlauncher.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import android.widget.Toast;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.DatabaseHelper;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.viewutil.DialogHelper;
import com.nononsenseapps.filepicker.FilePickerActivity;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.PermissionChecker;

public class SettingsMiscellaneousFragment extends SettingsBaseFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_advanced);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        HomeActivity homeActivity = HomeActivity._launcher;
        int key = new ContextUtils(getActivity()).getResId(ContextUtils.ResType.STRING, preference.getKey());
        switch (key) {
            case R.string.pref_key__backup:
                DialogHelper.backupDialog(getActivity());
                return true;
            case R.string.pref_key__restore:
                DialogHelper.restoreDialog(getActivity());
                return true;
            case R.string.pref_key__reset_settings:
                DialogHelper.alertDialog(getActivity(), getString(R.string.pref_title__reset_settings), getString(R.string.are_you_sure), new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        AppSettings.get().resetSettings();
                        if (homeActivity != null) homeActivity.recreate();
                        Toast.makeText(getActivity(), R.string.toast_settings_restored, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            case R.string.pref_key__reset_database:
                DialogHelper.alertDialog(getActivity(), getString(R.string.pref_title__reset_database), getString(R.string.are_you_sure), new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        DatabaseHelper db = HomeActivity._db;
                        db.onUpgrade(db.getWritableDatabase(), 1, 1);
                        AppSettings.get().setAppFirstLaunch(true);
                        if (homeActivity != null) homeActivity.recreate();
                        Toast.makeText(getActivity(), R.string.toast_database_deleted, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            case R.string.pref_key__restart:
                if (homeActivity != null) homeActivity.recreate();
                getActivity().finish();
                return true;
            case R.string.pref_key__crash_test:
                throw new RuntimeException("Crash Test triggered by user.");
        }
        return false;
    }
}
