package com.benny.openlauncher.activity.homeparts;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.view.View;
import androidx.core.content.ContextCompat;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.interfaces.DialogListener;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.LauncherAction;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DialogHelper;
import com.benny.openlauncher.widget.CellContainer;
import com.benny.openlauncher.widget.Desktop;
import com.benny.openlauncher.widget.DesktopOptionView;

import java.util.List;

import static com.benny.openlauncher.activity.HomeActivity.REQUEST_CREATE_APPWIDGET;
import static com.benny.openlauncher.activity.HomeActivity.REQUEST_PICK_APPWIDGET;

@SuppressWarnings("deprecation")
public class HpDesktopOption implements DesktopOptionView.DesktopOptionViewListener, DialogListener.OnActionDialogListener {
    private HomeActivity _homeActivity;

    public HpDesktopOption(HomeActivity homeActivity) {
        _homeActivity = homeActivity;
    }

    @Override
    public void onRemovePage() {
        if (_homeActivity.getDesktop().isCurrentPageEmpty()) {
            _homeActivity.getDesktop().removeCurrentPage();
            return;
        }
        DialogHelper.alertDialog(_homeActivity, _homeActivity.getString(R.string.remove), "This page is not empty. Those items will also be removed.", new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                _homeActivity.getDesktop().removeCurrentPage();
            }
        });
    }

    @Override
    public void onSetHomePage() {
        AppSettings appSettings = Setup.appSettings();
        appSettings.setDesktopPageCurrent(_homeActivity.getDesktop().getCurrentPageIndex());
    }

    @Override
    public void onPickWidget() {
        final List<AppWidgetProviderInfo> widgetList = _homeActivity._appWidgetManager.getInstalledProviders();
        final com.mikepenz.fastadapter.commons.adapters.FastItemAdapter<com.benny.openlauncher.viewutil.IconLabelItem> fastItemAdapter = new com.mikepenz.fastadapter.commons.adapters.FastItemAdapter<>();

        for (AppWidgetProviderInfo widget : widgetList) {
            android.graphics.drawable.Drawable icon = widget.loadPreviewImage(_homeActivity, 0);
            if (icon == null) {
                icon = widget.loadIcon(_homeActivity, 0);
            }
            fastItemAdapter.add(new com.benny.openlauncher.viewutil.IconLabelItem(icon, widget.loadLabel(_homeActivity.getPackageManager()))
                    .withIconSize(100)
                    .withIconGravity(android.view.Gravity.START)
                    .withIconPadding(8)
                    .withTag(widget));
        }

        new MaterialDialog.Builder(_homeActivity)
                .title(R.string.widget)
                .adapter(fastItemAdapter, null)
                .autoDismiss(true)
                .build()
                .show();

        fastItemAdapter.withOnClickListener(new com.mikepenz.fastadapter.listeners.OnClickListener<com.benny.openlauncher.viewutil.IconLabelItem>() {
            @Override
            public boolean onClick(View v, com.mikepenz.fastadapter.IAdapter<com.benny.openlauncher.viewutil.IconLabelItem> adapter, com.benny.openlauncher.viewutil.IconLabelItem item, int position) {
                AppWidgetProviderInfo widgetInfo = (AppWidgetProviderInfo) item.getTag();
                int appWidgetId = _homeActivity._appWidgetHost.allocateAppWidgetId();
                
                if (_homeActivity._appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, widgetInfo.provider)) {
                    Intent data = new Intent();
                    data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    configureWidget(data);
                } else {
                    com.benny.openlauncher.util.Logger.log(this, "bindAppWidgetIdIfAllowed failed for widget: " + widgetInfo.provider);
                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetInfo.provider);
                    _homeActivity.startActivityForResult(intent, HomeActivity.REQUEST_PICK_APPWIDGET);
                }
                return true;
            }
        });
    }

    @Override
    public void onBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "openlauncher_backup.zip");
        _homeActivity.startActivityForResult(intent, HomeActivity.REQUEST_BACKUP);
    }

    @Override
    public void onRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        _homeActivity.startActivityForResult(intent, HomeActivity.REQUEST_RESTORE);
    }

    @Override
    public void onPickAction() {
        // Placeholder for other actions if needed
    }

    @Override
    public void onLaunchSettings() {
        Setup.eventHandler().showLauncherSettings(_homeActivity);
    }

    public void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt("appWidgetId", -1);
        AppWidgetProviderInfo appWidgetInfo = _homeActivity._appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra("appWidgetId", appWidgetId);
            _homeActivity.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(data);
        }
    }

    public void createWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) {
            appWidgetId = extras.getInt("appWidgetId", -1);
        }
        com.benny.openlauncher.util.Logger.log(this, "createWidget: appWidgetId=" + appWidgetId);
        AppWidgetProviderInfo appWidgetInfo = _homeActivity._appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            com.benny.openlauncher.util.Logger.log(this, "createWidget: appWidgetInfo is NULL");
            return;
        }
        Item item = Item.newWidgetItem(appWidgetInfo.provider, appWidgetId);
        Desktop desktop = _homeActivity.getDesktop();
        List<CellContainer> pages = desktop.getPages();
        item._spanX = (appWidgetInfo.minWidth - 1) / pages.get(desktop.getCurrentPageIndex()).getCellWidth() + 1;
        item._spanY = (appWidgetInfo.minHeight - 1) / pages.get(desktop.getCurrentPageIndex()).getCellHeight() + 1;
        Point point = desktop.getCurrentPage().findFreeSpace(item._spanX, item._spanY);
        if (point != null) {
            item._x = point.x;
            item._y = point.y;

            // add item to database
            Setup.dataManager().saveItem(item, desktop.getCurrentPageIndex(), Definitions.ItemPosition.Desktop);
            desktop.addItemToPage(item, desktop.getCurrentPageIndex());
        } else {
            Tool.toast(_homeActivity, R.string.toast_not_enough_space);
        }
    }

    @Override
    public void onAdd(int type) {
        Point pos = _homeActivity.getDesktop().getCurrentPage().findFreeSpace();
        if (pos != null) {
            _homeActivity.getDesktop().addItemToCell(Item.newActionItem(type), pos.x, pos.y);
        } else {
            Tool.toast(_homeActivity, R.string.toast_not_enough_space);
        }
    }

    @Override
    public void onAddApp() {
        final List<com.benny.openlauncher.model.App> apps = Setup.appLoader().getApps();
        final com.mikepenz.fastadapter.commons.adapters.FastItemAdapter<com.benny.openlauncher.viewutil.IconLabelItem> fastItemAdapter = new com.mikepenz.fastadapter.commons.adapters.FastItemAdapter<>();
        
        // Add special actions at the top
        fastItemAdapter.add(new com.benny.openlauncher.viewutil.IconLabelItem(ContextCompat.getDrawable(_homeActivity, R.drawable.ic_behavior), "Entwickler Optionen öffnen")
                .withIconSize(40).withIconColor(android.graphics.Color.WHITE).withIdentifier(10001).withTag("DEV_OPTIONS"));
        fastItemAdapter.add(new com.benny.openlauncher.viewutil.IconLabelItem(ContextCompat.getDrawable(_homeActivity, R.drawable.ic_settings), "Launcher settings")
                .withIconSize(40).withIconColor(android.graphics.Color.WHITE).withIdentifier(10002).withTag("LAUNCHER_SETTINGS"));

        for (com.benny.openlauncher.model.App app : apps) {
            fastItemAdapter.add(new com.benny.openlauncher.viewutil.IconLabelItem(app.getIcon(), app._label)
                    .withIconSize(40)
                    .withIdentifier(app.hashCode())
                    .withTag(app));
        }

        final MaterialDialog dialog = new MaterialDialog.Builder(_homeActivity)
                .title(R.string.add_to_desktop)
                .adapter(fastItemAdapter, null)
                .autoDismiss(false)
                .positiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();

        fastItemAdapter.withOnClickListener(new com.mikepenz.fastadapter.listeners.OnClickListener<com.benny.openlauncher.viewutil.IconLabelItem>() {
            @Override
            public boolean onClick(View v, com.mikepenz.fastadapter.IAdapter<com.benny.openlauncher.viewutil.IconLabelItem> adapter, com.benny.openlauncher.viewutil.IconLabelItem item, int position) {
                Item desktopItem = null;
                Object tag = item.getTag();
                
                if (tag instanceof String) {
                    String action = (String) tag;
                    if (action.equals("DEV_OPTIONS")) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        desktopItem = Item.newShortcutItem(intent, ContextCompat.getDrawable(_homeActivity, R.drawable.ic_behavior), "Entwickler Optionen");
                    } else if (action.equals("LAUNCHER_SETTINGS")) {
                        desktopItem = Item.newActionItem(LauncherAction.Action.LauncherSettings.ordinal());
                        desktopItem._label = "Launcher settings";
                        desktopItem._icon = ContextCompat.getDrawable(_homeActivity, R.drawable.ic_settings);
                    } else if (action.equals("RESTART_LAUNCHER")) {
                        desktopItem = Item.newActionItem(LauncherAction.Action.Restart.ordinal());
                        desktopItem._label = "Restart launcher";
                        desktopItem._icon = ContextCompat.getDrawable(_homeActivity, R.drawable.ic_android);
                    }
                } else if (tag instanceof com.benny.openlauncher.model.App) {
                    com.benny.openlauncher.model.App app = (com.benny.openlauncher.model.App) tag;
                    desktopItem = Item.newAppItem(app);
                }

                if (desktopItem != null) {
                    Desktop desktop = _homeActivity.getDesktop();
                    Point point = desktop.getCurrentPage().findFreeSpace(desktopItem._spanX, desktopItem._spanY);
                    if (point != null) {
                        desktopItem._x = point.x;
                        desktopItem._y = point.y;
                        Setup.dataManager().saveItem(desktopItem, desktop.getCurrentPageIndex(), Definitions.ItemPosition.Desktop);
                        desktop.addItemToPage(desktopItem, desktop.getCurrentPageIndex());
                        Tool.toast(_homeActivity, desktopItem._label + " added");
                    } else {
                        Tool.toast(_homeActivity, R.string.toast_not_enough_space);
                    }
                }
                return true;
            }
        });

        dialog.show();
    }

    @Override
    public void onQuickRemove() {
        final Desktop desktop = _homeActivity.getDesktop();
        final CellContainer currentPage = desktop.getCurrentPage();
        final List<View> cells = currentPage.getAllCells();
        
        final com.mikepenz.fastadapter.commons.adapters.FastItemAdapter<com.benny.openlauncher.viewutil.IconLabelItem> fastItemAdapter = new com.mikepenz.fastadapter.commons.adapters.FastItemAdapter<>();
        
        for (View view : cells) {
            Item item = (Item) view.getTag();
            if (item != null) {
                String label = "";
                android.graphics.drawable.Drawable icon = null;
                if (item._type == Item.Type.APP || item._type == Item.Type.SHORTCUT) {
                    com.benny.openlauncher.model.App app = Setup.appLoader().findItemApp(item);
                    label = app != null ? app._label : "Unknown";
                    icon = app != null ? app.getIcon() : ContextCompat.getDrawable(_homeActivity, R.drawable.ic_android);
                } else if (item._type == Item.Type.GROUP) {
                    label = item._label != null ? item._label : "Group";
                    icon = ContextCompat.getDrawable(_homeActivity, R.drawable.ic_group);
                } else if (item._type == Item.Type.WIDGET) {
                    label = "Widget";
                    icon = ContextCompat.getDrawable(_homeActivity, R.drawable.ic_dashboard);
                } else {
                    label = "Action";
                    icon = ContextCompat.getDrawable(_homeActivity, R.drawable.ic_launch);
                }
                
                fastItemAdapter.add(new com.benny.openlauncher.viewutil.IconLabelItem(icon, label)
                        .withIconSize(40)
                        .withIconColor(android.graphics.Color.WHITE)
                        .withIdentifier(view.hashCode())
                        .withTag(view));
            }
        }

        if (fastItemAdapter.getAdapterItemCount() == 0) {
            Tool.toast(_homeActivity, "Page is empty");
            return;
        }

        final MaterialDialog dialog = new MaterialDialog.Builder(_homeActivity)
                .title("Quick Remove")
                .adapter(fastItemAdapter, null)
                .autoDismiss(false)
                .positiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();

        fastItemAdapter.withOnClickListener(new com.mikepenz.fastadapter.listeners.OnClickListener<com.benny.openlauncher.viewutil.IconLabelItem>() {
            @Override
            public boolean onClick(View v, com.mikepenz.fastadapter.IAdapter<com.benny.openlauncher.viewutil.IconLabelItem> adapter, com.benny.openlauncher.viewutil.IconLabelItem item, int position) {
                View viewToRemove = (View) item.getTag();
                Item itemToRemove = (Item) viewToRemove.getTag();
                
                currentPage.removeView(viewToRemove);
                Setup.dataManager().deleteItem(itemToRemove, true);
                
                fastItemAdapter.remove(position);
                if (fastItemAdapter.getAdapterItemCount() == 0) {
                    dialog.dismiss();
                }
                
                Tool.toast(_homeActivity, "Removed");
                return true;
            }
        });

        dialog.show();
    }
}