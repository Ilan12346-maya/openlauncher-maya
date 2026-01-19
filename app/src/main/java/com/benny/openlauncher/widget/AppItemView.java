package com.benny.openlauncher.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Process;
import androidx.core.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.notifications.NotificationListener;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.LauncherAction;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DesktopCallback;
import com.benny.openlauncher.viewutil.GroupDrawable;

import com.benny.openlauncher.util.iconloader.AsyncIconLoader;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

public class AppItemView extends View implements Drawable.Callback, NotificationListener.NotificationCallback {
    private static final int MIN_ICON_TEXT_MARGIN = 8;
    private static final char ELLIPSIS = '…';

    private Drawable _icon = null;
    private boolean _isIconLoading = false;
    private String _label;
    private TextPaint _textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint _notifyTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Paint _notifyPaint = new Paint();
    private Rect _textContainer = new Rect(), testTextContainer = new Rect();
    private float _iconSize;
    private boolean _showLabel = true;
    private boolean _vibrateWhenLongPress;
    private float _labelHeight;
    private int _targetedWidth;
    private int _targetedHeightPadding;
    private float _heightPadding;

    private int _notificationCount = 0;
    private boolean _isSelected = false;

    public AppItemView(Context context) {
        this(context, null);
    }

    public AppItemView(Context context, AttributeSet attrs) {
        super(context, attrs);

        _labelHeight = Tool.dp2px(14);
        _textPaint.setTextSize(Tool.sp2px(12));
        _textPaint.setColor(Color.WHITE);
        _notifyTextPaint.setColor(Color.WHITE);
        _notifyPaint.setColor(Color.RED);
    }

    public void setSelected(boolean selected) {
        _isSelected = selected;
        invalidate();
    }

    public boolean isSelected() {
        return _isSelected;
    }

    public Drawable getIcon() {
        return _icon;
    }

    public void setIcon(Drawable icon) {
        _icon = icon;
    }

    public String getLabel() {
        return _label;
    }

    public void setLabel(String label) {
        _label = label;
        updateLabels();
    }

    public void notificationCallback(Integer count) {
        _notificationCount = count;

        invalidate();
    }

    public float getIconSize() {
        return _iconSize;
    }

    public void setIconSize(float iconSize) {
        _iconSize = iconSize;
        updateLabels();
    }

    public boolean getShowLabel() {
        return _showLabel;
    }

    public void setTargetedWidth(int width) {
        _targetedWidth = width;
    }

    public void setTargetedHeightPadding(int padding) {
        _targetedHeightPadding = padding;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float mWidth = _iconSize;
        float mHeight = _iconSize + (_showLabel ? _labelHeight : 0);
        if (_targetedWidth != 0) {
            mWidth = _targetedWidth;
        }
        setMeasuredDimension((int) Math.ceil(mWidth), (int) Math.ceil((int) mHeight) + Tool.dp2px(2) + _targetedHeightPadding * 2);
    }

    private String _displayLabel;
    private float _textX, _textY;

    private void updateLabels() {
        if (_label == null || !_showLabel) {
            _displayLabel = null;
            return;
        }

        int maxTextWidth = getWidth() - MIN_ICON_TEXT_MARGIN * 2;
        if (maxTextWidth <= 0) return;

        _displayLabel = android.text.TextUtils.ellipsize(_label, _textPaint, maxTextWidth, android.text.TextUtils.TruncateAt.END).toString();
        _textPaint.getTextBounds(_displayLabel, 0, _displayLabel.length(), _textContainer);
        
        _textX = (getWidth() - _textContainer.width()) / 2f;
        _textY = getHeight() - _heightPadding;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        _heightPadding = (getHeight() - _iconSize - (_showLabel ? _labelHeight : 0)) / 2f;
        updateLabels();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_displayLabel == null && _label != null && _showLabel) {
            _heightPadding = (getHeight() - _iconSize - (_showLabel ? _labelHeight : 0)) / 2f;
            updateLabels();
        }

        if (_displayLabel != null && _showLabel) {
            canvas.drawText(_displayLabel, _textX, _textY, _textPaint);
        }

