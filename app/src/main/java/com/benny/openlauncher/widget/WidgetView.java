package com.benny.openlauncher.widget;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetView extends AppWidgetHostView {
    private OnTouchListener _onTouchListener;
    private OnLongClickListener _longClick;
    private long _down;

    public WidgetView(Context context) {
        super(context);
        setLongClickable(true);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setScale(float scale) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        try {
            super.onLayout(changed, left, top, right, bottom);
        } catch (Exception e) {
            // Keep the crash protection but remove verbose logging
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener onTouchListener) {
        _onTouchListener = onTouchListener;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        _longClick = l;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (_onTouchListener != null) {
            _onTouchListener.onTouch(this, ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                _down = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                long delta = System.currentTimeMillis() - _down;
                if (delta > 300L) {
                    if (_longClick != null) {
                        _longClick.onLongClick(this);
                        return true;
                    }
                }
                break;
        }

        return false;
    }
}
