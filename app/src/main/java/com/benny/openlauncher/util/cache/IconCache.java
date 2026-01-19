package com.benny.openlauncher.util.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.Tool;

public class IconCache {
    private final LruCache<String, Bitmap> _cache;
    private static IconCache _instance;

    public static IconCache getInstance() {
        if (_instance == null) {
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 8; // 1/8 des verfügbaren RAMs
            _instance = new IconCache(cacheSize);
        }
        return _instance;
    }

    private IconCache(int cacheSize) {
        _cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public Bitmap getIcon(String key) {
        Bitmap bitmap = _cache.get(key);
        if (bitmap == null) {
            Context context = Setup.appContext();
            if (context != null) {
                Drawable drawable = Tool.getIcon(context, key.replaceAll("[^a-zA-Z0-9.-]", "_"));
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                    if (bitmap != null) {
                        _cache.put(key, bitmap);
                    }
                }
            }
        }
        return bitmap;
    }

    public void addIcon(String key, Bitmap bitmap) {
        if (_cache.get(key) == null) {
            _cache.put(key, bitmap);
        }
    }

    public void addIcon(String key, Drawable drawable) {
        if (drawable == null) return;
        addIcon(key, drawableToBitmap(drawable));
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int width = drawable.getIntrinsicWidth() <= 0 ? 1 : drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight() <= 0 ? 1 : drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
