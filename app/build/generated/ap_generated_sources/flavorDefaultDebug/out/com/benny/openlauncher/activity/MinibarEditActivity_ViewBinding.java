// Generated code from Butter Knife. Do not modify!
package com.benny.openlauncher.activity;

import android.view.View;
import androidx.annotation.CallSuper;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.benny.openlauncher.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class MinibarEditActivity_ViewBinding implements Unbinder {
  private MinibarEditActivity target;

  @UiThread
  public MinibarEditActivity_ViewBinding(MinibarEditActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public MinibarEditActivity_ViewBinding(MinibarEditActivity target, View source) {
    this.target = target;

    target._toolbar = Utils.findRequiredViewAsType(source, R.id.toolbar, "field '_toolbar'", Toolbar.class);
    target._enableSwitch = Utils.findRequiredViewAsType(source, R.id.enableSwitch, "field '_enableSwitch'", SwitchCompat.class);
    target._recyclerView = Utils.findRequiredViewAsType(source, R.id.recyclerView, "field '_recyclerView'", RecyclerView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    MinibarEditActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target._toolbar = null;
    target._enableSwitch = null;
    target._recyclerView = null;
  }
}
