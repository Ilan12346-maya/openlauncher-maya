package com.benny.openlauncher.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetManager;
import android.graphics.Color;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.BuildConfig;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.homeparts.HpAppDrawer;
import com.benny.openlauncher.activity.homeparts.HpDesktopOption;
import com.benny.openlauncher.activity.homeparts.HpDragOption;
import com.benny.openlauncher.activity.homeparts.HpInitSetup;
import com.benny.openlauncher.activity.homeparts.HpSearchBar;
import com.benny.openlauncher.interfaces.AppDeleteListener;
import com.benny.openlauncher.interfaces.AppUpdateListener;
import com.benny.openlauncher.manager.LauncherReceiverManager;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.DatabaseHelper;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.Definitions.ItemPosition;
import com.benny.openlauncher.util.LauncherAction;
import com.benny.openlauncher.util.LauncherAction.Action;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DialogHelper;
import com.benny.openlauncher.viewutil.WidgetHost;
import com.benny.openlauncher.widget.AppDrawerController;
import com.benny.openlauncher.widget.AppItemView;
import com.benny.openlauncher.widget.Desktop;
import com.benny.openlauncher.widget.Desktop.OnDesktopEditListener;
import com.benny.openlauncher.widget.DesktopOptionView;
import com.benny.openlauncher.widget.Dock;
import com.benny.openlauncher.widget.GroupPopupView;
import com.benny.openlauncher.widget.ItemOptionView;
import com.benny.openlauncher.widget.PagerIndicator;
import com.benny.openlauncher.widget.SearchBar;
import com.jakewharton.threetenabp.AndroidThreeTen;

import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.List;

public final class HomeActivity extends ColorActivity implements OnDesktopEditListener {
    public static final Companion Companion = new Companion();
    public static final int REQUEST_CREATE_APPWIDGET = 0x6475;
    public static final int REQUEST_PERMISSION_STORAGE = 0x3648;
    public static final int REQUEST_PICK_APPWIDGET = 0x2678;
    public static final int REQUEST_BACKUP = Definitions.INTENT_BACKUP;
    public static final int REQUEST_RESTORE = Definitions.INTENT_RESTORE;
    public static WidgetHost _appWidgetHost;
    public static AppWidgetManager _appWidgetManager;
    public static boolean ignoreResume;
    public static float _itemTouchX;
    public static float _itemTouchY;

    // static launcher variables
    private static HomeActivity _launcher;
    public static HpDesktopOption _desktopOption;

    // receiver variables
    private LauncherReceiverManager _receiverManager;
    private boolean _page0Enabled;

    private int cx;
    private int cy;

    public static final class Companion {
        private Companion() {
        }

        public final HomeActivity getLauncher() {
            return _launcher;
        }

        public final void setLauncher(@Nullable HomeActivity v) {
            _launcher = v;
        }
    }

    public final DrawerLayout getDrawerLayout() {
        return findViewById(R.id.drawer_layout);
    }

    public final Desktop getDesktop() {
        return findViewById(R.id.desktop);
    }

    public final Dock getDock() {
        return findViewById(R.id.dock);
    }

    public final AppDrawerController getAppDrawerController() {
        return findViewById(R.id.appDrawerController);
    }

    public final GroupPopupView getGroupPopup() {
        return findViewById(R.id.groupPopup);
    }

    public final GroupPopupView getGroupPopupView() {
        return findViewById(R.id.groupPopup);
    }

    public final SearchBar getSearchBar() {
        return findViewById(R.id.searchBar);
    }

    public final View getBackground() {
        return findViewById(R.id.background_frame);
    }

    public final PagerIndicator getDesktopIndicator() {
        return findViewById(R.id.desktopIndicator);
    }

    public final DesktopOptionView getDesktopOptionView() {
        return findViewById(R.id.desktop_option);
    }

    public final ItemOptionView getItemOptionView() {
        return findViewById(R.id.item_option);
    }

    public final View getStatusView() {
        return findViewById(R.id.status_frame);
    }

