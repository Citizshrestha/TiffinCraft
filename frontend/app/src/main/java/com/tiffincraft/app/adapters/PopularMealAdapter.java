package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Meal;

import java.util.List;

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

            // Delivery time
            if (meal.getPreparationTime() != null) {
                tvDeliveryTime.setText(meal.getPreparationTime() + " min");
            } else {
                tvDeliveryTime.setText("30 min");
            }

            // Load meal image
            if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(meal.getImageUrl())
                        .apply(new RequestOptions()
                                .transform(new RoundedCorners(48))
                                .placeholder(R.drawable.meal_placeholder)
                                .error(R.drawable.meal_placeholder))
                        .into(imgMealPhoto);
            } else {
                imgMealPhoto.setImageResource(R.drawable.meal_placeholder);
            }

            // Load cook image
            if (meal.getCookImage() != null && !meal.getCookImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(meal.getCookImage())
                        .placeholder(R.drawable.avatar_cook)
                        .error(R.drawable.avatar_cook)
                        .into(imgCookPhoto);
            } else {
                imgCookPhoto.setImageResource(R.drawable.avatar_cook);
            }

            // Show best seller badge (you can add logic based on rating or sales)
            if (meal.getCookRating() != null && meal.getCookRating() >= 4.5) {
                tvBestSeller.setVisibility(View.VISIBLE);
            } else {
                tvBestSeller.setVisibility(View.GONE);
            }

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
    }
}
