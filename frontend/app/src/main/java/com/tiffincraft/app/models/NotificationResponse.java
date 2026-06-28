package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("notifications")
    private List<CustomerDashboardResponse.Notification> notifications;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public List<CustomerDashboardResponse.Notification> getNotifications() {
        return notifications;
    }

    public String getMessage() {
        return message;
    }
}
