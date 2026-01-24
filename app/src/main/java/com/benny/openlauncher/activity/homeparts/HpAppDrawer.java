package com.benny.openlauncher.activity.homeparts;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.widget.AppDrawerController;
import com.benny.openlauncher.widget.PagerIndicator;

import net.gsantner.opoc.util.Callback;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;

public class HpAppDrawer implements Callback.a2<Boolean, Boolean> {
    private HomeActivity _homeActivity;
    private PagerIndicator _appDrawerIndicator;

    public HpAppDrawer(HomeActivity homeActivity, PagerIndicator appDrawerIndicator) {
        _homeActivity = homeActivity;
        _appDrawerIndicator = appDrawerIndicator;
    }

    public void initAppDrawer(AppDrawerController appDrawerController) {
        appDrawerController.setCallBack(this);
    }

    @Override
    public void callback(Boolean openingOrClosing, Boolean startOrEnd) {
        if (openingOrClosing) {
            if (startOrEnd) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    _homeActivity.getDesktop().setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.MIRROR));
                }
                _homeActivity.getAppDrawerController().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Tool.visibleViews(200, _appDrawerIndicator);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            Tool.invisibleViews(200, _homeActivity.getDesktop());
                        }
                        _homeActivity.updateDesktopIndicator(false);
                        _homeActivity.updateDock(false);
                        _homeActivity.updateSearchBar(false);
                        _homeActivity.getAppDrawerController().focusSearch();
                    }
                }, 100);
            }
        } else {
            if (startOrEnd) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    _homeActivity.getDesktop().setRenderEffect(null);
                }
                Tool.invisibleViews(200, _appDrawerIndicator);
                Tool.visibleViews(200, _homeActivity.getDesktop());
                _homeActivity.updateDesktopIndicator(true);
                _homeActivity.updateDock(true);
                _homeActivity.updateSearchBar(true);
            } else {
                if (!Setup.appSettings().getDrawerRememberPosition()) {
                    _homeActivity.getAppDrawerController().reset();
                }
            }
        }
    }
}
