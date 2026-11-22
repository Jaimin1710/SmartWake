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
import com.sololine.smartwake.model.SmartWakeRemoteModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SmartWakeAdManager {

    private static final String TAG = "SmartWake";
    private static Object cachedAdMobNativeAd = null;

    public interface AdLoadCallback {
        void onAdLoaded(View adView);
        void onAdFailedAll();
        void onAdClicked();
    }

    public static void preloadAds(Context context, SmartWakeRemoteModel model) {
        if (context == null || model == null || !model.showAd) return;
        if (cachedAdMobNativeAd != null) return; // Already cached

        if (model.admobNative != null && !model.admobNative.trim().isEmpty()) {
            loadAdMobNativeObj(context, model.admobNative, new AdMobRawCallback() {
                @Override
                public void onSuccess(Object nativeAdObj) {
                    cachedAdMobNativeAd = nativeAdObj;
                    Log.d(TAG, "AD MANAGER: Preloaded AdMob Native Ad successfully into memory cache!");
                }

                @Override
                public void onFailure() {
                    Log.w(TAG, "AD MANAGER: Preload AdMob Native Ad failed.");
                }
            });
        }
    }

    public static void loadAndDisplayAd(Context context, ViewGroup container, SmartWakeRemoteModel model, AdLoadCallback callback) {
        if (container == null || model == null || !model.showAd) {
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        // 1. Check if we have a preloaded AdMob Native Ad in memory cache
        if (cachedAdMobNativeAd != null) {
            Log.d(TAG, "AD MANAGER: Displaying preloaded AdMob Native Ad instantly from memory cache!");
            Object adObj = cachedAdMobNativeAd;
            cachedAdMobNativeAd = null; // Consume cache
            View renderedView = renderAdMobNativeView(context, container, adObj, callback);
            if (renderedView != null) {
                container.removeAllViews();
                container.addView(renderedView);
                if (callback != null) callback.onAdLoaded(renderedView);
                // Trigger background preloading for next screen-on event
                preloadAds(context, model);
                return;
            }
        }

        List<Integer> priorityList = model.adsPriority;
        if (priorityList == null || priorityList.isEmpty()) {
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        int screenOnCount = SmartWakeConfig.getScreenOnCount(context);
        int primaryType = priorityList.get(screenOnCount % priorityList.size());

        List<Integer> attemptQueue = new ArrayList<>();
        attemptQueue.add(primaryType);

        if (model.adsFailPriority != null) {
            for (int type : model.adsFailPriority) {
                if (!attemptQueue.contains(type)) {
                    attemptQueue.add(type);
                }
            }
        }

        Log.d(TAG, "AD MANAGER: Attempting ad load queue: " + attemptQueue);
        attemptNextAd(context, container, model, attemptQueue, 0, callback);
    }

    private static void attemptNextAd(Context context, ViewGroup container, SmartWakeRemoteModel model, List<Integer> queue, int index, AdLoadCallback callback) {
        if (index >= queue.size()) {
            Log.d(TAG, "AD MANAGER: All ad attempts in queue failed or unavailable. Hiding ad container.");
            if (callback != null) callback.onAdFailedAll();
            return;
        }

        int adType = queue.get(index);
        Log.d(TAG, "AD MANAGER: Attempting ad type " + adType + " at queue index " + index);

        switch (adType) {
            case 1: // AdMob Native
                String admobId = (model.admobNative != null && !model.admobNative.trim().isEmpty())
                        ? model.admobNative : "ca-app-pub-3940256099942544/2247696110";

                loadAdMobNativeObj(context, admobId, new AdMobRawCallback() {
                    @Override
                    public void onSuccess(Object nativeAdObj) {
                        View adView = renderAdMobNativeView(context, container, nativeAdObj, callback);
                        if (adView != null) {
                            container.removeAllViews();
                            container.addView(adView);
                            if (callback != null) callback.onAdLoaded(adView);
                        } else {
                            attemptNextAd(context, container, model, queue, index + 1, callback);
                        }
                    }

                    @Override
                    public void onFailure() {
                        attemptNextAd(context, container, model, queue, index + 1, callback);
                    }
                });
                return;

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

                        @Override
                        public void onAdClicked() {
                            if (callback != null) callback.onAdClicked();
                        }
                    });
                    return;
                }
                break;

            case 3: // Custom Native
                View customView = buildCustomNativeAdView(context, container, model, callback);
                if (customView != null) {
                    container.removeAllViews();
                    container.addView(customView);
                    if (callback != null) callback.onAdLoaded(customView);
                    return;
                }
                break;
        }

        attemptNextAd(context, container, model, queue, index + 1, callback);
    }

    private interface AdMobRawCallback {
        void onSuccess(Object nativeAdObj);
        void onFailure();
    }

    private static void loadAdMobNativeObj(Context context, String adUnitId, AdMobRawCallback callback) {
        try {
            Class<?> adLoaderClass = Class.forName("com.google.android.gms.ads.AdLoader");
            Class<?> adLoaderBuilderClass = Class.forName("com.google.android.gms.ads.AdLoader$Builder");
            Class<?> nativeAdLoadedListenerClass = Class.forName("com.google.android.gms.ads.nativead.NativeAd$OnNativeAdLoadedListener");
            Class<?> adListenerClass = Class.forName("com.google.android.gms.ads.AdListener");
            Class<?> adRequestClass = Class.forName("com.google.android.gms.ads.AdRequest");

            Object builder = adLoaderBuilderClass.getConstructor(Context.class, String.class).newInstance(context, adUnitId);

            // Reflectively set OnNativeAdLoadedListener
            java.lang.reflect.InvocationHandler loadedHandler = (proxy, method, args) -> {
                if ("onNativeAdLoaded".equals(method.getName()) && args != null && args.length > 0) {
                    Object nativeAdObj = args[0];
                    Log.d(TAG, "AD MANAGER: AdMob Native Ad Loaded successfully!");
                    if (callback != null) callback.onSuccess(nativeAdObj);
                }
                return null;
            };

            Object loadedProxy = java.lang.reflect.Proxy.newProxyInstance(
                    nativeAdLoadedListenerClass.getClassLoader(),
                    new Class<?>[]{nativeAdLoadedListenerClass},
                    loadedHandler
            );

            Method forNativeAdMethod = adLoaderBuilderClass.getMethod("forNativeAd", nativeAdLoadedListenerClass);
            forNativeAdMethod.invoke(builder, loadedProxy);

            // Reflectively set AdListener for load errors
            java.lang.reflect.InvocationHandler listenerHandler = (proxy, method, args) -> {
                if ("onAdFailedToLoad".equals(method.getName())) {
                    Log.w(TAG, "AD MANAGER: AdMob Native onAdFailedToLoad.");
                    if (callback != null) callback.onFailure();
                }
                return null;
            };

            Object adListenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                    adListenerClass.getClassLoader(),
                    new Class<?>[]{adListenerClass},
                    listenerHandler
            );

            Method withAdListenerMethod = adLoaderBuilderClass.getMethod("withAdListener", adListenerClass);
            withAdListenerMethod.invoke(builder, adListenerProxy);

            Method buildMethod = adLoaderBuilderClass.getMethod("build");
            Object adLoaderObj = buildMethod.invoke(builder);

            Object adRequestObj = adRequestClass.getConstructor().newInstance();
            Method loadAdMethod = adLoaderClass.getMethod("loadAd", adRequestClass);
            loadAdMethod.invoke(adLoaderObj, adRequestObj);

            Log.d(TAG, "AD MANAGER: AdMob Native loadAd requested for ID: " + adUnitId);
        } catch (Exception e) {
            Log.w(TAG, "AD MANAGER: AdMob reflection failed: " + e.getMessage());
            if (callback != null) callback.onFailure();
        }
    }

    private static View renderAdMobNativeView(Context context, ViewGroup container, Object nativeAdObj, AdLoadCallback callback) {
        if (nativeAdObj == null) return null;

        try {
            Class<?> nativeAdClass = Class.forName("com.google.android.gms.ads.nativead.NativeAd");
            Class<?> nativeAdViewClass = Class.forName("com.google.android.gms.ads.nativead.NativeAdView");

            // Instantiate com.google.android.gms.ads.nativead.NativeAdView
            ViewGroup nativeAdView = (ViewGroup) nativeAdViewClass.getConstructor(Context.class).newInstance(context);
            nativeAdView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            View adLayout = LayoutInflater.from(context).inflate(R.layout.item_smart_wake_custom_ad, nativeAdView, false);
            nativeAdView.addView(adLayout);

            TextView titleTv = adLayout.findViewById(R.id.smartwake_ad_title);
            TextView descTv = adLayout.findViewById(R.id.smartwake_ad_desc);
            ImageView iconIv = adLayout.findViewById(R.id.smartwake_ad_icon);
            TextView ctaBtn = adLayout.findViewById(R.id.smartwake_ad_cta);

            // Extract NativeAd fields reflectively
            Method getHeadlineMethod = nativeAdClass.getMethod("getHeadline");
            Object headlineObj = getHeadlineMethod.invoke(nativeAdObj);
            if (titleTv != null && headlineObj != null) {
                titleTv.setText(headlineObj.toString());
                Method setHeadlineViewMethod = nativeAdViewClass.getMethod("setHeadlineView", View.class);
                setHeadlineViewMethod.invoke(nativeAdView, titleTv);
            }

            Method getBodyMethod = nativeAdClass.getMethod("getBody");
            Object bodyObj = getBodyMethod.invoke(nativeAdObj);
            if (descTv != null && bodyObj != null) {
                descTv.setText(bodyObj.toString());
                Method setBodyViewMethod = nativeAdViewClass.getMethod("setBodyView", View.class);
                setBodyViewMethod.invoke(nativeAdView, descTv);
            }

            Method getCallToActionMethod = nativeAdClass.getMethod("getCallToAction");
            Object ctaObj = getCallToActionMethod.invoke(nativeAdObj);
            if (ctaBtn != null && ctaObj != null) {
                ctaBtn.setText(ctaObj.toString());
                Method setCallToActionViewMethod = nativeAdViewClass.getMethod("setCallToActionView", View.class);
                setCallToActionViewMethod.invoke(nativeAdView, ctaBtn);
            }

            Method getIconMethod = nativeAdClass.getMethod("getIcon");
            Object iconObj = getIconMethod.invoke(nativeAdObj);
            if (iconIv != null && iconObj != null) {
                Method getDrawableMethod = iconObj.getClass().getMethod("getDrawable");
                Object drawableObj = getDrawableMethod.invoke(iconObj);
                if (drawableObj instanceof android.graphics.drawable.Drawable) {
                    iconIv.setImageDrawable((android.graphics.drawable.Drawable) drawableObj);
                }
                Method setIconViewMethod = nativeAdViewClass.getMethod("setIconView", View.class);
                setIconViewMethod.invoke(nativeAdView, iconIv);
            }

            // Register NativeAd Object into NativeAdView
            Method setNativeAdMethod = nativeAdViewClass.getMethod("setNativeAd", nativeAdClass);
            setNativeAdMethod.invoke(nativeAdView, nativeAdObj);

            Log.d(TAG, "AD MANAGER: AdMob NativeAdView rendered & bound successfully!");
            return nativeAdView;
        } catch (Exception e) {
            Log.e(TAG, "AD MANAGER ERROR rendering AdMob NativeView: " + e.getMessage(), e);
            return buildCustomNativeAdView(context, container, null, callback);
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
                } else if ("onAdClicked".equals(method.getName())) {
                    if (callback != null) callback.onAdClicked();
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            Log.w(TAG, "AD MANAGER: Facebook SDK reflection failed: " + e.getMessage());
            if (callback != null) callback.onAdFailedAll();
        }
    }

    private static View buildCustomNativeAdView(Context context, ViewGroup container, SmartWakeRemoteModel model, AdLoadCallback callback) {
        String title = (model != null && model.customNativeTitle != null && !model.customNativeTitle.trim().isEmpty())
                ? model.customNativeTitle : "Special Offer";
        String desc = (model != null && model.customNativeDesc != null && !model.customNativeDesc.trim().isEmpty())
                ? model.customNativeDesc : "Discover amazing features in our app!";
        String cta = (model != null && model.customNativeCta != null && !model.customNativeCta.trim().isEmpty())
                ? model.customNativeCta : "Install Now";
        String link = (model != null && model.customNativeLink != null && !model.customNativeLink.trim().isEmpty())
                ? model.customNativeLink : "https://play.google.com/store";

        try {
            View adView = LayoutInflater.from(context).inflate(R.layout.item_smart_wake_custom_ad, container, false);

            TextView titleTv = adView.findViewById(R.id.smartwake_ad_title);
            TextView descTv = adView.findViewById(R.id.smartwake_ad_desc);
            TextView ctaBtn = adView.findViewById(R.id.smartwake_ad_cta);

            if (titleTv != null) titleTv.setText(title);
            if (descTv != null) descTv.setText(desc);
            if (ctaBtn != null) ctaBtn.setText(cta);

            View.OnClickListener clickListener = v -> {
                if (link != null && !link.trim().isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception ignored) {
                    }
                }
                if (callback != null) callback.onAdClicked();
            };

            adView.setOnClickListener(clickListener);
            if (ctaBtn != null) ctaBtn.setOnClickListener(clickListener);

            return adView;
        } catch (Exception e) {
            Log.e(TAG, "AD MANAGER ERROR building custom ad view: " + e.getMessage());
            return null;
        }
    }
}
