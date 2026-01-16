package com.benny.openlauncher.model;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.benny.openlauncher.util.cache.IconCache;

import java.util.List;

public class App {
    public Drawable _icon;
    public String _label;
    public String _packageName;
    public String _className;
    public UserHandle _userHandle;
    public List<ShortcutInfo> _shortcutInfo;

    private PackageManager _pm;
    private ResolveInfo _info;
    private LauncherActivityInfo _launcherInfo;

    public App(PackageManager pm, ResolveInfo info, List<ShortcutInfo> shortcutInfo) {
        _pm = pm;
        _info = info;
        _label = info.loadLabel(pm).toString();
        _packageName = info.activityInfo.packageName;
        _className = info.activityInfo.name;
        _shortcutInfo = shortcutInfo;
    }

    @SuppressLint("NewApi")
    public App(PackageManager pm, LauncherActivityInfo info, List<ShortcutInfo> shortcutInfo) {
        _pm = pm;
        _launcherInfo = info;
        _label = info.getLabel().toString();
        _packageName = info.getComponentName().getPackageName();
        _className = info.getName();
        _shortcutInfo = shortcutInfo;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof App) {
            App app = (App) object;
            return _packageName.equals(app._packageName) && _className.equals(app._className);
        } else {
            return false;
        }
    }

    public void setIcon(Drawable icon) {
        _icon = icon;
        if (icon != null) {
            IconCache.getInstance().addIcon(getComponentName(), icon);
        }
    }

    public Drawable getIcon() {
        if (_icon != null) return _icon;

        android.graphics.Bitmap cachedBitmap = IconCache.getInstance().getIcon(getComponentName());
        if (cachedBitmap != null) {
            _icon = new android.graphics.drawable.BitmapDrawable(null, cachedBitmap);
            return _icon;
        }

        if (_pm != null) {
            if (_info != null) {
                _icon = _info.loadIcon(_pm);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && _launcherInfo != null) {
                _icon = _launcherInfo.getIcon(0);
            }
            if (_icon != null) {
                IconCache.getInstance().addIcon(getComponentName(), _icon);
            }
        }
        return _icon;
    }

    public String getLabel() {
        return _label;
    }

    public String getPackageName() {
        return _packageName;
    }

    public String getClassName() {
        return _className;
    }

    public String getComponentName() {
        return new ComponentName(_packageName, _className).toString();
    }

    public List<ShortcutInfo> getShortcutInfo() {
        return _shortcutInfo;
    }
}