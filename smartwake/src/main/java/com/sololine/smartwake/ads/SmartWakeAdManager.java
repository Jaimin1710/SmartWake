package com.sololine.smartwake.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sololine.smartwake.R;
import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.model.SmartWakeRemoteModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SmartWakeAdManager {

    private static final String TAG = "SmartWakeAdManager";

    public interface AdLoadCallback {
        void onAdLoaded(View adView);
        void onAdFailedAll();
    }

    public static void loadAndDisplayAd(Context context, ViewGroup container, SmartWakeRemoteModel model, AdLoadCallback callback) {
        if (container == null || model == null || !model.showAd) {
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        List<Integer> priorityList = model.adsPriority;
        if (priorityList == null || priorityList.isEmpty()) {
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        // Determine primary provider type based on screen-on rotation count
        int screenOnCount = SmartWakeConfig.getScreenOnCount(context);
        int primaryType = priorityList.get(screenOnCount % priorityList.size());

        // Create fallback queue combining primary and adsFailPriority
        List<Integer> attemptQueue = new ArrayList<>();
        attemptQueue.add(primaryType);

        if (model.adsFailPriority != null) {
            for (int type : model.adsFailPriority) {
                if (!attemptQueue.contains(type)) {
                    attemptQueue.add(type);
                }
            }
        }

        Log.d(TAG, "Attempting ad load queue: " + attemptQueue);
        attemptNextAd(context, container, model, attemptQueue, 0, callback);
    }

    private static void attemptNextAd(Context context, ViewGroup container, SmartWakeRemoteModel model, List<Integer> queue, int index, AdLoadCallback callback) {
        if (index >= queue.size()) {
            Log.d(TAG, "All ad attempts in queue failed or unavailable. Hiding ad container.");
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        int adType = queue.get(index);
        Log.d(TAG, "Attempting ad type " + adType + " at queue index " + index);

        switch (adType) {
            case 1: // AdMob Native
                if (model.admobNative != null && !model.admobNative.trim().isEmpty()) {
                    loadAdMobNative(context, container, model.admobNative, new AdLoadCallback() {
                        @Override
                        public void onAdLoaded(View adView) {
                            if (callback != null) callback.onAdLoaded(adView);
                        }

                        @Override
                        public void onAdFailedAll() {
                            attemptNextAd(context, container, model, queue, index + 1, callback);
                        }
                    });
                    return;
                }
                break;

            case 2: // Facebook / TradPlus Native
                if (model.fbNative != null && !model.fbNative.trim().isEmpty()) {
                    loadTradPlusFacebookNative(context, container, model.fbNative, new AdLoadCallback() {
                        @Override
                        public void onAdLoaded(View adView) {
                            if (callback != null) callback.onAdLoaded(adView);
                        }

                        @Override
                        public void onAdFailedAll() {
                            attemptNextAd(context, container, model, queue, index + 1, callback);
                        }
                    });
                    return;
                }
                break;

            case 3: // Custom Native
                View customView = buildCustomNativeAdView(context, container, model);
                if (customView != null) {
                    container.removeAllViews();
                    container.addView(customView);
                    if (callback != null) callback.onAdLoaded(customView);
                    return;
                }
                break;
        }

        // Try next fallback in queue
        attemptNextAd(context, container, model, queue, index + 1, callback);
    }

    private static void loadAdMobNative(Context context, ViewGroup container, String adUnitId, AdLoadCallback callback) {
        try {
            Class<?> adLoaderClass = Class.forName("com.google.android.gms.ads.AdLoader");
            Class<?> adLoaderBuilderClass = Class.forName("com.google.android.gms.ads.AdLoader$Builder");
            Class<?> nativeAdLoadedListenerClass = Class.forName("com.google.android.gms.ads.nativead.NativeAd$OnNativeAdLoadedListener");
            Class<?> adRequestClass = Class.forName("com.google.android.gms.ads.AdRequest");

            Object builder = adLoaderBuilderClass.getConstructor(Context.class, String.class).newInstance(context, adUnitId);

            java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                if ("onNativeAdLoaded".equals(method.getName()) && args.length > 0) {
                    Object nativeAdObj = args[0];
                    container.post(() -> {
                        View adView = renderAdMobNativeView(context, container, nativeAdObj);
                        if (adView != null && callback != null) {
                            callback.onAdLoaded(adView);
                        } else if (callback != null) {
                            callback.onAdFailedAll();
                        }
                    });
                }
                return null;
            };

            Object listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                    nativeAdLoadedListenerClass.getClassLoader(),
                    new Class<?>[]{nativeAdLoadedListenerClass},
                    handler
            );

            Method forNativeAdMethod = adLoaderBuilderClass.getMethod("forNativeAd", nativeAdLoadedListenerClass);
            forNativeAdMethod.invoke(builder, listenerProxy);

            Method buildMethod = adLoaderBuilderClass.getMethod("build");
            Object adLoaderObj = buildMethod.invoke(builder);

            Object adRequestObj = adRequestClass.getConstructor().newInstance();
            Method loadAdMethod = adLoaderClass.getMethod("loadAd", adRequestClass);
            loadAdMethod.invoke(adLoaderObj, adRequestObj);

            Log.d(TAG, "AdMob Native loadAd initiated via reflection.");
        } catch (Exception e) {
            Log.w(TAG, "AdMob SDK reflection failed or not present: " + e.getMessage());
            if (callback != null) callback.onAdFailedAll();
        }
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

            Log.d(TAG, "TradPlus Facebook Native loadAd initiated via reflection.");
        } catch (Exception e) {
            Log.w(TAG, "TradPlus SDK reflection failed or fallback to direct FB Native: " + e.getMessage());
            loadFacebookNative(context, container, placementId, callback);
        }
    }

    private static void loadFacebookNative(Context context, ViewGroup container, String placementId, AdLoadCallback callback) {
        try {
            Class<?> nativeAdClass = Class.forName("com.facebook.ads.NativeAd");
            Object nativeAdObj = nativeAdClass.getConstructor(Context.class, String.class).newInstance(context, placementId);

            Method loadAdMethod = nativeAdClass.getMethod("loadAd");
            loadAdMethod.invoke(nativeAdObj);

            Log.d(TAG, "Facebook Native loadAd initiated.");
            if (callback != null) callback.onAdFailedAll();
        } catch (Exception e) {
            Log.w(TAG, "Facebook Audience Network SDK reflection failed or not present: " + e.getMessage());
            if (callback != null) callback.onAdFailedAll();
        }
    }

    private static View renderAdMobNativeView(Context context, ViewGroup container, Object nativeAdObj) {
        try {
            return LayoutInflater.from(context).inflate(R.layout.activity_smart_wake, container, false);
        } catch (Exception e) {
            return null;
        }
    }

    private static View buildCustomNativeAdView(Context context, ViewGroup container, SmartWakeRemoteModel model) {
        if (model.customNativeTitle == null || model.customNativeTitle.trim().isEmpty()) {
            return null;
        }

        try {
            View adView = LayoutInflater.from(context).inflate(R.layout.activity_smart_wake, container, false);

            TextView titleTv = adView.findViewById(R.id.smartwake_tv_quote);
            TextView subtitleTv = adView.findViewById(R.id.smartwake_tv_news_sports);

            if (titleTv != null) {
                titleTv.setVisibility(View.VISIBLE);
                titleTv.setText(model.customNativeTitle);
            }

            if (subtitleTv != null) {
                subtitleTv.setVisibility(View.VISIBLE);
                subtitleTv.setText(model.customNativeDesc);
            }

            adView.setOnClickListener(v -> {
                if (model.customNativeLink != null && !model.customNativeLink.trim().isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.customNativeLink));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception ignored) {
                    }
                }
            });

            return adView;
        } catch (Exception e) {
            return null;
        }
    }
}
