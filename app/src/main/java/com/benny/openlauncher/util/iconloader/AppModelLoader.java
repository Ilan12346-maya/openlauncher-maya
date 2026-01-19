package com.benny.openlauncher.util.iconloader;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.util.IconPackHelper;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.Tool;

public class AppModelLoader implements ModelLoader<App, Drawable> {

    @Nullable
    @Override
    public LoadData<Drawable> buildLoadData(@NonNull App app, int width, int height, @NonNull Options options) {
        String signature = app.getComponentName() + "_" + AppSettings.get().getIconPack();
        return new LoadData<>(new ObjectKey(signature), new AppIconFetcher(app, width, height));
    }

    @Override
    public boolean handles(@NonNull App app) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<App, Drawable> {
        @NonNull
        @Override
        public ModelLoader<App, Drawable> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new AppModelLoader();
        }

        @Override
        public void teardown() {
        }
    }

    private static class AppIconFetcher implements DataFetcher<Drawable> {
        private final App app;
        private final int width;
        private final int height;

        AppIconFetcher(App app, int width, int height) {
            this.app = app;
            this.width = width;
            this.height = height;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Drawable> callback) {
            int iconSize = Math.max(width, height);
            if (iconSize <= 0) {
                iconSize = Tool.dp2px(AppSettings.get().getIconSize());
            }

            Drawable icon = IconPackHelper.getInstance(com.benny.openlauncher.manager.Setup.appContext()).getIcon(app, iconSize);
            if (icon == null) {
                icon = app.getIcon();
            }

            if (icon != null) {
                callback.onDataReady(icon);
            } else {
                callback.onLoadFailed(new Exception("Failed to load icon for " + app.getLabel()));
            }
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void cancel() {
        }

        @NonNull
        @Override
        public Class<Drawable> getDataClass() {
            return Drawable.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
