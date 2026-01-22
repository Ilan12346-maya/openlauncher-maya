package com.benny.openlauncher.activity.homeparts;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Process;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.interfaces.DialogListener;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.Definitions.ItemPosition;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.PopupDynamicIconLabelItem;
import com.benny.openlauncher.widget.Desktop;
import com.benny.openlauncher.widget.Dock;
import android.view.LayoutInflater;
import android.widget.SeekBar;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.benny.openlauncher.widget.WidgetContainer;
import com.benny.openlauncher.widget.WidgetView;

public class HpItemOption implements DialogListener.OnEditDialogListener {
    private HomeActivity _homeActivity;
    private Item _item;

    public HpItemOption(HomeActivity homeActivity) {
        _homeActivity = homeActivity;
    }

    public void onEditItem(final Item item) {
        _item = item;
        Setup.eventHandler().showEditDialog(_homeActivity, item, this);
    }

    public final void onUninstallItem(@NonNull Item item) {
        _homeActivity.ignoreResume = true;
        Setup.eventHandler().showDeletePackageDialog(_homeActivity, item);
    }

    public final void onRemoveItem(@NonNull Item item) {
        View coordinateToChildView;
        if (item._location.equals(ItemPosition.Group)) {
            Tool.toast(_homeActivity, R.string.toast_remove_from_group_first);
            return;
        } else if (item._location.equals(ItemPosition.Desktop)) {
            Desktop desktop = _homeActivity.getDesktop();
            coordinateToChildView = desktop.getCurrentPage().coordinateToChildView(new Point(item._x, item._y));
            desktop.removeItem(coordinateToChildView, true);
        } else {
            Dock dock = _homeActivity.getDock();
            coordinateToChildView = dock.coordinateToChildView(new Point(item._x, item._y));
            dock.removeItem(coordinateToChildView, true);
        }
        Setup.dataManager().deleteItem(item, true);
    }

    public final void onInfoItem(@NonNull Item item) {
        if (item._type == Item.Type.APP) {
            try {
                String str = "android.settings.APPLICATION_DETAILS_SETTINGS";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("package:");
                Intent intent = item._intent;
                ComponentName component = intent.getComponent();
                stringBuilder.append(component.getPackageName());
                _homeActivity.startActivity(new Intent(str, Uri.parse(stringBuilder.toString())));
            } catch (Exception e) {
                Tool.toast(_homeActivity, R.string.toast_app_uninstalled);
            }
        }
    }

    public final void onResizeItem(@NonNull final Item item) {
        View coordinateToChildView;
        if (item._location.equals(ItemPosition.Desktop)) {
            Desktop desktop = _homeActivity.getDesktop();
            coordinateToChildView = desktop.getCurrentPage().coordinateToChildView(new Point(item._x, item._y));
        } else {
            Dock dock = _homeActivity.getDock();
            coordinateToChildView = dock.coordinateToChildView(new Point(item._x, item._y));
        }

        if (coordinateToChildView != null && coordinateToChildView instanceof WidgetContainer) {
            final WidgetContainer container = (WidgetContainer) coordinateToChildView;
            final WidgetView widgetView = (WidgetView) container.getChildAt(0);
            
            // Show the resize handles
            container.showResize();

            // Create and show the scale dialog
            MaterialDialog.Builder builder = new MaterialDialog.Builder(_homeActivity);
            builder.title(R.string.resize);
            
            View dialogView = LayoutInflater.from(_homeActivity).inflate(R.layout.dialog_widget_scale, null);
            final SeekBar seekBar = dialogView.findViewById(R.id.scale_seekbar);
            final TextView label = dialogView.findViewById(R.id.scale_label);
            
            seekBar.setProgress((int) (item.getWidgetScale() * 100));
            label.setText("Scale: " + (int) (item.getWidgetScale() * 100) + "%");
            
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float scale = progress / 100f;
                    item.setWidgetScale(scale);
                    widgetView.setScale(scale);
                    label.setText("Scale: " + progress + "%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Setup.dataManager().saveItem(item);
                }
            });
            
            builder.customView(dialogView, true);
            builder.positiveText(android.R.string.ok);
            builder.show();
        }
    }

    public final void onStartShortcutItem(@NonNull Item item, int position) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            LauncherApps launcherApps = (LauncherApps) _homeActivity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            ShortcutInfo shortcutInfo = item.getShortcutInfo().get(position);
            launcherApps.startShortcut(shortcutInfo, null, null);
        }
    }

    public final void onAddToDesktop(@NonNull Item item) {
        Point position = _homeActivity.getDesktop().getCurrentPage().findFreeSpace();
        if (position != null) {
            Item desktopItem = new Item();
            desktopItem._type = Item.Type.APP;
            desktopItem._label = item._label;
            desktopItem._icon = item._icon;
            desktopItem._intent = item._intent;
            desktopItem._x = position.x;
            desktopItem._y = position.y;
            desktopItem._location = ItemPosition.Desktop;
            // Use createItem to ensure it's added correctly to the current page
            Setup.dataManager().createItem(desktopItem, _homeActivity.getDesktop().getCurrentPageIndex(), ItemPosition.Desktop);
            _homeActivity.getDesktop().addItemToCell(desktopItem, desktopItem._x, desktopItem._y);
        } else {
            Tool.toast(_homeActivity, R.string.toast_no_free_space);
        }
    }

    public final void onForceStop(@NonNull Item item) {
        if (item._type == Item.Type.APP) {
            onInfoItem(item); // Opening info is the safest way to force stop
            Tool.toast(_homeActivity, "Tippe auf 'Stoppen erzwingen'");
        }
    }

    @Override
    public void onRename(String name) {
        _item.setLabel(name);
        Setup.dataManager().saveItem(_item);
        Point point = new Point(_item._x, _item._y);

        if (_item._location.equals(ItemPosition.Group)) {
            return;
        } else if (_item._location.equals(ItemPosition.Desktop)) {
            Desktop desktop = _homeActivity.getDesktop();
            desktop.removeItem(desktop.getCurrentPage().coordinateToChildView(point), false);
            desktop.addItemToCell(_item, _item._x, _item._y);
        } else {
            Dock dock = _homeActivity.getDock();
            _homeActivity.getDock().removeItem(dock.coordinateToChildView(point), false);
            dock.addItemToCell(_item, _item._x, _item._y);
        }
    }
}