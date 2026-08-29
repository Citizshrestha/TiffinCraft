package com.tiffincraft.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    /** Meal names shown on the card; the rest collapse into "+N more". */
    private static final int CARD_ITEM_PREVIEW = 2;

    public interface OnOrderActionListener {
        void onActionClick(Order order, String nextStatus);
        void onVerifyPaymentClick(Order order);
        void onDeleteOrder(Order order);
        void onOrderClick(Order order);
        void onTrackOrder(Order order);
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

        // Items count
        int totalItems = order.getItemsCount() > 0
                ? order.getItemsCount()
                : Math.max(order.getQuantity(), 1);
        holder.tvItemsCount.setText(totalItems + (totalItems == 1 ? " item in order" : " items in order"));

        // Setup image carousel
        setupImageCarousel(holder, order);

        // Setup item chips
        setupItemChips(holder, order);

        // Status badge with icon
        String status = order.getStatus() != null ? order.getStatus() : "pending";
        holder.tvStatus.setText(formatStatus(status));
        applyStatusStyle(holder.tvStatus, status);

        // Payment method + date (same row style as the customer's card)
        String paymentLabel = "cod".equalsIgnoreCase(order.getPaymentMethod())
                ? "Cash on Delivery" : "Online Payment";
        holder.tvOrderDate.setText(paymentLabel + " · " + formatDateShort(order.getCreatedAt()));

        // Order ID
        holder.tvOrderId.setText("Order #" + order.getId());

        // Customer name
        String customerName = order.getCustomerName();
        holder.tvCustomerName.setText(
                customerName != null && !customerName.isEmpty() ? customerName : "Customer"
        );

        // Order summary — two names, then "+N more". The cook opens the order to
        // see everything they have to cook; the card is for recognising it.
        String itemsSummary = order.getItemsSummary();
        if (itemsSummary != null && !itemsSummary.isEmpty()) {
            holder.tvOrderSummary.setText(order.getItemsSummaryPreview(CARD_ITEM_PREVIEW));
        } else {
            String mealName = order.getMealName();
            int qty = order.getQuantity();
            holder.tvOrderSummary.setText(
                    qty + "× " + (mealName != null ? mealName : "Meal")
            );
        }

        // Special instructions
        String specialInstructions = order.getSpecialInstructions();
        if (specialInstructions != null && !specialInstructions.isEmpty()) {
            holder.layoutSpecialInstructions.setVisibility(View.VISIBLE);
            holder.tvSpecialInstructions.setText(specialInstructions);
        } else {
            holder.layoutSpecialInstructions.setVisibility(View.GONE);
        }

        // Verify Payment (cook-only) — shown only while an online-payment order
        // has a customer-uploaded screenshot awaiting the cook's verification.
        // An eSewa-confirmed payment also reaches payment_status="paid" but
        // needs no manual verification — eSewa already confirmed it.
        boolean needsPaymentVerification = "online".equals(order.getPaymentMethod())
                && "paid".equals(order.getPaymentStatus())
                && !order.isEsewaConfirmed();
        holder.btnVerifyPayment.setVisibility(needsPaymentVerification ? View.VISIBLE : View.GONE);
        holder.btnVerifyPayment.setOnClickListener(v -> {
            if (listener != null) listener.onVerifyPaymentClick(order);
        });

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

        // Three-dot menu button - only show for completed/delivered orders
        boolean canDelete = "delivered".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
        holder.btnMoreOptions.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.btnMoreOptions.setOnClickListener(v -> showDeleteConfirmation(v, order));

        // Open details — card tap and the explicit button both land on the same screen
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
        holder.btnTrackOrder.setOnClickListener(v -> {
            if (listener != null) listener.onTrackOrder(order);
        });
    }

    private void showDeleteConfirmation(View anchorView, Order order) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(context, anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_order_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_order) {
                // Show confirmation dialog
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Delete Order")
                        .setMessage("Are you sure you want to delete Order #" + order.getId() + "? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (listener != null) {
                                listener.onDeleteOrder(order);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void setupImageCarousel(OrderViewHolder holder, Order order) {
        // Real per-item carousel (one page per order item, with its own image) —
        // identical behavior to the customer's card. Falls back to the single
        // representative image when no per-item data came back.
        List<Order.OrderItem> items = order.getItems();

        if (items != null && !items.isEmpty()) {
            holder.imgMealPlaceholder.setVisibility(View.GONE);
            holder.viewPagerImages.setVisibility(View.VISIBLE);

            OrderItemCarouselAdapter carouselAdapter =
                    new OrderItemCarouselAdapter(items, order.getSpecialInstructions());
            holder.viewPagerImages.setAdapter(carouselAdapter);

            if (items.size() > 1) {
                holder.btnPrevImage.setVisibility(View.VISIBLE);
                holder.btnNextImage.setVisibility(View.VISIBLE);
                holder.layoutIndicators.setVisibility(View.VISIBLE);

                setupCarouselNavigation(holder, items.size());
                setupIndicators(holder, items.size());
            } else {
                holder.btnPrevImage.setVisibility(View.GONE);
                holder.btnNextImage.setVisibility(View.GONE);
                holder.layoutIndicators.setVisibility(View.GONE);
            }
        } else {
            holder.imgMealPlaceholder.setVisibility(View.VISIBLE);
            holder.viewPagerImages.setVisibility(View.GONE);
            holder.btnPrevImage.setVisibility(View.GONE);
            holder.btnNextImage.setVisibility(View.GONE);
            holder.layoutIndicators.setVisibility(View.GONE);

            String mealImage = order.getMealImage();
            if (mealImage != null && !mealImage.isEmpty()) {
                ImageUrlHelper.load(holder.imgMealPlaceholder, mealImage, R.drawable.ic_meal);
            }
        }
    }

    private void setupCarouselNavigation(OrderViewHolder holder, int imageCount) {
        holder.btnPrevImage.setOnClickListener(v -> {
            int current = holder.viewPagerImages.getCurrentItem();
            if (current > 0) {
                holder.viewPagerImages.setCurrentItem(current - 1, true);
            }
        });

        holder.btnNextImage.setOnClickListener(v -> {
            int current = holder.viewPagerImages.getCurrentItem();
            if (current < imageCount - 1) {
                holder.viewPagerImages.setCurrentItem(current + 1, true);
            }
        });

        holder.viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(holder, position, imageCount);
            }
        });
    }

    private void setupIndicators(OrderViewHolder holder, int count) {
        holder.layoutIndicators.removeAllViews();

        for (int i = 0; i < count; i++) {
            View indicator = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(6), dpToPx(6)
            );
            params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            indicator.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(i == 0 ? 0xFFFFFFFF : 0x66FFFFFF);
            indicator.setBackground(gd);

            holder.layoutIndicators.addView(indicator);
        }
    }

    private void updateIndicators(OrderViewHolder holder, int position, int count) {
        for (int i = 0; i < holder.layoutIndicators.getChildCount(); i++) {
            View indicator = holder.layoutIndicators.getChildAt(i);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(i == position ? 0xFFFFFFFF : 0x66FFFFFF);
            indicator.setBackground(gd);
        }
    }

    private void setupItemChips(OrderViewHolder holder, Order order) {
        holder.layoutItemChips.removeAllViews();

        String itemsSummary = order.getItemsSummary();
        if (itemsSummary != null && !itemsSummary.isEmpty()) {
            String[] parts = itemsSummary.split(",\\s*");
            int shown = Math.min(CARD_ITEM_PREVIEW, parts.length);
            for (int i = 0; i < shown; i++) {
                addItemChip(holder.layoutItemChips, parts[i]);
            }
            if (parts.length > shown) {
                addItemChip(holder.layoutItemChips, "+" + (parts.length - shown) + " more");
            }
        } else {
            String mealName = order.getMealName();
            int qty = order.getQuantity();
            if (mealName != null && !mealName.isEmpty()) {
                addItemChip(holder.layoutItemChips, qty + "× " + mealName);
            }
        }
    }

    private void addItemChip(LinearLayout container, String text) {
        TextView chip = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(dpToPx(8));
        chip.setLayoutParams(params);

        chip.setText(text.trim());
        chip.setTextSize(11.5f);
        chip.setTextColor(0xFF2E7D32);
        chip.setBackgroundResource(R.drawable.bg_pill_green_soft);
        chip.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

        container.addView(chip);
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
            case "ready":            return "Ready for Pickup";
            case "delivered":        return "Delivered";
            case "cancelled":        return "Cancelled";
            default:                 return status;
        }
    }

    private void applyStatusStyle(TextView tv, String status) {
        // Same drawable chips + colors as the customer's order card for a consistent look.
        String s = status != null ? status.toLowerCase(Locale.US) : "";
        int bgRes, fg, iconRes;
        switch (s) {
            case "pending":
                bgRes = R.drawable.status_chip_new;             fg = 0xFF1565C0; iconRes = R.drawable.ic_clock;        break;
            case "confirmed":
                bgRes = R.drawable.status_chip_delivered;       fg = 0xFF2E7D32; iconRes = R.drawable.ic_check_circle; break;
            case "preparing":
                bgRes = R.drawable.status_chip_preparing;       fg = 0xFFA8660B; iconRes = R.drawable.ic_clock;        break;
            case "ready":
                bgRes = R.drawable.status_chip_out_for_delivery; fg = 0xFF1565C0; iconRes = R.drawable.ic_check_circle; break;
            case "delivered":
                bgRes = R.drawable.status_chip_delivered;       fg = 0xFF2E7D32; iconRes = R.drawable.ic_check_circle; break;
            case "cancelled":
                bgRes = R.drawable.status_chip_sold_out;        fg = 0xFFC62828; iconRes = R.drawable.ic_close;        break;
            default:
                bgRes = R.drawable.status_chip_new;             fg = 0xFF555555; iconRes = R.drawable.ic_info;         break;
        }

        tv.setBackgroundResource(bgRes);
        tv.setTextColor(fg);

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
        tv.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(fg));
    }

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
        // All action buttons use consistent green theme
        return 0xFF4CAF50;
    }

    private String formatDateShort(String isoDate) {
        return com.tiffincraft.app.utils.TimeFormat.dayOnly(isoDate);
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemsCount, tvOrderId, tvStatus, tvOrderDate, tvCustomerName,
                 tvOrderSummary, tvSpecialInstructions;
        ViewPager2 viewPagerImages;
        ImageView imgMealPlaceholder;
        ImageButton btnPrevImage, btnNextImage, btnMoreOptions;
        LinearLayout layoutIndicators, layoutItemChips, layoutSpecialInstructions;
        MaterialButton btnNextAction, btnVerifyPayment, btnViewDetails, btnTrackOrder;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemsCount                = itemView.findViewById(R.id.tvItemsCount);
            tvOrderId                   = itemView.findViewById(R.id.tvOrderId);
            tvStatus                    = itemView.findViewById(R.id.tvStatus);
            tvOrderDate                 = itemView.findViewById(R.id.tvOrderDate);
            tvCustomerName              = itemView.findViewById(R.id.tvCustomerName);
            tvOrderSummary              = itemView.findViewById(R.id.tvOrderSummary);
            tvSpecialInstructions       = itemView.findViewById(R.id.tvSpecialInstructions);
            viewPagerImages             = itemView.findViewById(R.id.viewPagerImages);
            imgMealPlaceholder          = itemView.findViewById(R.id.imgMealPlaceholder);
            btnPrevImage                = itemView.findViewById(R.id.btnPrevImage);
            btnNextImage                = itemView.findViewById(R.id.btnNextImage);
            btnMoreOptions              = itemView.findViewById(R.id.btnMoreOptions);
            layoutIndicators            = itemView.findViewById(R.id.layoutIndicators);
            layoutItemChips             = itemView.findViewById(R.id.layoutItemChips);
            layoutSpecialInstructions   = itemView.findViewById(R.id.layoutSpecialInstructions);
            btnNextAction               = itemView.findViewById(R.id.btnNextAction);
            btnVerifyPayment            = itemView.findViewById(R.id.btnVerifyPayment);
            btnViewDetails              = itemView.findViewById(R.id.btnViewDetails);
            btnTrackOrder               = itemView.findViewById(R.id.btnTrackOrder);
        }
    }
}
