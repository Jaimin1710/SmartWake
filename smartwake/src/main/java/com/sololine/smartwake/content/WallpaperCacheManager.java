package com.sololine.smartwake.content;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WallpaperCacheManager {

    private static final String TAG = "SmartWakeWallpaper";
    private static final String CACHE_DIR_NAME = "smartwake_wallpapers";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BitmapCallback {
        void onSuccess(Bitmap bitmap);
        void onFailure();
    }

    private static File getWallpaperCacheDir(Context context) {
        File dir = new File(context.getApplicationContext().getCacheDir(), CACHE_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static String getCacheKey(String url) {
        return "wp_" + Math.abs(url.hashCode()) + ".jpg";
    }

    /**
     * Pre-downloads initial wallpapers into local disk cache.
     */
    public static void preloadWallpapersAsync(Context context, java.util.List<String> urls) {
        if (context == null || urls == null || urls.isEmpty()) return;

        new Thread(() -> {
            int count = Math.min(urls.size(), 5);
            for (int i = 0; i < count; i++) {
                String url = urls.get(i);
                if (url != null && !url.trim().isEmpty()) {
                    getOrDownloadBitmapSync(context, url);
                }
            }
        }).start();
    }

    public static void loadWallpaperBitmapAsync(Context context, java.util.List<String> urls, int startIndex, BitmapCallback callback) {
        if (context == null || urls == null || urls.isEmpty()) {
            if (callback != null) callback.onFailure();
            return;
        }

        new Thread(() -> {
            int total = urls.size();
            for (int attempt = 0; attempt < total; attempt++) {
                int currentIndex = (startIndex + attempt) % total;
                String urlStr = urls.get(currentIndex);

                Bitmap bitmap = getOrDownloadBitmapSync(context, urlStr);
                if (bitmap != null) {
                    final Bitmap finalBitmap = bitmap;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(finalBitmap);
                    });
                    return;
                }
            }

            mainHandler.post(() -> {
                if (callback != null) callback.onFailure();
            });
        }).start();
    }

    private static Bitmap getOrDownloadBitmapSync(Context context, String urlString) {
        File cacheDir = getWallpaperCacheDir(context);
        File cachedFile = new File(cacheDir, getCacheKey(urlString));

        if (cachedFile.exists() && cachedFile.length() > 0) {
            try {
                Bitmap cachedBitmap = BitmapFactory.decodeFile(cachedFile.getAbsolutePath());
                if (cachedBitmap != null) {
                    return cachedBitmap;
                }
            } catch (Throwable ignored) {
            }
        }

        // Download from network
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream input = conn.getInputStream();
                Bitmap downloadedBitmap = BitmapFactory.decodeStream(input);
                input.close();

                if (downloadedBitmap != null) {
                    // Save to local disk cache
                    try (FileOutputStream out = new FileOutputStream(cachedFile)) {
                        downloadedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    } catch (Throwable ignored) {
                    }
                    return downloadedBitmap;
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "Failed downloading wallpaper: " + urlString + " - " + e.getMessage());
        }

        return null;
    }
}
