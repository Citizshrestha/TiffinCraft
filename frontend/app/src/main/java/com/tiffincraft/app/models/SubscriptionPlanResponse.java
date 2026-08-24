package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SubscriptionPlanResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("plan")
    private Plan plan;

    @SerializedName("plans")
    private List<Plan> plans;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Plan getPlan() {
        return plan;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public static class Plan {
        @SerializedName("id")
        private int id;

        @SerializedName("cook_id")
        private int cookId;

        @SerializedName("name")
        private String name;

        @SerializedName("duration")
        private String duration; // "weekly" | "monthly"

        @SerializedName("description")
        private String description;

        @SerializedName("is_active")
        private boolean active;

        @SerializedName("cook_name")
        private String cookName;

        @SerializedName("kitchen_name")
        private String kitchenName;

        @SerializedName("items")
        private List<PlanItem> items;

        @SerializedName("price_per_delivery")
        private double pricePerDelivery;

        @SerializedName("individual_total")
        private double individualTotal;

        @SerializedName("savings")
        private double savings;

        @SerializedName("has_custom_price")
        private boolean hasCustomPrice;

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

        public String getDuration() {
            return duration;
        }

        public String getDurationLabel() {
            if ("2_weeks".equals(duration) || "biweekly".equals(duration) || "2_week".equals(duration)) return "2 Weeks";
            if ("monthly".equals(duration) || "1_month".equals(duration) || "1_months".equals(duration)) return "1 Month";
            return "1 Week";
        }

        public String getDescription() {
            return description;
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

        public List<PlanItem> getItems() {
            return items;
        }

        public double getPricePerDelivery() {
            return pricePerDelivery;
        }

        /** What the same items would cost ordered separately — for "Save ₹X" framing. */
        public double getIndividualTotal() {
            return individualTotal;
        }

        public double getSavings() {
            return savings;
        }

        /** True only when the cook actually set a price override — false means
         *  getPricePerDelivery() is just the auto-summed menu price fallback. */
        public boolean hasCustomPrice() {
            return hasCustomPrice;
        }

        /** False when the plan is paused, or one of its meals is currently
         *  unavailable — the cook restocking it is all that's needed to
         *  make it subscribable again. */
        public boolean isAvailable() {
            return available;
        }
    }

    public static class PlanItem {
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
