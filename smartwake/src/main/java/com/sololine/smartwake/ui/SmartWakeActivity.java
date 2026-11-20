package com.sololine.smartwake.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.sololine.smartwake.R;
import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.SmartWakeListener;

public class SmartWakeActivity extends Activity {

    private ObjectAnimator bounceAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupLockScreenFlags();
        super.onCreate(savedInstanceState);

        int customLayoutRes = SmartWakeConfig.getCustomLayoutResId(this);
        if (customLayoutRes != 0) {
            setContentView(customLayoutRes);
        } else {
            setContentView(R.layout.activity_smart_wake);
            setupDefaultUI();
        }

        View rootLayout = findViewById(R.id.smartwake_root_layout);
        if (rootLayout == null) {
            rootLayout = getWindow().getDecorView().findViewById(android.R.id.content);
        }

        int direction = SmartWakeConfig.getSwipeDirection(this);
        if (rootLayout != null) {
            rootLayout.setOnTouchListener(new SwipeDismissTouchListener(rootLayout, direction, new SwipeDismissTouchListener.OnDismissCallback() {
                @Override
                public void onDismiss(View view) {
                    onLockScreenDismissed();
                }
            }));
        }

        SmartWakeListener listener = SmartWake.getListener();
        if (listener != null) {
            listener.onScreenOn();
        }
    }

    private void setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
    }

    private void setupDefaultUI() {
        TextView titleTv = findViewById(R.id.smartwake_title);
        TextView subtitleTv = findViewById(R.id.smartwake_subtitle);
        ImageView iconIv = findViewById(R.id.smartwake_icon);
        View clockContainer = findViewById(R.id.smartwake_clock_container);
        View swipeIcon = findViewById(R.id.smartwake_swipe_icon);

        String customTitle = SmartWakeConfig.getTitle(this);
        if (customTitle != null && titleTv != null) {
            titleTv.setText(customTitle);
        }

        String customSubtitle = SmartWakeConfig.getSubtitle(this);
        if (customSubtitle != null && subtitleTv != null) {
            subtitleTv.setText(customSubtitle);
        }

        int iconRes = SmartWakeConfig.getIconResId(this);
        if (iconRes != 0 && iconIv != null) {
            iconIv.setImageResource(iconRes);
        }

        if (clockContainer != null) {
            boolean showClock = SmartWakeConfig.isShowClock(this);
            clockContainer.setVisibility(showClock ? View.VISIBLE : View.GONE);
        }

        if (swipeIcon != null) {
            bounceAnimator = ObjectAnimator.ofFloat(swipeIcon, "translationY", 0f, -16f, 0f);
            bounceAnimator.setDuration(1200);
            bounceAnimator.setRepeatCount(ValueAnimator.INFINITE);
            bounceAnimator.start();
        }
    }

    private void onLockScreenDismissed() {
        triggerHapticFeedback();

        SmartWakeListener listener = SmartWake.getListener();
        if (listener != null) {
            listener.onDismissed();
        }

        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    private void triggerHapticFeedback() {
        if (!SmartWakeConfig.isVibrationEnabled(this)) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    vibratorManager.getDefaultVibrator().vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                }
            } else {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(40);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        if (bounceAnimator != null) {
            bounceAnimator.cancel();
        }
        super.onDestroy();
    }
}
