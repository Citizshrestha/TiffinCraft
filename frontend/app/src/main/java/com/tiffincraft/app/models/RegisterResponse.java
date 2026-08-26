package com.tiffincraft.app.models;

public class RegisterResponse {

    @com.google.gson.annotations.SerializedName("orderId")
    private int orderId;
    private boolean success;
    private String message;
    private int userId;
    private boolean autoVerified;

    public boolean isSuccess() {
        return success;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isAutoVerified() {
        return autoVerified;
    }

    public void setAutoVerified(boolean autoVerified) {
        this.autoVerified = autoVerified;
    }
}
