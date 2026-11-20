package com.sololine.smartwake;

/**
 * Listener interface for SmartWake events.
 */
public interface SmartWakeListener {
    /**
     * Called when screen turns on and SmartWake activity is launched.
     */
    void onScreenOn();

    /**
     * Called when user dismisses the SmartWake lock screen (e.g. via swipe).
     */
    void onDismissed();

    /**
     * Called when SmartWake background monitoring service starts.
     */
    void onServiceStarted();

    /**
     * Called when SmartWake background monitoring service stops.
     */
    void onServiceStopped();
}
