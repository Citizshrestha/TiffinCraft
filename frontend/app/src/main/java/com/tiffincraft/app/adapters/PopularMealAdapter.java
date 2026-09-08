package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PopularMealAdapter extends RecyclerView.Adapter<PopularMealAdapter.MealViewHolder> {
    private List<Meal> meals;
    private OnMealClickListener listener;

    public interface OnMealClickListener {
        void onMealClick(Meal meal);
        void onFavoriteClick(Meal meal, int position);
    }

    public PopularMealAdapter(List<Meal> meals, OnMealClickListener listener) {
        this.meals = meals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.bind(meal, position);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    class MealViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private ImageView imgMealPhoto;
        private ImageView btnFavorite;
        private TextView tvBestSeller;
        private TextView tvMealName;
        private CircleImageView imgCookPhoto;
        private TextView tvCookName;
        private TextView tvRating;
        private TextView tvDeliveryTime;
        private TextView tvPrice;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMealPhoto = itemView.findViewById(R.id.imgMealPhoto);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvBestSeller = itemView.findViewById(R.id.tvBestSeller);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            imgCookPhoto = itemView.findViewById(R.id.imgCookPhoto);
            tvCookName = itemView.findViewById(R.id.tvCookName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDeliveryTime = itemView.findViewById(R.id.tvDeliveryTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }

        public void bind(Meal meal, int position) {
            tvMealName.setText(meal.getName());
            tvCookName.setText(meal.getCookName() != null ? meal.getCookName() : "Unknown Cook");
            tvPrice.setText("₹" + String.format("%.0f", meal.getPrice()));

            // Rating
            if (meal.getCookRating() != null) {
                tvRating.setText("⭐ " + String.format("%.1f", meal.getCookRating()));
            } else {
                tvRating.setText("⭐ N/A");
            }

            tvDeliveryTime.setText(formatDistance(meal.getDistanceKm()));

            // Load meal + cook images (tunnel-safe + relative /uploads paths)
            ImageUrlHelper.load(imgMealPhoto, meal.getImageUrl(), R.drawable.meal_placeholder, 48);
            ImageUrlHelper.load(imgCookPhoto, meal.getCookImage(), R.drawable.avatar_cook);

            // The server awards this only from verified completed-order demand.
            if (meal.isBestseller()) {
                tvBestSeller.setVisibility(View.VISIBLE);
            } else {
                tvBestSeller.setVisibility(View.GONE);
            }

            btnFavorite.setImageResource(meal.isFavoriteCook()
                    ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            btnFavorite.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.red));
            btnFavorite.setContentDescription(meal.isFavoriteCook()
                    ? "Remove cook from favorites" : "Add cook to favorites");

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMealClick(meal);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(meal, position);
                }
            });
        }

        private String formatDistance(Double distanceKm) {
            if (distanceKm == null) return "Enable location";
            if (distanceKm < 1) return String.format(Locale.getDefault(), "%d m away", Math.round(distanceKm * 1000));
            return String.format(Locale.getDefault(), "%.1f km away", distanceKm);
        }
    }
}
