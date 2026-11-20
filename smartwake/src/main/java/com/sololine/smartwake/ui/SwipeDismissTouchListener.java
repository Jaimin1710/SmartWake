package com.sololine.smartwake.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.sololine.smartwake.SmartWakeConfig;

public class SwipeDismissTouchListener implements View.OnTouchListener {

    public interface OnDismissCallback {
        void onDismiss(View view);
    }

    private final View view;
    private final OnDismissCallback callback;
    private final int swipeDirection;

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;

    private float downX;
    private float downY;
    private boolean isSwiping;
    private int slopDirection;
    private VelocityTracker velocityTracker;
    private float translationX;
    private float translationY;

    public SwipeDismissTouchListener(View view, int swipeDirection, OnDismissCallback callback) {
        this.view = view;
        this.callback = callback;
        this.swipeDirection = swipeDirection;

        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        this.touchSlop = vc.getScaledTouchSlop();
        this.minFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        this.maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        event.offsetLocation(translationX, translationY);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (velocityTracker == null) {
                    break;
                }
                velocityTracker.addMovement(event);
                float deltaX = event.getRawX() - downX;
                float deltaY = event.getRawY() - downY;

                if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                    isSwiping = true;
                    slopDirection = Math.abs(deltaX) > Math.abs(deltaY) ? 1 : 2;
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (isSwiping) {
                    if (swipeDirection == SmartWakeConfig.SWIPE_UP && deltaY < 0) {
                        translationY = deltaY;
                        view.setTranslationY(translationY);
                        view.setAlpha(Math.max(0.1f, 1f - Math.abs(deltaY) / view.getHeight()));
                    } else if (swipeDirection == SmartWakeConfig.SWIPE_DOWN && deltaY > 0) {
                        translationY = deltaY;
                        view.setTranslationY(translationY);
                        view.setAlpha(Math.max(0.1f, 1f - Math.abs(deltaY) / view.getHeight()));
                    } else if (swipeDirection == SmartWakeConfig.SWIPE_LEFT && deltaX < 0) {
                        translationX = deltaX;
                        view.setTranslationX(translationX);
                        view.setAlpha(Math.max(0.1f, 1f - Math.abs(deltaX) / view.getWidth()));
                    } else if (swipeDirection == SmartWakeConfig.SWIPE_RIGHT && deltaX > 0) {
                        translationX = deltaX;
                        view.setTranslationX(translationX);
                        view.setAlpha(Math.max(0.1f, 1f - Math.abs(deltaX) / view.getWidth()));
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (velocityTracker == null) {
                    break;
                }

                float totalDeltaX = event.getRawX() - downX;
                float totalDeltaY = event.getRawY() - downY;
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                float vx = velocityTracker.getXVelocity();
                float vy = velocityTracker.getYVelocity();

                boolean dismiss = false;
                boolean dismissUp = false;
                boolean dismissDown = false;
                boolean dismissLeft = false;
                boolean dismissRight = false;

                if (swipeDirection == SmartWakeConfig.SWIPE_UP) {
                    if (totalDeltaY < -view.getHeight() / 4f || (vy < -minFlingVelocity && totalDeltaY < 0)) {
                        dismiss = true;
                        dismissUp = true;
                    }
                } else if (swipeDirection == SmartWakeConfig.SWIPE_DOWN) {
                    if (totalDeltaY > view.getHeight() / 4f || (vy > minFlingVelocity && totalDeltaY > 0)) {
                        dismiss = true;
                        dismissDown = true;
                    }
                } else if (swipeDirection == SmartWakeConfig.SWIPE_LEFT) {
                    if (totalDeltaX < -view.getWidth() / 4f || (vx < -minFlingVelocity && totalDeltaX < 0)) {
                        dismiss = true;
                        dismissLeft = true;
                    }
                } else if (swipeDirection == SmartWakeConfig.SWIPE_RIGHT) {
                    if (totalDeltaX > view.getWidth() / 4f || (vx > minFlingVelocity && totalDeltaX > 0)) {
                        dismiss = true;
                        dismissRight = true;
                    }
                }

                if (dismiss) {
                    performDismissAnimation(dismissUp, dismissDown, dismissLeft, dismissRight);
                } else {
                    // Reset animation
                    view.animate()
                            .translationX(0)
                            .translationY(0)
                            .alpha(1.0f)
                            .setDuration(200)
                            .setListener(null);
                }

                velocityTracker.recycle();
                velocityTracker = null;
                translationX = 0;
                translationY = 0;
                isSwiping = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    view.animate()
                            .translationX(0)
                            .translationY(0)
                            .alpha(1.0f)
                            .setDuration(200)
                            .setListener(null);
                    velocityTracker.recycle();
                    velocityTracker = null;
                    translationX = 0;
                    translationY = 0;
                    isSwiping = false;
                }
                return true;
        }

        return false;
    }

    private void performDismissAnimation(boolean up, boolean down, boolean left, boolean right) {
        float targetY = 0;
        float targetX = 0;
        if (up) targetY = -view.getHeight();
        if (down) targetY = view.getHeight();
        if (left) targetX = -view.getWidth();
        if (right) targetX = view.getWidth();

        view.animate()
                .translationX(targetX)
                .translationY(targetY)
                .alpha(0.0f)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (callback != null) {
                            callback.onDismiss(view);
                        }
                    }
                });
    }
}
