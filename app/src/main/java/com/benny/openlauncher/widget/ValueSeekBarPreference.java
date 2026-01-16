package com.benny.openlauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.SeekBarPreference;
import androidx.preference.PreferenceViewHolder;
import android.widget.TextView;

public class ValueSeekBarPreference extends SeekBarPreference {
    private String mOriginalTitle;

    public ValueSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        saveOriginalTitle();
    }

    public ValueSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        saveOriginalTitle();
    }

    public ValueSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        saveOriginalTitle();
    }

    public ValueSeekBarPreference(Context context) {
        super(context);
        saveOriginalTitle();
    }

    private void saveOriginalTitle() {
        CharSequence title = getTitle();
        if (title != null) {
            String titleStr = title.toString();
            int index = titleStr.indexOf(" (");
            if (index != -1) {
                mOriginalTitle = titleStr.substring(0, index);
            } else {
                mOriginalTitle = titleStr;
            }
        } else {
            mOriginalTitle = "";
        }
        setShowSeekBarValue(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        updateTitleWithValue();
        
        android.widget.SeekBar seekBar = (android.widget.SeekBar) holder.findViewById(androidx.preference.R.id.seekbar);
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        // We need to update the title text directly in the view to avoid 
                        // full re-bind which might interrupt sliding
                        android.widget.TextView titleView = (android.widget.TextView) holder.findViewById(android.R.id.title);
                        if (titleView != null) {
                            titleView.setText(mOriginalTitle + " ( " + (progress + getMin()) + " )");
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    setValue(seekBar.getProgress() + getMin());
                }
            });
        }

        android.widget.TextView valueView = (android.widget.TextView) holder.findViewById(androidx.preference.R.id.seekbar_value);
        if (valueView != null) {
            valueView.setVisibility(android.view.View.GONE);
        }
        valueView = (TextView) holder.findViewById(com.benny.openlauncher.R.id.seekbar_value);
        if (valueView != null) {
            valueView.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        updateTitleWithValue();
    }

    private void updateTitleWithValue() {
        if (mOriginalTitle == null || mOriginalTitle.isEmpty()) {
            saveOriginalTitle();
        }
        setTitle(mOriginalTitle + " ( " + getValue() + " )");
    }
}