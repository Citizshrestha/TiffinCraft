package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.CookProfile;

import java.util.List;

public class PopularCookAdapter extends RecyclerView.Adapter<PopularCookAdapter.CookViewHolder> {
    private List<CookProfile> cooks;
    private OnCookClickListener listener;

    public interface OnCookClickListener {
        void onCookClick(CookProfile cook);
    }

    public PopularCookAdapter(List<CookProfile> cooks, OnCookClickListener listener) {
        this.cooks = cooks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_cook, parent, false);
        return new CookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CookViewHolder holder, int position) {
        holder.bind(cooks.get(position));
    }

    @Override
    public int getItemCount() {
        return cooks.size();
    }

    class CookViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgCook;
        private TextView tvCookName;
        private TextView tvRating;
        private TextView tvDishCount;

        public CookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCook = itemView.findViewById(R.id.imgCook);
            tvCookName = itemView.findViewById(R.id.tvCookName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDishCount = itemView.findViewById(R.id.tvDishCount);
        }

        public void bind(CookProfile cook) {
            String name = cook.getKitchenName() != null ? cook.getKitchenName() : cook.getFullName();
            tvCookName.setText(name);
            tvRating.setText(String.format("⭐ %.1f (%d)", cook.getRating(), cook.getReviewCount()));
            tvDishCount.setText(cook.getDishCount() + " dishes");

            if (cook.getProfileImage() != null && !cook.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(cook.getProfileImage())
                        .placeholder(R.drawable.avatar_cook)
                        .error(R.drawable.avatar_cook)
                        .into(imgCook);
            } else {
                imgCook.setImageResource(R.drawable.avatar_cook);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCookClick(cook);
                }
            });
        }
    }
}
