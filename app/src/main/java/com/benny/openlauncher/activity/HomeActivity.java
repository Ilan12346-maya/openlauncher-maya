package com.benny.openlauncher.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
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
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.notifications.NotificationListener;
import com.benny.openlauncher.receivers.AppUpdateReceiver;
import com.benny.openlauncher.receivers.ShortcutReceiver;
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
import java.util.Set;

public final class HomeActivity extends Activity implements OnDesktopEditListener {
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
    public static HomeActivity _launcher;
    public static DatabaseHelper _db;
    public static HpDesktopOption _desktopOption;

    // receiver variables
    private static final IntentFilter _appUpdateIntentFilter = new IntentFilter();
    private static final IntentFilter _shortcutIntentFilter = new IntentFilter();
    private static final IntentFilter _timeChangedIntentFilter = new IntentFilter();
    private AppUpdateReceiver _appUpdateReceiver;
    private ShortcutReceiver _shortcutReceiver;
    private BroadcastReceiver _timeChangedReceiver;

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

    static {
        _timeChangedIntentFilter.addAction(Intent.ACTION_TIME_TICK);
        _timeChangedIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        _timeChangedIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        _appUpdateIntentFilter.addDataScheme("package");
        _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        _shortcutIntentFilter.addAction("com.android.launcher.action.INSTALL_SHORTCUT");
    }

    public final DrawerLayout getDrawerLayout() {
        return findViewById(R.id.drawer_layout);
    }

    public final Desktop getDesktop() {
        return findViewById(R.id.desktop);
    }

