package com.tiffincraft.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.tiffincraft.app.api.RetrofitClient;

/**
 * Resolves relative upload paths from the backend into full URLs Glide can load,
 * and loads images with the Bypass-Tunnel-Reminder header required by loca.lt /
 * localtunnel free tiers (otherwise meal photos appear broken).
 */
public final class ImageUrlHelper {

    private ImageUrlHelper() {}

    public static String resolve(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        String url = imageUrl.trim();
        // Incomplete Cloudinary folder paths (no filename) cannot be loaded
        if (url.endsWith("/tiffincraft/meals/") || url.endsWith("/tiffincraft/meals")
                || url.endsWith("/tiffincraft/profiles/") || url.endsWith("/tiffincraft/profiles")) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = RetrofitClient.SERVER_URL;
        if (base == null || base.isEmpty()) {
            return url;
        }
        // SERVER_URL is like http://host:port/ — avoid double slashes
        if (base.endsWith("/") && url.startsWith("/")) {
            return base.substring(0, base.length() - 1) + url;
        }
        if (!base.endsWith("/") && !url.startsWith("/")) {
            return base + "/" + url;
        }
        return base + url;
    }

    /**
     * Load a meal/profile image into an ImageView with tunnel-safe headers.
     */
    public static void load(ImageView target, @Nullable String imageUrl, @DrawableRes int placeholder) {
        load(target, imageUrl, placeholder, 0);
    }

    /**
     * @param cornerRadiusPx optional rounded corners (0 = none, only centerCrop)
     */
    public static void load(ImageView target, @Nullable String imageUrl,
                            @DrawableRes int placeholder, int cornerRadiusPx) {
        if (target == null) return;
        String full = resolve(imageUrl);
        if (full == null) {
            target.setImageResource(placeholder);
            return;
        }

        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(placeholder)
                .error(placeholder);

        if (cornerRadiusPx > 0) {
            options = options.transform(new CenterCrop(), new RoundedCorners(cornerRadiusPx));
        } else {
            options = options.centerCrop();
        }

        Glide.with(target.getContext())
                .load(modelFor(full))
                .apply(options)
                .into(target);
    }

    /**
     * Loads without any crop transform — for content where cropping would lose meaning
     * (e.g. payment QR codes, where the top/bottom carries the account name and number).
     */
    public static void loadNoCrop(ImageView target, @Nullable String imageUrl, @DrawableRes int placeholder) {
        if (target == null) return;
        String full = resolve(imageUrl);
        if (full == null) {
            target.setImageResource(placeholder);
            return;
        }

        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(placeholder)
                .error(placeholder)
                .fitCenter();

        Glide.with(target.getContext())
                .load(modelFor(full))
                .apply(options)
                .into(target);
    }

    /**
     * Loads the full-resolution, un-cropped bitmap for a URL (e.g. to display in a
     * full-screen viewer and/or save to the gallery at full quality).
     */
    public static void loadBitmap(Context context, @Nullable String imageUrl, Target<Bitmap> target) {
        String full = resolve(imageUrl);
        if (full == null) return;

        Glide.with(context)
                .asBitmap()
                .load(modelFor(full))
                .into(target);
    }

    private static Object modelFor(String full) {
        if (needsTunnelBypass(full)) {
            return new GlideUrl(full, new LazyHeaders.Builder()
                    .addHeader("Bypass-Tunnel-Reminder", "true")
                    .build());
        }
        return full;
    }

    private static boolean needsTunnelBypass(String url) {
        String lower = url.toLowerCase();
        return lower.contains("loca.lt")
                || lower.contains("localtunnel")
                || lower.contains("ngrok")
                || lower.contains("/uploads/");
    }
}
