package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.List;

/**
 * One page per order item (real meal image + name + qty), used by the "My Orders"
 * card so a multi-item order can be swiped through instead of only showing one
 * representative image. The order-level special-instructions note is only ever
 * attached to the first page — order_items has no per-item note in the schema, so
 * showing it on every page would misleadingly imply it applies to each meal.
 */
public class OrderItemCarouselAdapter extends RecyclerView.Adapter<OrderItemCarouselAdapter.PageViewHolder> {

    private final List<Order.OrderItem> items;
    @Nullable private final String firstPageNote;

    public OrderItemCarouselAdapter(List<Order.OrderItem> items, @Nullable String firstPageNote) {
        this.items = items;
        this.firstPageNote = firstPageNote;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_carousel_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Order.OrderItem item = items.get(position);

        ImageUrlHelper.load(holder.imgOrderItem, item.getImageUrl(), R.drawable.ic_meal);
        holder.tvItemName.setText(item.getMealName() != null ? item.getMealName() : "Meal");
        holder.tvItemQty.setText("×" + item.getQuantity());

        if (position == 0 && firstPageNote != null && !firstPageNote.isEmpty()) {
            holder.tvItemNote.setVisibility(View.VISIBLE);
            holder.tvItemNote.setText(firstPageNote);
        } else {
            holder.tvItemNote.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgOrderItem;
        final TextView tvItemName;
        final TextView tvItemNote;
        final TextView tvItemQty;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOrderItem = itemView.findViewById(R.id.imgOrderItem);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemNote = itemView.findViewById(R.id.tvItemNote);
            tvItemQty = itemView.findViewById(R.id.tvItemQty);
        }
    }
}
