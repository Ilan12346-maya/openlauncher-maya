package com.benny.openlauncher.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.benny.openlauncher.util.Tool;

public final class PagerIndicator extends View implements ViewPager.OnPageChangeListener {
    private ViewPager _pager;
    private Paint _paint = new Paint(1);

    private int _mode = Mode.DOTS;
    private float _pad;
    private float _dotSize;
    private int _previousPage = -1;
    private int _realPreviousPage;

    // current position and offset
    private float _scrollOffset;
    private int _scrollPosition;

    // dot animations
    private float _shrinkFactor = 1.0f;
    private float _expandFactor = 1.5f;

    public static class Mode {
        public static final int DOTS = 0;
        public static final int LINES = 1;
    }

    public PagerIndicator(Context context) {
        this(context, null);
    }

    public PagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        _pad = Tool.dp2px(4);
        _paint.setColor(Color.WHITE);
        _paint.setStrokeWidth(Tool.dp2px(4));
        _paint.setAntiAlias(true);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        _dotSize = getHeight() / 2;
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (_pager == null) return;
        
        com.benny.openlauncher.util.AppSettings appSettings = com.benny.openlauncher.util.AppSettings.get();
        boolean page0Enabled = appSettings.getDesktopPage0Enabled();
        boolean infinite = appSettings.getDesktopInfiniteScrolling();
        
        int pageCount = _pager.getAdapter().getCount();
        int displayPageCount = pageCount;
        int currentItem = _pager.getCurrentItem();
        int displayCurrentItem = currentItem;
        
        if (infinite && pageCount > 1) {
            if (page0Enabled) {
                displayPageCount = pageCount - 1;
                if (currentItem == pageCount - 1) {
                    displayCurrentItem = 1;
                }
            } else {
                displayPageCount = pageCount - 2;
                if (currentItem == 0) {
                    displayCurrentItem = displayPageCount - 1;
                } else if (currentItem == pageCount - 1) {
                    displayCurrentItem = 0;
                } else {
                    displayCurrentItem = currentItem - 1;
                }
            }
        }

        switch (_mode) {
            case Mode.DOTS:
                float circlesWidth = displayPageCount * (_dotSize + _pad * 2);
                canvas.translate(getWidth() / 2 - circlesWidth / 2, 0f);

                if (_realPreviousPage != displayCurrentItem) {
                    _shrinkFactor = 1f;
                    _realPreviousPage = displayCurrentItem;
                }

                for (int dot = 0; dot < displayPageCount; dot++) {
                    float stepFactor = 0.05f;
                    float smallFactor = 1.0f;
                    float largeFactor = 1.5f;
                    if (dot == displayCurrentItem) {
                        // draw shrinking dot
                        if (_previousPage == -1)
                            _previousPage = dot;
                        _shrinkFactor = Tool.clampFloat(_shrinkFactor + stepFactor, smallFactor, largeFactor);
                        canvas.drawCircle(_dotSize / 2 + _pad + (_dotSize + _pad * 2) * dot, (float) (getHeight() / 2), _shrinkFactor * _dotSize / 2, _paint);
                        if (_shrinkFactor != largeFactor)
                            invalidate();
                    } else if (dot != displayCurrentItem && dot == _previousPage) {
                        // draw expanding dot
                        _expandFactor = Tool.clampFloat(_expandFactor - stepFactor, smallFactor, largeFactor);
                        canvas.drawCircle(_dotSize / 2 + _pad + (_dotSize + _pad * 2) * dot, (float) (getHeight() / 2), _expandFactor * _dotSize / 2, _paint);
                        if (_expandFactor != smallFactor)
                            invalidate();
                        else {
                            _expandFactor = 1.5f;
                            _previousPage = -1;
                        }
                    } else {
                        // draw normal dot
                        canvas.drawCircle(_dotSize / 2 + _pad + (_dotSize + _pad * 2) * dot, (float) (getHeight() / 2), _dotSize / 2, _paint);
                    }
                }
                break;
            case Mode.LINES:
                float width = getWidth() / displayPageCount;
                float startX = (_scrollPosition + _scrollOffset) * width;
                
                // For simplicity in LINE mode, we just use the raw scroll values or adjust them
                // Adjusting line mode for infinite scroll is complex, but DOTS is default.
                
                startX = displayCurrentItem * width; // Simple snap for line mode

                float startY = getHeight() / 2;

                canvas.drawLine(startX, startY, startX + width, startY, _paint);
                break;
        }
    }

    public final void setMode(int mode) {
        _mode = mode;
        invalidate();
    }

    public final void setViewPager(ViewPager pager) {
        if (pager == null && _pager != null) {
            _pager.removeOnPageChangeListener(this);
            _pager = null;
        } else {
            _pager = pager;
            if (pager != null) {
                pager.addOnPageChangeListener(this);
            }
        }
        invalidate();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        _scrollOffset = positionOffset;
        _scrollPosition = position;
        invalidate();
    }

    @Override
    public void onPageSelected(int position) {
        // nothing
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // nothing
    }
}
