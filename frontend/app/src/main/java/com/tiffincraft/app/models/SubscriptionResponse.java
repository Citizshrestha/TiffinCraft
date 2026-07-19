package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SubscriptionResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("subscriptions")
    private List<Subscription> subscriptions;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public static class Subscription {
        @SerializedName("id")
        private int id;

        @SerializedName("cook_id")
        private int cookId;

        @SerializedName("meal_id")
        private int mealId;

        @SerializedName("frequency")
        private String frequency;

        @SerializedName("status")
        private String status;

        @SerializedName("delivery_address")
        private String deliveryAddress;

        @SerializedName("next_delivery_date")
        private String nextDeliveryDate;

        @SerializedName("meal_name")
        private String mealName;

        @SerializedName("price")
        private double price;

        @SerializedName("cook_name")
        private String cookName;

        @SerializedName("kitchen_name")
        private String kitchenName;

        public int getId() {
            return id;
        }

        public int getCookId() {
            return cookId;
        }

        public int getMealId() {
            return mealId;
        }

        public String getFrequency() {
            return frequency;
        }

        public String getStatus() {
            return status;
        }

        public String getDeliveryAddress() {
            return deliveryAddress;
        }

        public String getNextDeliveryDate() {
            return nextDeliveryDate;
        }

        public String getMealName() {
            return mealName;
        }

        public double getPrice() {
            return price;
        }

        public String getCookName() {
            return cookName;
        }

        public String getKitchenName() {
            return kitchenName;
        }
    }
}
