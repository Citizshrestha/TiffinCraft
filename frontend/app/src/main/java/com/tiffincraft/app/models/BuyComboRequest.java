package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class BuyComboRequest {
    @SerializedName("delivery_address")
    private String deliveryAddress;

    @SerializedName("payment_method")
    private String paymentMethod; // "cod" | "online"

    @SerializedName("special_instructions")
    private String specialInstructions;

    public BuyComboRequest(String deliveryAddress, String paymentMethod, String specialInstructions) {
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.specialInstructions = specialInstructions;
    }
}
