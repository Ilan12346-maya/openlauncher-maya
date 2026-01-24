package com.benny.openlauncher.util;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.benny.openlauncher.BuildConfig;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.HistoryManager;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.widget.AppItemView;

import java.util.List;

public class AppLauncher {

    public static void startApp(@NonNull Context context, @NonNull App app, @Nullable View view) {
        HistoryManager.getInstance(context).addRecentApp(app);

        if (BuildConfig.APPLICATION_ID.equals(app._packageName)) {
            LauncherAction.RunAction(LauncherAction.Action.LauncherSettings, context);
            return;
        }

        try {
            Bundle opts = getActivityAnimationOpts(view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && app._userHandle != null) {
                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> activities = launcherApps.getActivityList(app.getPackageName(), app._userHandle);
                for (int i = 0; i < activities.size(); i++) {
                    if (app.getComponentName().equals(activities.get(i).getComponentName().toString())) {
                        launcherApps.startMainActivity(activities.get(i).getComponentName(), app._userHandle, null, opts);
                        break;
                    }
                }
            } else {
                Intent intent = Tool.getIntentFromApp(app);
                context.startActivity(intent, opts);
            }

            // Collapse search bar, app drawer etc.
            HomeActivity launcher = HomeActivity.Companion.getLauncher();
            if (launcher != null) {
                launcher.onAppLaunched();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Tool.toast(context, R.string.toast_app_uninstalled);
        }
    }

    private static Bundle getActivityAnimationOpts(View view) {
        Bundle bundle = null;
        if (view == null) return null;

        ActivityOptions options = null;
        if (Build.VERSION.SDK_INT >= 23) {
            int left = 0, top = 0;
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
            if (view instanceof AppItemView) {
                width = (int) ((AppItemView) view).getIconSize();
                left = (int) ((AppItemView) view).getDrawIconLeft();
                top = (int) ((AppItemView) view).getDrawIconTop();
            }
            options = ActivityOptions.makeClipRevealAnimation(view, left, top, width, height);
        } else if (Build.VERSION.SDK_INT < 21) {
            options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        if (options != null) {
            bundle = options.toBundle();
        }
        return bundle;
    }
}