        // center the _icon
        if (_icon != null) {
            canvas.save();
            canvas.translate((getWidth() - _iconSize) / 2, _heightPadding);
            _icon.setBounds(0, 0, (int) _iconSize, (int) _iconSize);
            _icon.draw(canvas);

            if (_notificationCount > 0) {
                final String count = (_notificationCount > 99 ? "++" : String.valueOf(_notificationCount));                
                float radius = _iconSize * .15f;
                float offset = Tool.dp2px(3);
                canvas.drawCircle(_iconSize - radius + offset, radius - offset, radius, _notifyPaint);

                _notifyTextPaint.setTextSize((int) (radius * 1.5));

                canvas.drawText(count,
                        _iconSize - radius + offset - (_notifyTextPaint.measureText(count) / 2),
                        radius - offset - ((_notifyTextPaint.descent() + _notifyTextPaint.ascent()) / 2),
                        _notifyTextPaint);
            }

            if (_isSelected) {
                float radius = _iconSize * .2f;
                _notifyPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                canvas.drawCircle(_iconSize - radius, _iconSize - radius, radius, _notifyPaint);
                _notifyPaint.setColor(Color.RED); // Reset to default

                _notifyTextPaint.setColor(Color.WHITE);
                _notifyTextPaint.setTextSize((int) (radius * 1.5));
                canvas.drawText("✓",
                        _iconSize - radius - (_notifyTextPaint.measureText("✓") / 2),
                        _iconSize - radius - ((_notifyTextPaint.descent() + _notifyTextPaint.ascent()) / 2),
                        _notifyTextPaint);
            }

            canvas.restore();
        }
    }

    public float getDrawIconTop() {
        return _heightPadding;
    }

    public float getDrawIconLeft() {
        return (getWidth() - _iconSize) / 2;
    }

    public static class Builder {
        // TODO accept any view and just add click and long click listeners
        // this class isn't necessary
        // remove in favor of using ItemViewFactory
        AppItemView _view;

        public Builder(Context context) {
            _view = new AppItemView(context);
        }

        public Builder(AppItemView view) {
            _view = view;
        }

        public AppItemView getView() {
            return _view;
        }

        public Builder setAppItem(final Item item) {
            _view.setLabel(item.getLabel());
            final App app = AppManager.getInstance(_view.getContext()).findApp(item._intent);
            if (app != null) {
                Context context = _view.getContext();
                if (context instanceof Activity && ((Activity) context).isDestroyed()) {
                    return this;
                }
                Glide.with(context)
                     .load(app)
                     .into(new CustomTarget<Drawable>() {
                         @Override
                         public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                             _view.setIcon(resource);
                             _view.invalidate();
                         }
                         @Override
                         public void onLoadCleared(@Nullable Drawable placeholder) {
                             _view.setIcon(placeholder);
                         }
                     });
            } else {
                _view.setIcon(item.getIcon());
            }

            _view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tool.createScaleInScaleOutAnim(_view, new Runnable() {
                        @Override
                        public void run() {
                            Tool.startApp(_view.getContext(), AppManager.getInstance(_view.getContext()).findApp(item._intent), _view);
                        }
                    });
                }
            });
            return this;
        }

        public Builder setShortcutItem(final Item item) {
            _view.setLabel(item.getLabel());
            _view.setIcon(item.getIcon());
            _view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tool.createScaleInScaleOutAnim(_view, new Runnable() {
                        @Override
                        public void run() {

                            Intent intent = item.getIntent();
                            String id = intent.getStringExtra("shortcut_id");

                            /* old style shortcut */
                            if (id == null || id.trim().isEmpty()) {
                                _view.getContext().startActivity(intent);
                            } 
                            /* new style shortcut */
                            else {
                                LauncherApps launcherApps = (LauncherApps) _view.getContext().getSystemService(Context.LAUNCHER_APPS_SERVICE);
                                String packageName = intent.getPackage();
                                launcherApps.startShortcut(packageName, id, intent.getSourceBounds(), null, Process.myUserHandle());
                            }
                        }
                    });
                }
            });
            return this;
        }

        public Builder setGroupItem(Context context, final DesktopCallback callback, final Item item) {
            _view.setLabel(item.getLabel());
            _view.setIcon(new GroupDrawable(context, item, Setup.appSettings().getIconSize()));
            _view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (HomeActivity.Companion.getLauncher() != null && (HomeActivity.Companion.getLauncher()).getGroupPopup().showPopup(item, v, callback)) {
                        ((GroupDrawable) ((AppItemView) v).getIcon()).popUp();
                    }
                }
            });
            return this;
        }

        public Builder setActionItem(final Item item) {
            _view.setLabel(item.getLabel());
            _view.setIcon(item.getIcon());
            _view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Tool.createScaleInScaleOutAnim(_view, new Runnable() {
                        @Override
                        public void run() {
                            LauncherAction.ActionDisplayItem actionItem = LauncherAction.getActionItem(item.getActionValue(), _view.getContext());
                            if (actionItem != null) {
                                LauncherAction.RunAction(actionItem, _view.getContext());
                            } else if (item.getActionValue() == 99) {
                                // Special case for Restart
                                if (_view.getContext() instanceof android.app.Activity) {
                                    ((android.app.Activity) _view.getContext()).recreate();
                                }
                            }
                        }
                    });
                }
            });
            return this;
        }

        public Builder withOnLongClick(final Item item, final DragAction.Action action, DesktopCallback desktopCallback) {
            _view.setOnLongClickListener(DragHandler.getLongClick(item, action, desktopCallback));
            return this;
        }

        public Builder setTextColor(int color) {
            _view._textPaint.setColor(color);
            return this;
        }

        public Builder setIconSize(int iconSize) {
            _view.setIconSize(Tool.dp2px(iconSize));
            return this;
        }

        public Builder setLabelVisibility(boolean visible) {
            _view._showLabel = visible;
            return this;
        }

        public Builder vibrateWhenLongPress(boolean vibrate) {
            _view._vibrateWhenLongPress = vibrate;
            return this;
        }
    }
}
