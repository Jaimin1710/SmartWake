package com.sololine.smartwake;

import android.content.Context;
import android.content.SharedPreferences;

public class SmartWakeConfig {

    private static final String PREF_NAME = "smartwake_prefs";
    private static final String KEY_ENABLED = "key_enabled";
    private static final String KEY_TITLE = "key_title";
    private static final String KEY_SUBTITLE = "key_subtitle";
    private static final String KEY_ICON_RES = "key_icon_res";
    private static final String KEY_CUSTOM_LAYOUT_RES = "key_custom_layout_res";
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
        return getPrefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static String getTitle(Context context) {
        return getPrefs(context).getString(KEY_TITLE, null);
    }

    public static void setTitle(Context context, String title) {
        getPrefs(context).edit().putString(KEY_TITLE, title).apply();
    }

    public static String getSubtitle(Context context) {
        return getPrefs(context).getString(KEY_SUBTITLE, null);
    }

    public static void setSubtitle(Context context, String subtitle) {
        getPrefs(context).edit().putString(KEY_SUBTITLE, subtitle).apply();
    }

    public static int getIconResId(Context context) {
        return getPrefs(context).getInt(KEY_ICON_RES, 0);
    }

    public static void setIconResId(Context context, int resId) {
        getPrefs(context).edit().putInt(KEY_ICON_RES, resId).apply();
    }

    public static int getCustomLayoutResId(Context context) {
        return getPrefs(context).getInt(KEY_CUSTOM_LAYOUT_RES, 0);
    }

    public static void setCustomLayoutResId(Context context, int resId) {
        getPrefs(context).edit().putInt(KEY_CUSTOM_LAYOUT_RES, resId).apply();
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
