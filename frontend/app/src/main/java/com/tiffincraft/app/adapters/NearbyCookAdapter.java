package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Cook;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NearbyCookAdapter extends RecyclerView.Adapter<NearbyCookAdapter.CookViewHolder> {
    private List<Cook> cooks;
    private OnCookClickListener listener;

    public interface OnCookClickListener {
        void onCookClick(Cook cook);
        void onViewMenuClick(Cook cook);
    }

    public NearbyCookAdapter(List<Cook> cooks, OnCookClickListener listener) {
        this.cooks = cooks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_cook, parent, false);
        return new CookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CookViewHolder holder, int position) {
        Cook cook = cooks.get(position);
        holder.bind(cook);
    }

    @Override
    public int getItemCount() {
        return cooks.size();
    }

    class CookViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView imgCookPhoto;
        private TextView tvCookName;
        private TextView tvRating;
        private TextView tvDistance;
        private TextView tvDishCount;
        private TextView tvReviewCount;
        private MaterialButton btnViewMenu;

        public CookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCookPhoto = itemView.findViewById(R.id.imgCookPhoto);
            tvCookName = itemView.findViewById(R.id.tvCookName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDishCount = itemView.findViewById(R.id.tvDishCount);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            btnViewMenu = itemView.findViewById(R.id.btnViewMenu);
        }

        public void bind(Cook cook) {
            tvCookName.setText(cook.getKitchenName());
            tvRating.setText("⭐ " + String.format("%.1f", cook.getRating()));
            tvDistance.setText(cook.getDistance() + " away");
            tvDishCount.setText(cook.getDishCount() + " Dishes");
            tvReviewCount.setText(cook.getReviewCount() + " Reviews");

            // Load cook profile image
            if (cook.getProfileImage() != null && !cook.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(cook.getProfileImage())
                        .placeholder(R.drawable.avatar_cook)
                        .error(R.drawable.avatar_cook)
                        .into(imgCookPhoto);
            } else {
                imgCookPhoto.setImageResource(R.drawable.avatar_cook);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCookClick(cook);
                }
            });

            btnViewMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewMenuClick(cook);
                }
            });
        }
    }
}
