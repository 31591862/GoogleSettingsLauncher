package com.example.googlesettingslauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String GMS_ACTIVITY =
            "com.google.android.gms.app.settings.GoogleSettingsIALink";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openGoogleSettings();
    }

    private void openGoogleSettings() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(GMS_PACKAGE, GMS_ACTIVITY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            // finish() alone leaves an empty task shell in the recents list;
            // finishAndRemoveTask() drops the task itself, matching excludeFromRecents.
            finishAndRemoveTask();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "无法打开 Google 服务入口：\n" + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG
            ).show();
            finishAndRemoveTask();
        }
    }
}
