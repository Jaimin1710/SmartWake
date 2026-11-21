package com.sololine.smartwake.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.sololine.smartwake.R;
import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.SmartWakeListener;
import com.sololine.smartwake.ads.SmartWakeAdManager;
import com.sololine.smartwake.analytics.SmartWakeAnalytics;
import com.sololine.smartwake.content.WeatherManager;
import com.sololine.smartwake.model.SmartWakeRemoteModel;
import com.sololine.smartwake.net.SmartWakeApiClient;

import java.util.ArrayList;
import java.util.List;

public class SmartWakeActivity extends Activity {

    private static final String TAG = "SmartWake";

    private ObjectAnimator bounceAnimator;
    private BroadcastReceiver unlockReceiver;
    private boolean isTorchOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupLockScreenFlags();
        super.onCreate(savedInstanceState);

        SmartWakeConfig.incrementScreenOnCount(this);

        setContentView(R.layout.activity_smart_wake);

        String apiUrl = SmartWakeConfig.getApiUrl(this);
        String jsonConfig = SmartWakeConfig.getLastConfigJson(this);

        Log.d(TAG, "ACTIVITY onCreate. ApiUrl: " + apiUrl + ", jsonConfig length: " + (jsonConfig != null ? jsonConfig.length() : 0));

        // Render immediately using last saved config
        if (jsonConfig != null && !jsonConfig.trim().isEmpty()) {
            SmartWakeRemoteModel model = SmartWakeApiClient.parseConfigJson(jsonConfig);
            setupServerUI(model);
        } else {
            setupServerUI(new SmartWakeRemoteModel());
        }

        // Fetch fresh config from server asynchronously
        if (apiUrl != null && !apiUrl.trim().isEmpty()) {
            new Thread(() -> {
                SmartWakeRemoteModel freshModel = SmartWakeApiClient.fetchConfigSync(this, apiUrl);
                runOnUiThread(() -> {
                    if (freshModel != null && freshModel.showLockscreen) {
                        setupServerUI(freshModel);
                    }
                });
            }).start();
        }

        registerUnlockReceiver();

        View rootLayout = findViewById(R.id.smartwake_root_layout);
        if (rootLayout != null) {
            rootLayout.setOnTouchListener(new SwipeDismissTouchListener(rootLayout, SmartWakeConfig.SWIPE_UP, view -> onLockScreenDismissed()));
        }

        SmartWakeAnalytics.logEvent(this, SmartWakeAnalytics.EVENT_SHOW, null);

