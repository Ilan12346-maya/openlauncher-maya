package com.benny.openlauncher.viewutil;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.notifications.NotificationListener;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.widget.AppItemView;
import com.benny.openlauncher.widget.CellContainer;
import com.benny.openlauncher.widget.WidgetContainer;
import com.benny.openlauncher.widget.WidgetView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemViewFactory {
    private static Logger LOG = LoggerFactory.getLogger("ItemViewFactory");

    public static View getItemView(final Context context, final DesktopCallback callback, final DragAction.Action type, final Item item, Boolean showLabel) {
        View view = null;
        if (item.getType().equals(Item.Type.WIDGET)) {
            view = getWidgetView(context, callback, type, item);
        } else {
            AppItemView.Builder builder = new AppItemView.Builder(context);
            builder.setIconSize(Setup.appSettings().getIconSize());
            builder.vibrateWhenLongPress(Setup.appSettings().getGestureFeedback());
            builder.withOnLongClick(item, type, callback);
            switch (type) {
                case DRAWER:
                    builder.setLabelVisibility(Setup.appSettings().getDrawerShowLabel());
                    builder.setTextColor(Setup.appSettings().getDrawerLabelColor());
                    break;
                case DESKTOP:
                default:
                    builder.setLabelVisibility(Setup.appSettings().getDesktopShowLabel());
                    builder.setTextColor(Color.WHITE);
                    break;
            }
            if (showLabel != null) {
                boolean labelVisibility = showLabel.booleanValue();
                builder.setLabelVisibility(labelVisibility);
            }

            switch (item.getType()) {
                case APP:
                    final App app = Setup.appLoader().findItemApp(item);
                    if (app == null) break;
                    view = builder.setAppItem(item).getView();

                    if (Setup.appSettings().getNotificationStatus()) {
                        NotificationListener.setNotificationCallback(app.getPackageName(), (NotificationListener.NotificationCallback) view);
                    }
                    break;
                case SHORTCUT:
                    view = builder.setShortcutItem(item).getView();
                    break;
                case GROUP:
                    view = builder.setGroupItem(context, callback, item).getView();
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    break;
                case ACTION:
                    view = builder.setActionItem(item).getView();
                    break;
            }
        }

        // TODO find out why tag is set here
        if (view != null) {
            view.setTag(item);
            view.setLayoutParams(new CellContainer.LayoutParams(CellContainer.LayoutParams.WRAP_CONTENT, CellContainer.LayoutParams.WRAP_CONTENT, item._x, item._y, item._xL, item._yL, item._spanX, item._spanY));
        }

        return view;
    }

    public static View getItemView(final Context context, final DesktopCallback callback, final DragAction.Action type, final Item item) {
        return getItemView(context, callback, type, item, null);
    }

    public static View getWidgetView(final Context context, final DesktopCallback callback, final DragAction.Action type, final Item item) {
        if (HomeActivity._appWidgetHost == null) {
            com.benny.openlauncher.util.Logger.log("ItemViewFactory", "getWidgetView: _appWidgetHost is NULL");
            return null;
        }

        com.benny.openlauncher.util.Logger.log("ItemViewFactory", "getWidgetView: appWidgetId=" + item.getWidgetValue());
        AppWidgetProviderInfo appWidgetInfo = HomeActivity._appWidgetManager.getAppWidgetInfo(item.getWidgetValue());

        if (appWidgetInfo == null) {
            com.benny.openlauncher.util.Logger.log("ItemViewFactory", "getWidgetView: appWidgetInfo is NULL for id " + item.getWidgetValue());
            if (item._label.contains(Definitions.DELIMITER)) {
                String[] cnSplit = item._label.split(Definitions.DELIMITER);
                ComponentName cn = new ComponentName(cnSplit[0], cnSplit[1]);

                int appWidgetId = HomeActivity._appWidgetHost.allocateAppWidgetId();
                com.benny.openlauncher.util.Logger.log("ItemViewFactory", "getWidgetView: trying to rebind cn=" + cn + " to new id " + appWidgetId);
                if (HomeActivity._appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    appWidgetInfo = HomeActivity._appWidgetManager.getAppWidgetInfo(appWidgetId);
                    item.setWidgetValue(appWidgetId);
                    Setup.dataManager().updateItem(item);
                } else {
                    LOG.error("Unable to bind app widget id: {}; showing placeholder", cn);
                    HomeActivity._appWidgetHost.deleteAppWidgetId(appWidgetId);
                    return getWidgetPlaceholder(context, item);
                }
            } else {
                LOG.debug("Unable to identify Widget for rehydration; showing placeholder");
                return getWidgetPlaceholder(context, item);
            }
        }

        View view = null;
        try {
            Context widgetContext = HomeActivity._widgetContext != null ? HomeActivity._widgetContext : context.getApplicationContext();
            view = HomeActivity._appWidgetHost.createView(widgetContext, item.getWidgetValue(), appWidgetInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (view == null) {
            return getWidgetPlaceholder(context, item);
        }
        
        final WidgetView widgetView = (WidgetView) view;
        widgetView.setAppWidget(item.getWidgetValue(), appWidgetInfo);
        widgetView.setScale(item.getWidgetScale());

        final WidgetContainer widgetContainer = new WidgetContainer(context, widgetView, item);
        widgetContainer.updateWidgetOption(item);

        widgetView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (Setup.appSettings().getDesktopLock()) {
                    return false;
                }
                if (Setup.appSettings().getGestureFeedback()) {
                    Tool.vibrate(view);
                }
                DragHandler.startDrag(widgetContainer, widgetContainer.getItem(), DragAction.Action.DESKTOP, callback);
                return true;
            }
        });

        return widgetContainer;
    }

    private static View getWidgetPlaceholder(final Context context, final Item item) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(Color.argb(180, 40, 40, 40));
        
        android.widget.TextView textView = new android.widget.TextView(context);
        textView.setText("Restoration Failed\n" + item.getLabel());
        textView.setTextColor(Color.WHITE);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(0, 0, 0, Tool.dp2px(8));
        
        android.widget.Button repairBtn = new android.widget.Button(context);
        repairBtn.setText("Repair Widget");
        repairBtn.setTextSize(12);
        
        layout.addView(textView);
        layout.addView(repairBtn);
        
        final WidgetContainer widgetContainer = new WidgetContainer(context, layout, item);
        
        View.OnLongClickListener longClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (Setup.appSettings().getDesktopLock()) return false;
                if (Setup.appSettings().getGestureFeedback()) Tool.vibrate(view);
                DragHandler.startDrag(widgetContainer, widgetContainer.getItem(), DragAction.Action.DESKTOP, null);
                return true;
            }
        };
        
        layout.setOnLongClickListener(longClick);
        repairBtn.setOnLongClickListener(longClick);
        
        repairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeActivity launcher = HomeActivity.Companion.getLauncher();
                if (launcher != null) {
                    // Start the pick process
                    launcher.ignoreResume = true;
                    int appWidgetId = HomeActivity._appWidgetHost.allocateAppWidgetId();
                    
                    if (item.getLabel().contains(Definitions.DELIMITER)) {
                        String[] cnSplit = item.getLabel().split(Definitions.DELIMITER);
                        ComponentName cn = new ComponentName(cnSplit[0], cnSplit[1]);
                        
                        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, cn);
                        // We store the item ID in the request code to identify which item to update
                        // Use a safe range for request codes
                        launcher.startActivityForResult(intent, HomeActivity.REQUEST_PICK_APPWIDGET);
                        // Store the current item ID in HomeActivity for update after result
                        launcher._desktopOption.setRepairItem(item);
                    } else {
                        Tool.toast(context, "Cannot identify widget package.");
                    }
                }
            }
        });
        
        return widgetContainer;
    }
}
