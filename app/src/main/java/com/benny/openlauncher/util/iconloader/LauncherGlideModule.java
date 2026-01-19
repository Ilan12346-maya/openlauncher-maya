package com.benny.openlauncher.util.iconloader;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;

import java.io.InputStream;
import java.nio.ByteBuffer;

@GlideModule
public class LauncherGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.append(App.class, android.graphics.drawable.Drawable.class, new AppModelLoader.Factory());
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
