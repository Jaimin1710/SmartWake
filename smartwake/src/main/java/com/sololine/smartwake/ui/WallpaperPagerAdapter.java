package com.sololine.smartwake.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sololine.smartwake.R;
import com.sololine.smartwake.content.WallpaperCacheManager;

import java.util.List;

public class WallpaperPagerAdapter extends RecyclerView.Adapter<WallpaperPagerAdapter.WallpaperViewHolder> {

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
        if (wallpaperUrls == null || wallpaperUrls.isEmpty()) return;

        int actualIndex = position % wallpaperUrls.size();
        holder.bindWallpaper(context, wallpaperUrls, actualIndex);
    }

    @Override
    public int getItemCount() {
        return (wallpaperUrls != null && !wallpaperUrls.isEmpty()) ? Integer.MAX_VALUE : 0;
    }

    static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView;
        }

        public void bindWallpaper(Context context, List<String> urls, int startIndex) {
            imageView.setImageResource(R.drawable.bg_smartwake_card); // Default placeholder while loading

            WallpaperCacheManager.loadWallpaperBitmapAsync(context, urls, startIndex, new WallpaperCacheManager.BitmapCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.bg_smartwake_card);
                    }
                }

                @Override
                public void onFailure() {
                    imageView.setImageResource(R.drawable.bg_smartwake_card);
                }
            });
        }
    }
}
