package com.benny.openlauncher.widget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.DatabaseHelper;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.model.Item.Type;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.Definitions.ItemPosition;
import com.benny.openlauncher.util.Definitions.ItemState;
import com.benny.openlauncher.util.DragAction.Action;
import com.benny.openlauncher.util.DragHandler;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DesktopCallback;
import com.benny.openlauncher.viewutil.DesktopGestureListener;
import com.benny.openlauncher.viewutil.ItemViewFactory;
import com.benny.openlauncher.widget.CellContainer.DragState;

import java.util.ArrayList;
import java.util.List;

import in.championswimmer.sfg.lib.SimpleFingerGestures;
import in.championswimmer.sfg.lib.SimpleFingerGestures.OnFingerGestureListener;

import static com.benny.openlauncher.util.Definitions.WallpaperScroll.Inverse;
import static com.benny.openlauncher.util.Definitions.WallpaperScroll.Normal;
import static com.benny.openlauncher.util.Definitions.WallpaperScroll.Off;

public final class Desktop extends ViewPager implements DesktopCallback {
    private OnDesktopEditListener _desktopEditListener;
    private boolean _inEditMode;
    private PagerIndicator _pageIndicator;

    private final List<CellContainer> _pages = new ArrayList<>();
    private final Point _previousDragPoint = new Point();

    private float _lastDownY;
    private Point _coordinate = new Point(-1, -1);
    private DesktopAdapter _adapter;
    private Item _previousItem;
    private View _previousItemView;
    private int _previousPage;

    private float _parallaxX;
    private float _parallaxY;

    public float getLastDownY() {
        return _lastDownY;
    }

    public void setLastDownY(float lastDownY) {
        _lastDownY = lastDownY;
    }

    public Desktop(Context context) {
        super(context, null);
        setClipChildren(false);
        setClipToPadding(false);
        initScroller();
    }

    public Desktop(Context context, AttributeSet attr) {
        super(context, attr);
        setClipChildren(false);
        setClipToPadding(false);
        initScroller();
    }

