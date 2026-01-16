package com.benny.openlauncher.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.benny.openlauncher.R;
import com.benny.openlauncher.util.AppSettings;

public abstract class ColorActivity extends AppCompatActivity {

    protected AppSettings _appSettings;
    private String _currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        _appSettings = AppSettings.get();
        _currentTheme = _appSettings.getTheme();

        if (_appSettings.getTheme().equals("0")) {
            setTheme(R.style.NormalActivity_Light);
        } else if (_appSettings.getTheme().equals("1")) {
            setTheme(R.style.NormalActivity_Dark);
        } else {
            setTheme(R.style.NormalActivity_Black);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (_appSettings.getTheme().equals("0")) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        super.onCreate(savedInstanceState);
    }

    // Add necessary import for View if not present
    // ... wait, I need to check imports.


    @Override
    protected void onResume() {
        super.onResume();
        if (!_appSettings.getTheme().equals(_currentTheme)) {
            restart();
        }
    }

    protected void restart() {
        Intent intent = new Intent(this, getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    public int dark(int color, double factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(a, Math.max((int) (r * factor), 0), Math.max((int) (g * factor), 0), Math.max((int) (b * factor), 0));
    }
}
