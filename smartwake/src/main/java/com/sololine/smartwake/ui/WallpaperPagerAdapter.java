package com.sololine.smartwake.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sololine.smartwake.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class WallpaperPagerAdapter extends RecyclerView.Adapter<WallpaperPagerAdapter.WallpaperViewHolder> {

    private static final String TAG = "WallpaperPagerAdapter";

    private final Context context;
    private final List<String> wallpaperUrls;

    public WallpaperPagerAdapter(Context context, List<String> wallpaperUrls) {
        this.context = context;
        this.wallpaperUrls = wallpaperUrls;
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_smartwake_card);
        return new WallpaperViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        String url = wallpaperUrls.get(position);
        holder.bindWallpaper(context, url);
    }

    @Override
    public int getItemCount() {
        return wallpaperUrls != null ? wallpaperUrls.size() : 0;
    }

    static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView;
        }

        public void bindWallpaper(Context context, String urlStr) {
            if (urlStr == null || urlStr.trim().isEmpty()) {
                imageView.setImageResource(R.drawable.bg_smartwake_card);
                return;
            }

            // Check disk cache first for zero-latency local loading
            String cacheFileName = "wp_" + Math.abs(urlStr.hashCode()) + ".jpg";
            File cacheFile = new File(context.getCacheDir(), cacheFileName);

            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    Bitmap cachedBitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                    if (cachedBitmap != null) {
                        imageView.setImageBitmap(cachedBitmap);
                        Log.d(TAG, "Loaded wallpaper instantly from disk cache: " + cacheFileName);
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            // Download asynchronously and save to disk cache
            new Thread(() -> {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setDoInput(true);
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        is.close();

                        if (bitmap != null) {
                            // Save to disk cache
                            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                            } catch (Exception ignored) {
                            }

                            imageView.post(() -> imageView.setImageBitmap(bitmap));
                            Log.d(TAG, "Wallpaper downloaded & cached from " + urlStr);
                            return;
                        }
                    }
                    Log.w(TAG, "Wallpaper HTTP failed code: " + responseCode + " for " + urlStr);
                    imageView.post(() -> imageView.setImageResource(R.drawable.bg_smartwake_card));
                } catch (Exception e) {
                    Log.e(TAG, "Error loading wallpaper: " + e.getMessage());
                    imageView.post(() -> imageView.setImageResource(R.drawable.bg_smartwake_card));
                }
            }).start();
        }
    }
}
