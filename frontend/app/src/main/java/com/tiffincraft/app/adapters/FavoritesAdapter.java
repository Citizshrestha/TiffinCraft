package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.FavoriteResponse;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private Context context;
    private List<FavoriteResponse.FavoriteCook> favorites;
    private OnFavoriteActionListener listener;

    public interface OnFavoriteActionListener {
        void onRemoveFavorite(int cookId, int position);
        void onViewMenu(int cookId);
    }

    public FavoritesAdapter(Context context, OnFavoriteActionListener listener) {
        this.context = context;
        this.favorites = new ArrayList<>();
        this.listener = listener;
    }

    public void setFavorites(List<FavoriteResponse.FavoriteCook> favorites) {
        this.favorites = favorites != null ? favorites : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeFavorite(int position) {
        if (position >= 0 && position < favorites.size()) {
            favorites.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, favorites.size());
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite_cook, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoriteResponse.FavoriteCook cook = favorites.get(position);
        holder.bind(cook);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCook, imgFavoriteIcon;
        TextView tvCookName, tvRating, tvReviews, tvExperience;
        TextView tvSpecialties, tvTotalMeals, tvTotalOrders, tvResponseTime;
        MaterialButton btnViewMenu;
        View btnFavorite;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCook = itemView.findViewById(R.id.imgCook);
            imgFavoriteIcon = itemView.findViewById(R.id.imgFavoriteIcon);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvCookName = itemView.findViewById(R.id.tvCookName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviews = itemView.findViewById(R.id.tvReviews);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvSpecialties = itemView.findViewById(R.id.tvSpecialties);
            tvTotalMeals = itemView.findViewById(R.id.tvTotalMeals);
            tvTotalOrders = itemView.findViewById(R.id.tvTotalOrders);
            tvResponseTime = itemView.findViewById(R.id.tvResponseTime);
            btnViewMenu = itemView.findViewById(R.id.btnViewMenu);
        }

        public void bind(FavoriteResponse.FavoriteCook cook) {
            // Set cook name
            tvCookName.setText(cook.getCookName());

            // Set rating and reviews
            tvRating.setText(String.format("%.1f", cook.getAverageRating()));
            tvReviews.setText(String.format("(%d)", cook.getTotalReviews()));

            // Set experience
            if (cook.getExperienceYears() != null && cook.getExperienceYears() > 0) {
                tvExperience.setText(cook.getExperienceYears() + " years exp");
                tvExperience.setVisibility(View.VISIBLE);
            } else {
                tvExperience.setVisibility(View.GONE);
            }

            // Set specialties
            if (cook.getSpecialties() != null && !cook.getSpecialties().isEmpty()) {
                tvSpecialties.setText(String.join(", ", cook.getSpecialties()));
            } else {
                tvSpecialties.setText("Various cuisines");
            }

            // Set stats
            tvTotalMeals.setText(String.valueOf(cook.getTotalMeals()));
            
            // Show orders as "150+" if more than 150
            int totalOrders = cook.getTotalReviews(); // Using reviews as proxy for orders
            if (totalOrders > 150) {
                tvTotalOrders.setText("150+");
            } else {
                tvTotalOrders.setText(String.valueOf(totalOrders));
            }

            // Default delivery time
            tvResponseTime.setText("30 min");

            // Load cook profile image
            if (cook.getProfileImage() != null && !cook.getProfileImage().isEmpty()) {
                Glide.with(context)
                        .load(cook.getProfileImage())
                        .placeholder(R.drawable.avatar_cook)
                        .error(R.drawable.avatar_cook)
                        .circleCrop()
                        .into(imgCook);
            } else {
                imgCook.setImageResource(R.drawable.avatar_cook);
            }

            // Remove favorite click
            btnFavorite.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRemoveFavorite(cook.getCookId(), position);
                }
            });

            // View menu click
            btnViewMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewMenu(cook.getCookId());
                }
            });
        }
    }
}
