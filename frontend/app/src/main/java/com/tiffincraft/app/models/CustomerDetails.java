package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Read-only customer profile as seen by a cook (GET /customer/:customerId). */
public class CustomerDetails {

    @SerializedName("id")
    private int id;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("profile_image")
    private String profileImage;

    @SerializedName("address")
    private String address;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("total_orders")
    private int totalOrders;

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getProfileImage() { return profileImage; }
    public String getAddress() { return address; }
    public String getCreatedAt() { return createdAt; }
    public int getTotalOrders() { return totalOrders; }
}
