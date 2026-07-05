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
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.CartGroup;
import com.tiffincraft.app.models.CartItem;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface CartActionListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    // Flattened row model: either a header or an item
    private static class Row {
        boolean isHeader;
        CartGroup group;   // set if header
        CartItem item;     // set if item row
    }

    private final Context context;
    private final List<Row> rows = new ArrayList<>();
    private final CartActionListener listener;

    public CartAdapter(Context context, List<CartGroup> groups, CartActionListener listener) {
        this.context = context;
        this.listener = listener;
        setGroups(groups);
    }

    public void setGroups(List<CartGroup> groups) {
        rows.clear();
        for (CartGroup g : groups) {
            Row header = new Row();
            header.isHeader = true;
            header.group = g;
            rows.add(header);

            for (CartItem item : g.getItems()) {
                Row r = new Row();
                r.isHeader = false;
                r.item = item;
                rows.add(r);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_cart_cook_header, parent, false);
            return new HeaderHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_cart_meal, parent, false);
            return new ItemHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.isHeader && holder instanceof HeaderHolder) {
            HeaderHolder h = (HeaderHolder) holder;
            String name = row.group.getKitchenName() != null && !row.group.getKitchenName().isEmpty()
                    ? row.group.getKitchenName() : row.group.getCookName();
            h.tvCookName.setText(name);
            h.tvSubtotal.setText(String.format("Rs.%.0f", row.group.getSubtotal()));
        } else if (!row.isHeader && holder instanceof ItemHolder) {
            ItemHolder h = (ItemHolder) holder;
            CartItem item = row.item;

            h.tvMealName.setText(item.getMealName());
            h.tvPrice.setText(String.format("₹%.0f", item.getPrice()));
            h.tvQuantity.setText(String.valueOf(item.getQuantity()));

            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_food)
                    .centerCrop()
                    .into(h.imgMeal);

            if (!item.isAvailable()) {
                h.tvUnavailable.setVisibility(View.VISIBLE);
            } else {
                h.tvUnavailable.setVisibility(View.GONE);
            }

            h.tvMinus.setOnClickListener(v -> {
                int newQty = item.getQuantity() - 1;
                if (newQty < 1) {
                    listener.onRemoveItem(item);
                } else {
                    item.setQuantity(newQty);
                    h.tvQuantity.setText(String.valueOf(newQty));
                    listener.onQuantityChanged(item, newQty);
                }
            });

            h.tvPlus.setOnClickListener(v -> {
                int newQty = item.getQuantity() + 1;
                item.setQuantity(newQty);
                h.tvQuantity.setText(String.valueOf(newQty));
                listener.onQuantityChanged(item, newQty);
            });

            h.imgRemove.setOnClickListener(v -> listener.onRemoveItem(item));
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView tvCookName, tvSubtotal;
        HeaderHolder(View v) {
            super(v);
            tvCookName = v.findViewById(R.id.tvCookName);
            tvSubtotal = v.findViewById(R.id.tvSubtotal);
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        ImageView imgMeal, imgRemove;
        TextView tvMealName, tvPrice, tvQuantity, tvMinus, tvPlus, tvUnavailable;
        ItemHolder(View v) {
            super(v);
            imgMeal = v.findViewById(R.id.imgMeal);
            imgRemove = v.findViewById(R.id.imgRemove);
            tvMealName = v.findViewById(R.id.tvMealName);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvQuantity = v.findViewById(R.id.tvQuantity);
            tvMinus = v.findViewById(R.id.tvMinus);
            tvPlus = v.findViewById(R.id.tvPlus);
            tvUnavailable = v.findViewById(R.id.tvUnavailable);
        }
    }
}