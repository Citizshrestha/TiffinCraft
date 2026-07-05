package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class Order {

    @SerializedName("id")
    private int id;

    @SerializedName("customer_id")
    private int customerId;

    @SerializedName("cook_id")
    private int cookId;

    @SerializedName("total_amount")
    private double totalAmount;

    @SerializedName("delivery_address")
    private String deliveryAddress;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Joined fields from query
    @SerializedName("customer_name")
    private String customerName;

    @SerializedName("customer_phone")
    private String customerPhone;

    @SerializedName("meal_name")
    private String mealName;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("meal_price")
    private double mealPrice;

    @SerializedName("meal_image")
    private String mealImage;

    @SerializedName("special_instructions")
    private String specialInstructions;

    // Getters
    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getCookId() { return cookId; }
    public double getTotalAmount() { return totalAmount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getMealName() { return mealName; }
    public int getQuantity() { return quantity; }
    public double getMealPrice() { return mealPrice; }
    public String getMealImage() { return mealImage; }
    public String getSpecialInstructions() { return specialInstructions; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
}
