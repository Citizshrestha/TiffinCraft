package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;

public class MealResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("meal")
    private Meal meal;

    @SerializedName("meals")
    private List<Meal> meals = new ArrayList<>();  // Initialize to avoid null

    @SerializedName("mealId")
    private Integer mealId;

    @SerializedName("image_url")
    private String imageUrl;

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Meal getMeal() {
        return meal;
    }

    public void setMeal(Meal meal) {
        this.meal = meal;
    }

    public List<Meal> getMeals() {
        return meals != null ? meals : new ArrayList<>();  // Never return null
    }

    public void setMeals(List<Meal> meals) {
        this.meals = meals;
    }

    public Integer getMealId() {
        return mealId;
    }

    public void setMealId(Integer mealId) {
        this.mealId = mealId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
