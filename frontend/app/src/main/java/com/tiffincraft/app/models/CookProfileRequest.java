package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CookProfileRequest {
    @SerializedName("kitchen_name")
    private String kitchenName;
    @SerializedName("food_type")
    private String foodType;
    private String description;
    @SerializedName("capacity_per_day")
    private Integer capacityPerDay;
    private String bio;
    private String specialties;

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
}
