package com.benny.openlauncher.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.DatabaseHelper;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.Definitions.ItemPosition;
import com.benny.openlauncher.util.DragAction.Action;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DesktopCallback;
import com.benny.openlauncher.viewutil.ItemViewFactory;

import java.util.List;

public final class Dock extends CellContainer implements DesktopCallback {
    private HomeActivity _homeActivity;
    private final Point _coordinate = new Point();
    private final Point _previousDragPoint = new Point();

    private Item _previousItem;
    private View _previousItemView;

    // open app drawer on slide up gesture
    private float _startPosX;
    private float _startPosY;

    private Paint _paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF _bgRect = new RectF();

    public Dock(Context context, AttributeSet attr) {
        super(context, attr);
        setWillNotDraw(false);
        setAlpha(0f);
    }

    public final void initDock() {
        final int columns = Setup.appSettings().getDockColumnCount();
        final int rows = Setup.appSettings().getDockRowCount();
        setGridSize(columns, rows);
        HomeActivity._db.getDockAsync(new DatabaseHelper.DataCallback<List<Item>>() {
            @Override
            public void onDataLoaded(List<Item> dockItems) {
                removeAllViews();
                for (Item item : dockItems) {
                    if (item._x + item._spanX > columns) item._x = Math.max(0, columns - item._spanX);
                    if (item._y + item._spanY > rows) item._y = Math.max(0, rows - item._spanY);
                    addItemToPage(item, 0);
                }
                // Fade in after items are loaded and layout is ready
                post(new Runnable() {
                    @Override
                    public void run() {
                        animate().alpha(1f).setDuration(200).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                        if (Setup.appSettings().getDockIosStyle() && _homeActivity != null) {
                            View iosBg = _homeActivity.findViewById(com.benny.openlauncher.R.id.ios_dock_background);
                            if (iosBg != null) iosBg.animate().alpha(1f).setDuration(200).start();
                        }
                    }
                });
            }
        });

        // call onMeasure to set the height
        measure(getMeasuredWidth(), getMeasuredHeight());
    }

    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        detectSwipe(ev);
        super.dispatchTouchEvent(ev);
        return true;
    }

    private void detectSwipe(MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
                _startPosX = ev.getX();
                _startPosY = ev.getY();
                break;
            case 1:
                if (_startPosY - ev.getY() > 150.0f && Setup.appSettings().getGestureDockSwipeUp()) {
                    Point point = new Point((int) ev.getX(), (int) ev.getY());
                    point = Tool.convertPoint(point, this, _homeActivity.getAppDrawerController());
                    if (Setup.appSettings().getGestureFeedback()) {
                        Tool.vibrate(this);
                    }
                    _homeActivity.openAppDrawer(this, point.x, point.y);
                    break;
                }
            default:
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (Setup.appSettings().getDockIosStyle()) {
            int width = ((r - l) - getPaddingLeft()) - getPaddingRight();
            int height = ((b - t) - getPaddingTop()) - getPaddingBottom();
            int columns = getCellSpanH();
            if (columns == 0) columns = 1;

            int settingsIconSize = Tool.dp2px(Setup.appSettings().getIconSize());
            int dockIconSize = (int) (settingsIconSize * 1.1f);
            float gap = settingsIconSize * 0.30f;
            
            // Total background width matching HomeActivity
            float bgWidth = columns * dockIconSize + (columns + 1) * gap;
            
            float bgLeft = (width - bgWidth) / 2f;
            float startX = getPaddingLeft() + bgLeft + gap;

            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != View.GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    
                    // Apply 10% larger size to the view if it's an AppItemView
                    if (child instanceof AppItemView) {
                        ((AppItemView) child).setIconSize(dockIconSize);
                    }

                    int childWidth = dockIconSize;
                    int childHeight = height;
                    
                    child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), 
                                 MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));

                    float left = startX + (lp.getX() * (dockIconSize + gap));
                    child.layout((int)left, 0, (int)(left + childWidth), childHeight);
                }
            }
        } else {
            super.onLayout(changed, l, t, r, b);
        }
    }

    public final void updateIconProjection(int x, int y) {
        HomeActivity launcher = _homeActivity;
        ItemOptionView dragNDropView = launcher.getItemOptionView();
        DragState state = peekItemAndSwap(x, y, _coordinate);
        if (!_coordinate.equals(_previousDragPoint)) {
            dragNDropView.cancelFolderPreview();
        }
        _previousDragPoint.set(_coordinate.x, _coordinate.y);
        switch (state) {
            case CurrentNotOccupied:
                projectImageOutlineAt(_coordinate, DragHandler._cachedDragBitmap);
                break;
            case CurrentOccupied:
                Item.Type type = dragNDropView.getDragItem()._type;
                clearCachedOutlineBitmap();
                if (!type.equals(Item.Type.WIDGET) && (coordinateToChildView(_coordinate) instanceof AppItemView)) {
                    dragNDropView.showFolderPreviewAt(this, getCellWidth() * (_coordinate.x + 0.5f), getCellHeight() * (_coordinate.y + 0.5f) - (Setup.appSettings().getDockShowLabel() ? Tool.dp2px(7) : 0));
                }
                break;
            case OutOffRange:
            case ItemViewNotFound:
            default:
                break;
        }
    }

    @Override
    public void setLastItem(Item item, View view) {
        _previousItemView = view;
        _previousItem = item;
        removeView(view);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!isInEditMode()) {
            // set the height for the dock based on the number of rows and the show label preference
            int iconSize = Setup.appSettings().getDockIconSize();
            int height = Tool.dp2px((iconSize + 20) * getCellSpanV());
            if (Setup.appSettings().getDockShowLabel()) height += Tool.dp2px(20);
            
            if (Setup.appSettings().getDockIosStyle()) {
                // Ensure dock is high enough for the 170% background + 10dp
                height = (int) (Tool.dp2px(Setup.appSettings().getIconSize()) * 1.7f) + Tool.dp2px(10);
            }
            
            getLayoutParams().height = height;
            setMeasuredDimension(View.getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void consumeLastItem() {
        _previousItem = null;
        _previousItemView = null;
    }

    @Override
    public void revertLastItem() {
        if (_previousItemView != null && _previousItem != null) {
            addViewToGrid(_previousItemView);
            _previousItem = null;
            _previousItemView = null;
        }
    }

    public boolean addItemToPage(@NonNull Item item, int page) {
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item, isDockShowLabel());
        if (itemView == null) {
            // TODO see if this fixes SD card bug
            //HomeActivity._db.deleteItem(item, true);
            return false;
        }
        item._location = ItemPosition.Dock;
        addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
        return true;
    }

    public boolean addItemToPoint(@NonNull Item item, int x, int y) {
        LayoutParams positionToLayoutPrams = coordinateToLayoutParams(x, y, item._spanX, item._spanY);
        if (positionToLayoutPrams == null) {
            Point pos = new Point();
            touchPosToCoordinate(pos, x, y, item._spanX, item._spanY, false);
            if (pos.x != -1 && pos.y != -1) {
                positionToLayoutPrams = new LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, pos.x, pos.y, item._spanX, item._spanY);
            }
        }
        if (positionToLayoutPrams == null) {
            return false;
        }
        item._location = ItemPosition.Dock;
        item._x = positionToLayoutPrams.getX();
        item._y = positionToLayoutPrams.getY();
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item, isDockShowLabel());
        if (itemView != null) {
            itemView.setLayoutParams(positionToLayoutPrams);
            addView(itemView);
        }
        return true;
    }

    public boolean addItemToCell(@NonNull Item item, int x, int y) {
        item._location = ItemPosition.Dock;
        item._x = x;
        item._y = y;
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item, isDockShowLabel());
        if (itemView == null) {
            return false;
        }
        addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
        return true;
    }

    public void removeItem(final View view, boolean animate) {
        if (animate) {
            view.animate().setDuration(100).scaleX(0.0f).scaleY(0.0f).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (view.getParent().equals(Dock.this)) {
                        removeView(view);
                    }
                }
            });
        } else if (this.equals(view.getParent())) {
            removeView(view);
        }
    }

    public void setHome(HomeActivity homeActivity) {
        _homeActivity = homeActivity;
    }

    private Boolean isDockShowLabel() {
        boolean b = Setup.appSettings().getDockShowLabel();
        Boolean ret = new Boolean(b);
        return ret;
    }
}
