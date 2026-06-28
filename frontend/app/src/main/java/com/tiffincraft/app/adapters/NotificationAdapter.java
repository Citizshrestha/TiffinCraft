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

        // Set icon based on type
        if ("promo".equalsIgnoreCase(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_offers);
        } else if ("order_status".equalsIgnoreCase(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_home);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_notifications);
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = sdf.parse(dateString);
            if (date == null) return dateString;
            
            long diff = System.currentTimeMillis() - date.getTime();
            long hours = diff / (60 * 60 * 1000);
            
            if (hours < 1) return "Just now";
            if (hours < 24) return hours + " hours ago";
            return (hours / 24) + " days ago";
        } catch (ParseException e) {
            return dateString.split("T")[0];
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView ivIcon;
        View unreadDot, layoutContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            layoutContainer = itemView.findViewById(R.id.layoutNotificationContainer);
        }
    }
}
