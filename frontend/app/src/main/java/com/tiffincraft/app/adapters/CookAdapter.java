package com.tiffincraft.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.meal.CookDetailsActivity;
import com.tiffincraft.app.models.CookProfile;

import java.util.ArrayList;
import java.util.List;

public class CookAdapter extends RecyclerView.Adapter<CookAdapter.CookViewHolder> {

    private List<CookProfile> cooks;
    private final Context context;

    public CookAdapter(Context context, List<CookProfile> cooks) {
        this.context = context;
        this.cooks = cooks != null ? cooks : new ArrayList<>();
    }

    public void setCooks(List<CookProfile> newCooks) {
        this.cooks = newCooks != null ? newCooks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cook, parent, false);
        return new CookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CookViewHolder holder, int position) {
        CookProfile cook = cooks.get(position);

        String displayName = cook.getKitchenName() != null ? cook.getKitchenName() : cook.getFullName();
        holder.tvName.setText(displayName);

        if (holder.imgCook != null) {
            Glide.with(context)
                    .load(cook.getProfileImage())
                    .placeholder(R.drawable.ic_default_avatar)
                    .centerCrop()
                    .into(holder.imgCook);
        }

       holder.itemView.setOnClickListener(v -> {
    Intent intent = new Intent(context, CookDetailsActivity.class);
    intent.putExtra(CookDetailsActivity.EXTRA_COOK_ID, cook.getUserId());
    context.startActivity(intent);
});

if (holder.btnOrder != null) {
    holder.btnOrder.setOnClickListener(v -> {
        Intent intent = new Intent(context, CookDetailsActivity.class);
        intent.putExtra(CookDetailsActivity.EXTRA_COOK_ID, cook.getUserId());
        context.startActivity(intent);
    });
}
    }

    @Override
    public int getItemCount() {
        return cooks.size();
    }

    public static class CookViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgCook;
        View btnOrder;

        public CookViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCookName);
            imgCook = itemView.findViewById(R.id.imgCook);
            btnOrder = itemView.findViewById(R.id.btnOrder);
        }
    }
}