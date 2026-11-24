package com.sololine.smartwake.ads;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.sololine.smartwake.R;
import com.tradplus.ads.base.bean.TPAdError;
import com.tradplus.ads.base.bean.TPAdInfo;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.open.nativead.NativeAdListener;
import com.tradplus.ads.open.nativead.TPNative;

public class TradPlusHelper {

    private static final String TAG = "SmartWake";

    public interface TradPlusCallback {
        void onSuccess(TPNative tpNative);
        void onFailure(String reason);
        void onAdClicked();
    }

    public static void loadNativeAd(Context context, String placementId, TradPlusCallback callback) {
        try {
            TPNative nativeAd = new TPNative(context, placementId);
            nativeAd.setAdListener(new NativeAdListener() {
                @Override
                public void onAdLoaded(TPAdInfo tpAdInfo, TPBaseAd tpBaseAd) {
                    Log.d(TAG, "TRADPLUS HELPER: Native Ad loaded successfully!");
                    if (callback != null) callback.onSuccess(nativeAd);
                }

                @Override
                public void onAdClicked(TPAdInfo tpAdInfo) {
                    Log.d(TAG, "TRADPLUS HELPER: Ad clicked!");
                    if (callback != null) callback.onAdClicked();
                }

                @Override
                public void onAdImpression(TPAdInfo tpAdInfo) {
                    Log.d(TAG, "TRADPLUS HELPER: Impression logged.");
                }

                @Override
                public void onAdLoadFailed(TPAdError tpAdError) {
                    String err = (tpAdError != null) ? tpAdError.getErrorMsg() : "Load failed";
                    Log.w(TAG, "TRADPLUS HELPER: onAdLoadFailed - " + err);
                    if (callback != null) callback.onFailure(err);
                }
            });

            nativeAd.loadAd();
            Log.d(TAG, "TRADPLUS HELPER: Requested TradPlus Native Ad for placement: " + placementId);
        } catch (Throwable t) {
            Log.e(TAG, "TRADPLUS HELPER ERROR: " + t.getMessage(), t);
            if (callback != null) callback.onFailure(t.getMessage());
        }
    }

    public static boolean showAd(Context context, ViewGroup container, TPNative nativeAd) {
        if (container == null || nativeAd == null) return false;
        try {
            container.removeAllViews();
            container.setVisibility(ViewGroup.VISIBLE);

            nativeAd.showAd(container, R.layout.item_smart_wake_custom_ad);
            Log.d(TAG, "TRADPLUS HELPER: showAd rendered successfully!");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "TRADPLUS HELPER ERROR showing ad: " + t.getMessage(), t);
            return false;
        }
    }
}
