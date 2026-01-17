package com.benny.openlauncher.util;

import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.activity.SettingsActivity;
import com.benny.openlauncher.viewutil.DialogHelper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class LauncherAction {

    public enum Action {
        SetWallpaper, LockScreen, LauncherSettings, VolumeDialog, DeviceSettings, AppDrawer, SearchBar, MobileNetworkSettings, ShowNotifications, TurnOffScreen, Camera, Restart, RecentApps, OpenQuickRecentDrawer
    }

    public static ActionDisplayItem[] actionDisplayItems = new ActionDisplayItem[]{
            new ActionDisplayItem(Action.SetWallpaper, R.string.action_title__set_wallpaper, R.string.action_summary__set_wallpaper, R.drawable.ic_photo, 36),
            new ActionDisplayItem(Action.LockScreen, R.string.action_title__lock_screen, R.string.action_summary__lock_screen, R.drawable.ic_lock, 24),
            new ActionDisplayItem(Action.LauncherSettings, R.string.action_title__launcher_settings, R.string.action_summary__launcher_settings, R.drawable.ic_settings, 50),
            new ActionDisplayItem(Action.VolumeDialog, R.string.action_title__volume_dialog, R.string.action_summary__volume_dialog, R.drawable.ic_volume, 71),
            new ActionDisplayItem(Action.DeviceSettings, R.string.action_title__device_settings, R.string.action_summary__device_settings, R.drawable.ic_android, 25),
            new ActionDisplayItem(Action.AppDrawer, R.string.action_title__app_drawer, R.string.action_summary__app_drawer, R.drawable.ic_apps, 73),
            new ActionDisplayItem(Action.SearchBar, R.string.action_title__search_bar, R.string.action_summary__search_bar, R.drawable.ic_search, 89),
            new ActionDisplayItem(Action.MobileNetworkSettings, R.string.action_title__mobile_network, R.string.action_summary__mobile_network, R.drawable.ic_network, 46),
            new ActionDisplayItem(Action.ShowNotifications, R.string.action_title__notification_bar, R.string.action_summary__notification_bar, R.drawable.ic_notifications, 46),
            new ActionDisplayItem(Action.Camera, R.string.action_title__camera, R.string.action_summary__camera, R.drawable.ic_camera_, 13),
            new ActionDisplayItem(Action.RecentApps, R.string.action_title__recent_apps, R.string.action_summary__recent_apps, R.drawable.ic_desktop, 14),
            new ActionDisplayItem(Action.OpenQuickRecentDrawer, R.string.action_title__quick_recent_drawer, R.string.action_title__quick_recent_drawer, R.drawable.ic_apps, 15),
            new ActionDisplayItem(Action.Restart, R.string.on, R.string.on, R.drawable.ic_android, 99)
    };

    public static List<Action> defaultArrangement = Arrays.asList(
            Action.SetWallpaper,
            Action.LockScreen, Action.LauncherSettings,
            Action.VolumeDialog, Action.DeviceSettings,
            Action.Camera
    );

    public static void RunAction(Action action, final Context context) {
        LauncherAction.RunAction(getActionItem(action, context), context);
    }

    @SuppressWarnings("WrongConstant")
    public static void RunAction(ActionDisplayItem action, final Context context) {
        if (action == null) return;
        switch (action._action) {
            case SetWallpaper:
                context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), context.getString(R.string.select_wallpaper)));
                break;
            case LockScreen:
                try {
                    ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).lockNow();
                } catch (Exception e) {
                    DialogHelper.alertDialog(context, context.getString(R.string.device_admin_title), context.getString(R.string.device_admin_summary), context.getString(R.string.enable), new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Tool.toast(context, context.getString(R.string.toast_device_admin_required));
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"));
                            context.startActivity(intent);
                        }
                    });
                }
                break;
            case DeviceSettings:
                context.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                break;
            case LauncherSettings:
                context.startActivity(new Intent(context, SettingsActivity.class));
                break;
            case VolumeDialog:
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
                    } catch (Exception e) {
                        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            context.startActivity(intent);
                        }
                    }
                } else {
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
                }
                break;
            case AppDrawer:
                HomeActivity._launcher.openAppDrawer();
                break;
            case SearchBar:
                HomeActivity._launcher.getSearchBar().getSearchButton().performClick();
                break;
            case MobileNetworkSettings:
                context.startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
                break;
            case ShowNotifications:
                try {
                    Object statusBarService = context.getSystemService("statusbar");
                    Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");
                    Method statusBarExpand = statusBarManager.getMethod("expandNotificationsPanel");
                    statusBarExpand.invoke(statusBarService);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case TurnOffScreen:
                try {
                    // still needs to reset screen timeout back to default on activity destroy
                    int defaultTurnOffTime = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 60000);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, defaultTurnOffTime);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    context.startActivity(intent);
                }
                break;
            case Camera:
                context.startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                break;
            case RecentApps:
                try {
                    Object statusBarService = context.getSystemService("statusbar");
                    Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");
                    Method statusBarExpand = statusBarManager.getMethod("toggleRecentApps");
                    statusBarExpand.invoke(statusBarService);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case OpenQuickRecentDrawer:
                if (HomeActivity._launcher != null) {
                    HomeActivity._launcher.openQuickRecentDrawer();
                }
                break;
            case Restart:
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).recreate();
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                break;

        }
    }

    public static ActionDisplayItem getActionItem(int position, Context context) {
        // used for pick action dialog
        return getActionItem(Action.values()[position], context);
    }

    public static ActionDisplayItem getActionItem(Action action, Context context) {
        return getActionItem(action.toString(), context);
    }

    public static ActionDisplayItem getActionItem(String action, Context context) {
        for (ActionDisplayItem item : actionDisplayItems) {
            if (item._action.toString().equals(action)) {
                if (context != null) item.loadStrings(context);
                return item;
            }
        }
        return null;
    }

    public static class ActionDisplayItem {
        public Action _action;
        public String _label;
        public String _description;
        public int _labelRes;
        public int _descriptionRes;
        public int _icon;
        public int _id;

        public ActionDisplayItem(Action action, int labelRes, int descriptionRes, int icon, int id) {
            _action = action;
            _labelRes = labelRes;
            _descriptionRes = descriptionRes;
            _icon = icon;
            _id = id;
        }

        public void loadStrings(Context context) {
            _label = context.getString(_labelRes);
            _description = context.getString(_descriptionRes);
        }
    }
}
