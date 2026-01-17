package com.benny.openlauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.benny.openlauncher.R;
import com.benny.openlauncher.util.Logger;

public class LogViewPreference extends Preference {
    private static TextView logTextView;

    public LogViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        logTextView = (TextView) holder.findViewById(R.id.log_view);
        if (logTextView != null) {
            logTextView.setText(Logger.getLogContent());
        }
    }

    public static void updateLog() {
        if (logTextView != null) {
            logTextView.setText(Logger.getLogContent());
        }
    }
}
