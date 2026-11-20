package com.sololine.smartwake.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeConfig;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed.");
            if (SmartWakeConfig.isEnabled(context)) {
                Log.d(TAG, "SmartWake is enabled. Auto-starting SmartWake Service...");
                SmartWake.start(context);
            }
        }
    }
}
