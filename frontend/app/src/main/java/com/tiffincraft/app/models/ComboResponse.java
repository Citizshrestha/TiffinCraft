package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ComboResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("combo")
    private Combo combo;

    @SerializedName("combos")
    private List<Combo> combos;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Combo getCombo() {
        return combo;
    }

    public List<Combo> getCombos() {
        return combos;
    }

    public static class Combo {
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

        @SerializedName("is_active")
        private boolean active;

        @SerializedName("cook_name")
        private String cookName;

        @SerializedName("kitchen_name")
        private String kitchenName;

        @SerializedName("items")
        private List<ComboItem> items;

        @SerializedName("individual_total")
        private double individualTotal;

        @SerializedName("savings")
        private double savings;

        @SerializedName("is_available")
        private boolean available;

        public int getId() {
            return id;
        }

        public int getCookId() {
            return cookId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getPrice() {
            return price;
        }

        public boolean isActive() {
            return active;
        }

        public String getCookName() {
            return cookName;
        }

        public String getKitchenName() {
            return kitchenName;
        }

        public List<ComboItem> getItems() {
            return items;
        }

        public double getIndividualTotal() {
            return individualTotal;
        }

        public double getSavings() {
            return savings;
        }

        /** False when the combo is paused, or one of its meals is currently
         *  unavailable — buyCombo() also enforces this server-side, this is
         *  just so the UI can say so before the customer taps Buy. */
        public boolean isAvailable() {
            return available;
        }
    }

    public static class ComboItem {
        @SerializedName("id")
        private int id;

        @SerializedName("meal_id")
        private int mealId;

        @SerializedName("quantity")
        private int quantity;

        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        @SerializedName("price")
        private double price;

        @SerializedName("image_url")
        private String imageUrl;

        @SerializedName("is_available")
        private boolean available;

        public int getId() {
            return id;
        }

        public int getMealId() {
            return mealId;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getPrice() {
            return price;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public boolean isAvailable() {
            return available;
        }
    }
}
