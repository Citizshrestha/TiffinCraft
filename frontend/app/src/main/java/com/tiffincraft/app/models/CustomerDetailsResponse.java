package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CustomerDetailsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("customer")
    private CustomerDetails customer;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public CustomerDetails getCustomer() { return customer; }
}
