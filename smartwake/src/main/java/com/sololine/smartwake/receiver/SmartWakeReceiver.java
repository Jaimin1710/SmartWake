package com.sololine.smartwake.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.ads.SmartWakeAdManager;
import com.sololine.smartwake.model.SmartWakeRemoteModel;
import com.sololine.smartwake.net.SmartWakeApiClient;
import com.sololine.smartwake.util.SmartWakeGuard;

public class SmartWakeReceiver extends BroadcastReceiver {

    private static final String TAG = "SmartWake";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "RECEIVER onReceive action: " + action);

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            if (SmartWakeConfig.isEnabled(context)) {
                // Safety Guard Check: Skip if call active, VoIP call ringing, or Alarm/Timer ringing
                if (SmartWakeGuard.shouldSkipSmartWake(context)) {
                    Log.d(TAG, "RECEIVER: Active Call, Ringtone, or Alarm detected. Skipping SmartWake.");
                    return;
                }

                Log.d(TAG, "RECEIVER: Screen-On detected! Triggering SmartWake.showNow(context)...");
                SmartWake.showNow(context);
            } else {
                Log.d(TAG, "RECEIVER: SmartWake is disabled in config. Ignoring ACTION_SCREEN_ON.");
            }
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            if (SmartWakeConfig.isEnabled(context)) {
                Log.d(TAG, "RECEIVER: Screen-Off detected. Preloading ad in background for next screen-on event...");
                String cachedJson = SmartWakeConfig.getLastConfigJson(context);
                if (cachedJson != null && !cachedJson.trim().isEmpty()) {
                    SmartWakeRemoteModel model = SmartWakeApiClient.parseConfigJson(cachedJson);
                    SmartWakeAdManager.preloadAds(context, model);
                }
            }
        }
    }
}
