package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.CookProfile;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CookSearchResultAdapter extends RecyclerView.Adapter<CookSearchResultAdapter.ViewHolder> {

    public interface OnCookClickListener {
        void onCookClick(CookProfile cook);
    }

    private List<CookProfile> cooks = new ArrayList<>();
    private final OnCookClickListener listener;

    public CookSearchResultAdapter(OnCookClickListener listener) {
        this.listener = listener;
    }

    public void setCooks(List<CookProfile> cooks) {
        this.cooks = cooks != null ? cooks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cook_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(cooks.get(position));
    }

    @Override
    public int getItemCount() {
        return cooks.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView imgCook;
        private final TextView tvCookName;
        private final TextView tvCookMeta;
        private final TextView tvCookRating;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCook = itemView.findViewById(R.id.imgCook);
            tvCookName = itemView.findViewById(R.id.tvCookName);
            tvCookMeta = itemView.findViewById(R.id.tvCookMeta);
            tvCookRating = itemView.findViewById(R.id.tvCookRating);
        }

        void bind(CookProfile cook) {
            String displayName = cook.getKitchenName() != null && !cook.getKitchenName().isEmpty()
                    ? cook.getKitchenName()
                    : cook.getFullName();
            tvCookName.setText(displayName != null ? displayName : "Home Cook");

            String meta = cook.getAddress() != null && !cook.getAddress().isEmpty()
                    ? cook.getAddress()
                    : (cook.getFoodType() != null ? cook.getFoodType() : "");
            tvCookMeta.setText(meta);
            tvCookMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

            tvCookRating.setText(String.format("⭐ %.1f", cook.getRating()));

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
                if (listener != null) listener.onCookClick(cook);
            });
        }
    }
}
