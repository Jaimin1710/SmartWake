package com.sololine.smartwake.analytics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;

public class SmartWakeAnalytics {

    private static final String TAG = "SmartWakeAnalytics";

    // 4 Core Firebase Events (All starting with smartwake_)
    public static final String EVENT_SHOW = "smartwake_show";
    public static final String EVENT_DISMISS = "smartwake_dismiss";
    public static final String EVENT_AD_SHOW = "smartwake_ad_show";
    public static final String EVENT_AD_CLICK = "smartwake_ad_click";

    public static void logEvent(Context context, String eventName, Bundle params) {
        if (context == null || eventName == null) return;

        try {
            Class<?> firebaseAnalyticsClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics");
            Method getInstanceMethod = firebaseAnalyticsClass.getMethod("getInstance", Context.class);
            Object analyticsObj = getInstanceMethod.invoke(null, context.getApplicationContext());

            if (analyticsObj != null) {
                Method logEventMethod = firebaseAnalyticsClass.getMethod("logEvent", String.class, Bundle.class);
                logEventMethod.invoke(analyticsObj, eventName, params != null ? params : new Bundle());
                Log.d(TAG, "Logged Firebase event: " + eventName + " params: " + (params != null ? params.toString() : "{}"));
            }
        } catch (Throwable e) {
            Log.d(TAG, "Firebase Analytics not present or error logging event: " + eventName);
        }
    }

    public static void logAdShow(Context context, String adType) {
        Bundle bundle = new Bundle();
        bundle.putString("ad_type", adType != null ? adType : "unknown");
        logEvent(context, EVENT_AD_SHOW, bundle);
    }

    public static void logAdClick(Context context, String adType) {
        Bundle bundle = new Bundle();
        bundle.putString("ad_type", adType != null ? adType : "unknown");
        logEvent(context, EVENT_AD_CLICK, bundle);
    }
}
