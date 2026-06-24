package com.tiffincraft.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.CookDetailsActivity;

import java.util.List;

public class CookAdapter extends RecyclerView.Adapter<CookAdapter.CookViewHolder> {

    private List<String> cookNames;
    private Context context;

    public CookAdapter(Context context, List<String> cookNames) {
        this.context = context;
        this.cookNames = cookNames;
    }

    @NonNull
    @Override
    public CookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cook, parent, false);
        return new CookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CookViewHolder holder, int position) {
        String name = cookNames.get(position);
        holder.tvName.setText(name);
        
        // Link to Details Page
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CookDetailsActivity.class);
            intent.putExtra("cook_name", name);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cookNames.size();
    }

    public static class CookViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public CookViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCookName);
        }
    }
}