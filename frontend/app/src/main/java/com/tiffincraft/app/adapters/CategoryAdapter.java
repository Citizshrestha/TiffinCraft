package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, position);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView categoryCard;
        private TextView tvCategoryEmoji;
        private TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryCard = itemView.findViewById(R.id.categoryCard);
            tvCategoryEmoji = itemView.findViewById(R.id.tvCategoryEmoji);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }

        public void bind(Category category, int position) {
            tvCategoryEmoji.setText(category.getEmoji());
            tvCategoryName.setText(category.getName());

            // Update UI based on selection
            if (position == selectedPosition) {
                categoryCard.setCardBackgroundColor(itemView.getContext().getColor(R.color.green_primary));
                tvCategoryName.setTextColor(itemView.getContext().getColor(android.R.color.white));
                categoryCard.setStrokeWidth(0);
            } else {
                // setCardBackgroundColor only (not setBackgroundColor) — MaterialCardView
                // renders its own background drawable for the rounded shape; calling plain
                // View.setBackgroundColor on a recycled holder replaced that drawable and
                // left the next-selected state's green background not actually showing,
                // so the white selected text had nothing to contrast against.
                categoryCard.setCardBackgroundColor(0xFFF8F8F8);
                tvCategoryName.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                categoryCard.setStrokeWidth(0);
            }

            categoryCard.setOnClickListener(v -> {
                if (listener != null) {
                    setSelectedPosition(getAdapterPosition());
                    listener.onCategoryClick(category, getAdapterPosition());
                }
            });
        }
    }
}
