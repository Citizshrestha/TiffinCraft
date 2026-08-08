package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CreateCustomerSubscriptionRequest {
    @SerializedName("plan_id")
    private int planId;

    @SerializedName("delivery_address")
    private String deliveryAddress;

    @SerializedName("start_date")
    private String startDate; // yyyy-MM-dd

    public CreateCustomerSubscriptionRequest(int planId, String deliveryAddress, String startDate) {
        this.planId = planId;
        this.deliveryAddress = deliveryAddress;
        this.startDate = startDate;
    }
}
