package com.benny.openlauncher.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.activity.homeparts.HpItemOption;
import com.benny.openlauncher.interfaces.DropTargetListener;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.DragAction.Action;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.AbstractPopupIconLabelItem;
import com.benny.openlauncher.viewutil.PopupDynamicIconLabelItem;
import com.benny.openlauncher.viewutil.PopupIconLabelItem;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator;

public final class ItemOptionView extends FrameLayout {
    private final float DRAG_THRESHOLD;
    @Nullable
    private Action _dragAction;
    private boolean _dragExceedThreshold;
    @Nullable
    private Item _dragItem;
    @NonNull
    private PointF _dragLocation;
    private PointF _dragLocationConverted;
    private PointF _dragLocationStart;
    @Nullable
    private View _dragView;
    private boolean _dragging;
    private float _folderPreviewScale;
    private float _overlayIconScale;
    private final RecyclerView _overlayPopup;
    private final FastItemAdapter<AbstractPopupIconLabelItem> _overlayPopupAdapter;
    private boolean _overlayPopupShowing;
    private final OverlayView _overlayView;
    private final Paint _paint;
    private PointF _previewLocation;
    private final LinkedHashMap<DropTargetListener, DragFlag> _registeredDropTargetEntries;
    private boolean _showFolderPreview;
    private final SlideInLeftAnimator _slideInLeftAnimator;
    private final SlideInRightAnimator _slideInRightAnimator;
    private final int[] _tempArrayOfInt2;

    private final int uninstallItemIdentifier = 83;
    private final int infoItemIdentifier = 84;
    private final int editItemIdentifier = 85;
    private final int removeItemIdentifier = 86;
    private final int resizeItemIdentifier = 87;
    private final int startShortcutItemIdentifier = 88;
    private final int addToDesktopItemIdentifier = 89;
    private final int forceStopItemIdentifier = 90;

    private PopupIconLabelItem uninstallItem = new PopupIconLabelItem(R.string.uninstall, R.drawable.ic_delete).withIdentifier(uninstallItemIdentifier);
    private PopupIconLabelItem infoItem = new PopupIconLabelItem(R.string.info, R.drawable.ic_info).withIdentifier(infoItemIdentifier);
    private PopupIconLabelItem editItem = new PopupIconLabelItem(R.string.edit, R.drawable.ic_edit).withIdentifier(editItemIdentifier);
    private PopupIconLabelItem removeItem = new PopupIconLabelItem(R.string.remove, R.drawable.ic_close).withIdentifier(removeItemIdentifier);
    private PopupIconLabelItem resizeItem = new PopupIconLabelItem(R.string.resize, R.drawable.ic_resize).withIdentifier(resizeItemIdentifier);
    private PopupIconLabelItem addToDesktopItem = new PopupIconLabelItem(R.string.add_to_desktop, R.drawable.ic_star).withIdentifier(addToDesktopItemIdentifier);
    private PopupIconLabelItem forceStopItem = new PopupIconLabelItem(R.string.force_stop, R.drawable.ic_close).withIdentifier(forceStopItemIdentifier);


    public static final class DragFlag {
        private boolean _previousOutside = true;
        private boolean _shouldIgnore;

        public final boolean getPreviousOutside() {
            return _previousOutside;
        }

        public final void setPreviousOutside(boolean v) {
            _previousOutside = v;
        }

        public final boolean getShouldIgnore() {
            return _shouldIgnore;
        }

        public final void setShouldIgnore(boolean v) {
            _shouldIgnore = v;
        }
    }

    @SuppressLint({"ResourceType"})
    public final class OverlayView extends View {

        public OverlayView() {
            super(ItemOptionView.this.getContext());
            setWillNotDraw(false);
        }

        public boolean onTouchEvent(@Nullable MotionEvent event) {
            if (event == null || event.getActionMasked() != 0 || ItemOptionView.this.getDragging() || !ItemOptionView.this._overlayPopupShowing) {
                return super.onTouchEvent(event);
            }
            ItemOptionView.this.collapse();
            return true;
        }

