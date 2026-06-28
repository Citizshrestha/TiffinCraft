package com.tiffincraft.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.CustomerDashboardResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<CustomerDashboardResponse.Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(CustomerDashboardResponse.Notification notification);
    }

    public NotificationAdapter(List<CustomerDashboardResponse.Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void updateNotifications(List<CustomerDashboardResponse.Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerDashboardResponse.Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconNotification;
        private TextView tvTitle, tvMessage, tvTime;
        private View unreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconNotification = itemView.findViewById(R.id.iconNotification);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        public void bind(CustomerDashboardResponse.Notification notification) {
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());
            tvTime.setText(formatTime(notification.getCreatedAt()));

            // Show/hide unread indicator
            if (notification.isRead()) {
                unreadIndicator.setVisibility(View.GONE);
                itemView.setAlpha(0.7f);
            } else {
                unreadIndicator.setVisibility(View.VISIBLE);
                itemView.setAlpha(1.0f);
            }

            // Set icon based on notification type
            String type = notification.getType();
            if ("order".equals(type)) {
                iconNotification.setImageResource(R.drawable.ic_view_orders);
            } else if ("promo".equals(type)) {
                iconNotification.setImageResource(R.drawable.ic_offers);
            } else if ("cook".equals(type)) {
                iconNotification.setImageResource(R.drawable.ic_cook);
            } else {
                iconNotification.setImageResource(R.drawable.ic_notifications);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        private String formatTime(String timestamp) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(timestamp);
                
                long diff = System.currentTimeMillis() - date.getTime();
                long seconds = diff / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                if (days > 0) {
                    return days + "d ago";
                } else if (hours > 0) {
                    return hours + "h ago";
                } else if (minutes > 0) {
                    return minutes + "m ago";
                } else {
                    return "Just now";
                }
            } catch (ParseException e) {
                return timestamp;
            }
        }
    }
}
