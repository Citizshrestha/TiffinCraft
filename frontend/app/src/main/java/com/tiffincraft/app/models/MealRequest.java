package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class MealRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private double price;

    @SerializedName("category")
    private String category;

    @SerializedName("cuisine_type")
    private String cuisineType;

    @SerializedName("is_available")
    private boolean isAvailable;

    @SerializedName("preparation_time")
    private Integer preparationTime;

    @SerializedName("spice_level")
    private String spiceLevel;

    @SerializedName("is_vegetarian")
    private boolean isVegetarian;

    @SerializedName("is_vegan")
    private boolean isVegan;

    @SerializedName("allergens")
    private String allergens;

    @SerializedName("image_url")
    private String imageUrl;

    public MealRequest(String name, String description, double price, String category,
                       boolean isAvailable, boolean isVegetarian, boolean isSpicy, boolean isBestseller) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isAvailable = isAvailable;
        this.isVegetarian = isVegetarian;
        this.spiceLevel = isSpicy ? "hot" : "mild";
        // Note: bestseller would be tracked via orders/reviews, not a direct field
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public Integer getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(Integer preparationTime) {
        this.preparationTime = preparationTime;
    }

    public String getSpiceLevel() {
        return spiceLevel;
    }

    public void setSpiceLevel(String spiceLevel) {
        this.spiceLevel = spiceLevel;
    }

    public boolean isVegetarian() {
        return isVegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        isVegetarian = vegetarian;
    }

    public boolean isVegan() {
        return isVegan;
    }

    public void setVegan(boolean vegan) {
        isVegan = vegan;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
