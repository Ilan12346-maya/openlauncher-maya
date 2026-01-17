package com.benny.openlauncher.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.benny.openlauncher.R;
import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;

public class CrashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        Button restartButton = findViewById(R.id.restart_button);
        Button logButton = findViewById(R.id.log_button);

        final CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        if (config == null) {
            finish();
            return;
        }

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomActivityOnCrash.restartApplication(CrashActivity.this, config);
            }
        });

        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String report = CustomActivityOnCrash.getAllErrorDetailsFromIntent(CrashActivity.this, getIntent());
                
                com.afollestad.materialdialogs.MaterialDialog.Builder builder = new com.afollestad.materialdialogs.MaterialDialog.Builder(CrashActivity.this);
                builder.title("Crash Report")
                        .content(report)
                        .positiveText("Copy")
                        .negativeText("Close")
                        .onPositive(new com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@androidx.annotation.NonNull com.afollestad.materialdialogs.MaterialDialog dialog, @androidx.annotation.NonNull com.afollestad.materialdialogs.DialogAction which) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("Crash Report", report);
                                clipboard.setPrimaryClip(clip);
                                com.benny.openlauncher.util.Tool.toast(CrashActivity.this, "Copied to clipboard");
                            }
                        })
                        .show();
            }
        });
    }
}
