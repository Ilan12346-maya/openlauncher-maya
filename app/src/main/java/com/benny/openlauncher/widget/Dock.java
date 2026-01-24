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

    // We keep track of whether a swipe up gesture has been intercepted
    private boolean _interceptedSwipeUp = false;

    private Paint _paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF _bgRect = new RectF();

    public Dock(Context context, AttributeSet attr) {
        super(context, attr);
        setWillNotDraw(false);
    }

    public final void initDock() {
        if (getAlpha() > 0.1f) {
            animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(120).withEndAction(new Runnable() {
                @Override
                public void run() {
                    initDockInternal(false);
                }
            });
            if (Setup.appSettings().getDockIosStyle() && _homeActivity != null) {
                View iosBg = _homeActivity.findViewById(com.benny.openlauncher.R.id.ios_dock_background);
                if (iosBg != null) iosBg.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(120).start();
            }
        } else {
            setAlpha(0f);
            setScaleX(0.85f);
            setScaleY(0.85f);
            if (Setup.appSettings().getDockIosStyle() && _homeActivity != null) {
                View iosBg = _homeActivity.findViewById(com.benny.openlauncher.R.id.ios_dock_background);
                if (iosBg != null) {
                    iosBg.setAlpha(0f);
                    iosBg.setScaleX(0.85f);
                    iosBg.setScaleY(0.85f);
                }
            }
            initDockInternal(true);
        }
    }

    private void initDockInternal(boolean animateInImmediately) {
        setAlpha(0f);
        setScaleX(0.85f);
        setScaleY(0.85f);
        if (Setup.appSettings().getDockIosStyle() && _homeActivity != null) {
            View iosBg = _homeActivity.findViewById(com.benny.openlauncher.R.id.ios_dock_background);
            if (iosBg != null) {
                iosBg.setAlpha(0f);
                iosBg.setScaleX(0.85f);
                iosBg.setScaleY(0.85f);
            }
        }
        
        boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        
        // CellContainer.setGridSize(x, y) already handles swapping if landscape is detected.
        // It expects x = columns (horizontal) and y = rows (vertical) in portrait.
        setGridSize(Setup.appSettings().getDockColumnCount(), Setup.appSettings().getDockRowCount());
        
        final int spanH = getCellSpanH();
        final int spanV = getCellSpanV();

        Setup.dataManager().getDockAsync(new DatabaseHelper.DataCallback<List<Item>>() {
            @Override
            public void onDataLoaded(List<Item> dockItems) {
                removeAllViews();
                for (Item item : dockItems) {
                    // If coordinates are out of bounds, it's likely they were saved for the other orientation
                    if (item._x >= spanH || item._y >= spanV) {
                        int temp = item._x;
                        item._x = item._y;
                        item._y = temp;
                    }

                    // Final safety check
                    if (item._x >= spanH) item._x = Math.max(0, spanH - item._spanX);
                    if (item._y >= spanV) item._y = Math.max(0, spanV - item._spanY);
                    
                    addItemToPage(item, 0);
                }
                // Fade in after items are loaded and layout is ready
                post(new Runnable() {
                    @Override
                    public void run() {
                        int delay = animateInImmediately ? 150 : 600;
                        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setStartDelay(delay).setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start();
                        if (Setup.appSettings().getDockIosStyle() && _homeActivity != null) {
                            View iosBg = _homeActivity.findViewById(com.benny.openlauncher.R.id.ios_dock_background);
                            if (iosBg != null) {
                                iosBg.setScaleX(0.85f);
                                iosBg.setScaleY(0.85f);
                                iosBg.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setStartDelay(delay).setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start();
                            }
                        }
                    }
                });
            }
        });

        // call onMeasure to set the height
        measure(getMeasuredWidth(), getMeasuredHeight());
    }

    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!Setup.appSettings().getGestureDockSwipeUp()) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                _startPosX = ev.getX();
                _startPosY = ev.getY();
                _interceptedSwipeUp = false; // Reset interception state
                break;
            case MotionEvent.ACTION_MOVE:
                // Check for upward swipe. If significant, intercept.
                if (!_interceptedSwipeUp && _startPosY - ev.getY() > Tool.dp2px(20)) { // Small threshold for interception
                    _interceptedSwipeUp = true;
                    // Returning true here means this ViewGroup intercepts the touch event
                    // and subsequent events for this gesture will be sent to onTouchEvent.
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                _interceptedSwipeUp = false; // Reset on completion or cancellation
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!Setup.appSettings().getGestureDockSwipeUp()) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                // If we've already intercepted, continue to consume move events
                if (_interceptedSwipeUp) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (_interceptedSwipeUp) { // Only handle if a swipe was intercepted
                    if (_startPosY - event.getY() > Tool.dp2px(150)) { // Original threshold for triggering action
                        Point point = new Point((int) event.getX(), (int) event.getY());
                        point = Tool.convertPoint(point, this, _homeActivity.getAppDrawerController());
                        if (Setup.appSettings().getGestureFeedback()) {
                            Tool.vibrate(this);
                        }
                        _homeActivity.openAppDrawer(this, point.x, point.y);
                        _interceptedSwipeUp = false; // Reset
                        return true; // Consume the event
                    }
                    _interceptedSwipeUp = false; // Reset if swipe wasn't strong enough
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                _interceptedSwipeUp = false; // Reset on cancellation
                break;
        }
        return super.onTouchEvent(event); // Let super handle other events
    }

    /*
    // Original detectSwipe method - commented out
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
    */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (Setup.appSettings().getDockIosStyle()) {
            boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            int width = ((r - l) - getPaddingLeft()) - getPaddingRight();
            int height = ((b - t) - getPaddingTop()) - getPaddingBottom();
            
            int settingsIconSize = Tool.dp2px(Setup.appSettings().getIconSize());
            int dockIconSize = (int) (settingsIconSize * 1.1f);
            float gap = settingsIconSize * 0.30f;

            if (landscape) {
                int rows = getCellSpanV();
                if (rows == 0) rows = 1;
                float bgHeight = rows * dockIconSize + (rows + 1) * gap;
                float bgTop = (height - bgHeight) / 2f;
                float startY = getPaddingTop() + bgTop + gap;

                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != View.GONE) {
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        if (child instanceof AppItemView) ((AppItemView) child).setIconSize(dockIconSize);
                        
                        child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), 
                                     MeasureSpec.makeMeasureSpec(dockIconSize, MeasureSpec.EXACTLY));

                        float top = startY + (lp.getY() * (dockIconSize + gap));
                        child.layout(0, (int)top, width, (int)(top + dockIconSize));
                    }
                }
            } else {
                int columns = getCellSpanH();
                if (columns == 0) columns = 1;
                float bgWidth = columns * dockIconSize + (columns + 1) * gap;
                float bgLeft = (width - bgWidth) / 2f;
                float startX = getPaddingLeft() + bgLeft + gap;

                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != View.GONE) {
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        if (child instanceof AppItemView) ((AppItemView) child).setIconSize(dockIconSize);
                        
                        child.measure(MeasureSpec.makeMeasureSpec(dockIconSize, MeasureSpec.EXACTLY), 
                                     MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

                        float left = startX + (lp.getX() * (dockIconSize + gap));
                        child.layout((int)left, 0, (int)(left + dockIconSize), height);
                    }
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
            boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            int iconSize = Setup.appSettings().getDockIconSize();
            
            if (landscape) {
                int width = Tool.dp2px((iconSize + 20) * getCellSpanH());
                if (Setup.appSettings().getDockIosStyle()) {
                    width = (int) (Tool.dp2px(Setup.appSettings().getIconSize()) * 1.7f) + Tool.dp2px(10);
                }
                getLayoutParams().width = width;
                setMeasuredDimension(width, View.getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
            } else {
                int height = Tool.dp2px((iconSize + 20) * getCellSpanV());
                if (Setup.appSettings().getDockShowLabel()) height += Tool.dp2px(20);
                
                if (Setup.appSettings().getDockIosStyle()) {
                    height = (int) (Tool.dp2px(Setup.appSettings().getIconSize()) * 1.7f) + Tool.dp2px(10);
                }
                
                getLayoutParams().height = height;
                setMeasuredDimension(View.getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);
            }
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
            //Setup.dataManager().deleteItem(item, true);
            return false;
        }
        item._location = ItemPosition.Dock;
        addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
        return true;
    }

    public boolean addItemToPoint(@NonNull Item item, int x, int y) {
        Point pos = new Point();
        touchPosToCoordinate(pos, x, y, item._spanX, item._spanY, false, false);
        
        if (pos.x == -1 || pos.y == -1) {
            return false;
        }

        // Check if occupied and if so, find next free slot
        if (checkOccupied(pos, item._spanX, item._spanY)) {
            // Find any free slot
            boolean found = false;
            Point testPoint = new Point(0, 0);
            for (int y2 = 0; y2 < getCellSpanV(); y2++) {
                for (int x2 = 0; x2 < getCellSpanH(); x2++) {
                    testPoint.set(x2, y2);
                    if (!checkOccupied(testPoint, item._spanX, item._spanY)) {
                        pos.set(x2, y2);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) return false;
        }

        item._location = ItemPosition.Dock;
        item._x = pos.x;
        item._y = pos.y;
        
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item, isDockShowLabel());
        if (itemView != null) {
            addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
            return true;
        }
        return false;
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

    @Override
    public void touchPosToCoordinate(@NonNull Point coordinate, int mX, int mY, int xSpan, int ySpan, boolean checkAvailability, boolean checkBoundary) {
        if (Setup.appSettings().getDockIosStyle()) {
            boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            int settingsIconSize = Tool.dp2px(Setup.appSettings().getIconSize());
            int dockIconSize = (int) (settingsIconSize * 1.1f);
            float gap = settingsIconSize * 0.30f;

            if (landscape) {
                int height = (getHeight() - getPaddingTop()) - getPaddingBottom();
                int rows = getCellSpanV();
                if (rows == 0) rows = 1;
                float bgHeight = rows * dockIconSize + (rows + 1) * gap;
                float bgTop = (height - bgHeight) / 2f;
                float startY = getPaddingTop() + bgTop + gap;
                float relativeY = mY - startY;
                int row = Math.round(relativeY / (dockIconSize + gap));
                coordinate.set(0, Math.max(0, Math.min(row, rows - 1)));
            } else {
                int width = (getWidth() - getPaddingLeft()) - getPaddingRight();
                int columns = getCellSpanH();
                if (columns == 0) columns = 1;
                float bgWidth = columns * dockIconSize + (columns + 1) * gap;
                float bgLeft = (width - bgWidth) / 2f;
                float startX = getPaddingLeft() + bgLeft + gap;
                float relativeX = mX - startX;
                int col = Math.round(relativeX / (dockIconSize + gap));
                coordinate.set(Math.max(0, Math.min(col, columns - 1)), 0);
            }
        } else {
            super.touchPosToCoordinate(coordinate, mX, mY, xSpan, ySpan, checkAvailability, checkBoundary);
        }
    }

    public void setHome(HomeActivity homeActivity) {
        _homeActivity = homeActivity;
    }

    private Boolean isDockShowLabel() {
        boolean b = Setup.appSettings().getDockShowLabel();
        Boolean ret = Boolean.valueOf(b);
        return ret;
    }
}
