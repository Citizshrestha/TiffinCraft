package com.tiffincraft.app.models;

public class PopularCook {
    private String name;
    private String imageUrl;
    private float rating;
    private int reviewCount;
    private String deliveryTime;

    public PopularCook(String name, String imageUrl, float rating, int reviewCount, String deliveryTime) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.deliveryTime = deliveryTime;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public float getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }
}
