package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("notifications")
    private List<Notification> notifications;

    @SerializedName("unread_count")
    private int unreadCount;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
