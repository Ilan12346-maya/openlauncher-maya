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
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.IconLabelItem;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.turingtechnologies.materialscrollbar.AlphabetIndicator;
import com.turingtechnologies.materialscrollbar.DragScrollBar;
import com.turingtechnologies.materialscrollbar.INameableAdapter;

import java.util.ArrayList;
import java.util.List;

public class AppDrawerGrid extends FrameLayout {

    public static int _itemWidth;
    public static int _itemHeightPadding;

    public RecyclerView _recyclerView;
    public AppDrawerGridAdapter _gridDrawerAdapter;
    public DragScrollBar _scrollBar;
    public EditText _searchBar;
    public View _batchUninstallButton;

    private static List<App> _apps;
    private GridLayoutManager _layoutManager;
    private boolean _isSelectionMode = false;
    private List<App> _selectedApps = new ArrayList<>();

    public AppDrawerGrid(Context context) {
        super(context);
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.view_app_drawer_grid, AppDrawerGrid.this, false);
        addView(view);

        _recyclerView = findViewById(R.id.recycler_view);
        _recyclerView.setItemAnimator(null);
        _scrollBar = findViewById(R.id.scroll_bar);
        _searchBar = findViewById(R.id.search_bar);
        _batchUninstallButton = findViewById(R.id.batch_uninstall_button);
        _layoutManager = new GridLayoutManager(getContext(), Setup.appSettings().getDrawerColumnCount());

        init();
    }

    public void loadApps() {
        updateAdapter(Setup.appLoader().getAllApps(getContext(), false));
    }

    private void init() {
        if (!Setup.appSettings().getDrawerShowIndicator()) _scrollBar.setVisibility(View.GONE);
        _scrollBar.setIndicator(new AlphabetIndicator(getContext()), true);
        _scrollBar.setClipToPadding(true);
        _scrollBar.setDraggableFromAnywhere(true);
        _scrollBar.setHandleColor(Setup.appSettings().getDrawerFastScrollColor());

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
        _gridDrawerAdapter.getItemFilter().withFilterPredicate(new IItemAdapter.Predicate<IconLabelItem>() {
            @Override
            public boolean filter(IconLabelItem item, CharSequence constraint) {
                return item._label.toLowerCase().contains(constraint.toString().toLowerCase());
            }
        });

        _searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                _gridDrawerAdapter.filter(s);
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
        _apps = apps;
        ArrayList<IconLabelItem> items = new ArrayList<>();
        for (int i = 0; i < apps.size(); i++) {
            final App app = apps.get(i);
            final boolean isSelected = _selectedApps.contains(app);
            
            items.add(new IconLabelItem(app.getIcon(), app.getLabel())
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
                                Tool.startApp(v.getContext(), app, null);
                            }
                        }
                    })
                    .withOnLongClickListener(_isSelectionMode ? null : DragHandler.getLongClick(Item.newAppItem(app), DragAction.Action.DRAWER, null)));
        }
        _gridDrawerAdapter.set(items);
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

    public static class AppDrawerGridAdapter extends FastItemAdapter<IconLabelItem> implements INameableAdapter {
        public AppDrawerGridAdapter() {
        }

        @Override
        public Character getCharacterForElement(int element) {
            if (_apps != null && element < _apps.size() && _apps.get(element) != null && _apps.get(element).getLabel().length() > 0)
                return _apps.get(element).getLabel().charAt(0);
            else return '#';
        }
    }
}
