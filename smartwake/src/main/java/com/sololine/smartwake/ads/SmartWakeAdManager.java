package com.sololine.smartwake.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sololine.smartwake.R;
import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.analytics.SmartWakeAnalytics;
import com.sololine.smartwake.model.SmartWakeRemoteModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SmartWakeAdManager {

    private static final String TAG = "SmartWake";

    private static Object cachedAdMobNativeAd = null;
    private static Object cachedTradPlusNativeAd = null;

    private static boolean isAdMobLoading = false;
    private static boolean isTradPlusLoading = false;

    private static AdLoadCallback activeAdCallback = null;
    private static String activeAdType = "unknown";

    public interface AdLoadCallback {
        void onAdLoaded(View adView);
        void onAdFailedAll();
        void onAdClicked();
    }

    public static void notifyAdClicked() {
        if (activeAdCallback != null) {
            Log.d(TAG, "AD MANAGER: notifyAdClicked triggered! Notifying active listener.");
            activeAdCallback.onAdClicked();
        }
    }

    private static boolean isAdMobAvailable() {
        try {
            Class.forName("com.google.android.gms.ads.AdLoader");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isTradPlusAvailable() {
        try {
            Class.forName("com.tradplus.ads.open.nativead.TPNative");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Preloads ads in background memory so they are instantly ready for the next screen-on event.
     */
    public static void preloadAds(Context context, SmartWakeRemoteModel model) {
        if (context == null || model == null || !model.showAd) return;

        // 1. Preload AdMob Native Ad if not already cached and not currently loading
        if (model.admobNative != null && !model.admobNative.trim().isEmpty() && isAdMobAvailable()) {
            if (cachedAdMobNativeAd == null && !isAdMobLoading) {
                isAdMobLoading = true;
                Log.d(TAG, "AD MANAGER: Preloading AdMob Native Ad in background...");
                AdMobHelper.loadNativeAd(context, model.admobNative, new AdMobHelper.AdMobCallback() {
                    @Override
                    public void onSuccess(com.google.android.gms.ads.nativead.NativeAd nativeAd) {
                        cachedAdMobNativeAd = nativeAd;
                        isAdMobLoading = false;
                        Log.d(TAG, "AD MANAGER: Preloaded AdMob Native Ad successfully into memory cache!");
                    }

                    @Override
                    public void onFailure(String reason) {
                        isAdMobLoading = false;
                        Log.w(TAG, "AD MANAGER: Preload AdMob Native Ad failed: " + reason);
                    }

                    @Override
                    public void onAdClicked() {
                        SmartWakeAnalytics.logAdClick(context, "admob");
                        notifyAdClicked();
                    }
                });
            }
        }

        // 2. Preload TradPlus / Facebook Native Ad if not already cached and not currently loading
        if (model.fbNative != null && !model.fbNative.trim().isEmpty() && isTradPlusAvailable()) {
            if (cachedTradPlusNativeAd == null && !isTradPlusLoading) {
                isTradPlusLoading = true;
                Log.d(TAG, "AD MANAGER: Preloading TradPlus/Facebook Native Ad in background...");
                preloadTradPlusFacebookNative(context, model.fbNative);
            }
        }
    }

    private static void preloadTradPlusFacebookNative(Context context, String placementId) {
        try {
            Class<?> tpNativeClass = Class.forName("com.tradplus.ads.open.nativead.TPNative");
            Object tpNativeObj = tpNativeClass.getConstructor(Context.class, String.class).newInstance(context, placementId);

            Class<?> listenerClass = Class.forName("com.tradplus.ads.open.nativead.TPNativeAdListener");

            java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                if ("onAdLoaded".equals(method.getName())) {
                    cachedTradPlusNativeAd = tpNativeObj;
                    isTradPlusLoading = false;
                    Log.d(TAG, "AD MANAGER: Preloaded TradPlus Facebook Native Ad into memory cache!");
                } else if ("onAdClicked".equals(method.getName())) {
                    SmartWakeAnalytics.logAdClick(context, "facebook");
                    notifyAdClicked();
                } else if ("onAdFailed".equals(method.getName())) {
                    isTradPlusLoading = false;
                    Log.w(TAG, "AD MANAGER: Preload TradPlus Facebook Native Ad failed.");
                }
                return null;
            };

            Object listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    handler
            );

            Method setAdListenerMethod = tpNativeClass.getMethod("setAdListener", listenerClass);
            setAdListenerMethod.invoke(tpNativeObj, listenerProxy);

            Method loadAdMethod = tpNativeClass.getMethod("loadAd");
            loadAdMethod.invoke(tpNativeObj);

        } catch (Throwable e) {
            isTradPlusLoading = false;
            Log.w(TAG, "AD MANAGER: Preload TradPlus reflection failed: " + e.getMessage());
        }
    }

    public static void loadAndDisplayAd(Context context, ViewGroup container, SmartWakeRemoteModel model, AdLoadCallback callback) {
        if (container == null || model == null || !model.showAd) {
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        activeAdCallback = callback;

        List<Integer> priorityList = model.adsPriority;
        if (priorityList == null || priorityList.isEmpty()) {
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        int screenOnCount = SmartWakeConfig.getScreenOnCount(context);
        int primaryType = priorityList.get(screenOnCount % priorityList.size());

        Log.d(TAG, "AD MANAGER: LockScreen triggered. Primary ad type expected: " + primaryType);

        // --- CHECK MEMORY CACHE FIRST FOR INSTANT DISPLAY ---

        // Check AdMob preloaded cache
        if (primaryType == 1 && cachedAdMobNativeAd instanceof com.google.android.gms.ads.nativead.NativeAd && isAdMobAvailable()) {
            Log.d(TAG, "AD MANAGER: Displaying preloaded AdMob Native Ad instantly from memory cache!");
            com.google.android.gms.ads.nativead.NativeAd nativeAd = (com.google.android.gms.ads.nativead.NativeAd) cachedAdMobNativeAd;
            cachedAdMobNativeAd = null; // Consume cache so next screen-on gets a new ad

            View renderedView = AdMobHelper.renderNativeAdView(context, container, nativeAd);
            if (renderedView != null) {
                activeAdType = "admob";
                container.removeAllViews();
                container.addView(renderedView);
                SmartWakeAnalytics.logAdShow(context, "admob");
                if (callback != null) callback.onAdLoaded(renderedView);

                // Immediately trigger background preloading for the NEXT screen-on event!
                preloadAds(context, model);
                return;
            }
        }

        // Check TradPlus / Facebook preloaded cache
        if (primaryType == 2 && cachedTradPlusNativeAd != null && isTradPlusAvailable()) {
            Log.d(TAG, "AD MANAGER: Displaying preloaded TradPlus/Facebook Native Ad instantly from memory cache!");
            Object tpNativeObj = cachedTradPlusNativeAd;
            cachedTradPlusNativeAd = null; // Consume cache

            try {
                Class<?> tpNativeClass = Class.forName("com.tradplus.ads.open.nativead.TPNative");
                Method showAdMethod = tpNativeClass.getMethod("showAd", ViewGroup.class);
                activeAdType = "facebook";
                container.removeAllViews();
                showAdMethod.invoke(tpNativeObj, container);
                SmartWakeAnalytics.logAdShow(context, "facebook");
                if (callback != null) callback.onAdLoaded(container);

                // Immediately trigger background preloading for the NEXT screen-on event!
                preloadAds(context, model);
                return;
            } catch (Throwable e) {
                Log.w(TAG, "AD MANAGER: Error displaying preloaded TradPlus ad: " + e.getMessage());
            }
        }

        // Fallback or Live Loading Queue
        List<Integer> attemptQueue = new ArrayList<>();
        attemptQueue.add(primaryType);

        if (model.adsFailPriority != null) {
            for (int type : model.adsFailPriority) {
                if (!attemptQueue.contains(type)) {
                    attemptQueue.add(type);
                }
            }
        }

        Log.d(TAG, "AD MANAGER: Cache missed or unconsumed. Attempting live load queue: " + attemptQueue);
        attemptNextAd(context, container, model, attemptQueue, 0, callback);
    }

    private static void attemptNextAd(Context context, ViewGroup container, SmartWakeRemoteModel model, List<Integer> queue, int index, AdLoadCallback callback) {
        if (index >= queue.size()) {
            Log.d(TAG, "AD MANAGER: All ad attempts in queue failed or unavailable. Hiding ad container.");
            if (callback != null) callback.onAdFailedAll();
            // Still trigger preload attempt in background
            preloadAds(context, model);
            return;
        }

        int adType = queue.get(index);
        Log.d(TAG, "AD MANAGER: Attempting ad type " + adType + " at queue index " + index);

        switch (adType) {
            case 1: // AdMob Native
                if (isAdMobAvailable()) {
                    String admobId = (model.admobNative != null && !model.admobNative.trim().isEmpty())
                            ? model.admobNative : "ca-app-pub-3940256099942544/2247696110";

                    AdMobHelper.loadNativeAd(context, admobId, new AdMobHelper.AdMobCallback() {
                        @Override
                        public void onSuccess(com.google.android.gms.ads.nativead.NativeAd nativeAd) {
                            View adView = AdMobHelper.renderNativeAdView(context, container, nativeAd);
                            if (adView != null) {
                                activeAdType = "admob";
                                container.removeAllViews();
                                container.addView(adView);
                                SmartWakeAnalytics.logAdShow(context, "admob");
                                if (callback != null) callback.onAdLoaded(adView);

                                // Trigger preloading for NEXT screen-on event!
                                preloadAds(context, model);
                            } else {
                                attemptNextAd(context, container, model, queue, index + 1, callback);
                            }
                        }

                        @Override
                        public void onFailure(String reason) {
                            attemptNextAd(context, container, model, queue, index + 1, callback);
                        }

                        @Override
                        public void onAdClicked() {
                            SmartWakeAnalytics.logAdClick(context, "admob");
                            notifyAdClicked();
                        }
                    });
                    return;
                } else {
                    Log.w(TAG, "AD MANAGER: AdMob SDK not present in host app. Skipping type 1.");
                }
                break;

            case 2: // Facebook / TradPlus Native
                if (model.fbNative != null && !model.fbNative.trim().isEmpty()) {
                    loadTradPlusFacebookNative(context, container, model.fbNative, new AdLoadCallback() {
                        @Override
                        public void onAdLoaded(View adView) {
                            activeAdType = "facebook";
                            SmartWakeAnalytics.logAdShow(context, "facebook");
                            if (callback != null) callback.onAdLoaded(adView);
                            // Trigger preloading for NEXT screen-on event!
                            preloadAds(context, model);
                        }

                        @Override
                        public void onAdFailedAll() {
                            attemptNextAd(context, container, model, queue, index + 1, callback);
                        }

                        @Override
                        public void onAdClicked() {
                            SmartWakeAnalytics.logAdClick(context, "facebook");
                            notifyAdClicked();
                        }
                    });
                    return;
                }
                break;

            case 3: // Custom Native
                View customView = buildCustomNativeAdView(context, container, model, callback);
                if (customView != null) {
                    activeAdType = "custom";
                    container.removeAllViews();
                    container.addView(customView);
                    SmartWakeAnalytics.logAdShow(context, "custom");
                    if (callback != null) callback.onAdLoaded(customView);

                    // Trigger preloading for NEXT screen-on event!
                    preloadAds(context, model);
                    return;
                }
                break;
        }

        attemptNextAd(context, container, model, queue, index + 1, callback);
    }

    private static void loadTradPlusFacebookNative(Context context, ViewGroup container, String placementId, AdLoadCallback callback) {
        try {
            Class<?> tpNativeClass = Class.forName("com.tradplus.ads.open.nativead.TPNative");
            Object tpNativeObj = tpNativeClass.getConstructor(Context.class, String.class).newInstance(context, placementId);

            Class<?> listenerClass = Class.forName("com.tradplus.ads.open.nativead.TPNativeAdListener");

            java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                if ("onAdLoaded".equals(method.getName())) {
                    container.post(() -> {
                        try {
                            Method showAdMethod = tpNativeClass.getMethod("showAd", ViewGroup.class);
                            showAdMethod.invoke(tpNativeObj, container);
                            if (callback != null) callback.onAdLoaded(container);
                        } catch (Exception e) {
                            if (callback != null) callback.onAdFailedAll();
                        }
                    });
                } else if ("onAdClicked".equals(method.getName())) {
                    SmartWakeAnalytics.logAdClick(context, "facebook");
                    notifyAdClicked();
                } else if ("onAdFailed".equals(method.getName())) {
                    if (callback != null) callback.onAdFailedAll();
                }
                return null;
            };

            Object listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    handler
            );

            Method setAdListenerMethod = tpNativeClass.getMethod("setAdListener", listenerClass);
            setAdListenerMethod.invoke(tpNativeObj, listenerProxy);

            Method loadAdMethod = tpNativeClass.getMethod("loadAd");
            loadAdMethod.invoke(tpNativeObj);

            Log.d(TAG, "AD MANAGER: TradPlus Facebook Native loadAd initiated.");
        } catch (Throwable e) {
            Log.w(TAG, "AD MANAGER: TradPlus reflection failed: " + e.getMessage());
            loadFacebookNative(context, container, placementId, callback);
        }
    }

    private static void loadFacebookNative(Context context, ViewGroup container, String placementId, AdLoadCallback callback) {
        try {
            Class<?> nativeAdClass = Class.forName("com.facebook.ads.NativeAd");
            Object nativeAdObj = nativeAdClass.getConstructor(Context.class, String.class).newInstance(context, placementId);

            Method loadAdMethod = nativeAdClass.getMethod("loadAd");
            loadAdMethod.invoke(nativeAdObj);

            Log.d(TAG, "AD MANAGER: Facebook Native loadAd initiated.");
            if (callback != null) callback.onAdFailedAll();
        } catch (Throwable e) {
            Log.w(TAG, "AD MANAGER: Facebook SDK reflection failed: " + e.getMessage());
            if (callback != null) callback.onAdFailedAll();
        }
    }

    private static View buildCustomNativeAdView(Context context, ViewGroup container, SmartWakeRemoteModel model, AdLoadCallback callback) {
        String title = "Special Offer";
        String desc = "Discover amazing features in our app!";
        String cta = "Install Now";
        String link = "https://play.google.com/store";
        String iconUrl = "";

        if (model != null && model.customNativeList != null && !model.customNativeList.isEmpty()) {
            int index = SmartWakeConfig.getCustomAdIndex(context);
            SmartWakeConfig.incrementCustomAdIndex(context);
            SmartWakeRemoteModel.CustomNativeAd adObj = model.customNativeList.get(index % model.customNativeList.size());
            if (adObj != null) {
                if (adObj.title != null && !adObj.title.trim().isEmpty()) title = adObj.title;
                if (adObj.description != null && !adObj.description.trim().isEmpty()) desc = adObj.description;
                if (adObj.cta != null && !adObj.cta.trim().isEmpty()) cta = adObj.cta;
                if (adObj.link != null && !adObj.link.trim().isEmpty()) link = adObj.link;
                if (adObj.icon != null && !adObj.icon.trim().isEmpty()) iconUrl = adObj.icon;
            }
        }

        try {
            View adView = LayoutInflater.from(context).inflate(R.layout.item_smart_wake_custom_ad, container, false);

            TextView titleTv = adView.findViewById(R.id.smartwake_ad_title);
            TextView descTv = adView.findViewById(R.id.smartwake_ad_desc);
            ImageView iconIv = adView.findViewById(R.id.smartwake_ad_icon);
            TextView ctaBtn = adView.findViewById(R.id.smartwake_ad_cta);

            if (titleTv != null) titleTv.setText(title);
            if (descTv != null) descTv.setText(desc);
            if (ctaBtn != null) ctaBtn.setText(cta);

            if (iconIv != null && !iconUrl.trim().isEmpty()) {
                loadIconAsync(iconIv, iconUrl);
            }

            final String finalLink = link;
            View.OnClickListener clickListener = v -> {
                if (finalLink != null && !finalLink.trim().isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalLink));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        context.startActivity(intent);
                    } catch (Exception ignored) {
                    }
                }
                SmartWakeAnalytics.logAdClick(context, "custom");
                notifyAdClicked();
            };

            adView.setOnClickListener(clickListener);
            if (ctaBtn != null) ctaBtn.setOnClickListener(clickListener);

            return adView;
        } catch (Throwable e) {
            Log.e(TAG, "AD MANAGER ERROR building custom ad view: " + e.getMessage());
            return null;
        }
    }

    private static void loadIconAsync(ImageView imageView, String urlString) {
        if (imageView == null || urlString == null || urlString.trim().isEmpty()) return;

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openStream());
                if (bitmap != null) {
                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Throwable ignored) {
            }
        }).start();
    }
}
