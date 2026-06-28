package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CookProfileRequest {
    // Kitchen profile fields
    @SerializedName("kitchen_name")
    private String kitchenName;
    
    @SerializedName("food_type")
    private String foodType;
    
    private String description;
    
    @SerializedName("capacity_per_day")
    private Integer capacityPerDay;
    
    private String bio;
    private String specialties;

    // User profile fields
    @SerializedName("full_name")
    private String fullName;
    
    private String phone;
    private String address;
    private Double latitude;
    private Double longitude;

    // Constructors
    public CookProfileRequest() {
    }

    public CookProfileRequest(String kitchenName, String foodType, String description, Integer capacityPerDay) {
        this.kitchenName = kitchenName;
        this.foodType = foodType;
        this.description = description;
        this.capacityPerDay = capacityPerDay;
    }

    public CookProfileRequest(String kitchenName, String foodType, String description,
                              Integer capacityPerDay, String bio, String specialties) {
        this.kitchenName = kitchenName;
        this.foodType = foodType;
        this.description = description;
        this.capacityPerDay = capacityPerDay;
        this.bio = bio;
        this.specialties = specialties;
    }

    // Getters and Setters
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

    public Integer getCapacityPerDay() {
        return capacityPerDay;
    }

    public void setCapacityPerDay(Integer capacityPerDay) {
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