    public final void openQuickRecentDrawer() {
        final View container = findViewById(R.id.quick_recent_drawer_container);
        final android.widget.GridLayout grid = findViewById(R.id.recent_apps_grid);
        final View card = findViewById(R.id.quick_recent_card);

        if (container == null || grid == null || card == null) return;

        grid.removeAllViews();
        ArrayList<String> recentApps = AppSettings.get().getRecentApps();
        int iconSize = Setup.appSettings().getIconSize();

        for (String component : recentApps) {
            String[] parts = component.split("/");
            if (parts.length != 2) continue;

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new android.content.ComponentName(parts[0], parts[1]));

            final App app = Setup.appLoader().findApp(intent);
            if (app != null) {
                final AppItemView appView = new AppItemView(this);
                appView.setIconSize(Tool.dp2px(iconSize));
                appView.setLabel(app._label);
                
                // Nutze den Cache/Loader
                appView.setIcon(app.getIcon());
                if (app._icon == null) {
                    com.benny.openlauncher.util.iconloader.AsyncIconLoader.getInstance().loadIcon(app, new com.benny.openlauncher.util.iconloader.AsyncIconLoader.IconCallback() {
                        @Override
                        public void onIconLoaded(android.graphics.drawable.Drawable icon) {
                            appView.setIcon(icon);
                            appView.invalidate();
                        }
                    });
                }

                appView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Tool.startApp(HomeActivity.this, app, v);
                        container.setVisibility(View.GONE);
                    }
                });

                android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
                params.width = (getResources().getDisplayMetrics().widthPixels - Tool.dp2px(64)) / 5;
                params.setMargins(Tool.dp2px(4), Tool.dp2px(4), Tool.dp2px(4), Tool.dp2px(4));
                grid.addView(appView, params);
            }
        }

        container.setVisibility(View.VISIBLE);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                container.setVisibility(View.GONE);
            }
        });

        card.setTranslationY(Tool.dp2px(300));
        card.animate().translationY(0).setDuration(200).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
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

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Companion.setLauncher(this);
        AndroidThreeTen.init(this);

        AppSettings appSettings = AppSettings.get();

        ContextUtils contextUtils = new ContextUtils(getApplicationContext());
        contextUtils.setAppLanguage(appSettings.getLanguage());
        super.onCreate(savedInstanceState);
        if (!Setup.wasInitialised()) {
            Setup.init(new HpInitSetup(this));
        }

        Companion.setLauncher(this);
        _db = Setup.dataManager();

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
            Window window = getWindow();
            View decorView = window.getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            
            // Removed SYSTEM_UI_FLAG_LIGHT_STATUS_BAR to keep icons white
            decorView.setSystemUiVisibility(flags);
        }

        init();

        final View itemOption = findViewById(R.id.item_option);
        if (itemOption != null) {
            ViewGroup.LayoutParams params = itemOption.getLayoutParams();
            params.height = 2480;
            itemOption.setLayoutParams(params);
        }
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(itemOption, (v, insets) -> {
            if (getDesktop().getCurrentItem() == 0) {
                v.setPadding(0, 0, 0, 0);
                return insets.consumeSystemWindowInsets();
            }
            return insets; 
        });
    }

    private void init() {
        _appWidgetManager = AppWidgetManager.getInstance(this);
        _appWidgetHost = new WidgetHost(getApplicationContext(), R.id.app_widget_host);
        _appWidgetHost.startListening();

        // item drag and drop
        HpDragOption hpDragOption = new HpDragOption();
        View findViewById = findViewById(R.id.leftDragHandle);
        View findViewById2 = findViewById(R.id.rightDragHandle);
        hpDragOption.initDragNDrop(this, findViewById, findViewById2, getItemOptionView());

        registerBroadcastReceiver();
        initAppManager();
        initSettings();
        initViews();
    }

    protected void initAppManager() {
        if (Setup.appSettings().getAppFirstLaunch()) {
            Setup.appSettings().setAppFirstLaunch(false);
            Setup.appSettings().setAppShowIntro(false);
            Item appDrawerBtnItem = Item.newActionItem(8);
            appDrawerBtnItem._x = 2;
            _db.saveItem(appDrawerBtnItem, 0, ItemPosition.Dock);
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
        new HpSearchBar(this, getSearchBar()).initSearchBar();
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
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (getDesktop().getInEditMode()) {
                    return;
                }
                if (appSettings.getDesktopPage0Enabled() && position == 0) {
                    float alpha = positionOffset;
                    
                    getSearchBar().setAlpha(alpha);
                    getDock().setAlpha(alpha);
                    View iosDockBg = findViewById(R.id.ios_dock_background);
                    if (iosDockBg != null) iosDockBg.setAlpha(alpha);
                    getDesktopIndicator().setAlpha(alpha);
                    findViewById(R.id.status_frame).setAlpha(alpha);
                    findViewById(R.id.navigation_frame).setAlpha(alpha);
                    findViewById(R.id.leftDragHandle).setAlpha(0);
                    findViewById(R.id.rightDragHandle).setAlpha(0);

                    if (alpha == 0) {
                        getSearchBar().setVisibility(View.GONE);
                        getDock().setVisibility(View.GONE);
                        if (iosDockBg != null) iosDockBg.setVisibility(View.GONE);
                        getDesktopIndicator().setVisibility(View.GONE);
                        findViewById(R.id.status_frame).setVisibility(View.GONE);
                        findViewById(R.id.navigation_frame).setVisibility(View.GONE);
                    } else {
                        getSearchBar().setVisibility(appSettings.getSearchBarEnable() ? View.VISIBLE : View.GONE);
                        if (appSettings.getDockEnable()) {
                            getDock().setVisibility(View.VISIBLE);
                            if (iosDockBg != null && appSettings.getDockIosStyle()) iosDockBg.setVisibility(View.VISIBLE);
                        }
                        getDesktopIndicator().setVisibility(appSettings.getDesktopShowIndicator() ? View.VISIBLE : View.GONE);
                        findViewById(R.id.status_frame).setVisibility(View.VISIBLE);
                        findViewById(R.id.navigation_frame).setVisibility(View.VISIBLE);
                    }
                } else {
                    getSearchBar().setAlpha(1.0f);
                    getDock().setAlpha(1.0f);
                    View iosDockBg = findViewById(R.id.ios_dock_background);
                    if (iosDockBg != null) iosDockBg.setAlpha(1.0f);
                    getDesktopIndicator().setAlpha(1.0f);
                    findViewById(R.id.status_frame).setAlpha(1.0f);
                    findViewById(R.id.navigation_frame).setAlpha(1.0f);
                    findViewById(R.id.leftDragHandle).setAlpha(0.0f);
                    findViewById(R.id.rightDragHandle).setAlpha(0.0f);
                    
                    // Ensure visibility is restored if we were on page 0, but only if NOT in edit mode
                    if (!getDesktop().getInEditMode()) {
                        getSearchBar().setVisibility(appSettings.getSearchBarEnable() ? View.VISIBLE : View.GONE);
                        if (appSettings.getDockEnable()) {
                            getDock().setVisibility(View.VISIBLE);
                            if (iosDockBg != null && appSettings.getDockIosStyle()) iosDockBg.setVisibility(View.VISIBLE);
                        }
                        getDesktopIndicator().setVisibility(appSettings.getDesktopShowIndicator() ? View.VISIBLE : View.GONE);
                        findViewById(R.id.status_frame).setVisibility(View.VISIBLE);
                        findViewById(R.id.navigation_frame).setVisibility(View.VISIBLE);
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.view.WindowInsetsController controller = getWindow().getInsetsController();
                        if (controller != null) {
                            controller.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                            controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                        }
                    } else {
                        getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.view.WindowInsetsController controller = getWindow().getInsetsController();
                        if (controller != null) {
                            controller.show(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                        }
                    } else {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                    }
                }
            }

            public void onPageScrollStateChanged(int state) {
                com.benny.openlauncher.util.Logger.log("HomeActivity", "onPageScrollStateChanged: " + state);
                AppSettings appSettings = AppSettings.get();
                if (appSettings.getDesktopInfiniteScrolling() && !getDesktop().getInEditMode()) {
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
        if (appSettings.getDesktopFullscreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

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

    private void registerBroadcastReceiver() {
        _appUpdateReceiver = new AppUpdateReceiver();
        _shortcutReceiver = new ShortcutReceiver();
        _timeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };

        // register all receivers
        ContextCompat.registerReceiver(this, _appUpdateReceiver, _appUpdateIntentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, _shortcutReceiver, _shortcutIntentFilter, ContextCompat.RECEIVER_EXPORTED);
        ContextCompat.registerReceiver(this, _timeChangedReceiver, _timeChangedIntentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public final void onStartApp(@NonNull Context context, @NonNull App app, @Nullable View view) {
        if (BuildConfig.APPLICATION_ID.equals(app._packageName)) {
            LauncherAction.RunAction(Action.LauncherSettings, context);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && app._userHandle != null) {
                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> activities = launcherApps.getActivityList(app.getPackageName(), app._userHandle);
                for (int intent = 0; intent < activities.size(); intent++) {
                    if (app.getComponentName().equals(activities.get(intent).getComponentName().toString()))
                        launcherApps.startMainActivity(activities.get(intent).getComponentName(), app._userHandle, null, getActivityAnimationOpts(view));
                }
            } else {
                Intent intent = Tool.getIntentFromApp(app);
                context.startActivity(intent, getActivityAnimationOpts(view));
            }

            // close app drawer and other items in advance
            // annoying to wait for app drawer to close
            handleLauncherResume();
        } catch (Exception e) {
            e.printStackTrace();
            Tool.toast(context, R.string.toast_app_uninstalled);
        }
    }

    private Bundle getActivityAnimationOpts(View view) {
        Bundle bundle = null;
        if (view == null) {
            return null;
        }

        ActivityOptions options = null;
        if (VERSION.SDK_INT >= 23) {
            int left = 0;
            int top = 0;
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
            if (view instanceof AppItemView) {
                width = (int) ((AppItemView) view).getIconSize();
                left = (int) ((AppItemView) view).getDrawIconLeft();
                top = (int) ((AppItemView) view).getDrawIconTop();
            }
            options = ActivityOptions.makeClipRevealAnimation(view, left, top, width, height);
        } else if (VERSION.SDK_INT < 21) {
            options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        if (options != null) {
            bundle = options.toBundle();
        }

        return bundle;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (visible) {
                    controller.show(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                } else {
                    controller.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }
        } else {
            if (visible) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
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
        if (getDesktop() != null && getDesktop().getCurrentItem() == 0 && show) return;
        AppSettings appSettings = Setup.appSettings();
        if (appSettings.getSearchBarEnable() && show) {
            Tool.visibleViews(100, getSearchBar());
        } else {
            if (appSettings.getSearchBarEnable()) {
                Tool.invisibleViews(100, getSearchBar());
            } else {
                Tool.goneViews(100, getSearchBar());
            }
        }
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
                final StringBuilder log = new StringBuilder();
                final MaterialDialog progressDialog = new MaterialDialog.Builder(this)
                        .title("Sichere Backup...")
                        .content("Bitte warten...")
                        .progress(true, 0)
                        .cancelable(false)
                        .show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "w");
                            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(pfd.getFileDescriptor());
                            com.benny.openlauncher.util.BackupManager.createBackup(HomeActivity.this, fileOutputStream, new com.benny.openlauncher.util.BackupManager.BackupListener() {
                                @Override
                                public void onLog(final String message) {
                                    log.append(message).append("\n");
                                    runOnUiThread(() -> progressDialog.setContent(message));
                                }

                                @Override
                                public void onProgress(int progress, int max) {
                                }

                                @Override
                                public void onCompleted(final boolean success) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Intent logIntent = new Intent(HomeActivity.this, BackupLogActivity.class);
                                        logIntent.putExtra(BackupLogActivity.EXTRA_LOG_TEXT, log.toString());
                                        startActivity(logIntent);
                                    });
                                }
                            });
                            pfd.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else if (requestCode == REQUEST_RESTORE && data != null) {
                final StringBuilder log = new StringBuilder();
                final MaterialDialog progressDialog = new MaterialDialog.Builder(this)
                        .title("Wiederherstellung...")
                        .content("Bitte warten...")
                        .progress(true, 0)
                        .cancelable(false)
                        .show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            java.io.InputStream inputStream = getContentResolver().openInputStream(data.getData());
                            com.benny.openlauncher.util.BackupManager.restoreBackup(HomeActivity.this, inputStream, new com.benny.openlauncher.util.BackupManager.BackupListener() {
                                @Override
                                public void onLog(final String message) {
                                    log.append(message).append("\n");
                                    runOnUiThread(() -> progressDialog.setContent(message));
                                }

                                @Override
                                public void onProgress(int progress, int max) {
                                }

                                @Override
                                public void onCompleted(final boolean success) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Intent logIntent = new Intent(HomeActivity.this, BackupLogActivity.class);
                                        logIntent.putExtra(BackupLogActivity.EXTRA_LOG_TEXT, log.toString());
                                        if (success) {
                                            Tool.toast(HomeActivity.this, "Wiederherstellung erfolgreich. Launcher wird neu gestartet...");
                                            logIntent.putExtra(BackupLogActivity.EXTRA_RESTART_AFTER, true);
                                        }
                                        startActivity(logIntent);
                                    });
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra("appWidgetId", -1);
            if (appWidgetId != -1) {
                _appWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    @Override
    public void onBackPressed() {
        handleLauncherResume();
    }

    @Override
    protected void onStart() {
        _appWidgetHost.startListening();
        _launcher = this;

        super.onStart();
    }

    private void checkNotificationPermissions() {
        Set<String> appList = NotificationManagerCompat.getEnabledListenerPackages(this);
        for (String app : appList) {
            if (app.equals(getPackageName())) {
                // Already allowed, so request a full update when returning to the home screen from another app.
                Intent i = new Intent(NotificationListener.UPDATE_NOTIFICATIONS_ACTION);
                i.setPackage(getPackageName());
                i.putExtra(NotificationListener.UPDATE_NOTIFICATIONS_COMMAND, NotificationListener.UPDATE_NOTIFICATIONS_UPDATE);
                sendBroadcast(i);
                return;
            }
        }

        // Request the required permission otherwise.
        DialogHelper.alertDialog(this, getString(R.string.notification_title), getString(R.string.notification_summary), getString(R.string.enable), new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                Tool.toast(HomeActivity.this, getString(R.string.toast_notification_permission_required));
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.i("OpenLauncher", "HomeActivity: onResume");
        _appWidgetHost.startListening();
        _launcher = this;

        // handle restart if something needs to be reset
        AppSettings appSettings = Setup.appSettings();
        if (appSettings.getAppRestartRequired()) {
            android.util.Log.i("OpenLauncher", "HomeActivity: Restart required, recreating...");
            appSettings.setAppRestartRequired(false);
            recreate();
            return;
        }

        if (appSettings.getNotificationStatus()) {
            // Ask user to allow the Notification permission if not already provided.
            checkNotificationPermissions();
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

        unregisterReceiver(_appUpdateReceiver);
        unregisterReceiver(_shortcutReceiver);
        unregisterReceiver(_timeChangedReceiver);
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
