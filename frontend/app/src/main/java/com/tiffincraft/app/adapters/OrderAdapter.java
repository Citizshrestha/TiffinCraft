package com.tiffincraft.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Order;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onActionClick(Order order, String nextStatus);
    }

    private final Context context;
    private List<Order> orders;
    private final OnOrderActionListener listener;

    public OrderAdapter(Context context, List<Order> orders, OnOrderActionListener listener) {
        this.context = context;
        this.orders = orders != null ? orders : new ArrayList<>();
        this.listener = listener;
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders != null ? newOrders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        // Order ID + time in subtitle
        holder.tvOrderId.setText(String.format("Order #%d · %s", order.getId(), formatDate(order.getCreatedAt())));

        // Status badge
        String status = order.getStatus() != null ? order.getStatus() : "pending";
        holder.tvStatus.setText(formatStatus(status));
        applyStatusStyle(holder.tvStatus, status);

        // Customer
        String customerName = order.getCustomerName();
        holder.tvCustomerName.setText(
                customerName != null && !customerName.isEmpty() ? customerName : "Customer"
        );

        // Meal info
        String mealName = order.getMealName();
        int qty = order.getQuantity();
        holder.tvMealInfo.setText(
                (mealName != null ? mealName : "Meal") + " × " + (qty > 0 ? qty : 1)
        );

        // Amount
        holder.tvAmount.setText(String.format("₹%.0f", order.getTotalAmount()));

        // Address
        String address = order.getDeliveryAddress();
        holder.tvAddress.setText(address != null ? address : "—");

        // Time is shown in tvOrderId subtitle

        // Action button
        String nextStatus = getNextStatus(status);
        if (nextStatus != null) {
            holder.btnNextAction.setVisibility(View.VISIBLE);
            holder.btnNextAction.setText(getActionLabel(nextStatus));
            holder.btnNextAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getActionColor(nextStatus))
            );
            holder.btnNextAction.setOnClickListener(v -> {
                if (listener != null) listener.onActionClick(order, nextStatus);
            });
        } else {
            holder.btnNextAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String formatStatus(String status) {
        switch (status) {
            case "pending":          return "New Order";
            case "confirmed":        return "Confirmed";
            case "preparing":        return "Preparing";
            case "ready":            return "Ready";
            case "delivered":        return "Delivered";
            case "cancelled":        return "Cancelled";
            default:                 return status;
        }
    }

    private void applyStatusStyle(TextView tv, String status) {
        int bg, fg;
        switch (status) {
            case "pending":
                bg = 0xFFFFF3E0; fg = 0xFFE65100; break;
            case "confirmed":
                bg = 0xFFE3F2FD; fg = 0xFF1565C0; break;
            case "preparing":
                bg = 0xFFF3E5F5; fg = 0xFF6A1B9A; break;
            case "ready":
                bg = 0xFFE0F7FA; fg = 0xFF00838F; break;
            case "delivered":
                bg = 0xFFE8F5E9; fg = 0xFF2E7D32; break;
            case "cancelled":
                bg = 0xFFFFEBEE; fg = 0xFFC62828; break;
            default:
                bg = 0xFFF5F5F5; fg = 0xFF555555; break;
        }
        tv.setBackgroundColor(bg);
        tv.setTextColor(fg);
        // Rounded corners via padding only (no shape drawable needed)
        tv.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));

        // Use a rounded corner background programmatically
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(bg);
        gd.setCornerRadius(dpToPx(20));
        tv.setBackground(gd);
        tv.setTextColor(fg);
    }

    /** Returns the next status that the cook can transition to, or null if no action */
    private String getNextStatus(String current) {
        switch (current) {
            case "pending":   return "confirmed";
            case "confirmed": return "preparing";
            case "preparing": return "ready";
            case "ready":     return "delivered";
            default:          return null;
        }
    }

    private String getActionLabel(String nextStatus) {
        switch (nextStatus) {
            case "confirmed":        return "Confirm Order";
            case "preparing":        return "Start Preparing";
            case "ready":            return "Mark Ready";
            case "delivered":        return "Mark Delivered";
            default:                 return "Update";
        }
    }

    private int getActionColor(String nextStatus) {
        switch (nextStatus) {
            case "confirmed":        return 0xFF1565C0; // Blue
            case "preparing":        return 0xFF6A1B9A; // Purple
            case "ready":            return 0xFF00838F; // Teal
            case "delivered":        return 0xFF2E7D32; // Dark green
            default:                 return 0xFF4CAF50;
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "—";

        Date date = null;
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss"
        };
        for (String pattern : patterns) {
            SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.getDefault());
            if (pattern.contains("'Z'")) {
                fmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            }
            try {
                date = fmt.parse(isoDate);
                if (date != null) break;
            } catch (ParseException ignored) { }
        }
        if (date == null) return isoDate;

        Date now = new Date();
        long diffMs = now.getTime() - date.getTime();
        long diffDays = diffMs / (1000 * 60 * 60 * 24);

        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        if (diffDays == 0) {
            return "Today, " + timeFmt.format(date);
        } else if (diffDays == 1) {
            return "Yesterday, " + timeFmt.format(date);
        } else {
            SimpleDateFormat fullFmt = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            return fullFmt.format(date);
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvCustomerName, tvMealInfo,
                 tvAmount, tvAddress, tvTime;
        MaterialButton btnNextAction;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId      = itemView.findViewById(R.id.tvOrderId);
            tvStatus       = itemView.findViewById(R.id.tvStatus);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvMealInfo     = itemView.findViewById(R.id.tvMealInfo);
            tvAmount       = itemView.findViewById(R.id.tvAmount);
            tvAddress      = itemView.findViewById(R.id.tvAddress);
            tvTime         = itemView.findViewById(R.id.tvTime);
            btnNextAction  = itemView.findViewById(R.id.btnNextAction);
        }
    }
}
