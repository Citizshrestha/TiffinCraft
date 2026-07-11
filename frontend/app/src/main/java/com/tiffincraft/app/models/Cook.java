package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class Cook {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("profile_image")
    private String profileImage;

    @SerializedName("rating")
    private Double rating;

    @SerializedName("review_count")
    private Integer reviewCount;

    @SerializedName("dish_count")
    private Integer dishCount;

    @SerializedName("distance")
    private String distance;

    @SerializedName("delivery_time")
    private String deliveryTime;

    @SerializedName("kitchen_name")
    private String kitchenName;

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public Double getRating() {
        return rating != null ? rating : 0.0;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount != null ? reviewCount : 0;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getDishCount() {
        return dishCount != null ? dishCount : 0;
    }

    public void setDishCount(Integer dishCount) {
        this.dishCount = dishCount;
    }

    public String getDistance() {
        return distance != null ? distance : "N/A";
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getKitchenName() {
        return kitchenName != null ? kitchenName : name;
    }

    public void setKitchenName(String kitchenName) {
        this.kitchenName = kitchenName;
    }
}
