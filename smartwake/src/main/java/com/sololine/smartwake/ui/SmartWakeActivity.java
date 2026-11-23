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
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
    private Handler keyguardHandler = new Handler(Looper.getMainLooper());
    private Runnable keyguardCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupLockScreenFlags();
        hideSystemNavigationBar();
        super.onCreate(savedInstanceState);

        SmartWakeConfig.incrementScreenOnCount(this);

        setContentView(R.layout.activity_smart_wake);

        String apiUrl = SmartWakeConfig.getApiUrl(this);
        String jsonConfig = SmartWakeConfig.getLastConfigJson(this);

        Log.d(TAG, "ACTIVITY onCreate. ApiUrl: " + apiUrl + ", jsonConfig length: " + (jsonConfig != null ? jsonConfig.length() : 0));

        // Render immediately using last saved config
        SmartWakeRemoteModel cachedModel;
        if (jsonConfig != null && !jsonConfig.trim().isEmpty()) {
            cachedModel = SmartWakeApiClient.parseConfigJson(jsonConfig);
        } else {
            cachedModel = new SmartWakeRemoteModel();
        }
        setupServerUI(cachedModel);

        // Fetch fresh config from server ONLY IF the interval (in minutes) has elapsed
        if (apiUrl != null && !apiUrl.trim().isEmpty()) {
            int intervalMinutes = cachedModel.workerTimeHours;
            if (SmartWakeConfig.shouldFetchApi(this, intervalMinutes)) {
                Log.d(TAG, "ACTIVITY: Config interval elapsed (" + intervalMinutes + " minutes). Fetching fresh API config...");
                new Thread(() -> {
                    SmartWakeRemoteModel freshModel = SmartWakeApiClient.fetchConfigSync(this, apiUrl);
                    runOnUiThread(() -> {
                        if (freshModel != null && freshModel.showLockscreen) {
                            setupServerUI(freshModel);
                        }
                    });
                }).start();
            } else {
                Log.d(TAG, "ACTIVITY: Skipping API call on screen-on. Config interval (" + intervalMinutes + " minutes) has not elapsed yet.");
            }
        }

        registerUnlockReceiver();
        startKeyguardUnlockMonitor();

        View rootLayout = findViewById(R.id.smartwake_root_layout);
        if (rootLayout != null) {
            rootLayout.setOnTouchListener(new SwipeDismissTouchListener(rootLayout, SmartWakeConfig.SWIPE_UP, view -> onLockScreenDismissed()));
            rootLayout.setOnClickListener(v -> triggerFingerprintKeyguardPrompt());
        }

        SmartWakeAnalytics.logEvent(this, SmartWakeAnalytics.EVENT_SHOW, null);

        SmartWakeListener listener = SmartWake.getListener();
        if (listener != null) {
            listener.onScreenOn();
        }
    }

    private void triggerFingerprintKeyguardPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void startKeyguardUnlockMonitor() {
        keyguardCheckRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    if (km != null && !km.isKeyguardLocked()) {
                        Log.d(TAG, "FaceUnlock or Fingerprint unlocked device! Dismissing SmartWake Activity immediately.");
                        onLockScreenDismissed();
                        return;
                    }
                } catch (Exception ignored) {
                }
                keyguardHandler.postDelayed(this, 300); // Check every 300ms for instant face/fingerprint unlock
            }
        };
        keyguardHandler.postDelayed(keyguardCheckRunnable, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemNavigationBar();
        checkInstantUnlock();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemNavigationBar();
        } else {
            checkInstantUnlock();
        }
    }

    private void checkInstantUnlock() {
        try {
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && !km.isKeyguardLocked()) {
                Log.d(TAG, "Instant check: Keyguard unlocked! Closing Activity.");
                onLockScreenDismissed();
            }
        } catch (Exception ignored) {
        }
    }

    private void hideSystemNavigationBar() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        } catch (Exception ignored) {
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
        ImageView btnFlashlight = findViewById(R.id.smartwake_btn_flashlight);
        ImageView btnCamera = findViewById(R.id.smartwake_btn_camera);
        View swipeHintContainer = findViewById(R.id.smartwake_swipe_hint_container);
        View swipeIcon = findViewById(R.id.smartwake_swipe_icon);

        boolean isFullScreen = "FULL_SCREEN".equalsIgnoreCase(model.displayMode);
        Log.d(TAG, "setupServerUI displayMode: " + model.displayMode + ", isFullScreen: " + isFullScreen);

        // 1. Clock Container & Bottom Swipe Hint ALWAYS VISIBLE
        if (clockContainer != null) clockContainer.setVisibility(View.VISIBLE);
        if (swipeHintContainer != null) swipeHintContainer.setVisibility(View.VISIBLE);

        // 2. Display Mode Rules
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

        // 3. Flashlight & Camera Shortcuts
        if (btnFlashlight != null) {
            btnFlashlight.setOnClickListener(v -> toggleFlashlight(btnFlashlight));
        }

        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> launchCamera());
        }

        // 4. Weather
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

        // 5. Quotes
        if (model.showQuotes && model.quotes != null && !model.quotes.isEmpty() && tvQuote != null) {
            tvQuote.setVisibility(View.VISIBLE);
            int count = SmartWakeConfig.getScreenOnCount(this);
            String quote = model.quotes.get(count % model.quotes.size());
            tvQuote.setText("“" + quote + "”");
        }

        // 6. News / Sports
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

        // 7. Read More Redirect Button
        if (model.readMoreLinks != null && !model.readMoreLinks.isEmpty() && btnReadMore != null) {
            btnReadMore.setVisibility(View.VISIBLE);
            btnReadMore.setOnClickListener(v -> {
                SmartWakeAnalytics.logEvent(this, SmartWakeAnalytics.EVENT_READ_MORE, null);
                openReadMoreUrl(model.readMoreLinks);
                onLockScreenDismissed();
            });
        }

        // 8. Native Ad Engine
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

        // 9. Swipe Hint Animation
        if (swipeIcon != null) {
            bounceAnimator = ObjectAnimator.ofFloat(swipeIcon, "translationY", 0f, -14f, 0f);
            bounceAnimator.setDuration(1200);
            bounceAnimator.setRepeatCount(ValueAnimator.INFINITE);
            bounceAnimator.start();
        }
    }

    private void toggleFlashlight(ImageView btnFlashlight) {
        try {
            triggerHapticFeedback();
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                String cameraId = cm.getCameraIdList()[0];
                isTorchOn = !isTorchOn;
                cm.setTorchMode(cameraId, isTorchOn);

                if (btnFlashlight != null) {
                    if (isTorchOn) {
                        btnFlashlight.setBackgroundResource(R.drawable.bg_smartwake_shortcut_active);
                    } else {
                        btnFlashlight.setBackgroundResource(R.drawable.bg_smartwake_shortcut);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flashlight: " + e.getMessage());
        }
    }

    private void launchCamera() {
        triggerHapticFeedback();

        boolean launched = false;

        // 1. Secure LockScreen Camera Intent
        try {
            Intent secureIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
            secureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(secureIntent);
            launched = true;
            Log.d(TAG, "Launched secure camera intent.");
        } catch (Exception e1) {
            Log.w(TAG, "Secure camera intent failed, trying standard camera intent: " + e1.getMessage());
        }

        // 2. Standard Still Image Camera Intent
        if (!launched) {
            try {
                Intent standardIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                standardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(standardIntent);
                launched = true;
                Log.d(TAG, "Launched standard camera intent.");
            } catch (Exception e2) {
                Log.w(TAG, "Standard camera intent failed, trying action capture intent: " + e2.getMessage());
            }
        }

        // 3. Fallback Action Image Capture Intent
        if (!launched) {
            try {
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(captureIntent);
                launched = true;
                Log.d(TAG, "Launched action capture intent.");
            } catch (Exception e3) {
                Log.e(TAG, "All camera intents failed: " + e3.getMessage());
            }
        }

        if (launched) {
            onLockScreenDismissed();
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
        if (keyguardHandler != null && keyguardCheckRunnable != null) {
            keyguardHandler.removeCallbacks(keyguardCheckRunnable);
        }
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
        if (keyguardHandler != null && keyguardCheckRunnable != null) {
            keyguardHandler.removeCallbacks(keyguardCheckRunnable);
        }
        if (bounceAnimator != null) {
            bounceAnimator.cancel();
        }
        unregisterUnlockReceiver();

        // Immediately trigger background preloading for the next screen-on event!
        try {
            String cachedJson = SmartWakeConfig.getLastConfigJson(this);
            if (cachedJson != null && !cachedJson.trim().isEmpty()) {
                SmartWakeRemoteModel model = SmartWakeApiClient.parseConfigJson(cachedJson);
                SmartWakeAdManager.preloadAds(this, model);
            }
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}
