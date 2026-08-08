package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Body for POST/PUT combos. Any field left null is left untouched on update. */
public class ComboRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private Double price;

    @SerializedName("is_active")
    private Boolean isActive;

    @SerializedName("items")
    private List<Item> items;

    public ComboRequest(String name, String description, Double price, List<Item> items) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.items = items;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public static class Item {
        @SerializedName("meal_id")
        private int mealId;

        @SerializedName("quantity")
        private int quantity;

        public Item(int mealId, int quantity) {
            this.mealId = mealId;
            this.quantity = quantity;
        }
    }
}
