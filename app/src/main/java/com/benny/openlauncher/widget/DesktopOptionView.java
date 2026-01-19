package com.benny.openlauncher.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import android.graphics.Insets;

import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.IconLabelItem;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.util.ArrayList;
import java.util.List;

public class DesktopOptionView extends FrameLayout {

    private RecyclerView[] _actionRecyclerViews = new RecyclerView[2];
    @SuppressWarnings("unchecked")
    private FastItemAdapter<IconLabelItem>[] _actionAdapters = new FastItemAdapter[2];
    private DesktopOptionViewListener _desktopOptionViewListener;

    public DesktopOptionView(@NonNull Context context) {
        super(context);
        init();
    }

    public DesktopOptionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DesktopOptionView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setDesktopOptionViewListener(DesktopOptionViewListener desktopOptionViewListener) {
        _desktopOptionViewListener = desktopOptionViewListener;
    }

    public void updateHomeIcon(final boolean home) {
        post(new Runnable() {
            @Override
            public void run() {
                if (home) {
                    _actionAdapters[0].getAdapterItem(0)._icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_home);
                } else {
                    _actionAdapters[0].getAdapterItem(0)._icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_home_border);
                }
                _actionAdapters[0].notifyAdapterItemChanged(0);
            }
        });
    }

    public void updateLockIcon(final boolean lock) {
        if (_actionAdapters.length == 0) return;
        if (_actionAdapters[0].getAdapterItemCount() == 0) return;
        post(new Runnable() {
            @Override
            public void run() {
                if (lock) {
                    _actionAdapters[0].getAdapterItem(1)._icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_lock);
                } else {
                    _actionAdapters[0].getAdapterItem(1)._icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_lock_open);
                }
                _actionAdapters[0].notifyAdapterItemChanged(1);
            }
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            android.graphics.Insets systemBarInsets = insets.getInsets(WindowInsets.Type.systemBars());
            setPadding(0, systemBarInsets.top, 0, systemBarInsets.bottom);
            return insets;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) { // API 19-29 (old behavior)
            setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        }
        return insets;
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        final int paddingHorizontal = Tool.dp2px(42);
        final Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "RobotoCondensed-Regular.ttf");

        _actionAdapters[0] = new FastItemAdapter<>();
        _actionAdapters[1] = new FastItemAdapter<>();

        _actionRecyclerViews[0] = createRecyclerView(_actionAdapters[0], Gravity.TOP | Gravity.CENTER_HORIZONTAL, paddingHorizontal);
        _actionRecyclerViews[1] = createRecyclerView(_actionAdapters[1], Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, paddingHorizontal);

        final com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem> clickListener = new com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem>() {
            @Override
            public boolean onClick(View v, IAdapter<IconLabelItem> adapter, IconLabelItem item, int position) {
                if (_desktopOptionViewListener != null) {
                    final int id = (int) item.getIdentifier();
                    if (id == R.drawable.ic_home || id == R.drawable.ic_home_border) {
                        updateHomeIcon(true);
                        _desktopOptionViewListener.onSetHomePage();
                    } else if (id == R.drawable.ic_delete) {
                        if (!Setup.appSettings().getDesktopLock()) {
                            _desktopOptionViewListener.onRemovePage();
                        } else {
                            Tool.toast(getContext(), "Desktop is locked.");
                        }
                    } else if (id == R.drawable.ic_clear) {
                        if (!Setup.appSettings().getDesktopLock()) {
                            _desktopOptionViewListener.onQuickRemove();
                        } else {
                            Tool.toast(getContext(), "Desktop is locked.");
                        }
                    } else if (id == R.drawable.ic_dashboard) {
                        if (!Setup.appSettings().getDesktopLock()) {
                            _desktopOptionViewListener.onPickWidget();
                        } else {
                            Tool.toast(getContext(), "Desktop is locked.");
                        }
                    } else if (id == R.drawable.ic_star) {
                        if (!Setup.appSettings().getDesktopLock()) {
                            // Show Action Menu
                            MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext())
                                    .title("Launcher Aktionen")
                                    .items(new CharSequence[]{
                                            "Restart Launcher", 
                                            "Backup (Full ZIP)", 
                                            "Restore (Full ZIP)",
                                            "Entwickler Optionen",
                                            "Standard Launcher festlegen",
                                            "System Einstellungen",
                                            "App Info"
                                    })
                                    .itemsCallback(new MaterialDialog.ListCallback() {
                                        @Override
                                        public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                            switch (position) {
                                                case 0: // Restart
                                                    if (getContext() instanceof android.app.Activity) {
                                                        ((android.app.Activity) getContext()).recreate();
                                                    }
                                                    break;
                                                case 1: // Backup
                                                    _desktopOptionViewListener.onBackup();
                                                    break;
                                                case 2: // Restore
                                                    _desktopOptionViewListener.onRestore();
                                                    break;
                                                case 3: // Dev Options
                                                    getContext().startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                                                    break;
                                                case 4: // Default Launcher
                                                    Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                                                    getContext().startActivity(intent);
                                                    break;
                                                case 5: // System Settings
                                                    getContext().startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                                                    break;
                                                case 6: // App Info
                                                    Intent appInfoIntent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    appInfoIntent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
                                                    getContext().startActivity(appInfoIntent);
                                                    break;
                                            }
                                        }
                                    });
                            builder.show();
                        } else {
                            Tool.toast(getContext(), "Desktop is locked.");
                        }
                    } else if (id == R.drawable.ic_lock || id == R.drawable.ic_lock_open) {
                        Setup.appSettings().setDesktopLock(!Setup.appSettings().getDesktopLock());
                        updateLockIcon(Setup.appSettings().getDesktopLock());
                    } else if (id == R.drawable.ic_settings) {
                        _desktopOptionViewListener.onLaunchSettings();
                    } else if (id == R.drawable.ic_apps) {
                        _desktopOptionViewListener.onAddApp();
                    } else {
                        return false;
                    }
                    return true;
                }
                return false;
            }
        };

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                initItems(typeface, clickListener);
            }
        });
    }

    private void initItems(final Typeface typeface, final com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem> clickListener) {
        int itemWidthTop = (getWidth() - 2 * Tool.dp2px(42)) / 4;
        int itemWidthBottom = (getWidth() - 2 * Tool.dp2px(42)) / 4;

        List<IconLabelItem> itemsTop = new ArrayList<>();
        itemsTop.add(createItem(R.drawable.ic_home, R.string.home, typeface, itemWidthTop));
        itemsTop.add(createItem(Setup.appSettings().getDesktopLock() ? R.drawable.ic_lock : R.drawable.ic_lock_open, R.string.lock, typeface, itemWidthTop));
        itemsTop.add(createItem(R.drawable.ic_star, "Aktion", typeface, itemWidthTop));
        itemsTop.add(createItem(R.drawable.ic_settings, R.string.pref_title__settings, typeface, itemWidthTop));
        _actionAdapters[0].set(itemsTop);
        _actionAdapters[0].withOnClickListener(clickListener);

        List<IconLabelItem> itemsBottom = new ArrayList<>();
        itemsBottom.add(createItem(R.drawable.ic_dashboard, R.string.widget, typeface, itemWidthBottom));
        itemsBottom.add(createItem(R.drawable.ic_apps, R.string.add_to_desktop, typeface, itemWidthBottom));
        itemsBottom.add(createItem(R.drawable.ic_clear, "App löschen", typeface, itemWidthBottom));
        itemsBottom.add(createItem(R.drawable.ic_delete, "Seite löschen", typeface, itemWidthBottom));
        _actionAdapters[1].set(itemsBottom);
        _actionAdapters[1].withOnClickListener(clickListener);

        ((MarginLayoutParams) ((View) _actionRecyclerViews[0].getParent()).getLayoutParams()).topMargin = Tool.dp2px(Setup.appSettings().getSearchBarEnable() ? 36 : 4);
    }

    private RecyclerView createRecyclerView(FastAdapter adapter, int gravity, int paddingHorizontal) {
        RecyclerView actionRecyclerView = new RecyclerView(getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        actionRecyclerView.setClipToPadding(false);
        actionRecyclerView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        actionRecyclerView.setLayoutManager(linearLayoutManager);
        actionRecyclerView.setAdapter(adapter);
        actionRecyclerView.setOverScrollMode(OVER_SCROLL_ALWAYS);
        LayoutParams actionRecyclerViewLP = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionRecyclerViewLP.gravity = gravity;

        addView(actionRecyclerView, actionRecyclerViewLP);
        return actionRecyclerView;
    }

    private IconLabelItem createItem(int icon, int label, Typeface typeface, int width) {
        return new IconLabelItem(getContext(), icon, label)
                .withIdentifier(icon)
                .withOnClickListener(null)
                .withTextColor(Color.WHITE)
                .withIconSize(36)
                .withIconColor(Color.WHITE)
                .withIconPadding(4)
                .withIconGravity(Gravity.TOP)
                .withWidth(width)
                .withTextGravity(Gravity.CENTER);
    }

    private IconLabelItem createItem(int icon, String label, Typeface typeface, int width) {
        return new IconLabelItem(ContextCompat.getDrawable(getContext(), icon), label)
                .withIdentifier(icon)
                .withOnClickListener(null)
                .withTextColor(Color.WHITE)
                .withIconSize(36)
                .withIconColor(Color.WHITE)
                .withIconPadding(4)
                .withIconGravity(Gravity.TOP)
                .withWidth(width)
                .withTextGravity(Gravity.CENTER);
    }

    public interface DesktopOptionViewListener {
        void onRemovePage();

        void onQuickRemove();

        void onSetHomePage();

        void onPickWidget();

        void onPickAction();

        void onBackup();

        void onRestore();

        void onLaunchSettings();

        void onAddApp();
    }
}
