package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Body for POST/PUT subscription-plans. Any field left null is left untouched on update. */
public class SubscriptionPlanRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("duration")
    private String duration; // "weekly" | "monthly"

    @SerializedName("description")
    private String description;

    @SerializedName("is_active")
    private Boolean isActive;

    @SerializedName("items")
    private List<Item> items;

    // The plan's one-time subscription price. Required, and the server rejects
    // anything not below the items' combined menu price — see
    // subscriptionPlanController.js. The wire name is still price_per_delivery
    // because that's the column name; it is not a recurring charge.
    @SerializedName("price_per_delivery")
    private Double pricePerDelivery;

    public SubscriptionPlanRequest(String name, String duration, String description, List<Item> items) {
        this.name = name;
        this.duration = duration;
        this.description = description;
        this.items = items;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public void setPricePerDelivery(Double pricePerDelivery) {
        this.pricePerDelivery = pricePerDelivery;
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
