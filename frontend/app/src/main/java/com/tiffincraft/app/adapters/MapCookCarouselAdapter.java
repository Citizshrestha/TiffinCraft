package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookProfile;

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/** Horizontal cook-card strip shown under the nearby-cooks map — lets users browse without hunting for small pins. */
public class MapCookCarouselAdapter extends RecyclerView.Adapter<MapCookCarouselAdapter.CardViewHolder> {

    public interface OnCookCardClickListener {
        void onCookCardClick(CookProfile cook);
    }

    private final List<CookProfile> cooks;
    private final OnCookCardClickListener listener;

    public MapCookCarouselAdapter(List<CookProfile> cooks, OnCookCardClickListener listener) {
        this.cooks = cooks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_map_cook_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.bind(cooks.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return cooks.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView imgAvatar;
        private final TextView tvKitchenName;
        private final TextView tvVerified;
        private final TextView tvFoodType;
        private final TextView tvRating;
        private final TextView tvDistance;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvKitchenName = itemView.findViewById(R.id.tvKitchenName);
            tvVerified = itemView.findViewById(R.id.tvVerified);
            tvFoodType = itemView.findViewById(R.id.tvFoodType);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }

        void bind(CookProfile cook, OnCookCardClickListener listener) {
            String name = cook.getKitchenName();
            tvKitchenName.setText(name != null && !name.isEmpty() ? name : cook.getFullName());
            tvFoodType.setText(cook.getFoodType() != null && !cook.getFoodType().isEmpty()
                    ? cook.getFoodType() : "Home Cooking");
            tvRating.setText(String.format(Locale.getDefault(), "%.1f", cook.getRating()));
            tvDistance.setText(cook.getDistanceKm() != null
                    ? String.format(Locale.getDefault(), "%.1f km", cook.getDistanceKm())
                    : "");
            tvVerified.setVisibility(cook.isVerified() ? View.VISIBLE : View.GONE);

            String imageUrl = cook.getProfileImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String fullUrl = imageUrl.startsWith("http") ? imageUrl : RetrofitClient.SERVER_URL + imageUrl;
                Glide.with(itemView.getContext())
                        .load(fullUrl)
                        .placeholder(R.drawable.avatar_cook)
                        .error(R.drawable.avatar_cook)
                        .into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.avatar_cook);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCookCardClick(cook);
            });
        }
    }
}
