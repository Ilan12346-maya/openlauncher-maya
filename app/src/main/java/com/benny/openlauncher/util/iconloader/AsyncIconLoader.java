package com.benny.openlauncher.util.iconloader;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.benny.openlauncher.model.App;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncIconLoader {
    private static AsyncIconLoader _instance;
    private final ExecutorService _executorService;
    private final Handler _handler;

    public static AsyncIconLoader getInstance() {
        if (_instance == null) {
            _instance = new AsyncIconLoader();
        }
        return _instance;
    }

    private AsyncIconLoader() {
        _executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        _handler = new Handler(Looper.getMainLooper());
    }

    public void loadIcon(final App app, final IconCallback callback) {
        _executorService.execute(new Runnable() {
            @Override
            public void run() {
                final Drawable icon = app.getIcon();
                _handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onIconLoaded(icon);
                    }
                });
            }
        });
    }

    public interface IconCallback {
        void onIconLoaded(Drawable icon);
    }
}
