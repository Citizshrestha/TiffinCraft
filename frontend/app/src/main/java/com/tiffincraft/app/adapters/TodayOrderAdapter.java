package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.ChatMessage;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.utils.CurrencyUtils;

import java.util.List;
import java.util.Locale;

/** Compact order rows for the cook home "Today's Orders" section. */
public class TodayOrderAdapter extends RecyclerView.Adapter<TodayOrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    /** Avatar palette keyed by customer name hash — initials on a solid circle. */
    private static final int[] AVATAR_COLORS = {
            0xFF4CAF50, 0xFF43A047, 0xFF00897B, 0xFF7CB342, 0xFF388E3C, 0xFF26A69A
    };

    private final Context context;
    private final List<Order> orders;
    private final OnOrderClickListener listener;

    public TodayOrderAdapter(Context context, List<Order> orders, OnOrderClickListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_today_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        String name = order.getCustomerName() != null && !order.getCustomerName().isEmpty()
                ? order.getCustomerName() : "Customer";
        holder.tvCustomerName.setText(name);

        holder.tvAvatarInitials.setText(initialsFor(name));
        int color = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
        holder.tvAvatarInitials.getBackground().mutate().setTint(color);

        holder.tvOrderTime.setText(ChatMessage.formatTime(order.getCreatedAt()));

        String itemsSummary = order.getItemsSummary();
        if (itemsSummary != null && !itemsSummary.isEmpty()) {
            // Two names, then "+N more" — this row is one line high.
            holder.tvMealSummary.setText(order.getItemsSummaryPreview(2));
        } else {
            String meal = order.getMealName() != null ? order.getMealName() : "Meal";
            holder.tvMealSummary.setText(meal + " x " + Math.max(order.getQuantity(), 1));
        }

        holder.tvOrderAmount.setText(CurrencyUtils.formatRupees(order.getTotalAmount()));

        bindStatusChip(holder.tvOrderStatus, order.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    private static String initialsFor(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private void bindStatusChip(TextView chip, String status) {
        String s = status != null ? status.toLowerCase(Locale.US) : "";
        int bgRes;
        int textColor;
        String label;
        switch (s) {
            case "pending":
                bgRes = R.drawable.status_chip_new;       textColor = 0xFF1565C0; label = "Pending";          break;
            case "confirmed":
                bgRes = R.drawable.status_chip_delivered; textColor = 0xFF2E7D32; label = "Confirmed";        break;
            case "preparing":
                bgRes = R.drawable.status_chip_preparing; textColor = 0xFFA8660B; label = "Preparing";        break;
            case "ready":
                bgRes = R.drawable.status_chip_out_for_delivery; textColor = 0xFF1565C0; label = "Ready";     break;
            case "delivered":
                bgRes = R.drawable.status_chip_delivered; textColor = 0xFF2E7D32; label = "Delivered";        break;
            case "cancelled":
                bgRes = R.drawable.status_chip_sold_out;  textColor = 0xFFC62828; label = "Cancelled";        break;
            default:
                bgRes = R.drawable.status_chip_new;       textColor = 0xFF1565C0;
                label = s.isEmpty() ? "Unknown" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
                break;
        }
        chip.setBackgroundResource(bgRes);
        chip.setTextColor(textColor);
        chip.setText(label);
    }

    @Override
    public int getItemCount() {
        return orders == null ? 0 : orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarInitials, tvCustomerName, tvOrderTime, tvMealSummary, tvOrderAmount, tvOrderStatus;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitials = itemView.findViewById(R.id.tvAvatarInitials);
            tvCustomerName   = itemView.findViewById(R.id.tvCustomerName);
            tvOrderTime      = itemView.findViewById(R.id.tvOrderTime);
            tvMealSummary    = itemView.findViewById(R.id.tvMealSummary);
            tvOrderAmount    = itemView.findViewById(R.id.tvOrderAmount);
            tvOrderStatus    = itemView.findViewById(R.id.tvOrderStatus);
        }
    }
}
