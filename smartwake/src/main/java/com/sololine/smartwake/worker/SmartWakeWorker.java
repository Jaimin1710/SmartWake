package com.sololine.smartwake.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.model.SmartWakeRemoteModel;
import com.sololine.smartwake.net.SmartWakeApiClient;

public class SmartWakeWorker extends Worker {

    private static final String TAG = "SmartWakeWorker";

    public SmartWakeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SmartWakeWorker doWork started in background.");

        Context context = getApplicationContext();
        String apiUrl = SmartWakeConfig.getApiUrl(context);

        SmartWakeRemoteModel model = SmartWakeApiClient.fetchConfigSync(context, apiUrl);
        Log.d(TAG, "Worker fetched config. Next sync hours: " + model.workerTimeHours + ", showLockscreen: " + model.showLockscreen);

        if (model.workerTimeHours > 0) {
            SmartWakeSyncManager.schedulePeriodicSync(context, model.workerTimeHours);
        }

        return Result.success();
    }
}
