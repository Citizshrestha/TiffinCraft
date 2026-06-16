package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CookProfile {
    private int id;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("kitchen_name")
    private String kitchenName;
    @SerializedName("food_type")
    private String foodType;
    private String description;
    @SerializedName("capacity_per_day")
    private int capacityPerDay;
    private String bio;
    private String specialties;
    private double rating;
    @SerializedName("total_orders")
    private int totalOrders;
    @SerializedName("is_verified")
    private boolean isVerified;
    @SerializedName("is_approved")
    private boolean isApproved;

    // User info (from JOIN)
    @SerializedName("full_name")
    private String fullName;
    @SerializedName("profile_image")
    private String profileImage;
    private String address;
    private String email;
    private String phone;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getKitchenName() {
        return kitchenName;
    }

    public void setKitchenName(String kitchenName) {
        this.kitchenName = kitchenName;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacityPerDay() {
        return capacityPerDay;
    }

    public void setCapacityPerDay(int capacityPerDay) {
        this.capacityPerDay = capacityPerDay;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSpecialties() {
        return specialties;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
