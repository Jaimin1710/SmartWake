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

    public static void schedulePeriodicSync(Context context, int intervalHours) {
        if (context == null) return;

        try {
            int hours = Math.max(1, intervalHours);

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                    SmartWakeWorker.class,
                    hours,
                    TimeUnit.HOURS
            )
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    syncWorkRequest
            );

            Log.d(TAG, "Scheduled background sync every " + hours + " hour(s).");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling WorkManager sync: " + e.getMessage());
        }
    }
}