        protected void onDraw(@Nullable Canvas canvas) {
            super.onDraw(canvas);
            if (canvas == null || DragHandler._cachedDragBitmap == null || _dragLocation.equals(-1f, -1f))
                return;

            float x = _dragLocation.x;
            float y = _dragLocation.y;

            if (_dragging) {
                canvas.save();
                _overlayIconScale = Tool.clampFloat(_overlayIconScale + 0.05f, 1f, 1.1f);
                float itemWidth = DragHandler._cachedDragBitmap.getWidth();
                float itemHeight = DragHandler._cachedDragBitmap.getHeight();
                float drawX = x - HomeActivity._itemTouchX;
                float drawY = y - HomeActivity._itemTouchY;
                
                // Scale around the finger position (x, y) which is the current drag location
                canvas.scale(_overlayIconScale, _overlayIconScale, x, y);
                canvas.drawBitmap(DragHandler._cachedDragBitmap, drawX, drawY, _paint);
                canvas.restore();
            }

            if (_dragging)
                invalidate();
        }
    }

    public ItemOptionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.DRAG_THRESHOLD = 20.0f;
        _paint = new Paint(1);
        _registeredDropTargetEntries = new LinkedHashMap<>();
        _tempArrayOfInt2 = new int[2];
        _dragLocation = new PointF();
        _dragLocationStart = new PointF();
        _dragLocationConverted = new PointF();
        _overlayIconScale = 1.0f;
        _overlayPopupAdapter = new FastItemAdapter<>();
        _previewLocation = new PointF();
        _slideInLeftAnimator = new SlideInLeftAnimator(new AccelerateDecelerateInterpolator());
        _slideInRightAnimator = new SlideInRightAnimator(new AccelerateDecelerateInterpolator());
        _paint.setFilterBitmap(true);
        _paint.setColor(Setup.appSettings().getDesktopFolderColor());
        _overlayView = new OverlayView();
        _overlayPopup = new RecyclerView(context);
        _overlayPopup.setVisibility(View.INVISIBLE);
        _overlayPopup.setAlpha(0);
        _overlayPopup.setOverScrollMode(2);
        _overlayPopup.setLayoutManager(new LinearLayoutManager(context, 1, false));
        _overlayPopup.setItemAnimator(null);
        _overlayPopup.setAdapter(_overlayPopupAdapter);
        addView(_overlayView, new LayoutParams(-1, -1));
        addView(_overlayPopup, new LayoutParams(-2, -2));
        setWillNotDraw(false);
    }

    public final boolean getDragging() {
        return _dragging;
    }

    public final PointF getDragLocation() {
        return _dragLocation;
    }

    public final Action getDragAction() {
        return _dragAction;
    }

    public final boolean getDragExceedThreshold() {
        return _dragExceedThreshold;
    }

    @Nullable
    public final Item getDragItem() {
        return _dragItem;
    }

    public final void showFolderPreviewAt(@NonNull View fromView, float x, float y) {
        if (!_showFolderPreview) {
            _showFolderPreview = true;
            convertPoint(fromView, this, x, y);
            _folderPreviewScale = 0.0f;
            invalidate();
        }
    }

    public final void convertPoint(@NonNull View fromView, @NonNull View toView, float x, float y) {
        int[] fromCoordinate = new int[2];
        int[] toCoordinate = new int[2];
        fromView.getLocationOnScreen(fromCoordinate);
        toView.getLocationOnScreen(toCoordinate);
        _previewLocation.set(fromCoordinate[0] - toCoordinate[0] + x, fromCoordinate[1] - toCoordinate[1] + y);
    }

    public final void cancelFolderPreview() {
        _showFolderPreview = false;
        _previewLocation.set(-1.0f, -1.0f);
        invalidate();
    }

    protected void onDraw(@Nullable Canvas canvas) {
        super.onDraw(canvas);
        if (canvas != null && _showFolderPreview && !_previewLocation.equals(-1.0f, -1.0f)) {
            _folderPreviewScale += 0.08f;
            _folderPreviewScale = Tool.clampFloat(_folderPreviewScale, 0.5f, 1.0f);
            canvas.drawCircle(_previewLocation.x, _previewLocation.y, ((float) Tool.dp2px((Setup.appSettings().getDesktopIconSize() / 2) + 10)) * _folderPreviewScale, _paint);
        }
        if (_showFolderPreview) {
            invalidate();
        }
    }

    public void onViewAdded(@Nullable View child) {
        super.onViewAdded(child);
        _overlayView.bringToFront();
        _overlayPopup.bringToFront();
    }

    public final void showPopupMenuForItem(float x, float y, @NonNull List<AbstractPopupIconLabelItem> popupItem, com.mikepenz.fastadapter.listeners.OnClickListener<AbstractPopupIconLabelItem> listener) {
        if (!_overlayPopupShowing) {
            _overlayPopupShowing = true;
            _overlayPopup.setVisibility(View.VISIBLE);
            _overlayPopup.setTranslationX(x);
            _overlayPopup.setTranslationY(y);
            _overlayPopup.setAlpha(1.0f);
            _overlayPopupAdapter.add(popupItem);
            _overlayPopupAdapter.withOnClickListener(listener);
        }
    }

    public final void setPopupMenuShowDirection(boolean left) {
        if (left) {
            _overlayPopup.setItemAnimator(_slideInLeftAnimator);
        } else {
            _overlayPopup.setItemAnimator(_slideInRightAnimator);
        }
    }

    public final void collapse() {
        if (_overlayPopupShowing) {
            _overlayPopupShowing = false;
            _overlayPopup.animate().alpha(0.0f).withEndAction(new Runnable() {
                @Override
                public void run() {
                    _overlayPopup.setVisibility(View.INVISIBLE);
                    _overlayPopupAdapter.clear();
                }
            });
            if (!_dragging) {
                _dragView = null;
                _dragItem = null;
                _dragAction = null;
            }
        }
    }

    public final void startDragNDropOverlay(@NonNull View view, @NonNull Item item, @NonNull Action action) {
        if (com.benny.openlauncher.util.Logger.isEnabled()) {
            com.benny.openlauncher.util.Logger.log(this, "startDragNDropOverlay: " + item.getLabel() + ", action: " + action);
        }
        _dragging = true;
        _dragExceedThreshold = false;
        _overlayIconScale = 1.0f;
        _dragView = view;
        _dragItem = item;
        _dragAction = action;
        _dragLocationStart.set(_dragLocation);

        // Calculate touch offset within the item
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        int[] thisLoc = new int[2];
        getLocationOnScreen(thisLoc);
        HomeActivity._itemTouchX = _dragLocation.x - (loc[0] - thisLoc[0]);
        HomeActivity._itemTouchY = _dragLocation.y - (loc[1] - thisLoc[1]);

        for (Entry dropTarget : _registeredDropTargetEntries.entrySet()) {
            convertPoint(((DropTargetListener) dropTarget.getKey()).getView());
            DragFlag dragFlag = (DragFlag) dropTarget.getValue();
            DropTargetListener dropTargetListener = (DropTargetListener) dropTarget.getKey();
            dragFlag.setShouldIgnore(!dropTargetListener.onStart(_dragAction, _dragLocationConverted, isViewContains(((DropTargetListener) dropTarget.getKey()).getView(), (int) _dragLocation.x, (int) _dragLocation.y)));
        }

        _overlayView.invalidate();
    }

    protected void onDetachedFromWindow() {
        if (com.benny.openlauncher.util.Logger.isEnabled()) {
            com.benny.openlauncher.util.Logger.log(this, "onDetachedFromWindow");
        }
        cancelAllDragNDrop();
        super.onDetachedFromWindow();
    }

    public final void cancelAllDragNDrop() {
        if (com.benny.openlauncher.util.Logger.isEnabled()) {
            com.benny.openlauncher.util.Logger.log(this, "cancelAllDragNDrop, dragging: " + _dragging);
        }
        _dragging = false;
        if (!_overlayPopupShowing) {
            _dragView = null;
            _dragItem = null;
            _dragAction = null;
        }
        for (Entry dropTarget : _registeredDropTargetEntries.entrySet()) {
            ((DropTargetListener) dropTarget.getKey()).onEnd();
        }
    }

    public final void registerDropTarget(@NonNull DropTargetListener targetListener) {
        _registeredDropTargetEntries.put(targetListener, new DragFlag());
    }

    public boolean onInterceptTouchEvent(@Nullable MotionEvent event) {
        if (event != null && event.getActionMasked() == 1 && _dragging) {
            handleDragFinished();
        }
        if (_dragging) {
            return true;
        }
        if (event != null) {
            _dragLocation.set(event.getX(), event.getY());
        }
        return super.onInterceptTouchEvent(event);
    }

    public boolean onTouchEvent(@Nullable MotionEvent event) {
        if (event != null) {
            if (_dragging) {
                _dragLocation.set(event.getX(), event.getY());
                switch (event.getActionMasked()) {
                    case 1:
                        handleDragFinished();
                        break;
                    case 2:
                        handleMovement();
                        break;
                    default:
                        break;
                }
                if (_dragging) {
                    return true;
                }
                return super.onTouchEvent(event);
            }
        }
        return super.onTouchEvent(event);
    }

    public void showItemPopup(final HomeActivity homeActivity) {
        ArrayList<AbstractPopupIconLabelItem> itemList = new ArrayList<>();
        switch (getDragItem().getType()) {
            case APP:
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1 && getDragItem().getShortcutInfo() != null) {
                    for (ShortcutInfo shortcutInfo : getDragItem().getShortcutInfo()) {
                        itemList.add(getAppShortcutItem(shortcutInfo));
                    }
                }
                if (!getDragAction().equals(Action.DRAWER)) {
                    itemList.add(editItem);
                    itemList.add(removeItem);
                } else {
                    itemList.add(addToDesktopItem);
                }
                itemList.add(uninstallItem);
                itemList.add(infoItem);
                if (getDragAction().equals(Action.DRAWER)) {
                    itemList.add(forceStopItem);
                }
                break;
            case SHORTCUT:
                if (!getDragAction().equals(Action.DRAWER)) {
                    itemList.add(editItem);
                    itemList.add(removeItem);
                }
                itemList.add(infoItem);
                break;
            case ACTION:
            case GROUP:
                itemList.add(editItem);
                itemList.add(removeItem);
                break;
            case WIDGET:
                itemList.add(removeItem);
                itemList.add(resizeItem);
                break;
        }

        float x = getDragLocation().x - HomeActivity._itemTouchX + Tool.dp2px(10);
        float y = getDragLocation().y - HomeActivity._itemTouchY - Tool.dp2px((46 * itemList.size()));

        if ((x + Tool.dp2px(200)) > getWidth()) {
            setPopupMenuShowDirection(false);
            x = getDragLocation().x - HomeActivity._itemTouchX + homeActivity.getDesktop().getCurrentPage().getCellWidth() - Tool.dp2px(200) - Tool.dp2px(10);
        } else {
            setPopupMenuShowDirection(true);
        }

        if (y < 0) {
            y = getDragLocation().y - HomeActivity._itemTouchY + homeActivity.getDesktop().getCurrentPage().getCellHeight() + Tool.dp2px(4);
        } else {
            y -= Tool.dp2px(4);
        }

        showPopupMenuForItem(x, y, itemList, new com.mikepenz.fastadapter.listeners.OnClickListener<AbstractPopupIconLabelItem>() {
            @Override
            public boolean onClick(View v, IAdapter<AbstractPopupIconLabelItem> adapter, AbstractPopupIconLabelItem item, int position) {
                Item dragItem = getDragItem();
                if (dragItem != null) {
                    HpItemOption itemOption = new HpItemOption(homeActivity);
                    switch ((int) item.getIdentifier()) {
                        case uninstallItemIdentifier:
                            itemOption.onUninstallItem(dragItem);
                            break;
                        case editItemIdentifier:
                            itemOption.onEditItem(dragItem);
                            break;
                        case removeItemIdentifier:
                            itemOption.onRemoveItem(dragItem);
                            break;
                        case infoItemIdentifier:
                            itemOption.onInfoItem(dragItem);
                            break;
                        case resizeItemIdentifier:
                            itemOption.onResizeItem(dragItem);
                            break;
                        case addToDesktopItemIdentifier:
                            itemOption.onAddToDesktop(dragItem);
                            break;
                        case forceStopItemIdentifier:
                            itemOption.onForceStop(dragItem);
                            break;
                        case startShortcutItemIdentifier:
                            itemOption.onStartShortcutItem(dragItem, position);
                            break;
                    }
                }
                collapse();
                return true;
            }
        });
    }

    public void showItemPopupForLockedDesktop(Item item, final HomeActivity homeActivity) {
        ArrayList<AbstractPopupIconLabelItem> itemList = new ArrayList<>();
        switch (item.getType()) {
            case APP:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item.getShortcutInfo() != null) {
                    for (ShortcutInfo shortcutInfo : item.getShortcutInfo()) {
                        itemList.add(getAppShortcutItem(shortcutInfo));
                    }
                }
                break;
        }

        float x = getDragLocation().x - HomeActivity._itemTouchX + Tool.dp2px(10);
        float y = getDragLocation().y - HomeActivity._itemTouchY - Tool.dp2px((46 * itemList.size()));

        if ((x + Tool.dp2px(200)) > getWidth()) {
            setPopupMenuShowDirection(false);
            x = getDragLocation().x - HomeActivity._itemTouchX + homeActivity.getDesktop().getCurrentPage().getCellWidth() - Tool.dp2px(200) - Tool.dp2px(10);
        } else {
            setPopupMenuShowDirection(true);
        }

        if (y < 0) {
            y = getDragLocation().y - HomeActivity._itemTouchY + homeActivity.getDesktop().getCurrentPage().getCellHeight() + Tool.dp2px(4);
        } else {
            y -= Tool.dp2px(4);
        }

        showPopupMenuForItem(x, y, itemList, (v, adapter, item1, position) -> {
            HpItemOption itemOption = new HpItemOption(homeActivity);
            switch ((int) item1.getIdentifier()) {
                case uninstallItemIdentifier:
                    itemOption.onUninstallItem(item);
                    break;
                case infoItemIdentifier:
                    itemOption.onInfoItem(item);
                    break;
                case startShortcutItemIdentifier:
                    itemOption.onStartShortcutItem(item, position);
                    break;
            }
            collapse();
            return true;
        });
    }

    private PopupDynamicIconLabelItem getAppShortcutItem(@NonNull ShortcutInfo shortcutInfo) {
        PopupDynamicIconLabelItem popupDynamicIconLabelItem = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            LauncherApps launcherApps = (LauncherApps) getContext().getSystemService(Context.LAUNCHER_APPS_SERVICE);
            popupDynamicIconLabelItem = new PopupDynamicIconLabelItem(shortcutInfo.getShortLabel(), launcherApps.getShortcutIconDrawable(shortcutInfo, getContext().getResources().getDisplayMetrics().densityDpi)).withIdentifier(startShortcutItemIdentifier);
        }
        return popupDynamicIconLabelItem;
    }

    private final void handleMovement() {
        if (!_dragExceedThreshold && (Math.abs(_dragLocationStart.x - _dragLocation.x) > this.DRAG_THRESHOLD || Math.abs(_dragLocationStart.y - _dragLocation.y) > this.DRAG_THRESHOLD)) {
            _dragExceedThreshold = true;
            com.benny.openlauncher.util.Logger.log(this, "handleMovement: drag threshold exceeded");
            
            HomeActivity launcher = HomeActivity.Companion.getLauncher();
            if (launcher != null) {
                ((Desktop.DesktopAdapter) launcher.getDesktop().getAdapter()).setDragging(true);
            }

            for (Entry<DropTargetListener, DragFlag> dropTarget : _registeredDropTargetEntries.entrySet()) {
                if (!dropTarget.getValue().getShouldIgnore()) {
                    convertPoint(dropTarget.getKey().getView());
                    dropTarget.getKey().onStartDrag(_dragAction, _dragLocationConverted);
                }
            }
        }
        if (_dragExceedThreshold) {
            collapse();
        }

        DropTargetListener topTarget = null;
        for (Entry<DropTargetListener, DragFlag> dropTarget2 : _registeredDropTargetEntries.entrySet()) {
            if (!dropTarget2.getValue().getShouldIgnore()) {
                View targetView = dropTarget2.getKey().getView();
                if (isViewContains(targetView, (int) _dragLocation.x, (int) _dragLocation.y)) {
                    // Priority: If we already have a target, but this one is "smaller" or is a handle, prefer it.
                    // Handles should always win over the Desktop.
                    if (topTarget == null) {
                        topTarget = dropTarget2.getKey();
                    } else {
                        int id = targetView.getId();
                        if (id == R.id.leftDragHandle || id == R.id.rightDragHandle) {
                            topTarget = dropTarget2.getKey();
                        }
                    }
                }
            }
        }

        for (Entry<DropTargetListener, DragFlag> dropTarget2 : _registeredDropTargetEntries.entrySet()) {
            DropTargetListener dropTargetListener = dropTarget2.getKey();
            DragFlag dragFlag = dropTarget2.getValue();
            if (dragFlag.getShouldIgnore()) continue;

            if (dropTargetListener == topTarget) {
                convertPoint(dropTargetListener.getView());
                dropTargetListener.onMove(_dragAction, _dragLocationConverted);
                if (dragFlag.getPreviousOutside()) {
                    com.benny.openlauncher.util.Logger.log(this, "handleMovement: entering target " + dropTargetListener.getClass().getSimpleName());
                    dragFlag.setPreviousOutside(false);
                    dropTargetListener.onEnter(_dragAction, _dragLocationConverted);
                }
            } else {
                if (!dragFlag.getPreviousOutside()) {
                    com.benny.openlauncher.util.Logger.log(this, "handleMovement: exiting target " + dropTargetListener.getClass().getSimpleName());
                    dragFlag.setPreviousOutside(true);
                    convertPoint(dropTargetListener.getView());
                    dropTargetListener.onExit(_dragAction, _dragLocationConverted);
                }
            }
        }
    }

    private void handleDragFinished() {
        com.benny.openlauncher.util.Logger.log(this, "handleDragFinished, dragging: " + _dragging + ", x: " + _dragLocation.x + ", y: " + _dragLocation.y);
        _dragging = false;
        DropTargetListener topTarget = null;
        for (Entry<DropTargetListener, DragFlag> dropTarget : _registeredDropTargetEntries.entrySet()) {
            if (!dropTarget.getValue().getShouldIgnore()) {
                View targetView = dropTarget.getKey().getView();
                if (isViewContains(targetView, (int) _dragLocation.x, (int) _dragLocation.y)) {
                    if (topTarget == null) {
                        topTarget = dropTarget.getKey();
                    } else {
                        int id = targetView.getId();
                        if (id == R.id.leftDragHandle || id == R.id.rightDragHandle) {
                            topTarget = dropTarget.getKey();
                        }
                    }
                }
            }
        }

        if (topTarget != null) {
            com.benny.openlauncher.util.Logger.log(this, "handleDragFinished: dropping on target: " + topTarget.getClass().getSimpleName());
            convertPoint(topTarget.getView());
            topTarget.onDrop(_dragAction, _dragLocationConverted, _dragItem);
        } else {
            com.benny.openlauncher.util.Logger.log(this, "handleDragFinished: no target found, reverting item");
            // Get the Desktop instance and call revertLastItem()
            HomeActivity launcher = HomeActivity.Companion.getLauncher();
            if (launcher != null) {
                launcher.getDesktop().revertLastItem();
                launcher.getDock().revertLastItem();
            }
        }

        for (Entry<DropTargetListener, DragFlag> dropTarget2 : _registeredDropTargetEntries.entrySet()) {
            dropTarget2.getKey().onEnd();
        }
        
        HomeActivity launcher2 = HomeActivity.Companion.getLauncher();
        if (launcher2 != null) {
            ((Desktop.DesktopAdapter) launcher2.getDesktop().getAdapter()).setDragging(false);
        }

        cancelFolderPreview();
    }

    public void convertPoint(@NonNull View toView) {
        int[] fromCoordinate = new int[2];
        int[] toCoordinate = new int[2];
        getLocationOnScreen(fromCoordinate);
        toView.getLocationOnScreen(toCoordinate);
        _dragLocationConverted.set(((float) (fromCoordinate[0] - toCoordinate[0])) + _dragLocation.x, ((float) (fromCoordinate[1] - toCoordinate[1])) + _dragLocation.y);
    }

    private boolean isViewContains(View view, int rx, int ry) {
        view.getLocationOnScreen(_tempArrayOfInt2);
        int x = _tempArrayOfInt2[0];
        int y = _tempArrayOfInt2[1];
        
        int[] thisLocation = new int[2];
        getLocationOnScreen(thisLocation);
        
        x -= thisLocation[0];
        y -= thisLocation[1];
        
        int w = view.getWidth();
        int h = view.getHeight();

        if (_dragging) {
             com.benny.openlauncher.util.Logger.log(this, "Check: touch(" + rx + "," + ry + ") view(" + view.getClass().getSimpleName() + ", id:" + view.getId() + ") rect(" + x + "," + y + "," + (x+w) + "," + (y+h) + ")");
        }

        if (rx < x || rx > x + w || ry < y || ry > y + h) {
            return false;
        }

        return true;
    }
}
