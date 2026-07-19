package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class ChatUnreadCountResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("unread_count")
    private int unreadCount;

    public boolean isSuccess() { return success; }
    public int getUnreadCount() { return unreadCount; }
}