    public final View getNavigationView() {
        return findViewById(R.id.navigation_frame);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Companion.setLauncher(this);
        AndroidThreeTen.init(this);

        AppSettings appSettings = AppSettings.get();
        _page0Enabled = appSettings.getDesktopPage0Enabled();

        ContextUtils contextUtils = new ContextUtils(getApplicationContext());
        contextUtils.setAppLanguage(appSettings.getLanguage());
        super.onCreate(savedInstanceState);
        if (!Setup.wasInitialised()) {
            Setup.init(new HpInitSetup(this));
        }

        Companion.setLauncher(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleLauncherResume();
            }
        });

        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setContentView(getLayoutInflater().inflate(R.layout.activity_home, null));

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }

        // transparent status and navigation
        if (Build.VERSION.SDK_INT >= 21) {
            setSystemBarsVisible(false);
        }

        init(); // This call should be here

        final View itemOption = findViewById(R.id.item_option);
        if (itemOption != null) {
            ViewGroup.LayoutParams params = itemOption.getLayoutParams();
            params.height = 2480;
            itemOption.setLayoutParams(params);
        }
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(itemOption, (v, insets) -> {
            if (getDesktop().getCurrentItem() == 0) {
                v.setPadding(0, 0, 0, 0);
                return insets.inset(insets.getInsets(WindowInsetsCompat.Type.systemBars()));
            }
            return insets; 
        });
    }

    private void init() {
        _appWidgetManager = AppWidgetManager.getInstance(this);
        _appWidgetHost = new WidgetHost(getApplicationContext(), 100);
        _appWidgetHost.startListening();

        _receiverManager = new LauncherReceiverManager(this);
        _receiverManager.registerReceivers();

        initAppManager();
        initSettings();
        initViews();

        // item drag and drop
        HpDragOption hpDragOption = new HpDragOption();
        View findViewById = findViewById(R.id.leftDragHandle);
        View findViewById2 = findViewById(R.id.rightDragHandle);
        hpDragOption.initDragNDrop(this, findViewById, findViewById2, getItemOptionView());
    }

    protected void initAppManager() {
        if (Setup.appSettings().getAppFirstLaunch()) {
            Setup.appSettings().setAppFirstLaunch(false);
            Setup.appSettings().setAppShowIntro(false);
            Item appDrawerBtnItem = Item.newActionItem(8);
            appDrawerBtnItem._x = 2;
            Setup.dataManager().saveItem(appDrawerBtnItem, 0, ItemPosition.Dock);
        }
        Setup.appLoader().addUpdateListener(new AppUpdateListener() {
            @Override
            public boolean onAppUpdated(List<App> it) {
                getDesktop().initDesktop();
                getDock().initDock();
                return false;
            }
        });
        Setup.appLoader().addDeleteListener(new AppDeleteListener() {
            @Override
            public boolean onAppDeleted(List<App> apps) {
                getDesktop().initDesktop();
                getDock().initDock();
                return false;
            }
        });
        AppManager.getInstance(this).init();
    }

    protected void initViews() {
        getAppDrawerController().init();
        getDock().setHome(this);

        getDesktop().setDesktopEditListener(this);
        getDesktop().setPageIndicator(getDesktopIndicator());
        getDesktopIndicator().setMode(Setup.appSettings().getDesktopIndicatorMode());

        getDesktop().initDesktop(new Runnable() {
            @Override
            public void run() {
                getAppDrawerController().loadApps();
            }
        });

        AppSettings appSettings = Setup.appSettings();

        _desktopOption = new HpDesktopOption(this);

        getDesktopOptionView().setDesktopOptionViewListener(_desktopOption);
        getDesktopOptionView().postDelayed(new Runnable() {
            @Override
            public void run() {
                getDesktopOptionView().updateLockIcon(appSettings.getDesktopLock());
            }
        }, 100);

        getDesktop().addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int _lastVisibility = -1;
            private float _lastAlpha = -1f;

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (getDesktop().getInEditMode()) {
                    return;
                }
                View iosDockBg = findViewById(R.id.ios_dock_background);
                if (appSettings.getDesktopPage0Enabled() && position == 0) {
                    float alpha = positionOffset;
                    
                    if (Math.abs(_lastAlpha - alpha) > 0.005f) {
                        _lastAlpha = alpha;
                        getSearchBar().setAlpha(alpha);
                        getDock().setAlpha(alpha);
                        if (iosDockBg != null) iosDockBg.setAlpha(alpha);
                        getDesktopIndicator().setAlpha(alpha);
                    }

                    int visibility = (alpha <= 0.005f) ? View.GONE : View.VISIBLE;
                    if (_lastVisibility != visibility) {
                        _lastVisibility = visibility;
                        
                        if (visibility == View.GONE) {
                            getSearchBar().setVisibility(View.GONE);
                            getDock().setVisibility(View.GONE);
                            if (iosDockBg != null) iosDockBg.setVisibility(View.GONE);
                            getDesktopIndicator().setVisibility(View.GONE);
                        } else {
                            if (appSettings.getDockEnable()) {
                                getDock().setVisibility(View.VISIBLE);
                                if (iosDockBg != null && appSettings.getDockIosStyle()) iosDockBg.setVisibility(View.VISIBLE);
                            }
                            getDesktopIndicator().setVisibility(appSettings.getDesktopShowIndicator() ? View.VISIBLE : View.GONE);
                        }
                    }
                } else {
                    if (_lastAlpha != 1.0f) {
                        _lastAlpha = 1.0f;
                        getSearchBar().setAlpha(1.0f);
                        getDock().setAlpha(1.0f);
                        if (iosDockBg != null) iosDockBg.setAlpha(1.0f);
                        getDesktopIndicator().setAlpha(1.0f);
                    }
                    
                    if (_lastVisibility != View.VISIBLE) {
                        _lastVisibility = View.VISIBLE;
                        if (!getDesktop().getInEditMode()) {
                            if (appSettings.getDockEnable()) {
                                getDock().setVisibility(View.VISIBLE);
                                if (iosDockBg != null && appSettings.getDockIosStyle()) iosDockBg.setVisibility(View.VISIBLE);
                            }
                            getDesktopIndicator().setVisibility(appSettings.getDesktopShowIndicator() ? View.VISIBLE : View.GONE);
                        }
                    }
                }
            }

            public void onPageSelected(int position) {
                com.benny.openlauncher.util.Logger.log("HomeActivity", "onPageSelected: " + position);
                getDesktopOptionView().updateHomeIcon(appSettings.getDesktopPageCurrent() == (position - 1));
                
                // Force layout update
                final View itemOption = findViewById(R.id.item_option);
                itemOption.requestApplyInsets();

                if (getDesktop().getInEditMode()) {
                    return;
                }

                if (appSettings.getDesktopPage0Enabled() && position == 0) {
                    setSystemBarsVisible(false);
                } else {
                    setSystemBarsVisible(!appSettings.getDesktopFullscreen());
                }
            }

            public void onPageScrollStateChanged(int state) {
                com.benny.openlauncher.util.Logger.log("HomeActivity", "onPageScrollStateChanged: " + state);
                AppSettings appSettings = AppSettings.get();
                if (appSettings.getDesktopInfiniteScrolling() && !getDesktop().getInEditMode() && !getItemOptionView().getDragging()) {
                    int current = getDesktop().getCurrentItem();
                    int count = getDesktop().getAdapter().getCount();
                    boolean page0Enabled = appSettings.getDesktopPage0Enabled();
                    
                    if (state == ViewPager.SCROLL_STATE_IDLE || state == ViewPager.SCROLL_STATE_DRAGGING) {
                        if (page0Enabled) {
                            // WV | P1 | P2 | P3 | D(P1)
                            if (current == count - 1) {
                                getDesktop().setCurrentItem(1, false);
                            }
                        } else {
                            // D(PN) | P1 | P2 | P3 | D(P1)
                            if (current == 0) {
                                getDesktop().setCurrentItem(count - 2, false);
                            } else if (current == count - 1) {
                                getDesktop().setCurrentItem(1, false);
                            }
                        }
                    }
                }
            }
        });

        new HpAppDrawer(this, findViewById(R.id.appDrawerIndicator)).initAppDrawer(getAppDrawerController());
    }

    public final void initSettings() {
        updateHomeLayout();

        AppSettings appSettings = Setup.appSettings();
        setSystemBarsVisible(!appSettings.getDesktopFullscreen());

        // set background colors
        getDesktop().setBackgroundColor(appSettings.getDesktopBackgroundColor());
        getDock().setBackgroundColor(appSettings.getDockIosStyle() ? Color.TRANSPARENT : appSettings.getDockColor());

        // set frame colors
        getStatusView().setBackgroundColor(appSettings.getDesktopInsetColor());
        getNavigationView().setBackgroundColor(appSettings.getDesktopInsetColor());

        // iOS dock background
        View iosDockBg = findViewById(R.id.ios_dock_background);
        if (iosDockBg != null) {
            iosDockBg.setAlpha(0f);
            if (appSettings.getDockIosStyle()) {
                iosDockBg.setVisibility(View.VISIBLE);
                
                // Get user selected color and apply separate alpha
                int userColor = appSettings.getDockColor();
                int alpha = appSettings.getDockAlpha();
                
                // If user color is transparent (0), we fall back to a sensible default based on theme
                if (userColor == Color.TRANSPARENT) {
                    if (appSettings.getTheme().equals("0")) { // Light
                        userColor = Color.WHITE;
                    } else { // Dark or Black
                        userColor = Color.rgb(50, 50, 50);
                    }
                }
                
                // Combine RGB with our specific alpha
                int finalColor = Color.argb(alpha, Color.red(userColor), Color.green(userColor), Color.blue(userColor));
                
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColor(finalColor);
                gd.setCornerRadius(Tool.dp2px(20));
                iosDockBg.setBackground(gd);

                // iOS style: 110% larger icons than settings, 30% gaps
                int settingsIconSize = Tool.dp2px(appSettings.getIconSize());
                int dockIconSize = (int) (settingsIconSize * 1.1f);
                float gap = settingsIconSize * 0.30f;
                
                // Height: dockIconSize + 2 * gap = 1.1 + 0.6 = 1.7 * settingsIconSize
                int bgHeight = (int) (settingsIconSize * 1.7f) + Tool.dp2px(10);
                
                iosDockBg.post(() -> {
                    ViewGroup.LayoutParams params = iosDockBg.getLayoutParams();
                    params.height = bgHeight;
                    
                    int columns = appSettings.getDockColumnCount();
                    // Width: columns * dockIconSize + (columns + 1) * gap
                    int bgWidth = (int) (columns * dockIconSize + (columns + 1) * gap);
                    params.width = bgWidth;
                    
                    if (params instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) params;
                        lp.leftMargin = 0;
                        lp.rightMargin = 0;
                    }
                    iosDockBg.setLayoutParams(params);
                });
            } else {
                iosDockBg.setVisibility(View.GONE);
                getDock().setTranslationY(0);
            }
        }
    }

    public final void onAppLaunched() {
        handleLauncherResume();
    }

    public void onStartDesktopEdit() {
        Tool.visibleViews(100, getDesktopOptionView());
        updateDesktopIndicator(false);
        updateDock(false);
        updateSearchBar(false);
        setSystemBarsVisible(false);
    }

    public void onFinishDesktopEdit() {
        Tool.invisibleViews(100, getDesktopOptionView());
        updateDesktopIndicator(true);
        updateDock(true);
        updateSearchBar(true);
        
        // Restore system bars only if not on page 0
        if (getDesktop().getCurrentItem() != 0 || !Setup.appSettings().getDesktopPage0Enabled()) {
            setSystemBarsVisible(true);
        }
    }

    public final void setSystemBarsVisible(boolean visible) {
        WindowInsetsControllerCompat controller = androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (visible) {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars() | androidx.core.view.WindowInsetsCompat.Type.navigationBars());
        } else {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() | androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    public final void dimBackground() {
        Tool.visibleViews(200, getBackground());
    }

    public final void unDimBackground() {
        Tool.invisibleViews(200, getBackground());
    }

    public final void clearRoomForPopUp() {
        Tool.invisibleViews(200, getDesktop());
        updateDesktopIndicator(false);
        updateDock(false);
    }

    public final void unClearRoomForPopUp() {
        Tool.visibleViews(200, getDesktop());
        updateDesktopIndicator(true);
        updateDock(true);
    }

    public final void updateDock(boolean show) {
        if (getDesktop() != null && getDesktop().getCurrentItem() == 0 && show) return;
        AppSettings appSettings = Setup.appSettings();
        View iosDockBg = findViewById(R.id.ios_dock_background);
        if (appSettings.getDockEnable() && show) {
            Tool.visibleViews(100, getDock());
            if (iosDockBg != null && appSettings.getDockIosStyle()) {
                Tool.visibleViews(100, iosDockBg);
            }
        } else {
            if (appSettings.getDockEnable()) {
                Tool.invisibleViews(100, getDock());
                if (iosDockBg != null) Tool.invisibleViews(100, iosDockBg);
            } else {
                Tool.goneViews(100, getDock());
                if (iosDockBg != null) Tool.goneViews(100, iosDockBg);
            }
        }
    }

    public final void updateSearchBar(boolean show) {
        // Search bar on home screen is disabled
        getSearchBar().setVisibility(View.GONE);
    }

    public final void updateDesktopIndicator(boolean show) {
        if (getDesktop() != null && getDesktop().getCurrentItem() == 0 && show) return;
        AppSettings appSettings = Setup.appSettings();
        if (appSettings.getDesktopShowIndicator() && show) {
            Tool.visibleViews(100, getDesktopIndicator());
        } else {
            Tool.goneViews(100, getDesktopIndicator());
        }
    }

    public final void updateHomeLayout() {
        updateSearchBar(true);
        updateDock(true);
        updateDesktopIndicator(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                _desktopOption.configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                _desktopOption.createWidget(data);
            } else if (requestCode == REQUEST_BACKUP && data != null) {
                com.benny.openlauncher.util.BackupManager.startBackupTask(this, data.getData());
            } else if (requestCode == REQUEST_RESTORE && data != null) {
                com.benny.openlauncher.util.BackupManager.startRestoreTask(this, data.getData());
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra("appWidgetId", -1);
            if (appWidgetId != -1) {
                _appWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.i("OpenLauncher", "HomeActivity: onResume");
        _appWidgetHost.startListening();
        _launcher = this;

        // handle restart if something needs to be reset
        AppSettings appSettings = Setup.appSettings();
        if (appSettings.getDesktopPage0Enabled() != _page0Enabled) {
            _page0Enabled = appSettings.getDesktopPage0Enabled();
            getDesktop().initDesktop();
        }

        if (appSettings.getAppRestartRequired()) {
            android.util.Log.i("OpenLauncher", "HomeActivity: Restart required, recreating...");
            appSettings.setAppRestartRequired(false);
            recreate();
            return;
        }

        if (appSettings.getNotificationStatus()) {
            // Ask user to allow the Notification permission if not already provided.
            com.benny.openlauncher.util.PermissionManager.checkNotificationPermissions(this);
        }

        // handle launcher rotation
        if (appSettings.getDesktopOrientationMode() == 2) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (appSettings.getDesktopOrientationMode() == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        handleLauncherResume();
    }

    @Override
    protected void onDestroy() {
        android.util.Log.i("OpenLauncher", "HomeActivity: onDestroy");
        _appWidgetHost.stopListening();
        if (_launcher == this) {
            _launcher = null;
        }

        if (_receiverManager != null) {
            _receiverManager.unregisterReceivers();
        }
        super.onDestroy();
    }

    private void handleLauncherResume() {
        android.util.Log.i("OpenLauncher", "HomeActivity: handleLauncherResume (ignoreResume=" + ignoreResume + ")");
        if (ignoreResume) {
            // only triggers when a new activity is launched that should leave launcher state alone
            // uninstall package activity and pick widget activity
            ignoreResume = false;
        } else {
            getSearchBar().collapse();
            getGroupPopup().collapse();
            // close app option menu
            getItemOptionView().collapse();
            if (getDesktop().getInEditMode()) {
                android.util.Log.i("OpenLauncher", "HomeActivity: Exiting edit mode on resume");
                getDesktop().exitDesktopEditMode();
            } else if (getAppDrawerController().getDrawer().getVisibility() == View.VISIBLE) {
                android.util.Log.i("OpenLauncher", "HomeActivity: Closing app drawer on resume");
                closeAppDrawer();
            }
            if (getDesktop().getCurrentItem() != 0) {
                AppSettings appSettings = Setup.appSettings();
                getDesktop().setCurrentItem(appSettings.getDesktopPageCurrent() + (appSettings.getDesktopPage0Enabled() ? 1 : 0));
            }
        }
    }

    public final void openAppDrawer() {
        openAppDrawer(null, 0, 0);
    }

    public final void openAppDrawer(View view, int x, int y) {
        if (!(x > 0 && y > 0) && view != null) {
            int[] pos = new int[2];
            view.getLocationInWindow(pos);
            cx = pos[0];
            cy = pos[1];

            cx += view.getWidth() / 2f;
            cy += view.getHeight() / 2f;
            if (view instanceof AppItemView) {
                AppItemView appItemView = (AppItemView) view;
                if (appItemView != null && appItemView.getShowLabel()) {
                    cy -= Tool.dp2px(14) / 2f;
                }
            }
            cy -= getAppDrawerController().getPaddingTop();
        } else {
            cx = x;
            cy = y;
        }
        getAppDrawerController().open(cx, cy);
    }

    public final void closeAppDrawer() {
        getAppDrawerController().close(cx, cy);
    }
}