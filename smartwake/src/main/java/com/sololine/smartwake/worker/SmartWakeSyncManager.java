package com.sololine.smartwake.worker;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SmartWakeSyncManager {

    private static final String TAG = "SmartWakeSyncManager";
    private static final String UNIQUE_WORK_NAME = "smartwake_background_sync_work";

    /**
     * Schedules background periodic sync for API config.
     * Note: intervalMinutes is in minutes. Android WorkManager minimum periodic interval is 15 minutes.
     */
    public static void schedulePeriodicSync(Context context, int intervalMinutes) {
        if (context == null) return;

        try {
            int minutes = Math.max(15, intervalMinutes);

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                    SmartWakeWorker.class,
                    minutes,
                    TimeUnit.MINUTES
            )
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    syncWorkRequest
            );

            Log.d(TAG, "Scheduled background sync every " + minutes + " minute(s).");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling WorkManager sync: " + e.getMessage());
        }
    }
}
