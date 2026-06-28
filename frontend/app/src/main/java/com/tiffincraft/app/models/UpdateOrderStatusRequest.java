package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class UpdateOrderStatusRequest {

    @SerializedName("status")
    private String status;

    public UpdateOrderStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
