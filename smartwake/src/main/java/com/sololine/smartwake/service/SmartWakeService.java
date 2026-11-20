package com.sololine.smartwake.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.sololine.smartwake.R;
import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeListener;
import com.sololine.smartwake.receiver.SmartWakeReceiver;

public class SmartWakeService extends Service {

    private static final String TAG = "SmartWakeService";
    private static final String CHANNEL_ID = "smartwake_service_channel";
    private static final int NOTIFICATION_ID = 9901;

    private SmartWakeReceiver screenReceiver;
    private boolean isRegistered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SmartWakeService onCreate");

        registerScreenReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SmartWakeService onStartCommand");

        // Try foreground notification safely if permitted, otherwise run as background service seamlessly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                createNotificationChannel();
                Notification notification = createNotification();
                startForeground(NOTIFICATION_ID, notification);
            } catch (Exception e) {
                Log.w(TAG, "Could not start foreground notification (Running in standard background mode): " + e.getMessage());
            }
        }

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
            Log.d(TAG, "Screen-On Receiver registered successfully.");
        }
    }

    private void unregisterScreenReceiver() {
        if (isRegistered && screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
            isRegistered = false;
            Log.d(TAG, "Screen-On Receiver unregistered.");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SmartWake Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps SmartWake screen-on monitoring active.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = null;
        if (launchIntent != null) {
            int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, pFlags);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.smartwake_notification_title))
                .setContentText(getString(R.string.smartwake_notification_desc))
                .setSmallIcon(R.drawable.ic_smartwake_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
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
        Log.d(TAG, "SmartWakeService onDestroy");
        super.onDestroy();
    }
}
