package com.benny.openlauncher.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

import com.benny.openlauncher.AppObject;
import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.widget.AppDrawerController;
import com.benny.openlauncher.widget.PagerIndicator;

import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AppSettings extends SharedPreferencesPropertyBackend {
    private static AppSettings _instance;

    public AppSettings(Context context) {
        super(context, "app");
    }

    public static AppSettings get() {
        if (_instance == null) {
            _instance = new AppSettings(AppObject.get());
        }
        return _instance;
    }

    public int getDesktopColumnCount() {
        return getInt(R.string.pref_key__desktop_columns, 5);
    }

    public int getDesktopRowCount() {
        return getInt(R.string.pref_key__desktop_rows, 6);
    }

    public int getDesktopIndicatorMode() {
        return getIntOfStringPref(R.string.pref_key__desktop_indicator_style, PagerIndicator.Mode.DOTS);
    }

    public int getDesktopOrientationMode() {
        return getIntOfStringPref(R.string.pref_key__desktop_orientation, 0);
    }

    public Definitions.WallpaperScroll getDesktopWallpaperScroll() {
        int value = getIntOfStringPref(R.string.pref_key__desktop_wallpaper_scroll, 0);
        switch (value) {
            case 0:
            default:
                return Definitions.WallpaperScroll.Normal;
            case 1:
                return Definitions.WallpaperScroll.Inverse;
            case 2:
                return Definitions.WallpaperScroll.Off;
        }
    }

    public boolean getDesktopShowGrid() {
        return getBool(R.string.pref_key__desktop_show_grid, true);
    }

    public boolean getDesktopFullscreen() {
        return getBool(R.string.pref_key__desktop_fullscreen, false);
    }

    public boolean getDesktopShowIndicator() {
        return getBool(R.string.pref_key__desktop_show_position_indicator, true);
    }

    public boolean getDesktopShowLabel() {
        return getBool(R.string.pref_key__desktop_show_label, true);
    }

    public boolean getDesktopInfiniteScrolling() {
        return getBool(R.string.pref_key__desktop_infinite_scrolling, false);
    }

    public boolean getSearchBarEnable() {
        return getBool(R.string.pref_key__search_bar_enable, true);
    }

    public boolean getSearchBarStartsWith() {
        return getBool(R.string.pref_key__search_bar_starts_with, true);
    }

    public String getSearchBarBaseURI() {
        return getString(R.string.pref_key__search_bar_base_uri, R.string.pref_default__search_bar_base_uri);
    }

    public boolean getSearchBarForceBrowser() {
        return getBool(R.string.pref_key__search_bar_force_browser, false);
    }

    public boolean getSearchBarShouldShowHiddenApps() {
        return getBool(R.string.pref_key__search_bar_show_hidden_apps, false);
    }

    public int getDesktopBackgroundColor() {
        return getInt(R.string.pref_key__desktop_background_color, Color.TRANSPARENT);
    }

    public int getDesktopInsetColor() {
        return getInt(R.string.pref_key__desktop_inset_color, Color.TRANSPARENT);
    }

    public int getDesktopFolderColor() {
        return getInt(R.string.pref_key__desktop_folder_color, Color.WHITE);
    }

    public int getDesktopIconSize() {
        return getIconSize();
    }

    public boolean getDockEnable() {
        return getBool(R.string.pref_key__dock_enable, true);
    }

    public int getDockColumnCount() {
        return getInt(R.string.pref_key__dock_columns, 5);
    }

    public int getDockRowCount() {
        return getInt(R.string.pref_key__dock_rows, 1);
    }

    public boolean getDockShowLabel() {
        return getBool(R.string.pref_key__dock_show_label, false);
    }

    public boolean getDockIosStyle() {
        return getBool(R.string.pref_key__dock_ios_style, false);
    }

    public int getDockColor() {
        return getInt(R.string.pref_key__dock_background_color, Color.TRANSPARENT);
    }

    public int getDockAlpha() {
        return getInt(R.string.pref_key__dock_background_alpha, 150);
    }

    public int getDockIconSize() {
        return getIconSize();
    }

    public int getDrawerColumnCount() {
        return getInt(R.string.pref_key__drawer_columns, 5);
    }

    public int getDrawerRowCount() {
        return getInt(R.string.pref_key__drawer_rows, 6);
    }

    public int getDrawerStyle() {
        return getIntOfStringPref(R.string.pref_key__drawer_style, AppDrawerController.Mode.GRID);
    }

    public boolean getDrawerShowCardView() {
        return getBool(R.string.pref_key__drawer_show_card_view, true);
    }

    public boolean getDrawerRememberPosition() {
        return getBool(R.string.pref_key__drawer_remember_position, true);
    }

    public boolean getDrawerShowIndicator() {
        return getBool(R.string.pref_key__drawer_show_position_indicator, true);
    }

    public boolean getDrawerShowLabel() {
        return getBool(R.string.pref_key__drawer_show_label, true);
    }

    public int getDrawerBackgroundColor() {
        return getInt(R.string.pref_key__drawer_background_color, rcolor(R.color.shade));
    }

    public int getDrawerCardColor() {
        return getInt(R.string.pref_key__drawer_card_color, rcolor(R.color.shade));
    }

    public int getDrawerLabelColor() {
        return getInt(R.string.pref_key__drawer_label_color, Color.WHITE);
    }

    public int getDrawerFastScrollColor() {
        return getInt(R.string.pref_key__drawer_fast_scroll_color, ContextCompat.getColor(Setup.appContext(), R.color.materialRed));
    }

    public boolean getGestureFeedback() {
        return getBool(R.string.pref_key__gesture_feedback, false);
    }

    public boolean getGestureDockSwipeUp() {
        return getBool(R.string.pref_key__gesture_quick_swipe, true);
    }

    public Object getGestureDoubleTap() {
        return getGesture(R.string.pref_key__gesture_double_tap);
    }

    public Object getGestureSwipeUpTop() {
        return getGesture(R.string.pref_key__gesture_swipe_up_top);
    }

    public Object getGestureSwipeUpBottom() {
        return getGesture(R.string.pref_key__gesture_swipe_up_bottom);
    }

    public Object getGestureSwipeDownTop() {
        return getGesture(R.string.pref_key__gesture_swipe_down_top);
    }

    public Object getGestureSwipeDownBottom() {
        return getGesture(R.string.pref_key__gesture_swipe_down_bottom);
    }

    public Object getGesturePinch() {
        return getGesture(R.string.pref_key__gesture_pinch_in);
    }

    public Object getGestureUnpinch() {
        return getGesture(R.string.pref_key__gesture_pinch_out);
    }

    public Object getGesture(int key) {
        // return either ActionItem or Intent
        String result = getString(key, "");
        Object gesture = LauncherAction.getActionItem(result, _context);
        // no action was found so it must be an intent string
        if (gesture == null) {
            gesture = Tool.getIntentFromString(result);
            if (AppManager.getInstance(_context).findApp((Intent) gesture) == null) gesture = null;
        }
        // reset the setting if invalid value
        if (gesture == null) {
            setString(key, null);
        }
        return gesture;
    }

    public String getTheme() {
        return getString(R.string.pref_key__theme, "1");
    }

    public int getPrimaryColor() {
        return getInt(R.string.pref_key__primary_color, ContextCompat.getColor(_context, R.color.colorPrimary));
    }

    public int getIconSize() {
        return getInt(R.string.pref_key__icon_size, 52);
    }

    public String getIconPack() {
        return getString(R.string.pref_key__icon_pack, "");
    }

    public boolean getNotificationStatus() {
        return getBool(R.string.pref_key__gesture_notifications, false);
    }

    public void setIconPack(String value) {
        setString(R.string.pref_key__icon_pack, value);
    }

    public int getAnimationSpeed() {
        // invert the value because it is used as a multiplier
        return 100 - getInt(R.string.pref_key__animation_speed, 80);
    }

    public String getLanguage() {
        return getString(R.string.pref_key__language, "");
    }

    // internal preferences below here
    public boolean getSearchUseGrid() {
        return getBool(R.string.pref_key__desktop_search_use_grid, false);
    }

    public void setSearchUseGrid(boolean enabled) {
        setBool(R.string.pref_key__desktop_search_use_grid, enabled);
    }

    public ArrayList<String> getHiddenAppsList() {
        return getStringList(R.string.pref_key__hidden_apps);
    }

    public void setHiddenAppsList(ArrayList<String> value) {
        setStringList(R.string.pref_key__hidden_apps, value);
    }

    public int getDesktopPageCurrent() {
        return getInt(R.string.pref_key__desktop_current_position, 0);
    }

    public void setDesktopPageCurrent(int value) {
        setInt(R.string.pref_key__desktop_current_position, value);
    }

    public boolean getDesktopLock() {
        return getBool(R.string.pref_key__desktop_lock, false);
    }

    public void setDesktopLock(boolean value) {
        setBool(R.string.pref_key__desktop_lock, value);
    }

    public boolean getAppRestartRequired() {
        return getBool(R.string.pref_key__queue_restart, false);
    }

    @SuppressLint("ApplySharedPref")
    public void setAppRestartRequired(boolean value) {
        // MUST be committed
        _prefApp.edit().putBoolean(_context.getString(R.string.pref_key__queue_restart), value).commit();
    }

    @SuppressLint("ApplySharedPref")
    public void setAppShowIntro(boolean value) {
        // MUST be committed
        _prefApp.edit().putBoolean(_context.getString(R.string.pref_key__show_intro), value).commit();
    }

    public boolean getAppFirstLaunch() {
        return getBool(R.string.pref_key__first_start, true);
    }

    @SuppressLint("ApplySharedPref")
    public void setAppFirstLaunch(boolean value) {
        // MUST be committed
        _prefApp.edit().putBoolean(_context.getString(R.string.pref_key__first_start), value).commit();
    }

    // Page 0 Settings
    public boolean getDesktopPage0Enabled() {
        return getBool("pref_key__desktop_page_0_enabled", true);
    }

    public void setDesktopPage0Enabled(boolean value) {
        setBool("pref_key__desktop_page_0_enabled", value);
    }

    public String getDesktopPage0Url() {
        return getString("pref_key__desktop_page_0_url", "https://www.google.de");
    }

    public void setDesktopPage0Url(String value) {
        setString("pref_key__desktop_page_0_url", value);
    }

    public boolean getDesktopPage0Persistence() {
        return getBool("pref_key__desktop_page_0_persistence", false);
    }

    public void setDesktopPage0Persistence(boolean value) {
        setBool("pref_key__desktop_page_0_persistence", value);
    }

    public boolean getDesktopPage0ShowNavigation() {
        return getBool("pref_key__desktop_page_0_show_navigation", true);
    }

    public void setDesktopPage0ShowNavigation(boolean value) {
        setBool("pref_key__desktop_page_0_show_navigation", value);
    }

    public int getDesktopPage0PillSize() {
        return getInt("pref_key__desktop_page_0_pill_size", 60);
    }

    public void setDesktopPage0PillSize(int value) {
        setInt("pref_key__desktop_page_0_pill_size", value);
    }

    public int getDesktopPage0PillMargin() {
        return getInt("pref_key__desktop_page_0_pill_margin", 20);
    }

    public void setDesktopPage0PillMargin(int value) {
        setInt("pref_key__desktop_page_0_pill_margin", value);
    }

    public float getDesktopPage0PillAspectRatio() {
        return (float) getInt("pref_key__desktop_page_0_pill_aspect_ratio", 25) / 10f;
    }

    public void setDesktopPage0PillAspectRatio(float value) {
        setInt("pref_key__desktop_page_0_pill_aspect_ratio", (int) (value * 10f));
    }

    public ArrayList<String> getRecentApps() {
        ArrayList<String> recentApps = getStringList("pref_key__recent_apps");
        return recentApps != null ? recentApps : new ArrayList<String>();
    }

    public void addRecentApp(String packageName, String className) {
        ArrayList<String> recentApps = getRecentApps();
        String component = packageName + "/" + className;
        recentApps.remove(component);
        recentApps.add(0, component);
        while (recentApps.size() > 10) {
            recentApps.remove(recentApps.size() - 1);
        }
        setStringList("pref_key__recent_apps", recentApps);
    }

    public boolean getDebugMode() {
        return getBool(R.string.pref_key__debug_mode, false);
    }

    public void setDebugMode(boolean value) {
        setBool(R.string.pref_key__debug_mode, value);
    }
}
