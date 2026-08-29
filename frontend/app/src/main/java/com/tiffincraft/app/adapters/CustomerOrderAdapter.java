package com.tiffincraft.app.adapters;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Order-history card for the customer's "My Orders" screen — one card per order
 * (already aggregated server-side), as opposed to {@link OrderAdapter} which is
 * the cook's per-incoming-order manage view with status-transition actions.
 */
public class CustomerOrderAdapter extends RecyclerView.Adapter<CustomerOrderAdapter.OrderViewHolder> {

    /** Meal names shown on the card; the rest collapse into "+N more". */
    private static final int CARD_ITEM_PREVIEW = 2;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public interface OnTrackOrderListener {
        void onTrackOrder(Order order);
    }

    public interface OnCancelOrderListener {
        void onCancelOrder(Order order);
    }

    /** Orders in one of these statuses are still cancellable-eligible (matches OrderHistoryActivity's "Active" filter). */
    private static final java.util.Set<String> CANCELLABLE_STATUSES = new java.util.HashSet<>(
            java.util.Arrays.asList("pending", "confirmed", "preparing", "ready"));

    private final List<Order> orders;
    private final OnOrderClickListener listener;
    private final OnTrackOrderListener trackListener;
    private final OnCancelOrderListener cancelListener;

    public CustomerOrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this(orders, listener, null, null);
    }

    public CustomerOrderAdapter(List<Order> orders, OnOrderClickListener listener, OnTrackOrderListener trackListener) {
        this(orders, listener, trackListener, null);
    }

    public CustomerOrderAdapter(List<Order> orders, OnOrderClickListener listener,
                                 OnTrackOrderListener trackListener, OnCancelOrderListener cancelListener) {
        this.orders = orders;
        this.listener = listener;
        this.trackListener = trackListener;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_customer, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvItemsCount;
        private final ViewPager2 viewPagerImages;
        private final ImageView imgMealPlaceholder;
        private final ImageButton btnPrevImage;
        private final ImageButton btnNextImage;
        private final LinearLayout layoutIndicators;
        private final LinearLayout layoutItemChips;
        private final TextView tvNewBadge;
        private final TextView tvItemCountBadge;
        private final TextView tvStatus;
        private final TextView tvOrderDate;
        private final TextView tvOrderId;
        private final TextView tvCustomerName; // repurposed to show the cook/kitchen name
        private final TextView tvOrderSummary;
        private final LinearLayout layoutSpecialInstructions;
        private final TextView tvSpecialInstructions;
        private final MaterialButton btnTrackOrder;
        private final MaterialButton btnCancelOrder;
        private final MaterialButton btnViewDetails;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
            viewPagerImages = itemView.findViewById(R.id.viewPagerImages);
            imgMealPlaceholder = itemView.findViewById(R.id.imgMealPlaceholder);
            btnPrevImage = itemView.findViewById(R.id.btnPrevImage);
            btnNextImage = itemView.findViewById(R.id.btnNextImage);
            layoutIndicators = itemView.findViewById(R.id.layoutIndicators);
            layoutItemChips = itemView.findViewById(R.id.layoutItemChips);
            tvNewBadge = itemView.findViewById(R.id.tvNewBadge);
            tvItemCountBadge = itemView.findViewById(R.id.tvItemCountBadge);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderSummary = itemView.findViewById(R.id.tvOrderSummary);
            layoutSpecialInstructions = itemView.findViewById(R.id.layoutSpecialInstructions);
            tvSpecialInstructions = itemView.findViewById(R.id.tvSpecialInstructions);
            btnTrackOrder = itemView.findViewById(R.id.btnTrackOrder);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }

        void bind(Order order) {
            int count = order.getItemsCount();
            tvItemsCount.setText(count + (count == 1 ? " item in order" : " items in order"));

            setupImageCarousel(order);

            layoutItemChips.removeAllViews();
            // Two chips, then a count. A five-item order used to lay out five
            // chips and wrap the row; the details screen has the full list.
            if (order.getItemsSummary() != null && !order.getItemsSummary().isEmpty()) {
                String[] parts = order.getItemsSummary().split(",\\s*");
                int shown = Math.min(CARD_ITEM_PREVIEW, parts.length);
                for (int i = 0; i < shown; i++) {
                    layoutItemChips.addView(buildChip(parts[i]));
                }
                if (parts.length > shown) {
                    layoutItemChips.addView(buildChip("+" + (parts.length - shown) + " more"));
                }
            }

            String s = order.getStatus() != null ? order.getStatus().toLowerCase(Locale.US) : "";

            tvNewBadge.setVisibility("pending".equals(s) ? View.VISIBLE : View.GONE);
            tvItemCountBadge.setText(count + (count == 1 ? " item" : " items"));

            bindStatusChip(tvStatus, order.getStatus());

            String paymentLabel = "cod".equalsIgnoreCase(order.getPaymentMethod()) ? "Cash on Delivery" : "Online Payment";
            tvOrderDate.setText(paymentLabel + " · " + formatDate(order.getCreatedAt()));

            tvOrderId.setText("Order #" + order.getId());

            String cookLabel = order.getKitchenName() != null && !order.getKitchenName().isEmpty()
                    ? order.getKitchenName()
                    : order.getCookName();
            tvCustomerName.setText(cookLabel != null ? cookLabel : "Unknown Cook");

            tvOrderSummary.setText(order.getItemsSummaryPreview(CARD_ITEM_PREVIEW));

            if (order.getSpecialInstructions() != null && !order.getSpecialInstructions().isEmpty()) {
                layoutSpecialInstructions.setVisibility(View.VISIBLE);
                tvSpecialInstructions.setText(order.getSpecialInstructions());
            } else {
                layoutSpecialInstructions.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });

            if (btnViewDetails != null) {
                btnViewDetails.setOnClickListener(v -> {
                    if (listener != null) listener.onOrderClick(order);
                });
            }

            if (btnTrackOrder != null) {
                btnTrackOrder.setOnClickListener(v -> {
                    if (trackListener != null) trackListener.onTrackOrder(order);
                });
            }

            if (btnCancelOrder != null) {
                btnCancelOrder.setVisibility(CANCELLABLE_STATUSES.contains(s) ? View.VISIBLE : View.GONE);
                btnCancelOrder.setOnClickListener(v -> {
                    if (cancelListener != null) cancelListener.onCancelOrder(order);
                });
            }
        }

        /** Real per-item carousel when order_items came back with structured data; falls back to the single representative image otherwise. */
        private void setupImageCarousel(Order order) {
            List<Order.OrderItem> items = order.getItems();

            if (items != null && !items.isEmpty()) {
                imgMealPlaceholder.setVisibility(View.GONE);
                viewPagerImages.setVisibility(View.VISIBLE);

                OrderItemCarouselAdapter carouselAdapter =
                        new OrderItemCarouselAdapter(items, order.getSpecialInstructions());
                viewPagerImages.setAdapter(carouselAdapter);

                if (items.size() > 1) {
                    btnPrevImage.setVisibility(View.VISIBLE);
                    btnNextImage.setVisibility(View.VISIBLE);
                    layoutIndicators.setVisibility(View.VISIBLE);
                    setupCarouselNavigation(items.size());
                    setupIndicators(items.size());
                } else {
                    btnPrevImage.setVisibility(View.GONE);
                    btnNextImage.setVisibility(View.GONE);
                    layoutIndicators.setVisibility(View.GONE);
                }
            } else {
                viewPagerImages.setVisibility(View.GONE);
                imgMealPlaceholder.setVisibility(View.VISIBLE);
                btnPrevImage.setVisibility(View.GONE);
                btnNextImage.setVisibility(View.GONE);
                layoutIndicators.setVisibility(View.GONE);
                ImageUrlHelper.load(imgMealPlaceholder, order.getMealImage(), R.drawable.ic_meal);
            }
        }

        private void setupCarouselNavigation(int imageCount) {
            btnPrevImage.setOnClickListener(v -> {
                int current = viewPagerImages.getCurrentItem();
                if (current > 0) viewPagerImages.setCurrentItem(current - 1, true);
            });
            btnNextImage.setOnClickListener(v -> {
                int current = viewPagerImages.getCurrentItem();
                if (current < imageCount - 1) viewPagerImages.setCurrentItem(current + 1, true);
            });
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateIndicators(position);
                }
            });
        }

        private void setupIndicators(int count) {
            layoutIndicators.removeAllViews();
            for (int i = 0; i < count; i++) {
                layoutIndicators.addView(buildIndicatorDot(i == 0));
            }
        }

        private void updateIndicators(int selectedPosition) {
            for (int i = 0; i < layoutIndicators.getChildCount(); i++) {
                layoutIndicators.getChildAt(i).setBackground(indicatorDrawable(i == selectedPosition));
            }
        }

        private View buildIndicatorDot(boolean selected) {
            View dot = new View(itemView.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(6), dp(6));
            params.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(params);
            dot.setBackground(indicatorDrawable(selected));
            return dot;
        }

        private android.graphics.drawable.GradientDrawable indicatorDrawable(boolean selected) {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(selected ? 0xFFFFFFFF : 0x66FFFFFF);
            return gd;
        }

        private TextView buildChip(String text) {
            TextView chip = new TextView(itemView.getContext());
            chip.setText(text.trim());
            chip.setTextSize(11.5f);
            chip.setTextColor(0xFF2E7D32);
            chip.setBackgroundResource(R.drawable.bg_pill_green_soft);
            int hPad = dp(10);
            int vPad = dp(6);
            chip.setPadding(hPad, vPad, hPad, vPad);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            chip.setLayoutParams(params);
            return chip;
        }

        private int dp(int value) {
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            return Math.round(value * density);
        }

        private void bindStatusChip(TextView chip, String status) {
            String s = status != null ? status.toLowerCase(Locale.US) : "";
            int bgRes;
            int textColor;
            int iconRes;
            String label;
            switch (s) {
                case "pending":
                    bgRes = R.drawable.status_chip_new;       textColor = 0xFF1565C0; iconRes = R.drawable.ic_clock;        label = "Waiting for kitchen"; break;
                case "confirmed":
                    bgRes = R.drawable.status_chip_delivered; textColor = 0xFF2E7D32; iconRes = R.drawable.ic_check_circle; label = "Confirmed";           break;
                case "preparing":
                    bgRes = R.drawable.status_chip_preparing; textColor = 0xFFA8660B; iconRes = R.drawable.ic_clock;        label = "Being Prepared";      break;
                case "ready":
                    bgRes = R.drawable.status_chip_out_for_delivery; textColor = 0xFF1565C0; iconRes = R.drawable.ic_check_circle; label = "Ready for Pickup"; break;
                case "delivered":
                case "completed":
                    bgRes = R.drawable.status_chip_delivered; textColor = 0xFF2E7D32; iconRes = R.drawable.ic_check_circle; label = "Delivered";           break;
                case "cancelled":
                    bgRes = R.drawable.status_chip_sold_out;  textColor = 0xFFC62828; iconRes = R.drawable.ic_close;        label = "Cancelled";           break;
                default:
                    bgRes = R.drawable.status_chip_new;       textColor = 0xFF1565C0; iconRes = R.drawable.ic_info;
                    label = s.isEmpty() ? "Unknown" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
                    break;
            }
            chip.setBackgroundResource(bgRes);
            chip.setTextColor(textColor);
            chip.setText(label);
            chip.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
            chip.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(textColor));
        }

        /**
         * "Today, 3:12 PM" · "Yesterday, 9:40 AM" · "27 Aug 2026, 3:12 PM".
         *
         * The time matters as much as the date on an order card — two orders on the
         * same day were indistinguishable, and "27 Aug 2026" told the customer
         * nothing about an order they placed twenty minutes ago. ISO inputs carry a
         * 'Z', so they are parsed as UTC and rendered in the device's zone; a naive
         * "yyyy-MM-dd HH:mm:ss" is taken as local, which is what the server means
         * when it sends one.
         */
        private String formatDate(String rawDate) {
            return com.tiffincraft.app.utils.TimeFormat.relative(rawDate);
        }
    }
}
