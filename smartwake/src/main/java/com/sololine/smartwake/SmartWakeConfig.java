package com.sololine.smartwake;

import android.content.Context;
import android.content.SharedPreferences;

public class SmartWakeConfig {

    private static final String PREF_NAME = "smartwake_prefs";
    private static final String KEY_ENABLED = "key_enabled";
    private static final String KEY_API_URL = "key_api_url";
    private static final String KEY_LAST_CONFIG_JSON = "key_last_config_json";
    private static final String KEY_SCREEN_ON_COUNT = "key_screen_on_count";
    private static final String KEY_READ_MORE_INDEX = "key_read_more_index";
    private static final String KEY_SWIPE_DIRECTION = "key_swipe_direction";
    private static final String KEY_SHOW_CLOCK = "key_show_clock";
    private static final String KEY_VIBRATION = "key_vibration";

    public static final int SWIPE_UP = 1;
    public static final int SWIPE_DOWN = 2;
    public static final int SWIPE_LEFT = 3;
    public static final int SWIPE_RIGHT = 4;

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static String getApiUrl(Context context) {
        return getPrefs(context).getString(KEY_API_URL, "");
    }

    public static void setApiUrl(Context context, String url) {
        getPrefs(context).edit().putString(KEY_API_URL, url).apply();
    }

    public static String getLastConfigJson(Context context) {
        return getPrefs(context).getString(KEY_LAST_CONFIG_JSON, "");
    }

    public static void setLastConfigJson(Context context, String json) {
        getPrefs(context).edit().putString(KEY_LAST_CONFIG_JSON, json).apply();
    }

    public static int getScreenOnCount(Context context) {
        return getPrefs(context).getInt(KEY_SCREEN_ON_COUNT, 0);
    }

    public static void incrementScreenOnCount(Context context) {
        int count = getScreenOnCount(context) + 1;
        getPrefs(context).edit().putInt(KEY_SCREEN_ON_COUNT, count).apply();
    }

    public static int getReadMoreIndex(Context context) {
        return getPrefs(context).getInt(KEY_READ_MORE_INDEX, 0);
    }

    public static void incrementReadMoreIndex(Context context) {
        int index = getReadMoreIndex(context) + 1;
        getPrefs(context).edit().putInt(KEY_READ_MORE_INDEX, index).apply();
    }

    public static int getSwipeDirection(Context context) {
        return getPrefs(context).getInt(KEY_SWIPE_DIRECTION, SWIPE_UP);
    }

    public static void setSwipeDirection(Context context, int direction) {
        getPrefs(context).edit().putInt(KEY_SWIPE_DIRECTION, direction).apply();
    }

    public static boolean isShowClock(Context context) {
        return getPrefs(context).getBoolean(KEY_SHOW_CLOCK, true);
    }

    public static void setShowClock(Context context, boolean show) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_CLOCK, show).apply();
    }

    public static boolean isVibrationEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_VIBRATION, true);
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION, enabled).apply();
    }
}
