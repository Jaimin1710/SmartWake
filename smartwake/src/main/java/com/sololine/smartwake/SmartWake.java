package com.sololine.smartwake;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.sololine.smartwake.model.SmartWakeRemoteModel;
import com.sololine.smartwake.net.SmartWakeApiClient;

import com.sololine.smartwake.service.SmartWakeService;
import com.sololine.smartwake.ui.SmartWakeActivity;
import com.sololine.smartwake.worker.SmartWakeSyncManager;

public class SmartWake {

    public static final String TAG = "SmartWake";
    private static SmartWakeListener globalListener;

    /**
     * Quick initialization of SmartWake SDK with Server API URL for 20+ Apps.
     */
    public static void init(Context context, String apiUrl) {
        Log.d(TAG, "INIT called with apiUrl: " + apiUrl);
        new Builder(context).setApiUrl(apiUrl).start();
    }

    /**
     * Quick initialization of SmartWake SDK with default settings.
     */
    public static void init(Context context) {
        Log.d(TAG, "INIT called with default settings.");
        new Builder(context).start();
    }

    /**
     * Obtains a Builder to configure SmartWake options.
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
            Log.d(TAG, "START: SmartWake background service launched successfully.");
        } catch (Exception e) {
            Log.e(TAG, "START ERROR: Failed to start SmartWake service", e);
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
            Log.d(TAG, "STOP: SmartWake background service stopped.");
        } catch (Exception e) {
            Log.e(TAG, "STOP ERROR: Failed to stop SmartWake service", e);
        }
    }

    /**
     * Returns true if SmartWake monitoring is enabled.
     */
    public static boolean isEnabled(Context context) {
        return context != null && SmartWakeConfig.isEnabled(context.getApplicationContext());
    }

    /**
     * Launches the LockScreen Activity directly on screen-on.
     */
    public static void launchLockScreen(Context context) {
        if (context == null) return;

        Log.d(TAG, "LAUNCH LOCKSCREEN: Triggering SmartWakeActivity...");
        Context appContext = context.getApplicationContext();

        try {
            Intent intent = new Intent(appContext, SmartWakeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            appContext.startActivity(intent);
            Log.d(TAG, "LAUNCH LOCKSCREEN: Direct startActivity executed.");
        } catch (Exception e) {
            Log.e(TAG, "LAUNCH LOCKSCREEN ERROR: Failed to launch SmartWakeActivity", e);
        }
    }

    /**
     * Manually triggers and shows the SmartWake lock screen activity immediately.
     */
    public static void showNow(Context context) {
        Log.d(TAG, "SHOW NOW called.");
        launchLockScreen(context);
    }

    /**
     * Checks whether SYSTEM_ALERT_WINDOW (Display over other apps) permission is granted.
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * Opens system settings to request Overlay ("Display over other apps") permission.
     */
    public static void requestOverlayPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, requestCode);
        }
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
        private String apiUrl;
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

        public Builder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
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
         * Saves configuration, fetches initial server config, and starts WorkManager background sync.
         */
        public void start() {
            Log.d(TAG, "BUILDER START called. ApiUrl: " + apiUrl);
            if (apiUrl != null && !apiUrl.trim().isEmpty()) {
                SmartWakeConfig.setApiUrl(context, apiUrl);

                String cachedJson = SmartWakeConfig.getLastConfigJson(context);
                SmartWakeRemoteModel cachedModel = SmartWakeApiClient.parseConfigJson(cachedJson);
                int intervalMinutes = cachedModel.workerTimeHours;

                if (SmartWakeConfig.shouldFetchApi(context, intervalMinutes)) {
                    new Thread(() -> {
                        Log.d(TAG, "BUILDER THREAD: Fetching initial config from " + apiUrl);
                        SmartWakeRemoteModel model = SmartWakeApiClient.fetchConfigSync(context, apiUrl);
                        if (model != null) {
                            SmartWakeSyncManager.schedulePeriodicSync(context, model.workerTimeHours);
                            com.sololine.smartwake.ads.SmartWakeAdManager.preloadAds(context, model);
                        }
                    }).start();
                } else {
                    Log.d(TAG, "BUILDER: Skipping initial API fetch. Interval (" + intervalMinutes + " minutes) not elapsed.");
                    SmartWakeSyncManager.schedulePeriodicSync(context, intervalMinutes);
                    com.sololine.smartwake.ads.SmartWakeAdManager.preloadAds(context, cachedModel);
                }
            }

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
