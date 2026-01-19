package com.benny.openlauncher.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.benny.openlauncher.R;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class HideAppsFragment extends Fragment {
    private static final String TAG = "RequestActivity";
    private static final boolean DEBUG = true;

    private ArrayList<String> _listActivitiesHidden = new ArrayList<>();
    private ArrayList<App> _listActivitiesAll = new ArrayList<>();
    private HideAppsAdapter _appInfoAdapter;
    private ViewSwitcher _switcherLoad;
    private ListView _grid;
    private final ExecutorService _executorService = Executors.newSingleThreadExecutor();
    private final Handler _mainHandler = new Handler(Looper.getMainLooper());
    private boolean _isLoading = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.view_hide_apps, container, false);
        _switcherLoad = rootView.findViewById(R.id.viewSwitcherLoadingMain);

        FloatingActionButton fab = rootView.findViewById(R.id.fab_rq);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmSelection();
            }
        });

        loadApps();

        return rootView;
    }

    private void loadApps() {
        if (_isLoading) return;
        _isLoading = true;

        _executorService.execute(() -> {
            List<String> hiddenList = AppSettings.get().getHiddenAppsList();
            _listActivitiesHidden.addAll(hiddenList);
            
             try {
                // compare to installed apps
                prepareData();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            _mainHandler.post(() -> {
                populateView();
                // switch from loading screen to the main view
                _switcherLoad.showNext();
                _isLoading = false;
            });
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    private void confirmSelection() {
        Thread actionSend_Thread = new Thread() {

            @Override
            public void run() {
                // update hidden apps
                AppSettings.get().setHiddenAppsList(_listActivitiesHidden);
                getActivity().finish();
            }
        };

        if (!actionSend_Thread.isAlive()) {
            // prevents thread from being executed more than once
            actionSend_Thread.start();
        }
    }

    private void prepareData() {
        List<App> apps = AppManager.getInstance(getContext()).getNonFilteredApps();
        _listActivitiesAll.addAll(apps);
    }

    private void populateView() {
        if (getActivity() == null) return;
        _grid = getActivity().findViewById(R.id.app_grid);

        // _grid might be null if view hierarchy is not ready or ID is missing in layout
        // In the original code, it was finding it from getActivity(), implying it's in the activity's layout or added by fragment
        // The original code used getActivity().findViewById(R.id.app_grid), which is weird for a fragment unless the fragment view is attached.
        // Usually should be rootView.findViewById in onCreateView but here populateView is called later.
        // Assuming R.id.app_grid is part of R.layout.view_hide_apps
        
        if (_grid == null) {
             // Fallback to finding it in the fragment's root view if possible, but here we only have access to getActivity()
             // Let's hope R.id.app_grid is reachable.
             // Wait, in onCreateView we inflate R.layout.view_hide_apps. 
             // If R.id.app_grid is inside that, we should have cached it or looked it up from getView().
             if (getView() != null) {
                 _grid = getView().findViewById(R.id.app_grid);
             }
        }

        if (_grid != null) {
            _grid.setFastScrollEnabled(true);
            _grid.setFastScrollAlwaysVisible(false);

            _appInfoAdapter = new HideAppsAdapter(getActivity(), _listActivitiesAll);

            _grid.setAdapter(_appInfoAdapter);
            _grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> AdapterView, View view, int position, long row) {
                    App appInfo = (App) AdapterView.getItemAtPosition(position);
                    CheckBox checker = view.findViewById(R.id.checkbox);
                    ViewSwitcher icon = view.findViewById(R.id.viewSwitcherChecked);

                    checker.toggle();
                    if (checker.isChecked()) {
                        _listActivitiesHidden.add(appInfo.getComponentName());
                        if (DEBUG) Log.v(TAG, "Selected App: " + appInfo.getLabel());
                        if (icon.getDisplayedChild() == 0) {
                            icon.showNext();
                        }
                    } else {
                        _listActivitiesHidden.remove(appInfo.getComponentName());
                        if (DEBUG) Log.v(TAG, "Deselected App: " + appInfo.getLabel());
                        if (icon.getDisplayedChild() == 1) {
                            icon.showPrevious();
                        }
                    }
                }
            });
        }
    }

    private class HideAppsAdapter extends ArrayAdapter<App> {
        private HideAppsAdapter(Context context, ArrayList<App> adapterArrayList) {
            super(context, R.layout.item_hide_apps, adapterArrayList);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_hide_apps, parent, false);
                holder = new ViewHolder();
                holder._apkIcon = convertView.findViewById(R.id.appIcon);
                holder._apkName = convertView.findViewById(R.id.appName);
                holder._apkPackage = convertView.findViewById(R.id.appPackage);
                holder._checker = convertView.findViewById(R.id.checkbox);
                holder._switcherChecked = convertView.findViewById(R.id.viewSwitcherChecked);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            App appInfo = getItem(position);

            holder._apkPackage.setText(appInfo.getClassName());
            holder._apkName.setText(appInfo.getLabel());
            holder._apkIcon.setImageDrawable(appInfo.getIcon());

            holder._switcherChecked.setInAnimation(null);
            holder._switcherChecked.setOutAnimation(null);
            holder._checker.setChecked(_listActivitiesHidden.contains(appInfo.getComponentName()));
            if (_listActivitiesHidden.contains(appInfo.getComponentName())) {
                if (holder._switcherChecked.getDisplayedChild() == 0) {
                    holder._switcherChecked.showNext();
                }
            } else {
                if (holder._switcherChecked.getDisplayedChild() == 1) {
                    holder._switcherChecked.showPrevious();
                }
            }
            return convertView;
        }
    }

    private class ViewHolder {
        TextView _apkName;
        TextView _apkPackage;
        ImageView _apkIcon;
        CheckBox _checker;
        ViewSwitcher _switcherChecked;
    }
}