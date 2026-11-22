package com.sololine.smartwake.ads;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.sololine.smartwake.R;

public class AdMobHelper {

    private static final String TAG = "SmartWake";
    private static boolean initialized = false;

    public interface AdMobCallback {
        void onSuccess(NativeAd nativeAd);
        void onFailure(String reason);
    }

    public static void loadNativeAd(Context context, String adUnitId, AdMobCallback callback) {
        try {
            if (!initialized) {
                MobileAds.initialize(context.getApplicationContext());
                initialized = true;
            }

            AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                    .forNativeAd(nativeAd -> {
                        Log.d(TAG, "ADMOB HELPER: Native Ad loaded successfully!");
                        if (callback != null) callback.onSuccess(nativeAd);
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            String err = (loadAdError != null) ? loadAdError.getMessage() : "Unknown error";
                            Log.w(TAG, "ADMOB HELPER: onAdFailedToLoad - " + err);
                            if (callback != null) callback.onFailure(err);
                        }
                    })
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
            Log.d(TAG, "ADMOB HELPER: Requested AdMob Native Ad for unit ID: " + adUnitId);
        } catch (Throwable t) {
            Log.e(TAG, "ADMOB HELPER ERROR: " + t.getMessage(), t);
            if (callback != null) callback.onFailure(t.getMessage());
        }
    }

    public static View renderNativeAdView(Context context, ViewGroup container, NativeAd nativeAd) {
        if (context == null || nativeAd == null) return null;

        try {
            NativeAdView nativeAdView = new NativeAdView(context);
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

            if (titleTv != null && nativeAd.getHeadline() != null) {
                titleTv.setText(nativeAd.getHeadline());
                nativeAdView.setHeadlineView(titleTv);
            }

            if (descTv != null && nativeAd.getBody() != null) {
                descTv.setText(nativeAd.getBody());
                nativeAdView.setBodyView(descTv);
            }

            if (ctaBtn != null && nativeAd.getCallToAction() != null) {
                ctaBtn.setText(nativeAd.getCallToAction());
                nativeAdView.setCallToActionView(ctaBtn);
            }

            if (iconIv != null && nativeAd.getIcon() != null) {
                Drawable iconDrawable = nativeAd.getIcon().getDrawable();
                if (iconDrawable != null) {
                    iconIv.setImageDrawable(iconDrawable);
                }
                nativeAdView.setIconView(iconIv);
            }

            nativeAdView.setNativeAd(nativeAd);
            Log.d(TAG, "ADMOB HELPER: NativeAdView inflated and bound successfully!");
            return nativeAdView;
        } catch (Throwable t) {
            Log.e(TAG, "ADMOB HELPER ERROR rendering view: " + t.getMessage(), t);
            return null;
        }
    }
}
