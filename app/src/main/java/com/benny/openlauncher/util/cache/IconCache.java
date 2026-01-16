package com.benny.openlauncher.util.cache;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

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
        return _cache.get(key);
    }

    public void addIcon(String key, Bitmap bitmap) {
        if (getIcon(key) == null) {
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
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
