package com.benny.openlauncher.util;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;

import com.benny.openlauncher.model.App;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPackHelper {
    public static void applyIconPack(AppManager appManager, final int iconSize, String iconPackName, List<App> apps) {
        Resources iconPackResources = null;
        Map<String, String> appFilterMap = new HashMap<>();
        Map<String, String> configMap = new HashMap<>();

        if (!iconPackName.equals("")) {
            try {
                iconPackResources = appManager.getPackageManager().getResourcesForApplication(iconPackName);
                parseAppFilter(iconPackResources, iconPackName, appFilterMap, configMap);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        int intResourceIcon = 0;
        int intResourceBack = 0;
        int intResourceMask = 0;
        int intResourceUpon = 0;
        float scale = 1;

        if (iconPackResources != null) {
            if (configMap.get("iconback") != null)
                intResourceBack = iconPackResources.getIdentifier(configMap.get("iconback"), "drawable", iconPackName);
            if (configMap.get("iconmask") != null)
                intResourceMask = iconPackResources.getIdentifier(configMap.get("iconmask"), "drawable", iconPackName);
            if (configMap.get("iconupon") != null)
                intResourceUpon = iconPackResources.getIdentifier(configMap.get("iconupon"), "drawable", iconPackName);
            if (configMap.get("scale") != null)
                scale = Float.parseFloat(configMap.get("scale"));
        }

        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        p.setAntiAlias(true);

        Paint origP = new Paint(Paint.FILTER_BITMAP_FLAG);
        origP.setAntiAlias(true);

        Paint maskP = new Paint(Paint.FILTER_BITMAP_FLAG);
        maskP.setAntiAlias(true);
        maskP.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        BitmapFactory.Options uniformOptions = new BitmapFactory.Options();
        uniformOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        uniformOptions.inScaled = false;
        uniformOptions.inDither = false;

        Bitmap back = null;
        Bitmap mask = null;
        Bitmap upon = null;
        Canvas canvasOrig;
        Canvas canvas;
        Bitmap scaledBitmap;
        Bitmap scaledOrig;
        Bitmap orig;

        if (iconPackName.compareTo("") != 0 && iconPackResources != null) {
            try {
                if (intResourceBack != 0)
                    back = BitmapFactory.decodeResource(iconPackResources, intResourceBack, uniformOptions);
                if (intResourceMask != 0)
                    mask = BitmapFactory.decodeResource(iconPackResources, intResourceMask, uniformOptions);
                if (intResourceUpon != 0)
                    upon = BitmapFactory.decodeResource(iconPackResources, intResourceUpon, uniformOptions);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;

        for (int i = 0; i < apps.size(); i++) {
            if (iconPackResources != null) {
                String iconResource = appFilterMap.get(apps.get(i).getComponentName());
                if (iconResource != null) {
                    intResourceIcon = iconPackResources.getIdentifier(iconResource, "drawable", iconPackName);
                } else {
                    intResourceIcon = 0;
                }

                if (intResourceIcon != 0) {
                    // has single drawable for app
                    apps.get(i).setIcon(new BitmapDrawable(BitmapFactory.decodeResource(iconPackResources, intResourceIcon, uniformOptions)));
                } else {
                    try {
                        orig = Bitmap.createBitmap(apps.get(i).getIcon().getIntrinsicWidth(), apps.get(i).getIcon().getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    } catch (Exception e) {
                        continue;
                    }
                    apps.get(i).getIcon().setBounds(0, 0, apps.get(i).getIcon().getIntrinsicWidth(), apps.get(i).getIcon().getIntrinsicHeight());
                    apps.get(i).getIcon().draw(new Canvas(orig));

                    scaledOrig = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                    scaledBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(scaledBitmap);

                    if (back != null)
                        canvas.drawBitmap(back, getResizedMatrix(back, iconSize, iconSize), p);

                    canvasOrig = new Canvas(scaledOrig);
                    orig = getResizedBitmap(orig, (int) (iconSize * scale), (int) (iconSize * scale));
                    canvasOrig.drawBitmap(orig, scaledOrig.getWidth() - (orig.getWidth() / 2) - scaledOrig.getWidth() / 2, scaledOrig.getWidth() - (orig.getWidth() / 2) - scaledOrig.getWidth() / 2, origP);

                    if (mask != null)
                        canvasOrig.drawBitmap(mask, getResizedMatrix(mask, iconSize, iconSize), maskP);

                    canvas.drawBitmap(getResizedBitmap(scaledOrig, iconSize, iconSize), 0, 0, p);

                    if (upon != null)
                        canvas.drawBitmap(upon, getResizedMatrix(upon, iconSize, iconSize), p);

                    apps.get(i).setIcon(new BitmapDrawable(appManager.getContext().getResources(), scaledBitmap));
                }
            }
        }
    }

    private static void parseAppFilter(Resources resources, String packageName, Map<String, String> appFilterMap, Map<String, String> configMap) {
        try {
            int resourceValue = resources.getIdentifier("appfilter", "xml", packageName);
            if (resourceValue != 0) {
                XmlResourceParser xrp = resources.getXml(resourceValue);
                while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                    if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                        String name = xrp.getName();
                        if (name.equals("item")) {
                            String component = xrp.getAttributeValue(null, "component");
                            String drawable = xrp.getAttributeValue(null, "drawable");
                            if (component != null && drawable != null) {
                                appFilterMap.put(component, drawable);
                            }
                        } else if (name.equals("iconback") || name.equals("iconmask") || name.equals("iconupon") || name.equals("scale")) {
                            String img = xrp.getAttributeValue(null, "img");
                            if (img != null) {
                                configMap.put(name, img);
                            } else if (name.equals("scale")) {
                                String factor = xrp.getAttributeValue(null, "factor");
                                if (factor != null) configMap.put(name, factor);
                            }
                        }
                    }
                    xrp.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }

    private static Matrix getResizedMatrix(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return matrix;
    }
}
