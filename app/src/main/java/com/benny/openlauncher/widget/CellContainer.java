package com.benny.openlauncher.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.interfaces.DropTargetListener;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.Tool;

import java.util.ArrayList;
import java.util.List;

import in.championswimmer.sfg.lib.SimpleFingerGestures;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CellContainer extends ViewGroup implements DropTargetListener {
    private boolean _animateBackground;
    private final Paint _bgPaint = new Paint(1);
    private boolean _blockTouch;
    private Bitmap _cachedOutlineBitmap;
    private int _cellHeight;
    private int _cellSpanH;
    private int _cellSpanV;
    private int _cellWidth;
    private Rect[][] _cells;
    private Point _currentOutlineCoordinate = new Point(-1, -1);
    private Long _down = Long.valueOf(0);
    @Nullable
    private SimpleFingerGestures _gestures;
    private boolean _hideGrid = true;
    private final Paint _paint = new Paint(1);
    private boolean[][] _occupied;
    private final Paint _outlinePaint = new Paint(1);
    private PeekDirection _peekDirection;
    private Long _peekDownTime = Long.valueOf(-1);
    private Point _preCoordinate = new Point(-1, -1);
    private Point _startCoordinate = new Point();
    @NonNull
    private final Rect _tempRect = new Rect();

    public enum DragState {
        CurrentNotOccupied, OutOffRange, ItemViewNotFound, CurrentOccupied
    }

    public static final class LayoutParams extends android.view.ViewGroup.LayoutParams {
        private int _x;
        private int _xSpan = 1;
        private int _y;
        private int _ySpan = 1;
        private int _xL = -1;
        private int _yL = -1;

        public final int getX() {
            return _x;
        }

        public final void setX(int v) {
            _x = v;
        }

        public final int getY() {
            return _y;
        }

        public final void setY(int v) {
            _y = v;
        }

        public final int getXL() {
            return _xL;
        }

        public final void setXL(int v) {
            _xL = v;
        }

        public final int getYL() {
            return _yL;
        }

        public final void setYL(int v) {
            _yL = v;
        }

        public final int getXSpan() {
            return _xSpan;
        }

        public final void setXSpan(int v) {
            _xSpan = v;
        }

        public final int getYSpan() {
            return _ySpan;
        }

        public final void setYSpan(int v) {
            _ySpan = v;
        }

        public LayoutParams(int w, int h, int x, int y) {
            super(w, h);
            _x = x;
            _y = y;
        }

        public LayoutParams(int w, int h, int x, int y, int xSpan, int ySpan) {
            super(w, h);
            _x = x;
            _y = y;
            _xSpan = xSpan;
            _ySpan = ySpan;
        }

        public LayoutParams(int w, int h, int x, int y, int xL, int yL, int xSpan, int ySpan) {
            super(w, h);
            _x = x;
            _y = y;
            _xL = xL;
            _yL = yL;
            _xSpan = xSpan;
            _ySpan = ySpan;
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }
    }

    public enum PeekDirection {
        UP, LEFT, RIGHT, DOWN
    }

    public final int getCellWidth() {
        return _cellWidth;
    }

    public final int getCellHeight() {
        return _cellHeight;
    }

    public final int getCellSpanV() {
        return _cellSpanV;
    }

    public final int getCellSpanH() {
        return _cellSpanH;
    }

    public final void setBlockTouch(boolean v) {
        _blockTouch = v;
    }

    public final void setGestures(@Nullable SimpleFingerGestures v) {
        _gestures = v;
    }

    @NonNull
    public final List<View> getAllCells() {
        ArrayList<View> views = new ArrayList<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            views.add(getChildAt(i));
        }
        return views;
    }

    public CellContainer(Context context) {
        this(context, null);
    }

    public CellContainer(Context context, AttributeSet attr) {
        super(context, attr);
        _paint.setStyle(Style.STROKE);
        _paint.setStrokeWidth(2.0f);
        _paint.setStrokeJoin(Join.ROUND);
        _paint.setColor(Color.WHITE);
        _paint.setAlpha(0);
        _bgPaint.setStyle(Style.FILL);
        _bgPaint.setColor(Color.WHITE);
        _bgPaint.setAlpha(0);
        _outlinePaint.setColor(Color.WHITE);
        _outlinePaint.setAlpha(0);
        init();
    }

    public final void setGridSize(int x, int y) {
        boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        if (landscape) {
            _cellSpanV = x;
            _cellSpanH = y;
        } else {
            _cellSpanV = y;
            _cellSpanH = x;
        }

        _occupied = new boolean[_cellSpanH][_cellSpanV];
        for (int i = 0; i < _cellSpanH; i++) {
            for (int j = 0; j < _cellSpanV; j++) {
                _occupied[i][j] = false;
            }
        }

        requestLayout();
    }

    public final void setHideGrid(boolean hideGrid) {
        _hideGrid = hideGrid;
        invalidate();
    }

    public final void resetOccupiedSpace() {
        if (_cellSpanH > 0 && _cellSpanV > 0) {
            _occupied = new boolean[_cellSpanH][_cellSpanV];
        }
    }

    public void removeAllViews() {
        resetOccupiedSpace();
        super.removeAllViews();
    }

    public final void projectImageOutlineAt(@NonNull Point newCoordinate, @Nullable Bitmap bitmap) {
        _cachedOutlineBitmap = bitmap;
        if (!_currentOutlineCoordinate.equals(newCoordinate)) {
            _outlinePaint.setAlpha(0);
        }
        _currentOutlineCoordinate.set(newCoordinate.x, newCoordinate.y);
        invalidate();
    }

    private void drawCachedOutlineBitmap(Canvas canvas, Rect cell) {
        if (_cachedOutlineBitmap != null) {
            Bitmap bitmap = _cachedOutlineBitmap;
            // Align with top-left of the target cell to support multi-cell widgets correctly
            canvas.drawBitmap(bitmap, cell.left, cell.top, _outlinePaint);
        }
    }

    public final void clearCachedOutlineBitmap() {
        _outlinePaint.setAlpha(0);
        _cachedOutlineBitmap = null;
        invalidate();
    }

    @NonNull
    public final DragState peekItemAndSwap(@NonNull DragEvent event, @NonNull Point coordinate) {
        return peekItemAndSwap((int) event.getX(), (int) event.getY(), coordinate);
    }

    @NonNull
    public final DragState peekItemAndSwap(int x, int y, Point coordinate) {
        touchPosToCoordinate(coordinate, x, y, 1, 1, false, false);
        if (coordinate.x != -1 && coordinate.y != -1) {
            if (_startCoordinate == null) {
                _startCoordinate = coordinate;
            }
            if (!_preCoordinate.equals(coordinate)) {
                _peekDownTime = Long.valueOf(-1);
            }
            if (_peekDownTime != null && _peekDownTime == -1) {
                _peekDirection = getPeekDirectionFromCoordinate(_startCoordinate, coordinate);
                _peekDownTime = System.currentTimeMillis();
                _preCoordinate = coordinate;
            }
            if (_occupied[coordinate.x][coordinate.y]) {
                return DragState.CurrentOccupied;
            } else {
                return DragState.CurrentNotOccupied;
            }
        }
        return DragState.OutOffRange;
    }

    private PeekDirection getPeekDirectionFromCoordinate(Point from, Point to) {
        if (from.y - to.y > 0) {
            return PeekDirection.UP;
        }
        if (from.y - to.y < 0) {
            return PeekDirection.DOWN;
        }
        if (from.x - to.x > 0) {
            return PeekDirection.LEFT;
        }
        if (from.x - to.x < 0) {
            return PeekDirection.RIGHT;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (_blockTouch) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (com.benny.openlauncher.util.Logger.isEnabled()) {
                com.benny.openlauncher.util.Logger.log(this, "onTouchEvent: ACTION_DOWN at " + event.getX() + ", " + event.getY());
            }
            HomeActivity launcher = HomeActivity.Companion.getLauncher();
            if (launcher != null) {
                launcher.getDesktop().setLastDownY(event.getY());
            }
        }
        try {
            SimpleFingerGestures simpleFingerGestures = _gestures;
            simpleFingerGestures.onTouch(this, event);
        } catch (Exception e) {
            e.printStackTrace();
            return super.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        if (_blockTouch) return true;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
             if (com.benny.openlauncher.util.Logger.isEnabled()) {
                 com.benny.openlauncher.util.Logger.log(this, "onInterceptTouchEvent: ACTION_DOWN");
             }
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void init() {
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public final void animateBackgroundShow() {
        _animateBackground = true;
        invalidate();
    }

    public final void animateBackgroundHide() {
        _animateBackground = false;
        invalidate();
    }

    @Nullable
    public final Point findFreeSpace() {
        for (int y = 0; y < _occupied[0].length; y++) {
            for (int x = 0; x < _occupied.length; x++) {
                if (!_occupied[x][y]) {
                    return new Point(x, y);
                }
            }
        }

        return null;
    }

    @Nullable
    public final Point findFreeSpace(int spanX, int spanY) {
        for (int y = 0; y < _occupied[0].length; y++) {
            for (int x = 0; x < _occupied.length; x++) {
                if (!_occupied[x][y] && !checkOccupied(new Point(x, y), spanX, spanY)) {
                    return new Point(x, y);
                }
            }
        }

        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), _bgPaint);

        if (_cells == null)
            return;

        // Draw grid crosses
        if (!_hideGrid || _paint.getAlpha() > 0) {
            float s = Tool.dp2px(2);
            for (int x = 0; x <= _cellSpanH; x++) {
                for (int y = 0; y <= _cellSpanV; y++) {
                    float cx = x * _cellWidth + getPaddingLeft();
                    float cy = y * _cellHeight + getPaddingTop();
                    
                    canvas.drawLine(cx - s, cy, cx + s, cy, _paint);
                    canvas.drawLine(cx, cy - s, cx, cy + s, _paint);
                }
            }
        }

        //Animating alpha and drawing projected image
        HomeActivity homeActivity = HomeActivity.Companion.getLauncher();
        if (homeActivity != null && homeActivity.getItemOptionView().getDragExceedThreshold() && _currentOutlineCoordinate.x != -1 && _currentOutlineCoordinate.y != -1) {
            if (_outlinePaint.getAlpha() != 160)
                _outlinePaint.setAlpha(Math.min(_outlinePaint.getAlpha() + 20, 160));
            drawCachedOutlineBitmap(canvas, _cells[_currentOutlineCoordinate.x][_currentOutlineCoordinate.y]);

            if (_outlinePaint.getAlpha() <= 160)
                invalidate();
        }

        //Animating alpha
        if (_hideGrid && _paint.getAlpha() != 0) {
            _paint.setAlpha(Math.max(_paint.getAlpha() - 20, 0));
            invalidate();
        } else if (!_hideGrid && _paint.getAlpha() != 255) {
            _paint.setAlpha(Math.min(_paint.getAlpha() + 20, 255));
            invalidate();
        }

        //Animating alpha
        if (!_animateBackground && _bgPaint.getAlpha() != 0) {
            _bgPaint.setAlpha(Math.max(_bgPaint.getAlpha() - 10, 0));
            invalidate();
        } else if (_animateBackground && _bgPaint.getAlpha() != 100) {
            _bgPaint.setAlpha(Math.min(_bgPaint.getAlpha() + 10, 100));
            invalidate();
        }
    }

    public void addView(View view) {
        LayoutParams lp = (CellContainer.LayoutParams) view.getLayoutParams();
        setOccupied(true, lp);
        super.addView(view);
    }

    public void removeView(View view) {
        LayoutParams lp = (CellContainer.LayoutParams) view.getLayoutParams();
        setOccupied(false, lp);
        super.removeView(view);
    }

    public final void addViewToGrid(@NonNull View view, int x, int y, int xSpan, int ySpan) {
        view.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT, x, y, xSpan, ySpan));
        addView(view);
    }

    public final void addViewToGrid(@NonNull View view) {
        addView(view);
    }

    public final void setOccupied(boolean b, @NonNull LayoutParams lp) {
        com.benny.openlauncher.util.Logger.log(this, "setOccupied: " + b + " at (" + lp.getX() + "," + lp.getY() + ") span (" + lp.getXSpan() + "," + lp.getYSpan() + ")");
        int xSpan = lp.getX() + lp.getXSpan();
        int ySpan = lp.getY() + lp.getYSpan();

        for (int x = lp.getX(); x < xSpan; x++) {
            if (x < 0 || x >= _cellSpanH) continue;
            for (int y = lp.getY(); y < ySpan; y++) {
                if (y < 0 || y >= _cellSpanV) continue;
                _occupied[x][y] = b;
            }
        }
    }

    public final boolean checkOccupied(Point start, int spanX, int spanY) {
        int i = start.x + spanX;
        if (i <= _occupied.length) {
            i = start.y + spanY;
            if (i <= _occupied[0].length) {
                int i2 = start.y + spanY;
                for (i = start.y; i < i2; i++) {
                    int i3 = start.x + spanX;
                    for (int x = start.x; x < i3; x++) {
                        if (_occupied[x][i]) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    public final View coordinateToChildView(Point pos) {
        if (pos == null) {
            return null;
        }
        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            if (pos.x >= lp._x && pos.y >= lp._y && pos.x < lp._x + lp._xSpan && pos.y < lp._y + lp._ySpan) {
                return getChildAt(i);
            }
        }
        return null;
    }

    public final LayoutParams coordinateToLayoutParams(int mX, int mY, int xSpan, int ySpan) {
        return coordinateToLayoutParams(mX, mY, xSpan, ySpan, true);
    }

    public final LayoutParams coordinateToLayoutParams(int mX, int mY, int xSpan, int ySpan, boolean checkAvailability) {
        Point pos = new Point();
        touchPosToCoordinate(pos, mX, mY, xSpan, ySpan, checkAvailability);
        return !pos.equals(new Point(-1, -1)) ? new LayoutParams(WRAP_CONTENT, WRAP_CONTENT, pos.x, pos.y, xSpan, ySpan) : null;
    }

    public void touchPosToCoordinate(@NonNull Point coordinate, int mX, int mY, int xSpan, int ySpan, boolean checkAvailability) {
        touchPosToCoordinate(coordinate, mX, mY, xSpan, ySpan, checkAvailability, false);
    }

    public void touchPosToCoordinate(Point coordinate, int mX, int mY, int xSpan, int ySpan, boolean checkAvailability, boolean checkBoundary) {
        for (int i = 0; i < _cells.length; i++) {
            for (int j = 0; j < _cells[i].length; j++) {
                Rect cell = _cells[i][j];
                if (mY >= cell.top && mY <= cell.bottom && mX >= cell.left && mX <= cell.right) {
                    int x = i;
                    int y = j;

                    if (checkAvailability) {
                        int dx = x + xSpan - 1;
                        int dy = y + ySpan - 1;

                        if (dx >= _cellSpanH) {
                            x = _cellSpanH - xSpan;
                        }
                        if (dy >= _cellSpanV) {
                            y = _cellSpanV - ySpan;
                        }
                        
                        if (x < 0 || y < 0) {
                            coordinate.set(-1, -1);
                            return;
                        }

                        for (int x2 = x; x2 < x + xSpan; x2++) {
                            for (int y2 = y; y2 < y + ySpan; y2++) {
                                if (_occupied[x2][y2]) {
                                    coordinate.set(-1, -1);
                                    return;
                                }
                            }
                        }
                    }
                    if (checkBoundary) {
                        Rect offsetCell = new Rect(cell);
                        int dp2 = Tool.dp2px(6);
                        offsetCell.inset(dp2, dp2);
                        if (mY >= offsetCell.top && mY <= offsetCell.bottom && mX >= offsetCell.left && mX <= offsetCell.right) {
                            coordinate.set(-1, -1);
                            return;
                        }
                    }
                    coordinate.set(x, y);
                    return;
                }
            }
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean onStart(DragAction.Action action, PointF location, boolean isInside) {
        return true;
    }

    @Override
    public void onStartDrag(DragAction.Action action, PointF location) {
    }

    @Override
    public void onDrop(DragAction.Action action, PointF location, Item item) {
        Point pos = new Point();
        touchPosToCoordinate(pos, (int) location.x, (int) location.y, item._spanX, item._spanY, false);
        if (pos.x != -1 && pos.y != -1) {
            boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            if (landscape) {
                item.setXL(pos.x);
                item.setYL(pos.y);
            } else {
                item.setX(pos.x);
                item.setY(pos.y);
            }
            
            HomeActivity launcher = HomeActivity.Companion.getLauncher();
            if (launcher != null) {
                int page = launcher.getDesktop().getCurrentPageIndex();
                Setup.dataManager().saveItem(item, page, Definitions.ItemPosition.Desktop);
                launcher.getDesktop().addItemToPage(item, page);
            }
        }
    }

    @Override
    public void onMove(DragAction.Action action, PointF location) {
    }

    @Override
    public void onEnter(DragAction.Action action, PointF location) {
    }

    @Override
    public void onExit(DragAction.Action action, PointF location) {
    }

    @Override
    public void onEnd() {
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = ((r - l) - getPaddingLeft()) - getPaddingRight();
        int height = ((b - t) - getPaddingTop()) - getPaddingBottom();
        if (_cellSpanH == 0) {
            _cellSpanH = 1;
        }
        if (_cellSpanV == 0) {
            _cellSpanV = 1;
        }
        _cellWidth = width / _cellSpanH;
        _cellHeight = height / _cellSpanV;
        initCellInfo(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
        int count = getChildCount();
        if (_cells != null) {
            int i = 0;
            while (i < count) {
                View child = getChildAt(i);
                if (child.getVisibility() != View.GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    
                    boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
                    int x = lp.getX();
                    int y = lp.getY();
                    int xSpan = lp.getXSpan();
                    int ySpan = lp.getYSpan();

                    if (landscape) {
                        if (lp.getXL() != -1 && lp.getYL() != -1) {
                            x = lp.getXL();
                            y = lp.getYL();
                        } else {
                            // Default: Swap coordinates and spans
                            x = lp.getY();
                            y = lp.getX();
                            xSpan = lp.getYSpan();
                            ySpan = lp.getXSpan();
                        }
                    }

                    // Ensure coordinates are within bounds
                    x = Math.max(0, Math.min(x, _cellSpanH - 1));
                    y = Math.max(0, Math.min(y, _cellSpanV - 1));
                    xSpan = Math.min(xSpan, _cellSpanH - x);
                    ySpan = Math.min(ySpan, _cellSpanV - y);

                    child.measure(MeasureSpec.makeMeasureSpec(xSpan * _cellWidth, View.MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ySpan * _cellHeight, View.MeasureSpec.EXACTLY));
                    Rect upRect = _cells[x][y];
                    Rect downRect = _cells[Math.min(x + xSpan - 1, _cellSpanH - 1)][Math.min(y + ySpan - 1, _cellSpanV - 1)];
                    
                    child.layout(upRect.left, upRect.top, downRect.right, downRect.bottom);
                }

                i++;
            }
        }
    }

    private void initCellInfo(int l, int t, int r, int b) {
        _cells = new Rect[_cellSpanH][_cellSpanV];

        for (int i = 0; i < _cellSpanH; i++) {
            int curLeft = l + (i * _cellWidth);
            int curRight = curLeft + _cellWidth;

            for (int j = 0; j < _cellSpanV; j++) {
                int curTop = t + (j * _cellHeight);
                int curBottom = curTop + _cellHeight;

                Rect rect = new Rect(curLeft, curTop, curRight, curBottom);
                _cells[i][j] = rect;
            }
        }
    }
}