        SmartWakeListener listener = SmartWake.getListener();
        if (listener != null) {
            listener.onScreenOn();
        }
    }

    private void setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
    }

    private void setupServerUI(SmartWakeRemoteModel model) {
        if (model == null) return;

        ViewPager2 viewPager = findViewById(R.id.smartwake_wallpaper_viewpager);
        View clockContainer = findViewById(R.id.smartwake_clock_container);
        TextView tvWeather = findViewById(R.id.smartwake_tv_weather);
        TextView tvQuote = findViewById(R.id.smartwake_tv_quote);
        TextView tvNewsSports = findViewById(R.id.smartwake_tv_news_sports);
        ViewGroup adContainer = findViewById(R.id.smartwake_ad_container);
        View btnReadMore = findViewById(R.id.smartwake_btn_read_more);
        View btnFlashlight = findViewById(R.id.smartwake_btn_flashlight);
        View btnCamera = findViewById(R.id.smartwake_btn_camera);
        View swipeHintContainer = findViewById(R.id.smartwake_swipe_hint_container);
        View swipeIcon = findViewById(R.id.smartwake_swipe_icon);

        boolean isFullScreen = "FULL_SCREEN".equalsIgnoreCase(model.displayMode);
        Log.d(TAG, "setupServerUI displayMode: " + model.displayMode + ", isFullScreen: " + isFullScreen);

        // 1. Display Mode Rules
        if (clockContainer != null) clockContainer.setVisibility(View.VISIBLE);

        if (isFullScreen) {
            if (viewPager != null) {
                viewPager.setVisibility(View.VISIBLE);
                List<String> urls = model.wallpapers;
                if (urls == null || urls.isEmpty()) {
                    urls = new ArrayList<>();
                    urls.add("https://images.unsplash.com/photo-1506744038136-46273834b3fb");
                    urls.add("https://images.unsplash.com/photo-1511884642898-4c92249e20b6");
                }
                viewPager.setAdapter(new WallpaperPagerAdapter(this, urls));
            }
        } else if (viewPager != null) {
            viewPager.setVisibility(View.GONE);
        }

        // 2. Flashlight & Camera Shortcuts
        if (btnFlashlight != null) {
            btnFlashlight.setOnClickListener(v -> toggleFlashlight());
        }

        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> launchCamera());
        }

        // 3. Weather
        if (model.showWeather && tvWeather != null) {
            if (model.weatherTemp != null && !model.weatherTemp.isEmpty()) {
                tvWeather.setVisibility(View.VISIBLE);
                tvWeather.setText(" • " + model.weatherCity + " " + model.weatherTemp);
            } else {
                WeatherManager.fetchWeatherAsync((temp, condition) -> runOnUiThread(() -> {
                    if (tvWeather != null) {
                        tvWeather.setVisibility(View.VISIBLE);
                        tvWeather.setText(" • " + temp + " (" + condition + ")");
                    }
                }));
            }
        } else if (tvWeather != null) {
            tvWeather.setVisibility(View.GONE);
        }

        // 4. Quotes
        if (model.showQuotes && model.quotes != null && !model.quotes.isEmpty() && tvQuote != null) {
            tvQuote.setVisibility(View.VISIBLE);
            int count = SmartWakeConfig.getScreenOnCount(this);
            String quote = model.quotes.get(count % model.quotes.size());
            tvQuote.setText("“" + quote + "”");
        }

        // 5. News / Sports
        if (tvNewsSports != null) {
            StringBuilder sb = new StringBuilder();
            if (model.showSports && model.sportsCricket != null && !model.sportsCricket.isEmpty()) {
                sb.append("🏏 ").append(model.sportsCricket);
            } else if (model.showNews && model.news != null && !model.news.isEmpty()) {
                int count = SmartWakeConfig.getScreenOnCount(this);
                sb.append("📰 ").append(model.news.get(count % model.news.size()));
            }

            if (sb.length() > 0) {
                tvNewsSports.setVisibility(View.VISIBLE);
                tvNewsSports.setText(sb.toString());
            }
        }

        // 6. Read More Redirect Button
        if (model.readMoreLinks != null && !model.readMoreLinks.isEmpty() && btnReadMore != null) {
            btnReadMore.setVisibility(View.VISIBLE);
            btnReadMore.setOnClickListener(v -> {
                SmartWakeAnalytics.logEvent(this, SmartWakeAnalytics.EVENT_READ_MORE, null);
                openReadMoreUrl(model.readMoreLinks);
                onLockScreenDismissed();
            });
        }

        // 7. Native Ad Engine
        if (model.showAd && adContainer != null) {
            adContainer.setVisibility(View.VISIBLE);
            SmartWakeAdManager.loadAndDisplayAd(this, adContainer, model, new SmartWakeAdManager.AdLoadCallback() {
                @Override
                public void onAdLoaded(View adView) {
                    if (adContainer != null) adContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedAll() {
                    if (adContainer != null) adContainer.setVisibility(View.GONE);
                }

                @Override
                public void onAdClicked() {
                    SmartWakeAnalytics.logEvent(SmartWakeActivity.this, SmartWakeAnalytics.EVENT_AD_CLICK, null);
                    onLockScreenDismissed();
                }
            });
        }

        // 8. Swipe Hint Animation
        if (swipeIcon != null) {
            bounceAnimator = ObjectAnimator.ofFloat(swipeIcon, "translationY", 0f, -14f, 0f);
            bounceAnimator.setDuration(1200);
            bounceAnimator.setRepeatCount(ValueAnimator.INFINITE);
            bounceAnimator.start();
        }
    }

    private void toggleFlashlight() {
        try {
            triggerHapticFeedback();
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                String cameraId = cm.getCameraIdList()[0];
                isTorchOn = !isTorchOn;
                cm.setTorchMode(cameraId, isTorchOn);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flashlight: " + e.getMessage());
        }
    }

    private void launchCamera() {
        try {
            triggerHapticFeedback();
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            onLockScreenDismissed();
        } catch (Exception e) {
            Log.e(TAG, "Error launching camera: " + e.getMessage());
        }
    }

    private void openReadMoreUrl(List<String> links) {
        if (links == null || links.isEmpty()) return;

        int index = SmartWakeConfig.getReadMoreIndex(this);
        String url = links.get(index % links.size());
        SmartWakeConfig.incrementReadMoreIndex(this);

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching Read More URL: " + e.getMessage());
        }
    }

    private void registerUnlockReceiver() {
        unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                    Log.d(TAG, "Phone unlocked by user (ACTION_USER_PRESENT). Dismissing Activity.");
                    onLockScreenDismissed();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(unlockReceiver, filter);
        }
    }

    private void unregisterUnlockReceiver() {
        if (unlockReceiver != null) {
            try {
                unregisterReceiver(unlockReceiver);
            } catch (Exception ignored) {
            }
            unlockReceiver = null;
        }
    }

    private void onLockScreenDismissed() {
        triggerHapticFeedback();
        SmartWakeAnalytics.logEvent(this, SmartWakeAnalytics.EVENT_DISMISS, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }

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
        unregisterUnlockReceiver();
        super.onDestroy();
    }
}
