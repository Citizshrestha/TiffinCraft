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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Meal;

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
        holder.tvMealPrice.setText("₹" + meal.getPrice());
        
        // Load meal image
        if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
            String imageUrl = "http://192.168.100.115:5000" + meal.getImageUrl();
            Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.meal_veg_thali)
                .error(R.drawable.meal_veg_thali)
                .centerCrop()
                .into(holder.imgMeal);
        } else {
            holder.imgMeal.setImageResource(R.drawable.meal_veg_thali);
        }
        
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
        
        // Add to subscription button
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
