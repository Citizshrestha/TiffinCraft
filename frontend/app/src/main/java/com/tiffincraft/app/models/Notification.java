package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("type")
    private String type;

    @SerializedName("is_read")
    private int isReadInt;

    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isReadInt == 1; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setIsRead(boolean isRead) { this.isReadInt = isRead ? 1 : 0; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
