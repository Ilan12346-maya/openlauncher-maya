package com.benny.openlauncher.widget;
import com.benny.openlauncher.manager.Setup;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.Tool;

public class WidgetContainer extends FrameLayout {
    View ve;
    View he;
    View vl;
    View hl;
    Item _item;

    final Runnable action = new Runnable() {
        @Override
        public void run() {
            ve.animate().scaleY(0).scaleX(0);
            he.animate().scaleY(0).scaleX(0);
            vl.animate().scaleY(0).scaleX(0);
            hl.animate().scaleY(0).scaleX(0);
        }
    };

    public WidgetContainer(Context context, final View widgetView, final Item item) {
        super(context);
        _item = item;

        setPadding(0, 0, 0, 0);
        setClipChildren(false);
        setClipToPadding(false);

        addView(widgetView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_widget_container, this);

        ve = findViewById(R.id.vertexpand);
        he = findViewById(R.id.horiexpand);
        vl = findViewById(R.id.vertless);
        hl = findViewById(R.id.horiless);

        final WidgetContainer widgetContainer = this;
        ve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getScaleX() < 1) return;
                item.setSpanY(item.getSpanY() + 1);
                scaleWidget(widgetContainer, item);
                widgetContainer.removeCallbacks(action);
                widgetContainer.postDelayed(action, 2000);
            }
        });

        he.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getScaleX() < 1) return;
                item.setSpanX(item.getSpanX() + 1);
                scaleWidget(widgetContainer, item);
                widgetContainer.removeCallbacks(action);
                widgetContainer.postDelayed(action, 2000);
            }
        });

        vl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getScaleX() < 1) return;
                item.setSpanY(item.getSpanY() - 1);
                scaleWidget(widgetContainer, item);
                widgetContainer.removeCallbacks(action);
                widgetContainer.postDelayed(action, 2000);
            }
        });

        hl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getScaleX() < 1) return;
                item.setSpanX(item.getSpanX() - 1);
                scaleWidget(widgetContainer, item);
                widgetContainer.removeCallbacks(action);
                widgetContainer.postDelayed(action, 2000);
            }
        });
    }

    public void showResize() {
        ve.animate().scaleY(1).scaleX(1);
        he.animate().scaleY(1).scaleX(1);
        vl.animate().scaleY(1).scaleX(1);
        hl.animate().scaleY(1).scaleX(1);

        postDelayed(action, 3000);
    }

    public Item getItem() {
        return _item;
    }

    public void scaleWidget(View view, Item item) {
        Desktop desktop = HomeActivity.Companion.getLauncher().getDesktop();
        if (desktop == null || desktop.getCurrentPage() == null) return;

        item.setSpanX(Math.min(item.getSpanX(), desktop.getCurrentPage().getCellSpanH()));
        item.setSpanX(Math.max(item.getSpanX(), 1));
        item.setSpanY(Math.min(item.getSpanY(), desktop.getCurrentPage().getCellSpanV()));
        item.setSpanY(Math.max(item.getSpanY(), 1));

        desktop.getCurrentPage().setOccupied(false, (CellContainer.LayoutParams) view.getLayoutParams());

        if (!desktop.getCurrentPage().checkOccupied(new Point(item.getX(), item.getY()), item.getSpanX(), item.getSpanY())) {
            CellContainer.LayoutParams newWidgetLayoutParams = new CellContainer.LayoutParams(CellContainer.LayoutParams.WRAP_CONTENT, CellContainer.LayoutParams.WRAP_CONTENT, item.getX(), item.getY(), item.getSpanX(), item.getSpanY());

            // update occupied array
            desktop.getCurrentPage().setOccupied(true, newWidgetLayoutParams);

            // update the view
            view.setLayoutParams(newWidgetLayoutParams);
            updateWidgetOption(item);

            // update the widget size in the database
            Setup.dataManager().saveItem(item);
        } else {
            Toast.makeText(desktop.getContext(), R.string.toast_not_enough_space, Toast.LENGTH_SHORT).show();

            // add the old layout params to the occupied array
            desktop.getCurrentPage().setOccupied(true, (CellContainer.LayoutParams) view.getLayoutParams());
        }
    }

    public void updateWidgetOption(Item item) {
        if (item.getWidgetValue() == -1) return;
        
        Desktop desktop = HomeActivity.Companion.getLauncher().getDesktop();
        if (desktop == null || desktop.getCurrentPage() == null) return;

        int cellWidth = desktop.getCurrentPage().getCellWidth();
        int cellHeight = desktop.getCurrentPage().getCellHeight();

        if (cellWidth <= 0 || cellHeight <= 0) {
            // desktop isn't laid out yet, don't send updates
            return;
        }

        Bundle newOps = new Bundle();
        int width = Tool.px2dp(item.getSpanX() * cellWidth);
        int height = Tool.px2dp(item.getSpanY() * cellHeight);
        
        if (width <= 0 || height <= 0) return;

        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height);
        
        try {
            HomeActivity._appWidgetManager.updateAppWidgetOptions(item.getWidgetValue(), newOps);
        } catch (Exception e) {
            // Ignore
        }
    }
}