    private void initScroller() {
        try {
            Class<?> viewPager = ViewPager.class;
            java.lang.reflect.Field scroller = viewPager.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            java.lang.reflect.Field interpolator = viewPager.getDeclaredField("sInterpolator");
            interpolator.setAccessible(true);

            scroller.set(this, new FixedSpeedScroller(getContext(), (android.view.animation.Interpolator) interpolator.get(null)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class FixedSpeedScroller extends android.widget.Scroller {
        private int mDuration = 350;

        public FixedSpeedScroller(Context context) {
            super(context);
        }

        public FixedSpeedScroller(Context context, android.view.animation.Interpolator interpolator) {
            super(context, interpolator);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }
    }

    public static boolean handleOnDropOver(HomeActivity homeActivity, Item dropItem, Item item, View itemView, CellContainer parent, int page, ItemPosition itemPosition, DesktopCallback callback) {
        if (item != null) {
            if (dropItem != null) {
                Type type = item._type;
                if (type != null) {
                    switch (type) {
                        case APP:
                        case SHORTCUT:
                            if (Type.APP.equals(dropItem._type) || Type.SHORTCUT.equals(dropItem._type)) {
                                parent.removeView(itemView);
                                Item group = Item.newGroupItem();
                                item._location = ItemPosition.Group;
                                dropItem._location = ItemPosition.Group;
                                group.getGroupItems().add(item);
                                group.getGroupItems().add(dropItem);
                                group._x = item._x;
                                group._y = item._y;
                                HomeActivity._db.saveItem(dropItem, page, ItemPosition.Group);
                                HomeActivity._db.saveItem(item, ItemState.Hidden);
                                HomeActivity._db.saveItem(dropItem, ItemState.Hidden);
                                HomeActivity._db.saveItem(group, page, itemPosition);
                                callback.addItemToPage(group, page);
                                HomeActivity launcher = HomeActivity.Companion.getLauncher();
                                if (launcher != null) {
                                    launcher.getDesktop().consumeLastItem();
                                    launcher.getDock().consumeLastItem();
                                }
                                return true;
                            } else if (Type.GROUP.equals(dropItem._type) && dropItem.getGroupItems().size() < GroupPopupView.GroupDef._maxItem) {
                                parent.removeView(itemView);
                                Item group = Item.newGroupItem();
                                item._location = ItemPosition.Group;
                                dropItem._location = ItemPosition.Group;
                                group.getGroupItems().add(item);
                                group.getGroupItems().addAll(dropItem.getGroupItems());
                                group._x = item._x;
                                group._y = item._y;
                                HomeActivity._db.deleteItem(dropItem, false);
                                HomeActivity._db.saveItem(item, ItemState.Hidden);
                                HomeActivity._db.saveItem(group, page, itemPosition);
                                callback.addItemToPage(group, page);
                                HomeActivity launcher = HomeActivity.Companion.getLauncher();
                                if (launcher != null) {
                                    launcher.getDesktop().consumeLastItem();
                                    launcher.getDock().consumeLastItem();
                                }
                                return true;
                            }
                            break;
                        case GROUP:
                            if ((Item.Type.APP.equals(dropItem._type) || Type.SHORTCUT.equals(dropItem._type)) && item.getGroupItems().size() < GroupPopupView.GroupDef._maxItem) {
                                parent.removeView(itemView);
                                dropItem._location = ItemPosition.Group;
                                item.getGroupItems().add(dropItem);
                                HomeActivity._db.saveItem(dropItem, page, ItemPosition.Group);
                                HomeActivity._db.saveItem(dropItem, ItemState.Hidden);
                                HomeActivity._db.saveItem(item, page, itemPosition);
                                callback.addItemToPage(item, page);
                                HomeActivity launcher = HomeActivity.Companion.getLauncher();
                                if (launcher != null) {
                                    launcher.getDesktop().consumeLastItem();
                                    launcher.getDock().consumeLastItem();
                                }
                                return true;
                            } else if (Type.GROUP.equals(dropItem._type) && item.getGroupItems().size() < GroupPopupView.GroupDef._maxItem && dropItem.getGroupItems().size() < GroupPopupView.GroupDef._maxItem) {
                                parent.removeView(itemView);
                                item.getGroupItems().addAll(dropItem.getGroupItems());
                                HomeActivity._db.saveItem(item, page, itemPosition);
                                HomeActivity._db.deleteItem(dropItem, false);
                                callback.addItemToPage(item, page);
                                HomeActivity launcher = HomeActivity.Companion.getLauncher();
                                if (launcher != null) {
                                    launcher.getDesktop().consumeLastItem();
                                    launcher.getDock().consumeLastItem();
                                }
                                return true;
                            }
                            break;
                        default:
                            break;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public final class DesktopAdapter extends PagerAdapter {
        private final Desktop _desktop;
        private WebView _webView;

        public DesktopAdapter(Desktop desktop, int pageCount) {
            _desktop = desktop;
            _desktop.getPages().clear();
            if (pageCount == 0) pageCount++;
            for (int i = 0; i < pageCount; i++) {
                _desktop.getPages().add(getItemLayout());
            }
        }

        private OnFingerGestureListener getGestureListener() {
            return new DesktopGestureListener(_desktop, Setup.desktopGestureCallback());
        }

        private CellContainer getItemLayout() {
            Context context = _desktop.getContext();
            CellContainer layout = new CellContainer(context);
            SimpleFingerGestures mySfg = new SimpleFingerGestures();
            mySfg.setOnFingerGestureListener(getGestureListener());
            layout.setGestures(mySfg);
            layout.setGridSize(Setup.appSettings().getDesktopColumnCount(), Setup.appSettings().getDesktopRowCount());
            layout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Do nothing on click to prevent accidental exit
                }
            });
            layout.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (HomeActivity.Companion.getLauncher() == null) {
                        return false;
                    }
                    if (_desktop.getInEditMode()) {
                        exitDesktopEditMode();
                    } else {
                        enterDesktopEditMode();
                    }
                    if (Setup.appSettings().getGestureFeedback()) {
                        Tool.vibrate(_desktop);
                    }
                    return true;
                }
            });
            return layout;
        }

        public void addPageLeft() {
            // Shift pages to the right (including home page)
            HomeActivity._db.addPage(0);
            Setup.appSettings().setDesktopPageCurrent(Setup.appSettings().getDesktopPageCurrent()+1);

            _desktop.getPages().add(0, getItemLayout());
            notifyDataSetChanged();
        }

        public void addPageRight() {
            _desktop.getPages().add(getItemLayout());
            notifyDataSetChanged();
        }

        public void removePage(int position, boolean deleteItems) {
            int pageIndex = Setup.appSettings().getDesktopPage0Enabled() ? position - 1 : position;
            if (pageIndex < 0) return; // Cannot remove WebView page this way

            if (deleteItems) {
                for (View view : _desktop.getPages().get(pageIndex).getAllCells()) {
                    Object item = view.getTag();
                    if (item instanceof Item) {
                        HomeActivity._db.deleteItem((Item) item, true);
                    }
                }
            }

            // Shift pages to the left (including home page)
            HomeActivity._db.removePage(pageIndex);
            if (Setup.appSettings().getDesktopPageCurrent() > pageIndex) {
                Setup.appSettings().setDesktopPageCurrent(Setup.appSettings().getDesktopPageCurrent() - 1);
            }

            _desktop.getPages().remove(pageIndex);
            notifyDataSetChanged();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            if (_desktop.getInEditMode()) {
                return _desktop.getPages().size();
            }
            boolean page0Enabled = Setup.appSettings().getDesktopPage0Enabled();
            boolean infinite = Setup.appSettings().getDesktopInfiniteScrolling();
            int count = _desktop.getPages().size() + (page0Enabled ? 1 : 0);
            
            if (infinite && _desktop.getPages().size() > 1) {
                if (page0Enabled) {
                    count++; // Dummy at the end (Page 1)
                } else {
                    count += 2; // Dummy at both ends (Page N and Page 1)
                }
            }
            return count;
        }

        @Override
        public boolean isViewFromObject(View p1, Object p2) {
            return p1 == p2;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        public void clearPage0() {
            if (_webView != null) {
                _webView.clearCache(true);
                _webView.clearHistory();
                _webView.clearFormData();
                android.webkit.CookieManager.getInstance().removeAllCookies(null);
                android.webkit.CookieManager.getInstance().flush();
                _webView.loadUrl(Setup.appSettings().getDesktopPage0Url());
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            AppSettings appSettings = Setup.appSettings();
            if (_desktop.getInEditMode()) {
                CellContainer layout = _desktop.getPages().get(position);
                int topPadding = Tool.dp2px(Setup.appSettings().getSearchBarEnable() ? 120 : 70);
                int bottomPadding = Tool.dp2px(115);
                layout.setPadding(0, topPadding, 0, bottomPadding);
                if (layout.getParent() != null) {
                    ((ViewGroup) layout.getParent()).removeView(layout);
                }
                container.addView(layout);
                return layout;
            }
            
            boolean page0Enabled = appSettings.getDesktopPage0Enabled();
            boolean infinite = appSettings.getDesktopInfiniteScrolling() && _desktop.getPages().size() > 1;
            
            int realPosition = position;
            if (infinite) {
                if (page0Enabled) {
                    // WV (0) | P1 (1) | P2 (2) | D(P1) (3)
                    if (position == getCount() - 1) {
                        // Dummy at end
                        return createDummyView(container, 0);
                    }
                } else {
                    // D(PN) (0) | P1 (1) | P2 (2) | D(P1) (3)
                    if (position == 0) {
                        return createDummyView(container, Math.max(0, _desktop.getPages().size() - 1));
                    } else if (position == getCount() - 1) {
                        return createDummyView(container, 0);
                    }
                    realPosition = position - 1;
                }
            }

            if (page0Enabled && realPosition == 0) {
                // WebView logic
                if (_webView == null) {
                    _webView = new WebView(_desktop.getContext());
                    _webView.setFitsSystemWindows(false);
                    WebSettings settings = _webView.getSettings();
                    
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                    settings.setDatabaseEnabled(true);
                    settings.setAllowFileAccess(true);
                    settings.setCacheMode(WebSettings.LOAD_DEFAULT);
                    settings.setUseWideViewPort(true);
                    settings.setLoadWithOverviewMode(true);
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(_webView, true);
                    }
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true);

                    _webView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    _webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                    _webView.setVerticalScrollBarEnabled(false);
                    _webView.setHorizontalScrollBarEnabled(false);
                    _webView.setWebViewClient(new WebViewClient());
                    _webView.loadUrl(Setup.appSettings().getDesktopPage0Url());
                }

                android.widget.RelativeLayout layout = new android.widget.RelativeLayout(_desktop.getContext());
                
                // WebView fills the whole layout
                android.widget.RelativeLayout.LayoutParams webViewParams = new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                if (_webView.getParent() != null) {
                    ((ViewGroup) _webView.getParent()).removeView(_webView);
                }
                layout.addView(_webView, webViewParams);

                // Floating Pill Container
                android.widget.LinearLayout buttonContainer = new android.widget.LinearLayout(_desktop.getContext());
                buttonContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                
                // Pill Shape
                android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
                pill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                pill.setCornerRadius(Tool.dp2px(24));
                pill.setColor(android.graphics.Color.parseColor("#AA000000"));
                buttonContainer.setBackground(pill);
                
                buttonContainer.setGravity(android.view.Gravity.CENTER);
                buttonContainer.setPadding(Tool.dp2px(8), 0, Tool.dp2px(8), 0);
                
                android.widget.RelativeLayout.LayoutParams containerParams = new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, Tool.dp2px(48));
                containerParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
                containerParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
                containerParams.bottomMargin = Tool.dp2px(20);
                layout.addView(buttonContainer, containerParams);

                // Home Button
                android.widget.ImageButton homeBtn = new android.widget.ImageButton(_desktop.getContext());
                homeBtn.setImageResource(com.benny.openlauncher.R.drawable.ic_home);
                homeBtn.setBackground(null);
                homeBtn.setPadding(Tool.dp2px(12), 0, Tool.dp2px(12), 0);
                homeBtn.setColorFilter(android.graphics.Color.WHITE);
                homeBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        _webView.loadUrl(Setup.appSettings().getDesktopPage0Url());
                    }
                });
                buttonContainer.addView(homeBtn);

                // Back Button
                android.widget.ImageButton backBtn = new android.widget.ImageButton(_desktop.getContext());
                backBtn.setImageResource(com.benny.openlauncher.R.drawable.ic_arrow_back_white);
                backBtn.setBackground(null);
                backBtn.setPadding(Tool.dp2px(12), 0, Tool.dp2px(12), 0);
                backBtn.setColorFilter(android.graphics.Color.WHITE);
                backBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (_webView.canGoBack()) {
                            _webView.goBack();
                        }
                    }
                });
                buttonContainer.addView(backBtn);

                container.addView(layout);
                return layout;
            }
            
            int pageIndex;
            if (infinite) {
                if (page0Enabled) {
                    pageIndex = realPosition - 1;
                } else {
                    pageIndex = realPosition;
                }
            } else {
                pageIndex = page0Enabled ? position - 1 : position;
            }
            
            if (pageIndex < 0 || pageIndex >= _desktop.getPages().size()) {
                return new View(_desktop.getContext());
            }
            
            CellContainer layout = _desktop.getPages().get(pageIndex);
            
            // Maximize horizontal space for widgets
            int topPadding = Tool.dp2px(Setup.appSettings().getSearchBarEnable() ? 120 : 70);
            int bottomPadding = Tool.dp2px(115);
            layout.setPadding(0, topPadding, 0, bottomPadding);
            
            if (layout.getParent() != null) {
                ((ViewGroup) layout.getParent()).removeView(layout);
            }
            container.addView(layout);
            return layout;
        }

        private View createDummyView(ViewGroup container, int targetPageIndex) {
            CellContainer layout = getItemLayout();
            
            // Apply padding same as in instantiateItem
            int topPadding = Tool.dp2px(Setup.appSettings().getSearchBarEnable() ? 120 : 70);
            int bottomPadding = Tool.dp2px(115);
            layout.setPadding(0, topPadding, 0, bottomPadding);

            // Get items for the target page from the database (cached)
            List<List<Item>> desktopItems = HomeActivity._db.getDesktop();
            if (targetPageIndex < desktopItems.size()) {
                List<Item> pageItems = desktopItems.get(targetPageIndex);
                int columns = Setup.appSettings().getDesktopColumnCount();
                int rows = Setup.appSettings().getDesktopRowCount();
                for (Item item : pageItems) {
                    if (item._x < 0 || item._y < 0 || item._x + item._spanX > columns || item._y + item._spanY > rows) continue;
                    View itemView = ItemViewFactory.getItemView(_desktop.getContext(), _desktop, Action.DESKTOP, item);
                    if (itemView != null) {
                        layout.addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
                    }
                }
            }

            container.addView(layout);
            return layout;
        }

        private void enterDesktopEditMode() {
            int currentPageIndex = _desktop.getCurrentPageIndex();
            _desktop.setInEditMode(true);
            notifyDataSetChanged();
            _desktop.setCurrentItem(currentPageIndex, false);

            float scaleFactor = 0.8f;
            float translateFactor = (float) Tool.dp2px(Setup.appSettings().getSearchBarEnable() ? 20 : 40);
            for (CellContainer v : _desktop.getPages()) {
                v.setBlockTouch(true);
                v.animateBackgroundShow();
                ViewPropertyAnimator animation = v.animate().scaleX(scaleFactor).scaleY(scaleFactor).translationY(translateFactor);
                animation.setInterpolator(new AccelerateDecelerateInterpolator());
            }
            if (_desktop.getDesktopEditListener() != null) {
                OnDesktopEditListener desktopEditListener = _desktop.getDesktopEditListener();
                desktopEditListener.onStartDesktopEdit();
            }
        }

        private void exitDesktopEditMode() {
            int currentPageIndex = _desktop.getCurrentPageIndex();
            _desktop.setInEditMode(false);
            notifyDataSetChanged();
            boolean page0Enabled = Setup.appSettings().getDesktopPage0Enabled();
            _desktop.setCurrentItem(currentPageIndex + (page0Enabled ? 1 : 0), false);

            float scaleFactor = 1.0f;
            float translateFactor = 0.0f;
            for (CellContainer v : _desktop.getPages()) {
                v.setBlockTouch(false);
                v.animateBackgroundHide();
                ViewPropertyAnimator animation = v.animate().scaleX(scaleFactor).scaleY(scaleFactor).translationY(translateFactor);
                animation.setInterpolator(new AccelerateDecelerateInterpolator());
            }
            if (_desktop.getDesktopEditListener() != null) {
                OnDesktopEditListener desktopEditListener = _desktop.getDesktopEditListener();
                desktopEditListener.onFinishDesktopEdit();
            }
        }
    }

    public final List<CellContainer> getPages() {
        return _pages;
    }

    public final OnDesktopEditListener getDesktopEditListener() {
        return _desktopEditListener;
    }

    public final void setDesktopEditListener(@Nullable OnDesktopEditListener v) {
        _desktopEditListener = v;
    }

    public final boolean getInEditMode() {
        return _inEditMode;
    }

    public final void setInEditMode(boolean v) {
        _inEditMode = v;
    }

    public final void exitDesktopEditMode() {
        if (_adapter != null) {
            _adapter.exitDesktopEditMode();
        }
    }

    public final boolean isCurrentPageEmpty() {
        return getCurrentPage().getChildCount() == 0;
    }

    public final CellContainer getCurrentPage() {
        int index = getCurrentItem();
        int pageIndex = getCurrentPageIndex();
        return _pages.get(pageIndex);
    }

    public final int getCurrentPageIndex() {
        int index = getCurrentItem();
        if (_inEditMode) {
            return Math.max(0, Math.min(index, _pages.size() - 1));
        }
        boolean page0Enabled = Setup.appSettings().getDesktopPage0Enabled();
        int pageIndex = page0Enabled ? index - 1 : index;
        return Math.max(0, Math.min(pageIndex, _pages.size() - 1));
    }

    public final void setPageIndicator(PagerIndicator pageIndicator) {
        _pageIndicator = pageIndicator;
    }


    public final void initDesktop() {
        initDesktop(null);
    }

    public final void initDesktop(final Runnable onFinished) {
        HomeActivity._db.getDesktopAsync(new DatabaseHelper.DataCallback<List<List<Item>>>() {
            @Override
            public void onDataLoaded(List<List<Item>> desktopItems) {
                _adapter = new DesktopAdapter(Desktop.this, desktopItems.size());
                setAdapter(_adapter);

                boolean page0Enabled = Setup.appSettings().getDesktopPage0Enabled();
                setCurrentItem(Setup.appSettings().getDesktopPageCurrent() + (page0Enabled ? 1 : 0));

                if (Setup.appSettings().getDesktopShowIndicator() && _pageIndicator != null) {
                    _pageIndicator.setViewPager(Desktop.this);
                }
                addItemsToPage(desktopItems);
                
                if (onFinished != null) {
                    onFinished.run();
                }
            }
        });
    }

    private void addItemsToPage(List<List<Item>> desktopItems) {
        int columns = Setup.appSettings().getDesktopColumnCount();
        int rows = Setup.appSettings().getDesktopRowCount();
        int currentPageIndex = getCurrentPageIndex();

        // Load current page first
        if (currentPageIndex < desktopItems.size()) {
            List<Item> page = desktopItems.get(currentPageIndex);
            _pages.get(currentPageIndex).removeAllViews();
            for (int itemCount = 0; itemCount < page.size(); itemCount++) {
                Item item = page.get(itemCount);
                if (item._x + item._spanX <= columns && item._y + item._spanY <= rows) {
                    addItemToPage(item, currentPageIndex);
                }
            }
        }

        // Load other pages
        for (int pageCount = 0; pageCount < desktopItems.size(); pageCount++) {
            if (pageCount == currentPageIndex) continue;
            List<Item> page = desktopItems.get(pageCount);
            _pages.get(pageCount).removeAllViews();
            for (int itemCount = 0; itemCount < page.size(); itemCount++) {
                Item item = page.get(itemCount);
                if (item._x + item._spanX <= columns && item._y + item._spanY <= rows) {
                    addItemToPage(item, pageCount);
                }
            }
        }
    }

    public final void updateDesktop() {
        HomeActivity._db.getDesktopAsync(new DatabaseHelper.DataCallback<List<List<Item>>>() {
            @Override
            public void onDataLoaded(List<List<Item>> desktopItems) {
                addItemsToPage(desktopItems);
            }
        });
    }

    public final void addPageRight(boolean showGrid) {
        int previousPage = getCurrentItem();
        _adapter.addPageRight();
        setCurrentItem(previousPage + 1);
        if (Setup.appSettings().getDesktopShowGrid()) {
            for (CellContainer cellContainer : _pages) {
                cellContainer.setHideGrid(!showGrid);
            }
        }
        _pageIndicator.invalidate();
    }

    public final void addPageLeft(boolean showGrid) {
        int previousPage = getCurrentItem();
        _adapter.addPageLeft();
        setCurrentItem(previousPage + 1, false);
        setCurrentItem(previousPage);
        if (Setup.appSettings().getDesktopShowGrid()) {
            for (CellContainer cellContainer : _pages) {
                cellContainer.setHideGrid(!showGrid);
            }
        }
        _pageIndicator.invalidate();
    }

    public final void removeCurrentPage() {
        int previousPage = getCurrentItem();
        _adapter.removePage(getCurrentItem(), true);
        if (_pages.size() == 0) {
            addPageRight(false);
            _adapter.exitDesktopEditMode();
        } else {
            setCurrentItem(previousPage, true);
            _pageIndicator.invalidate();
        }
    }

    public final void updateIconProjection(int x, int y) {
        HomeActivity launcher = HomeActivity.Companion.getLauncher();
        ItemOptionView dragNDropView = launcher.getItemOptionView();
        DragState state = getCurrentPage().peekItemAndSwap(x, y, _coordinate);
        if (!_coordinate.equals(_previousDragPoint)) {
            dragNDropView.cancelFolderPreview();
        }
        _previousDragPoint.set(_coordinate.x, _coordinate.y);
        switch (state) {
            case CurrentNotOccupied:
                getCurrentPage().projectImageOutlineAt(_coordinate, DragHandler._cachedDragBitmap);
                break;
            case CurrentOccupied:
                Item.Type type = dragNDropView.getDragItem()._type;
                for (CellContainer page : _pages) {
                    page.clearCachedOutlineBitmap();
                }
                if (!type.equals(Type.WIDGET) && (getCurrentPage().coordinateToChildView(_coordinate) instanceof AppItemView)) {
                    dragNDropView.showFolderPreviewAt(this, getCurrentPage().getCellWidth() * (_coordinate.x + 0.5f), getCurrentPage().getCellHeight() * (_coordinate.y + 0.5f));
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
        com.benny.openlauncher.util.Logger.log(this, "setLastItem: " + item.getLabel());
        _previousPage = getCurrentPageIndex();
        _previousItemView = view;
        _previousItem = item;
        getCurrentPage().removeView(view);
    }

    @Override
    public void revertLastItem() {
        com.benny.openlauncher.util.Logger.log(this, "revertLastItem, hasPreviousView: " + (_previousItemView != null));
        if (_previousItemView != null) {
            if (_previousPage > -1 && _previousPage < _pages.size()) {
                CellContainer cellContainer = _pages.get(_previousPage);
                cellContainer.addViewToGrid(_previousItemView);
                _previousItem = null;
                _previousItemView = null;
                _previousPage = -1;
            }
        }
    }

    @Override
    public void consumeLastItem() {
        com.benny.openlauncher.util.Logger.log(this, "consumeLastItem");
        _previousItem = null;
        _previousItemView = null;
        _previousPage = -1;
    }

    public boolean addItemToPage(@NonNull Item item, int page) {
        if (com.benny.openlauncher.util.Logger.isEnabled()) {
            com.benny.openlauncher.util.Logger.log(this, "addItemToPage: " + item.getLabel() + " at page " + page + " (" + item._x + "," + item._y + ")");
        }
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item);
        if (itemView == null) {
            // TODO see if this fixes SD card bug
            // apps that are located on SD card disappear on reboot
            // might be from this line of code so comment out for now
            //HomeActivity._db.deleteItem(item, true);
            return false;
        }
        item._location = ItemPosition.Desktop;
        _pages.get(page).addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
        return true;
    }

    public boolean addItemToPoint(@NonNull Item item, int x, int y) {
        if (com.benny.openlauncher.util.Logger.isEnabled()) {
            com.benny.openlauncher.util.Logger.log(this, "addItemToPoint: " + item.getLabel() + " at point (" + x + "," + y + ")");
        }
        CellContainer.LayoutParams positionToLayoutPrams = getCurrentPage().coordinateToLayoutParams(x, y, item._spanX, item._spanY);
        if (positionToLayoutPrams == null) {
            if (com.benny.openlauncher.util.Logger.isEnabled()) {
                com.benny.openlauncher.util.Logger.log(this, "addItemToPoint: no layout params found for point");
            }
            return false;
        }
        item._location = ItemPosition.Desktop;
        item._x = positionToLayoutPrams.getX();
        item._y = positionToLayoutPrams.getY();
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item);
        if (itemView != null) {
            itemView.setLayoutParams(positionToLayoutPrams);
            getCurrentPage().addView(itemView);
        }
        return true;
    }

    public boolean addItemToCell(@NonNull Item item, int x, int y) {
        if (com.benny.openlauncher.util.Logger.isEnabled()) {
            com.benny.openlauncher.util.Logger.log(this, "addItemToCell: " + item.getLabel() + " at cell (" + x + "," + y + ")");
        }
        item._location = ItemPosition.Desktop;
        item._x = x;
        item._y = y;
        View itemView = ItemViewFactory.getItemView(getContext(), this, Action.DESKTOP, item);
        if (itemView == null) {
            if (com.benny.openlauncher.util.Logger.isEnabled()) {
                com.benny.openlauncher.util.Logger.log(this, "addItemToCell: could not create view for item");
            }
            return false;
        }
        getCurrentPage().addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY);
        return true;
    }

    public void removeItem(final View view, boolean animate) {
        if (animate) {
            view.animate().setDuration(100).scaleX(0.0f).scaleY(0.0f).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (getCurrentPage().equals(view.getParent())) {
                        getCurrentPage().removeView(view);
                    }
                }
            });
        } else if (getCurrentPage().equals(view.getParent())) {
            getCurrentPage().removeView(view);
        }
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        updateWallpaperOffset(position, offset);
        super.onPageScrolled(position, offset, offsetPixels);
    }

    private float _currentPosition;
    private float _currentOffset;

    public void updateWallpaperOffset() {
        updateWallpaperOffset(_currentPosition, _currentOffset);
    }

    private void updateWallpaperOffset(int position, float offset) {
        _currentPosition = position;
        _currentOffset = offset;
        updateWallpaperOffset((float) position, offset);
    }

    private void updateWallpaperOffset(float position, float offset) {
        Definitions.WallpaperScroll scroll = Setup.appSettings().getDesktopWallpaperScroll();
        float xOffset = (position + offset) / (getAdapter().getCount() - 1);
        if (scroll.equals(Inverse)) {
            xOffset = 1f - xOffset;
        } else if (scroll.equals(Off)) {
            xOffset = 0.5f;
        }

        xOffset = Math.max(0, Math.min(1, xOffset));

        // Add parallax
        float finalXOffset = xOffset + _parallaxX;
        float finalYOffset = 0.5f + _parallaxY;

        finalXOffset = Math.max(0, Math.min(1, finalXOffset));
        finalYOffset = Math.max(0, Math.min(1, finalYOffset));

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());
        try {
            wallpaperManager.setWallpaperOffsets(getWindowToken(), finalXOffset, finalYOffset);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void setParallaxOffsets(float x, float y) {
        _parallaxX = x;
        _parallaxY = y;
        updateWallpaperOffset();
    }

    public interface OnDesktopEditListener {
        void onStartDesktopEdit();

        void onFinishDesktopEdit();
    }
}