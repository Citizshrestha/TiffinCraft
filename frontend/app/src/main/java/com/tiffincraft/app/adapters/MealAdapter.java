package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private Context context;
    private List<Meal> mealsList;
    private OnMealClickListener listener;

    public interface OnMealClickListener {
        void onEditClick(Meal meal);
        void onAvailabilityToggle(Meal meal, boolean isAvailable);
        void onSubscriptionClick(Meal meal);
        void onDeleteClick(Meal meal);
    }

    public MealAdapter(Context context, List<Meal> mealsList, OnMealClickListener listener) {
        this.context = context;
        this.mealsList = mealsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealsList.get(position);
        
        holder.tvMealName.setText(meal.getName());
        holder.tvMealDescription.setText(meal.getDescription());
        holder.tvMealPrice.setText(CurrencyUtils.formatRupees(meal.getPrice()));
        
        // Load meal image with category-based placeholders
        int placeholderImage = R.drawable.meal_veg_thali; // Default
        
        // Choose placeholder based on category
        if (meal.getCategory() != null) {
            String category = meal.getCategory().toLowerCase();
            if (category.contains("dinner")) {
                placeholderImage = R.drawable.meal_special_thali;
            } else if (category.contains("snacks") || category.contains("dessert")) {
                placeholderImage = R.drawable.meal_paneer_thali;
            }
        }
        
        ImageUrlHelper.load(holder.imgMeal, meal.getImageUrl(), placeholderImage);
        
        // Set availability switch
        holder.switchAvailability.setOnCheckedChangeListener(null);
        holder.switchAvailability.setChecked(meal.isAvailable());
        holder.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onAvailabilityToggle(meal, isChecked);
            }
        });
        
        // Apply unavailable styling
        float alpha = meal.isAvailable() ? 1.0f : 0.4f;
        holder.imgMeal.setAlpha(alpha);
        holder.tvMealName.setTextColor(meal.isAvailable() ? 
            context.getColor(R.color.text_primary) : 
            context.getColor(R.color.text_disabled));
        
        // Show sold out badge if not available
        if (!meal.isAvailable()) {
            holder.tvSoldOut.setVisibility(View.VISIBLE);
        } else {
            holder.tvSoldOut.setVisibility(View.GONE);
        }
        
        // Edit button
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(meal);
            }
        });
        
        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(meal);
            }
        });
        
        // Add to subscription button - toggle based on subscription status
        if (meal.isInSubscription()) {
            holder.btnAddSubscription.setText("Added to Subscription");
            holder.btnAddSubscription.setBackgroundResource(R.drawable.btn_green_filled);
            holder.btnAddSubscription.setTextColor(context.getColor(R.color.white));
        } else {
            holder.btnAddSubscription.setText("Add to Subscription");
            holder.btnAddSubscription.setBackgroundResource(R.drawable.btn_green_outline);
            holder.btnAddSubscription.setTextColor(context.getColor(R.color.green_primary_dark));
        }
        
        holder.btnAddSubscription.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubscriptionClick(meal);
            }
        });
        
        // Meal card click
        holder.cardMeal.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealsList != null ? mealsList.size() : 0;
    }

    public void updateMeals(List<Meal> newMeals) {
        this.mealsList = newMeals;
        notifyDataSetChanged();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardMeal;
        ImageView imgMeal, btnEdit, btnDelete;
        TextView tvMealName, tvMealDescription, tvMealPrice, tvSoldOut, btnAddSubscription;
        SwitchMaterial switchAvailability;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMeal = itemView.findViewById(R.id.cardMeal);
            imgMeal = itemView.findViewById(R.id.imgMeal);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvMealDescription = itemView.findViewById(R.id.tvMealDescription);
            tvMealPrice = itemView.findViewById(R.id.tvMealPrice);
            tvSoldOut = itemView.findViewById(R.id.tvSoldOut);
            switchAvailability = itemView.findViewById(R.id.switchAvailability);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnAddSubscription = itemView.findViewById(R.id.btnAddSubscription);
        }
    }
}
