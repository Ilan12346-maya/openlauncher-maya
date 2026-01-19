package com.benny.openlauncher.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.appcompat.widget.Toolbar;

import com.benny.openlauncher.R;
import com.benny.openlauncher.databinding.ActivitySettingsBinding;
import com.benny.openlauncher.fragment.SettingsBaseFragment;
import com.benny.openlauncher.fragment.SettingsMasterFragment;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.Definitions;

import net.gsantner.opoc.util.ContextUtils;

public class SettingsActivity extends ColorActivity implements SettingsBaseFragment.OnPreferenceStartFragmentCallback {
    private ActivitySettingsBinding binding;

    public void onCreate(Bundle b) {
        // must be applied before setContentView
        super.onCreate(b);
        ContextUtils contextUtils = new ContextUtils(this);
        contextUtils.setAppLanguage(_appSettings.getLanguage());

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle(R.string.pref_title__settings);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_holder, new SettingsMasterFragment()).commit();

        // if system exit is called the app will open settings activity again
        // this pushes the user back out to the home activity
        if (_appSettings.getAppRestartRequired()) {
            startActivity(new Intent(this, HomeActivity.class));
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference preference) {
        Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), preference.getFragment());
        fragment.setArguments(preference.getExtras());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_holder, fragment).addToBackStack(fragment.getTag()).commit();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Setup.dataManager().close();
            switch (requestCode) {
                case Definitions.INTENT_BACKUP:
                    com.benny.openlauncher.util.BackupManager.startBackupTask(this, data.getData());
                    Setup.dataManager().open();
                    break;
                case Definitions.INTENT_RESTORE:
                    com.benny.openlauncher.util.BackupManager.startRestoreTask(this, data.getData());
                    break;
            }
        }
    }
}
