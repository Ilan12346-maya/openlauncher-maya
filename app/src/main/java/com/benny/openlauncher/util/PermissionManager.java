package com.benny.openlauncher.util;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.R;
import com.benny.openlauncher.notifications.NotificationListener;
import com.benny.openlauncher.viewutil.DialogHelper;

import java.util.Set;

public class PermissionManager {

    public static void checkNotificationPermissions(@NonNull final Context context) {
        Set<String> appList = NotificationManagerCompat.getEnabledListenerPackages(context);
        for (String app : appList) {
            if (app.equals(context.getPackageName())) {
                // Already allowed, so request a full update when returning to the home screen from another app.
                Intent i = new Intent(NotificationListener.UPDATE_NOTIFICATIONS_ACTION);
                i.setPackage(context.getPackageName());
                i.putExtra(NotificationListener.UPDATE_NOTIFICATIONS_COMMAND, NotificationListener.UPDATE_NOTIFICATIONS_UPDATE);
                context.sendBroadcast(i);
                return;
            }
        }

        // Request the required permission otherwise.
        DialogHelper.alertDialog(context, context.getString(R.string.notification_title), context.getString(R.string.notification_summary), context.getString(R.string.enable), new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                Tool.toast(context, context.getString(R.string.toast_notification_permission_required));
                context.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
    }
}
