package com.benny.openlauncher.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.benny.openlauncher.R;

public class BackupLogActivity extends Activity {
    public static String EXTRA_LOG_TEXT = "EXTRA_LOG_TEXT";
    public static String EXTRA_RESTART_AFTER = "EXTRA_RESTART_AFTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_log);

        TextView logTextView = findViewById(R.id.log_text);
        Button closeButton = findViewById(R.id.close_button);

        String logText = getIntent().getStringExtra(EXTRA_LOG_TEXT);
        final boolean restartAfter = getIntent().getBooleanExtra(EXTRA_RESTART_AFTER, false);
        if (logText != null) {
            logTextView.setText(logText);
        }

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (restartAfter) {
                    Intent intent = new Intent(BackupLogActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                finish();
            }
        });
    }
}
