package com.benny.openlauncher.util;

import android.content.Context;
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
import android.graphics.drawable.Drawable;

import com.benny.openlauncher.model.App;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPackHelper {
    private static IconPackHelper _instance;
    private Resources _iconPackResources;
    private String _iconPackName = "";
    private Map<String, String> _appFilterMap = new HashMap<>();
    private Map<String, String> _configMap = new HashMap<>();
    
    private Bitmap _back, _mask, _upon;
    private float _scale = 1f;
    private Paint _p, _origP, _maskP;
    private BitmapFactory.Options _uniformOptions;

    public static IconPackHelper getInstance(Context context) {
        if (_instance == null) {
            _instance = new IconPackHelper();
        }
        String currentPack = AppSettings.get().getIconPack();
        if (!currentPack.equals(_instance._iconPackName)) {
            _instance.loadIconPack(context, currentPack);
        }
        return _instance;
    }

    private IconPackHelper() {
        _p = new Paint(Paint.FILTER_BITMAP_FLAG);
        _p.setAntiAlias(true);

        _origP = new Paint(Paint.FILTER_BITMAP_FLAG);
        _origP.setAntiAlias(true);

        _maskP = new Paint(Paint.FILTER_BITMAP_FLAG);
        _maskP.setAntiAlias(true);
        _maskP.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        _uniformOptions = new BitmapFactory.Options();
        _uniformOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        _uniformOptions.inScaled = false;
    }

    private void loadIconPack(Context context, String packageName) {
        _iconPackName = packageName;
        _iconPackResources = null;
        _appFilterMap.clear();
        _configMap.clear();
        _back = _mask = _upon = null;
        _scale = 1f;

        if (packageName.isEmpty()) return;

        try {
            _iconPackResources = context.getPackageManager().getResourcesForApplication(packageName);
            parseAppFilter(_iconPackResources, packageName, _appFilterMap, _configMap);
            
            if (_configMap.get("iconback") != null) {
                int id = _iconPackResources.getIdentifier(_configMap.get("iconback"), "drawable", packageName);
                if (id != 0) _back = BitmapFactory.decodeResource(_iconPackResources, id, _uniformOptions);
            }
            if (_configMap.get("iconmask") != null) {
                int id = _iconPackResources.getIdentifier(_configMap.get("iconmask"), "drawable", packageName);
                if (id != 0) _mask = BitmapFactory.decodeResource(_iconPackResources, id, _uniformOptions);
            }
            if (_configMap.get("iconupon") != null) {
                int id = _iconPackResources.getIdentifier(_configMap.get("iconupon"), "drawable", packageName);
                if (id != 0) _upon = BitmapFactory.decodeResource(_iconPackResources, id, _uniformOptions);
            }
            if (_configMap.get("scale") != null) {
                _scale = Float.parseFloat(_configMap.get("scale"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Drawable getIcon(App app, int iconSize) {
        if (_iconPackResources == null) return null;

        String iconResource = _appFilterMap.get(app.getComponentName());
        if (iconResource != null) {
            int id = _iconPackResources.getIdentifier(iconResource, "drawable", _iconPackName);
            if (id != 0) {
                return new BitmapDrawable(_iconPackResources, BitmapFactory.decodeResource(_iconPackResources, id, _uniformOptions));
            }
        }

        // Apply masking if enabled in icon pack
        if (_back != null || _mask != null || _upon != null) {
            return applyIconPackEffects(app.getIcon(), iconSize);
        }

        return null;
    }

    private Drawable applyIconPackEffects(Drawable originalIcon, int iconSize) {
        if (originalIcon == null) return null;

        Bitmap orig = Bitmap.createBitmap(originalIcon.getIntrinsicWidth(), originalIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        originalIcon.setBounds(0, 0, originalIcon.getIntrinsicWidth(), originalIcon.getIntrinsicHeight());
        originalIcon.draw(new Canvas(orig));

        Bitmap scaledOrig = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Bitmap scaledBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);

        if (_back != null)
            canvas.drawBitmap(_back, getResizedMatrix(_back, iconSize, iconSize), _p);

        Canvas canvasOrig = new Canvas(scaledOrig);
        Bitmap resizedOrig = getResizedBitmap(orig, (int) (iconSize * _scale), (int) (iconSize * _scale));
        canvasOrig.drawBitmap(resizedOrig, (scaledOrig.getWidth() - resizedOrig.getWidth()) / 2f, (scaledOrig.getHeight() - resizedOrig.getHeight()) / 2f, _origP);

        if (_mask != null)
            canvasOrig.drawBitmap(_mask, getResizedMatrix(_mask, iconSize, iconSize), _maskP);

        canvas.drawBitmap(scaledOrig, 0, 0, _p);

        if (_upon != null)
            canvas.drawBitmap(_upon, getResizedMatrix(_upon, iconSize, iconSize), _p);

        return new BitmapDrawable(null, scaledBitmap);
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

    // Keep legacy method for compatibility if needed, but empty it out
    public static void applyIconPack(AppManager appManager, final int iconSize, String iconPackName, List<App> apps) {
        // Now handled by Glide/on-demand loading
    }
}