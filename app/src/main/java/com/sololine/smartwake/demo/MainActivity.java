package com.sololine.smartwake.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sololine.smartwake.SmartWake;
import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.SmartWakeListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartWakeDemo";
    private static final int REQ_NOTIFICATION_PERMISSION = 101;
    private static final int REQ_OVERLAY_PERMISSION = 102;

    private TextView tvStatus;
    private MaterialSwitch switchSmartWake;
    private MaterialButton btnTestNow;
    private MaterialButton btnCustomBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_service_status);
        switchSmartWake = findViewById(R.id.switch_smartwake);
        btnTestNow = findViewById(R.id.btn_test_now);
        btnCustomBuilder = findViewById(R.id.btn_custom_builder);

        // Register SmartWake Event Listener
        SmartWake.setListener(new SmartWakeListener() {
            @Override
            public void onScreenOn() {
                Log.d(TAG, "SmartWake Event: Screen On detected & Activity shown!");
            }

            @Override
            public void onDismissed() {
                Log.d(TAG, "SmartWake Event: LockScreen Activity dismissed by user.");
            }

            @Override
            public void onServiceStarted() {
                Log.d(TAG, "SmartWake Event: Service Started.");
                runOnUiThread(() -> updateStatusUI(true));
            }

            @Override
            public void onServiceStopped() {
                Log.d(TAG, "SmartWake Event: Service Stopped.");
                runOnUiThread(() -> updateStatusUI(false));
            }
        });

        // Initialize UI state
        boolean isEnabled = SmartWake.isEnabled(this);
        switchSmartWake.setChecked(isEnabled);
        updateStatusUI(isEnabled);

        switchSmartWake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkPermissionAndStartService();
            } else {
                SmartWake.stop(MainActivity.this);
                updateStatusUI(false);
            }
        });

        btnTestNow.setOnClickListener(v -> {
            Toast.makeText(this, "Opening SmartWake LockScreen...", Toast.LENGTH_SHORT).show();
            SmartWake.showNow(this);
        });

        btnCustomBuilder.setOnClickListener(v -> {
            SmartWake.builder(this)
                    .setTitle("Custom App Integration")
                    .setSubtitle("SmartWake configured for App #1 of 20")
                    .setSwipeDirection(SmartWakeConfig.SWIPE_UP)
                    .setShowClock(true)
                    .setVibrationEnabled(true)
                    .start();
            Toast.makeText(this, "Custom Builder Configured & Service Started!", Toast.LENGTH_SHORT).show();
            switchSmartWake.setChecked(true);
            updateStatusUI(true);
        });
    }

    private void checkPermissionAndStartService() {
        if (!SmartWake.hasOverlayPermission(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show();
            SmartWake.requestOverlayPermission(this, REQ_OVERLAY_PERMISSION);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATION_PERMISSION
                );
                return;
            }
        }
        SmartWake.start(this);
        updateStatusUI(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERMISSION) {
            if (SmartWake.hasOverlayPermission(this)) {
                checkPermissionAndStartService();
            } else {
                Toast.makeText(this, "Overlay permission is required for LockScreen overlay", Toast.LENGTH_SHORT).show();
                switchSmartWake.setChecked(false);
                updateStatusUI(false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SmartWake.start(this);
                updateStatusUI(true);
            } else {
                Toast.makeText(this, "Notification permission is required for background service", Toast.LENGTH_LONG).show();
                switchSmartWake.setChecked(false);
                updateStatusUI(false);
            }
        }
    }

    private void updateStatusUI(boolean enabled) {
        if (enabled) {
            tvStatus.setText(R.string.status_enabled);
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvStatus.setText(R.string.status_disabled);
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }
}
