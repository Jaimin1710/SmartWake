package com.sololine.smartwake.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeConfig;

public class SmartWakeReceiver extends BroadcastReceiver {

    private static final String TAG = "SmartWakeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            if (SmartWakeConfig.isEnabled(context)) {
                Log.d(TAG, "Screen-On detected. Triggering SmartWake LockScreen Activity.");
                SmartWake.showNow(context);
            }
        }
    }
}
