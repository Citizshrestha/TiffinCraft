package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class Meal {
    @SerializedName("id")
    private int id;

    @SerializedName("cook_id")
    private int cookId;

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
    private int isAvailableInt; // Backend sends 0 or 1
    
    private transient boolean isAvailable; // Computed field

    @SerializedName("preparation_time")
    private Integer preparationTime;

    @SerializedName("spice_level")
    private String spiceLevel;

    @SerializedName("is_vegetarian")
    private int isVegetarianInt; // Backend sends 0 or 1
    
    private transient boolean isVegetarian; // Computed field

    @SerializedName("is_vegan")
    private int isVeganInt; // Backend sends 0 or 1
    
    private transient boolean isVegan; // Computed field

    @SerializedName("allergens")
    private String allergens;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Cook info (for customer view)
    @SerializedName("cook_name")
    private String cookName;

    @SerializedName("cook_image")
    private String cookImage;

    @SerializedName("cook_rating")
    private Double cookRating;

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCookId() {
        return cookId;
    }

    public void setCookId(int cookId) {
        this.cookId = cookId;
    }

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
        return isAvailableInt == 1;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
        this.isAvailableInt = available ? 1 : 0;
    }
    
    public void setAvailableInt(int isAvailableInt) {
        this.isAvailableInt = isAvailableInt;
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
        return isVegetarianInt == 1;
    }

    public void setVegetarian(boolean vegetarian) {
        this.isVegetarian = vegetarian;
        this.isVegetarianInt = vegetarian ? 1 : 0;
    }
    
    public void setVegetarianInt(int isVegetarianInt) {
        this.isVegetarianInt = isVegetarianInt;
    }

    public boolean isVegan() {
        return isVeganInt == 1;
    }

    public void setVegan(boolean vegan) {
        this.isVegan = vegan;
        this.isVeganInt = vegan ? 1 : 0;
    }
    
    public void setVeganInt(int isVeganInt) {
        this.isVeganInt = isVeganInt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCookName() {
        return cookName;
    }

    public void setCookName(String cookName) {
        this.cookName = cookName;
    }

    public String getCookImage() {
        return cookImage;
    }

    public void setCookImage(String cookImage) {
        this.cookImage = cookImage;
    }

    public Double getCookRating() {
        return cookRating;
    }

    public void setCookRating(Double cookRating) {
        this.cookRating = cookRating;
    }
}
