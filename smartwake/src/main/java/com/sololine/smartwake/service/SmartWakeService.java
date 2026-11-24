package com.sololine.smartwake.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeListener;
import com.sololine.smartwake.receiver.SmartWakeReceiver;

public class SmartWakeService extends Service {

    private static final String TAG = "SmartWake";

    private SmartWakeReceiver screenReceiver;
    private boolean isRegistered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SERVICE onCreate executed.");

        registerScreenReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE onStartCommand executed.");

        SmartWakeListener listener = SmartWake.getListener();
        if (listener != null) {
            listener.onServiceStarted();
        }

        return START_STICKY;
    }

    private void registerScreenReceiver() {
        if (!isRegistered) {
            screenReceiver = new SmartWakeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
            isRegistered = true;
            Log.d(TAG, "SERVICE: Dynamic ScreenReceiver registered.");
        }
    }

    private void unregisterScreenReceiver() {
        if (isRegistered && screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                Log.e(TAG, "SERVICE: Error unregistering receiver", e);
            }
            isRegistered = false;
            Log.d(TAG, "SERVICE: Dynamic ScreenReceiver unregistered.");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterScreenReceiver();
        SmartWakeListener listener = SmartWake.getListener();
        if (listener != null) {
            listener.onServiceStopped();
        }
        Log.d(TAG, "SERVICE onDestroy executed.");
        super.onDestroy();
    }
}
