package com.benny.openlauncher.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Intent;
import android.net.Uri;

import com.benny.openlauncher.R;
import com.benny.openlauncher.interfaces.AppUpdateListener;
import com.benny.openlauncher.manager.HistoryManager;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.IconLabelItem;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.turingtechnologies.materialscrollbar.INameableAdapter;

import java.util.ArrayList;
import java.util.List;

public class AppDrawerGrid extends FrameLayout {

    public static int _itemWidth;
    public static int _itemHeightPadding;

    public RecyclerView _recyclerView;
    public AppDrawerGridAdapter _gridDrawerAdapter;
    public EditText _searchBar;
    public View _batchUninstallButton;

    private static List<App> _apps;
    private GridLayoutManager _layoutManager;
    private boolean _isSelectionMode = false;
    private boolean _searchBarFocused = false;
    private List<App> _selectedApps = new ArrayList<>();

    public AppDrawerGrid(Context context) {
        super(context);
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.view_app_drawer_grid, AppDrawerGrid.this, false);
        addView(view);

        _recyclerView = findViewById(R.id.recycler_view);
        _recyclerView.setItemAnimator(null);
        _searchBar = findViewById(R.id.search_bar);
        _batchUninstallButton = findViewById(R.id.batch_uninstall_button);
        
        final int columns = Setup.appSettings().getDrawerColumnCount();
        _layoutManager = new GridLayoutManager(getContext(), columns);
        _layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (_gridDrawerAdapter != null && position < _gridDrawerAdapter.getItemCount()) {
                    IconLabelItem item = _gridDrawerAdapter.getItem(position);
                    if (item != null && item.isHeader()) {
                        return _layoutManager.getSpanCount();
                    }
                }
                return 1;
            }
        });

        init();
    }

    public void loadApps() {
        updateAdapter(Setup.appLoader().getAllApps(getContext(), false));
    }

    private void init() {
        _batchUninstallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!_isSelectionMode) {
                    _isSelectionMode = true;
                    ((android.widget.ImageButton) _batchUninstallButton).setImageResource(R.drawable.ic_check_white);
                    _selectedApps.clear();
                    Tool.toast(getContext(), "Wähle Apps zum Deinstallieren aus");
                    updateAdapter(_apps);
                } else {
                    if (_selectedApps.isEmpty()) {
                        _isSelectionMode = false;
                        ((android.widget.ImageButton) _batchUninstallButton).setImageResource(R.drawable.ic_delete);
                        updateAdapter(_apps);
                    } else {
                        // Perform batch uninstall
                        uninstallSelectedApps();
                    }
                }
            }
        });

        _gridDrawerAdapter = new AppDrawerGridAdapter();

        _searchBar.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                _searchBarFocused = hasFocus;
                updateAdapter(_apps);
            }
        });

        _searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAdapter(_apps);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitValue();
        } else {
            setLandscapeValue();
        }
        _recyclerView.setAdapter(_gridDrawerAdapter);
        _recyclerView.setLayoutManager(_layoutManager);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                _itemWidth = getWidth() / _layoutManager.getSpanCount();
                _itemHeightPadding = Tool.dp2px(20);
                
                Setup.appLoader().addUpdateListener(new AppUpdateListener() {
                    @Override
                    public boolean onAppUpdated(final List<App> apps) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                updateAdapter(apps);
                            }
                        });
                        return false;
                    }
                });
            }
        });
    }

    private void uninstallSelectedApps() {
        for (App app : _selectedApps) {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + app.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
        _isSelectionMode = false;
        _selectedApps.clear();
        ((android.widget.ImageButton) _batchUninstallButton).setImageResource(R.drawable.ic_delete);
        updateAdapter(_apps);
    }

    public void updateAdapter(List<App> apps) {
        if (apps == null) return;
        _apps = apps;
        ArrayList<IconLabelItem> items = new ArrayList<>();
        String filter = _searchBar.getText().toString().toLowerCase();

        // Add Recent Apps Header & Items only if not searching
        if (filter.isEmpty()) {
            List<App> recents = HistoryManager.getInstance(getContext()).getRecentApps(_layoutManager.getSpanCount() * 2);
            if (!recents.isEmpty()) {
                items.add(new IconLabelItem((android.graphics.drawable.Drawable)null, getContext().getString(R.string.recent_apps)).withIsHeader(true));
                for (App app : recents) {
                    items.add(createAppItem(app));
                }
            }
        }

        // Add All Apps with alphabetical headers
        String lastHeader = "";
        for (int i = 0; i < apps.size(); i++) {
            App app = apps.get(i);
            String label = app.getLabel();
            
            // Skip if doesn't match filter
            if (!filter.isEmpty() && (label == null || !label.toLowerCase().contains(filter))) {
                continue;
            }

            String currentHeader = "";
            if (label != null && !label.isEmpty()) {
                char firstChar = Character.toUpperCase(label.charAt(0));
                if (Character.isDigit(firstChar)) {
                    currentHeader = "#";
                } else if (Character.isLetter(firstChar)) {
                    currentHeader = String.valueOf(firstChar);
                } else {
                    currentHeader = "#";
                }
            } else {
                currentHeader = "#";
            }

            if (!currentHeader.equals(lastHeader)) {
                items.add(new IconLabelItem((android.graphics.drawable.Drawable)null, currentHeader).withIsHeader(true));
                lastHeader = currentHeader;
            }
            items.add(createAppItem(app));
        }
        _gridDrawerAdapter.set(items);
    }
    
    public void focusSearch() {
        if (_searchBar != null) {
            _searchBar.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(_searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private IconLabelItem createAppItem(final App app) {
        final boolean isSelected = _selectedApps.contains(app);
        return new IconLabelItem(app.getIcon(), app.getLabel())
                .withIconSize(Setup.appSettings().getIconSize())
                .withTextColor(Color.WHITE)
                .withTextVisibility(Setup.appSettings().getDrawerShowLabel())
                .withIconPadding(8)
                .withTextGravity(Gravity.CENTER)
                .withIconGravity(Gravity.TOP)
                .withOnClickAnimate(false)
                .withIsAppLauncher(true)
                .withSelected(isSelected)
                .withOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (_isSelectionMode) {
                            if (_selectedApps.contains(app)) {
                                _selectedApps.remove(app);
                            } else {
                                _selectedApps.add(app);
                            }
                            updateAdapter(_apps);
                        } else {
                            Tool.startApp(v.getContext(), app, v);
                        }
                    }
                })
                .withOnLongClickListener(_isSelectionMode ? null : DragHandler.getLongClick(Item.newAppItem(app), DragAction.Action.DRAWER, null));
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (_apps == null || _layoutManager == null) {
            super.onConfigurationChanged(newConfig);
            return;
        }

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLandscapeValue();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitValue();
        }
        super.onConfigurationChanged(newConfig);
    }

    private void setPortraitValue() {
        _layoutManager.setSpanCount(Setup.appSettings().getDrawerColumnCount());
        _gridDrawerAdapter.notifyAdapterDataSetChanged();
    }

    private void setLandscapeValue() {
        _layoutManager.setSpanCount(Setup.appSettings().getDrawerRowCount());
        _gridDrawerAdapter.notifyAdapterDataSetChanged();
    }

    public class AppDrawerGridAdapter extends FastItemAdapter<IconLabelItem> implements INameableAdapter {
        public AppDrawerGridAdapter() {
        }

        @Override
        public Character getCharacterForElement(int element) {
            IconLabelItem item = getAdapterItem(element);
            if (item == null || item._label == null || item._label.isEmpty()) return '#';

            if (item.isHeader()) {
                // If it is the Recent Apps header, return '#'
                String recentApps = getContext().getString(R.string.recent_apps);
                if (item._label.equals(recentApps)) return '#';

                // Otherwise it is an alphabetical header, return its character
                return item._label.charAt(0);
            }

            // For normal items, return the first character
            return Character.toUpperCase(item._label.charAt(0));
        }
    }
}