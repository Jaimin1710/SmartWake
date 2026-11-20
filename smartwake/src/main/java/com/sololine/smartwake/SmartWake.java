package com.sololine.smartwake;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.sololine.smartwake.service.SmartWakeService;
import com.sololine.smartwake.ui.SmartWakeActivity;

public class SmartWake {

    private static final String TAG = "SmartWake";
    private static SmartWakeListener globalListener;

    /**
     * Quick 1-line initialization of SmartWake SDK.
     * Enables SmartWake with default settings and starts background monitoring.
     *
     * @param context App or Activity Context
     */
    public static void init(Context context) {
        new Builder(context).start();
    }

    /**
     * Obtains a Builder to configure SmartWake options.
     *
     * @param context Context
     * @return Builder instance
     */
    public static Builder builder(Context context) {
        return new Builder(context);
    }

    /**
     * Starts the SmartWake background monitoring service.
     */
    public static void start(Context context) {
        if (context == null) return;

        Context appContext = context.getApplicationContext();
        SmartWakeConfig.setEnabled(appContext, true);

        Intent serviceIntent = new Intent(appContext, SmartWakeService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent);
            } else {
                appContext.startService(serviceIntent);
            }
            Log.d(TAG, "SmartWake background service started successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start SmartWake service", e);
        }
    }

    /**
     * Stops the SmartWake background service and disables monitoring.
     */
    public static void stop(Context context) {
        if (context == null) return;

        Context appContext = context.getApplicationContext();
        SmartWakeConfig.setEnabled(appContext, false);

        Intent serviceIntent = new Intent(appContext, SmartWakeService.class);
        try {
            appContext.stopService(serviceIntent);
            Log.d(TAG, "SmartWake background service stopped.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop SmartWake service", e);
        }
    }

    /**
     * Returns true if SmartWake monitoring is enabled.
     */
    public static boolean isEnabled(Context context) {
        return context != null && SmartWakeConfig.isEnabled(context.getApplicationContext());
    }

    /**
     * Manually triggers and shows the SmartWake lock screen activity immediately.
     */
    public static void showNow(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, SmartWakeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * Sets a global listener for SmartWake events.
     */
    public static void setListener(SmartWakeListener listener) {
        globalListener = listener;
    }

    public static SmartWakeListener getListener() {
        return globalListener;
    }

    /**
     * Builder class for customized SmartWake integration across 20+ apps.
     */
    public static class Builder {
        private final Context context;
        private String title;
        private String subtitle;
        private int iconResId;
        private int customLayoutResId;
        private int swipeDirection = SmartWakeConfig.SWIPE_UP;
        private boolean showClock = true;
        private boolean vibrationEnabled = true;
        private SmartWakeListener listener;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setSubtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            this.iconResId = iconResId;
            return this;
        }

        public Builder setCustomLayoutResId(int customLayoutResId) {
            this.customLayoutResId = customLayoutResId;
            return this;
        }

        public Builder setSwipeDirection(int swipeDirection) {
            this.swipeDirection = swipeDirection;
            return this;
        }

        public Builder setShowClock(boolean showClock) {
            this.showClock = showClock;
            return this;
        }

        public Builder setVibrationEnabled(boolean vibrationEnabled) {
            this.vibrationEnabled = vibrationEnabled;
            return this;
        }

        public Builder setListener(SmartWakeListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Saves configuration and starts the background service.
         */
        public void start() {
            if (title != null) SmartWakeConfig.setTitle(context, title);
            if (subtitle != null) SmartWakeConfig.setSubtitle(context, subtitle);
            if (iconResId != 0) SmartWakeConfig.setIconResId(context, iconResId);
            if (customLayoutResId != 0) SmartWakeConfig.setCustomLayoutResId(context, customLayoutResId);
            SmartWakeConfig.setSwipeDirection(context, swipeDirection);
            SmartWakeConfig.setShowClock(context, showClock);
            SmartWakeConfig.setVibrationEnabled(context, vibrationEnabled);

            if (listener != null) {
                SmartWake.setListener(listener);
            }

            SmartWake.start(context);
        }
    }
}
