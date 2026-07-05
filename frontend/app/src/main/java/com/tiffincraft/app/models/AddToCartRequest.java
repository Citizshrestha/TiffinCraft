package com.tiffincraft.app.models;

public class AddToCartRequest {
    private int meal_id;
    private int quantity;

    public AddToCartRequest(int mealId, int quantity) {
        this.meal_id = mealId;
        this.quantity = quantity;
    }
}