package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class DashboardResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("dashboard")
    private DashboardStats dashboard;

    public boolean isSuccess() {
        return success;
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

    public DashboardStats getDashboard() {
        return dashboard;
    }

    public void setDashboard(DashboardStats dashboard) {
        this.dashboard = dashboard;
    }
}
