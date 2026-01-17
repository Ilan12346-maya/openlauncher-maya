package com.benny.openlauncher;

import android.app.Application;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.activity.CrashActivity;

public class AppObject extends Application {
    private static AppObject _instance;

    public static AppObject get() {
        return _instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;

        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT)
                .trackActivities(true)
                .minTimeBetweenCrashesMs(2000)
                .restartActivity(HomeActivity.class)
                .errorActivity(CrashActivity.class)
                .errorDrawable(R.drawable.ic_bug)
                .apply();
    }
}