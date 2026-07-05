package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

public class OrderImageAdapter extends RecyclerView.Adapter<OrderImageAdapter.ImageViewHolder> {

    private final Context context;
    private List<String> imageUrls;

    public OrderImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public void updateImages(List<String> newImages) {
        this.imageUrls = newImages != null ? newImages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullUrl = getFullImageUrl(imageUrl);

            GlideUrl glideUrl = new GlideUrl(fullUrl, new LazyHeaders.Builder()
                .addHeader("Bypass-Tunnel-Reminder", "true")
                .build());

            Glide.with(context)
                .load(glideUrl)
                .placeholder(R.drawable.ic_meal)
                .error(R.drawable.ic_meal)
                .centerCrop()
                .into(holder.imageView);
        } else {
            Glide.with(context)
                .load(R.drawable.ic_meal)
                .centerCrop()
                .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return RetrofitClient.SERVER_URL + imageUrl;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgOrderItem);
        }
    }
}
