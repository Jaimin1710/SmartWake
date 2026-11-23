package com.sololine.smartwake.util;

import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

public class SmartWakeGuard {

    private static final String TAG = "SmartWakeGuard";

    /**
     * Returns true if SmartWake lockscreen SHOULD BE SKIPPED (e.g. active call, ringing alarm, or VoIP call).
     */
    public static boolean shouldSkipSmartWake(Context context) {
        if (context == null) return false;

        // 1. Check Telephony Call State (Phone Call Ringing or Active Offhook)
        if (isTelephonyCallActive(context)) {
            Log.d(TAG, "GUARD: Telephony call active or ringing. Skipping SmartWake.");
            return true;
        }

        // 2. Check AudioManager Mode (Phone Call, Ringtone, or VoIP Call like WhatsApp/Telegram)
        if (isAudioModeInCall(context)) {
            Log.d(TAG, "GUARD: Audio mode is IN_CALL, IN_COMMUNICATION or RINGTONE. Skipping SmartWake.");
            return true;
        }

        // 3. Check Foreground Active Alarm / Timer Activity
        if (isAlarmOrTimerActive(context)) {
            Log.d(TAG, "GUARD: Active Alarm or Timer activity detected. Skipping SmartWake.");
            return true;
        }

        return false;
    }

    private static boolean isTelephonyCallActive(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                int state = tm.getCallState();
                return state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isAudioModeInCall(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int mode = am.getMode();
                return mode == AudioManager.MODE_IN_CALL
                        || mode == AudioManager.MODE_IN_COMMUNICATION
                        || mode == AudioManager.MODE_RINGTONE;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isAlarmOrTimerActive(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            String pkg = process.processName != null ? process.processName.toLowerCase() : "";
                            if (pkg.contains("clock") || pkg.contains("alarm") || pkg.contains("timer") || pkg.contains("deskclock")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
