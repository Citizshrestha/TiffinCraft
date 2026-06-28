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

        // Set icon and background based on type
        String type = notification.getType() != null ? notification.getType().toLowerCase() : "system";
        switch (type) {
            case "order":
                holder.ivIcon.setImageResource(R.drawable.ic_cart);
                holder.iconContainer.setBackgroundResource(R.drawable.circle_bg_orange_light);
                break;
            case "promo":
                holder.ivIcon.setImageResource(R.drawable.ic_discount);
                holder.iconContainer.setBackgroundResource(R.drawable.circle_bg_red_light);
                break;
            case "cook":
                holder.ivIcon.setImageResource(R.drawable.ic_chef);
                holder.iconContainer.setBackgroundResource(R.drawable.circle_bg_green_light);
                break;
            case "system":
            default:
                holder.ivIcon.setImageResource(R.drawable.ic_notifications);
                holder.iconContainer.setBackgroundResource(R.drawable.circle_bg_blue_light);
                break;
        }

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
