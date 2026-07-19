package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.List;

public class RecommendedMealAdapter extends RecyclerView.Adapter<RecommendedMealAdapter.MealViewHolder> {
    private List<Meal> meals;
    private OnMealActionListener listener;
    private final int itemLayoutRes;

    public interface OnMealActionListener {
        void onMealClick(Meal meal);
        void onFavoriteClick(Meal meal, int position);
        void onAddToCartClick(Meal meal);
    }

    /** Uses the grid card layout (match_parent width) — for the Menu screen's grid. */
    public RecommendedMealAdapter(List<Meal> meals, OnMealActionListener listener) {
        this(meals, listener, false);
    }

    /**
     * @param horizontal when true, uses the fixed-width card layout meant for a
     *                   horizontal carousel (e.g. the home screen's "Recommended For You" row).
     */
    public RecommendedMealAdapter(List<Meal> meals, OnMealActionListener listener, boolean horizontal) {
        this.meals = meals;
        this.listener = listener;
        this.itemLayoutRes = horizontal ? R.layout.item_recommended_meal_horizontal : R.layout.item_recommended_meal;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
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
        private ImageView imgMealPhoto;
        private ImageView btnFavorite;
        private TextView tvFoodType;
        private TextView tvMealName;
        private TextView tvCookName;
        private TextView tvRating;
        private TextView tvDeliveryTime;
        private TextView tvPrice;
        private MaterialButton btnAddToCart;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMealPhoto = itemView.findViewById(R.id.imgMealPhoto);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvFoodType = itemView.findViewById(R.id.tvFoodType);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvCookName = itemView.findViewById(R.id.tvCookName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDeliveryTime = itemView.findViewById(R.id.tvDeliveryTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
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

            // Food type badge
            if (meal.isVegetarian()) {
                tvFoodType.setText("🌱 Veg");
                tvFoodType.setVisibility(View.VISIBLE);
            } else if (meal.isVegan()) {
                tvFoodType.setText("🌿 Vegan");
                tvFoodType.setVisibility(View.VISIBLE);
            } else {
                tvFoodType.setText("🍖 Non-Veg");
                tvFoodType.setVisibility(View.VISIBLE);
            }

            // Load meal image (tunnel-safe + relative /uploads paths)
            ImageUrlHelper.load(imgMealPhoto, meal.getImageUrl(), R.drawable.meal_placeholder, 48);

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

            btnAddToCart.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCartClick(meal);
                }
            });
        }
    }
}
