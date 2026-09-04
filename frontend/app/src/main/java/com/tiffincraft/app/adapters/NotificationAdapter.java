package com.tiffincraft.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Notification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context context;
    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification, int position);
    }

    public NotificationAdapter(Context context, List<Notification> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        
        // Format date
        holder.tvTime.setText(formatDate(notification.getCreatedAt()));

        // Unread styling
        if (!notification.isRead()) {
            holder.layoutContainer.setBackgroundColor(Color.parseColor("#F5F9F5")); // Light green tint
            holder.unreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.layoutContainer.setBackgroundColor(Color.WHITE);
            holder.unreadDot.setVisibility(View.GONE);
        }

        // Set icon, tint and background based on the notification's real type
        // (and, for the generic "order_status" type, the specific status word
        // in its message — one DB type covers confirmed/preparing/ready/
        // delivered/cancelled alike). The icon's tint is hardcoded green in
        // item_notification.xml and RecyclerView reuses views, so every case
        // below must explicitly set the tint — never rely on the XML default.
        String type = notification.getType() != null ? notification.getType().toLowerCase() : "system";
        String haystack = ((notification.getTitle() != null ? notification.getTitle() : "")
                + " " + (notification.getMessage() != null ? notification.getMessage() : "")).toLowerCase();

        int iconRes;
        int bgRes;
        int tintColor;

        switch (type) {
            case "new_order":
                // Distinguish combo orders from regular orders by checking title/message
                if (haystack.contains("combo")) {
                    iconRes = R.drawable.ic_offers;
                    bgRes = R.drawable.circle_icon_purple;
                    tintColor = 0xFF9C27B0;
                } else {
                    iconRes = R.drawable.ic_orders;
                    bgRes = R.drawable.circle_icon_orange;
                    tintColor = 0xFFF57C00;
                }
                break;
            case "order_status":
                if (haystack.contains("cancel")) {
                    iconRes = R.drawable.ic_close;
                    bgRes = R.drawable.circle_icon_red;
                    tintColor = 0xFFE53935;
                } else if (haystack.contains("delivered")) {
                    iconRes = R.drawable.ic_check_circle;
                    bgRes = R.drawable.circle_icon_green;
                    tintColor = 0xFF388E3C;
                } else if (haystack.contains("ready")) {
                    iconRes = R.drawable.ic_delivery;
                    bgRes = R.drawable.circle_icon_blue;
                    tintColor = 0xFF1976D2;
                } else {
                    // confirmed / preparing — order accepted, being cooked
                    iconRes = R.drawable.ic_cook;
                    bgRes = R.drawable.circle_icon_orange;
                    tintColor = 0xFFF57C00;
                }
                break;
            case "payment_verified":
                iconRes = R.drawable.ic_check_circle;
                bgRes = R.drawable.circle_icon_green;
                tintColor = 0xFF388E3C;
                break;
            case "payment_verification":
                // Cook-side: a customer just submitted proof, awaiting the cook's review.
                iconRes = R.drawable.ic_receipt;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "payment_rejected":
                // Customer-side: needs to resubmit — same red treatment as a cancellation.
                iconRes = R.drawable.ic_close;
                bgRes = R.drawable.circle_icon_red;
                tintColor = 0xFFE53935;
                break;
            case "review":
                iconRes = R.drawable.ic_star;
                bgRes = R.drawable.circle_icon_purple;
                tintColor = 0xFFFFB300;
                break;
            case "cook_approved":
                iconRes = R.drawable.ic_check_circle;
                bgRes = R.drawable.circle_icon_green;
                tintColor = 0xFF388E3C;
                break;
            case "cook_rejected":
                iconRes = R.drawable.ic_info;
                bgRes = R.drawable.circle_icon_red;
                tintColor = 0xFFE53935;
                break;
            case "chat_message":
                iconRes = R.drawable.ic_chat;
                bgRes = R.drawable.circle_icon_blue;
                tintColor = 0xFF1976D2;
                break;
            case "commission_due":
            case "commission_submitted":
                iconRes = R.drawable.ic_wallet;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "commission_verified":
                iconRes = R.drawable.ic_check_circle;
                bgRes = R.drawable.circle_icon_green;
                tintColor = 0xFF388E3C;
                break;
            case "commission_rejected":
                iconRes = R.drawable.ic_close;
                bgRes = R.drawable.circle_icon_red;
                tintColor = 0xFFE53935;
                break;
            case "refund_requested":
            case "refund_feedback":
                iconRes = R.drawable.ic_info;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "refund_status":
                iconRes = R.drawable.ic_receipt;
                bgRes = R.drawable.circle_icon_blue;
                tintColor = 0xFF1976D2;
                break;
            case "subscription_payment_submitted":
                // Cook-side: a customer just submitted proof, awaiting review — same
                // treatment as commission_submitted / payment_verification above.
                iconRes = R.drawable.ic_receipt;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "subscription_verified":
                iconRes = R.drawable.ic_check_circle;
                bgRes = R.drawable.circle_icon_green;
                tintColor = 0xFF388E3C;
                break;
            case "subscription_rejected":
                iconRes = R.drawable.ic_close;
                bgRes = R.drawable.circle_icon_red;
                tintColor = 0xFFE53935;
                break;
            case "custom_meal_request":
                // Cook-side: customer is asking for a different meal — prompt the cook
                // to act. Use the orange cook icon to match other action-required alerts.
                iconRes = R.drawable.ic_cook;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "custom_meal_accepted":
                // Customer-side: cook agreed — show green check for good news.
                iconRes = R.drawable.ic_check_circle;
                bgRes = R.drawable.circle_icon_green;
                tintColor = 0xFF388E3C;
                break;
            case "custom_meal_declined":
                // Customer-side: cook declined — show red close so the customer
                // knows they need to check the chat or plan accordingly.
                iconRes = R.drawable.ic_close;
                bgRes = R.drawable.circle_icon_red;
                tintColor = 0xFFE53935;
                break;
            case "subscription_paused":
                iconRes = R.drawable.ic_info;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "subscription_cancelled":
                iconRes = R.drawable.ic_close;
                bgRes = R.drawable.circle_icon_red;
                tintColor = 0xFFE53935;
                break;
            case "subscription_day_skipped":
                // Cook-side: customer skipped a day — orange info so the cook knows
                // not to cook that meal but it is not an error.
                iconRes = R.drawable.ic_info;
                bgRes = R.drawable.circle_icon_orange;
                tintColor = 0xFFF57C00;
                break;
            case "system":
            default:
                iconRes = R.drawable.ic_notifications;
                bgRes = R.drawable.circle_icon_blue;
                tintColor = 0xFF1976D2;
                break;
        }

        holder.ivIcon.setImageResource(iconRes);
        holder.ivIcon.setColorFilter(tintColor);
        holder.iconContainer.setBackgroundResource(bgRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications == null ? 0 : notifications.size();
    }

    public void markAsRead(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.get(position).setIsRead(true);
            notifyItemChanged(position);
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null) return "Just now";
        try {
            // Handle ISO 8601 format from API: "2026-06-28T08:34:27.000Z"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateString);
            if (date == null) return "Just now";

            long diff = System.currentTimeMillis() - date.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) return "Just now";
            if (minutes < 60) return minutes + " min ago";
            if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dateString.split("T")[0];
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView ivIcon;
        View unreadDot, layoutContainer, iconContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            layoutContainer = itemView.findViewById(R.id.layoutNotificationContainer);
            iconContainer = itemView.findViewById(R.id.iconContainer);
        }
    }
}
