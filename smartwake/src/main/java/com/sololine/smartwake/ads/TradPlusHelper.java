package com.sololine.smartwake.ads;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.tradplus.ads.base.bean.TPAdError;
import com.tradplus.ads.base.bean.TPAdInfo;
import com.tradplus.ads.open.nativead.NativeAdListener;
import com.tradplus.ads.open.nativead.TPNative;
import com.tradplus.ads.open.nativead.TPNativeAdRender;

public class TradPlusHelper {

    private static final String TAG = "SmartWake";

    public interface TradPlusCallback {
        void onSuccess(TPNative tpNative);
        void onFailure(String reason);
        void onAdClicked();
    }

    public static void loadNativeAd(Context context, String placementId, TradPlusCallback callback) {
        try {
            TPNative tpNative = new TPNative(context, placementId);
            tpNative.setAdListener(new NativeAdListener() {
                public void onAdLoaded(TPAdInfo tpAdInfo, TPNativeAdRender tpNativeAdRender) {
                    Log.d(TAG, "TRADPLUS HELPER: Native Ad loaded successfully!");
                    if (callback != null) callback.onSuccess(tpNative);
                }

                public void onAdLoaded(TPAdInfo tpAdInfo) {
                    Log.d(TAG, "TRADPLUS HELPER: Native Ad loaded successfully!");
                    if (callback != null) callback.onSuccess(tpNative);
                }

                public void onAdClicked(TPAdInfo tpAdInfo) {
                    Log.d(TAG, "TRADPLUS HELPER: Ad clicked!");
                    if (callback != null) callback.onAdClicked();
                }

                public void onAdClosed(TPAdInfo tpAdInfo) {
                }

                public void onAdRenderFailed(TPAdError tpAdError, TPAdInfo tpAdInfo) {
                    String err = (tpAdError != null) ? tpAdError.getErrorMsg() : "Render failed";
                    Log.w(TAG, "TRADPLUS HELPER: onAdRenderFailed - " + err);
                    if (callback != null) callback.onFailure(err);
                }

                public void onAdImpression(TPAdInfo tpAdInfo) {
                }

                public void onAdLoadFailed(TPAdError tpAdError) {
                    String err = (tpAdError != null) ? tpAdError.getErrorMsg() : "Load failed";
                    Log.w(TAG, "TRADPLUS HELPER: onAdLoadFailed - " + err);
                    if (callback != null) callback.onFailure(err);
                }
            });

            tpNative.loadAd();
            Log.d(TAG, "TRADPLUS HELPER: Requested TradPlus Native Ad for placement: " + placementId);
        } catch (Throwable t) {
            Log.e(TAG, "TRADPLUS HELPER ERROR: " + t.getMessage(), t);
            if (callback != null) callback.onFailure(t.getMessage());
        }
    }

    public static boolean showAd(Context context, ViewGroup container, TPNative tpNative) {
        if (container == null || tpNative == null) return false;
        try {
            container.removeAllViews();
            tpNative.showAd(container, com.sololine.smartwake.R.layout.item_smart_wake_custom_ad);
            Log.d(TAG, "TRADPLUS HELPER: showAd executed successfully!");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "TRADPLUS HELPER ERROR showing ad: " + t.getMessage(), t);
            return false;
        }
    }
}
