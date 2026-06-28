package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class DashboardStats {

    @SerializedName("today_orders")
    private OrderStats todayOrders;

    @SerializedName("today_earnings")
    private EarningsStats todayEarnings;

    @SerializedName("active_orders")
    private ActiveOrdersStats activeOrders;

    @SerializedName("average_rating")
    private RatingStats averageRating;

    // Getters
    public OrderStats getTodayOrders() {
        return todayOrders;
    }

    public EarningsStats getTodayEarnings() {
        return todayEarnings;
    }

    public ActiveOrdersStats getActiveOrders() {
        return activeOrders;
    }

    public RatingStats getAverageRating() {
        return averageRating;
    }

    // Nested class for today's orders
    public static class OrderStats {
        @SerializedName("count")
        private int count;

        @SerializedName("change_percentage")
        private double changePercentage;

        @SerializedName("vs_label")
        private String vsLabel;

        public int getCount() {
            return count;
        }

        public double getChangePercentage() {
            return changePercentage;
        }

        public String getVsLabel() {
            return vsLabel;
        }
    }

    // Nested class for today's earnings
    public static class EarningsStats {
        @SerializedName("amount")
        private double amount;

        @SerializedName("change_percentage")
        private double changePercentage;

        @SerializedName("vs_label")
        private String vsLabel;

        public double getAmount() {
            return amount;
        }

        public double getChangePercentage() {
            return changePercentage;
        }

        public String getVsLabel() {
            return vsLabel;
        }
    }

    // Nested class for active orders
    public static class ActiveOrdersStats {
        @SerializedName("count")
        private int count;

        @SerializedName("change_percentage")
        private double changePercentage;

        @SerializedName("vs_label")
        private String vsLabel;

        public int getCount() {
            return count;
        }

        public double getChangePercentage() {
            return changePercentage;
        }

        public String getVsLabel() {
            return vsLabel;
        }
    }

    // Nested class for average rating
    public static class RatingStats {
        @SerializedName("rating")
        private double rating;

        @SerializedName("change_value")
        private double changeValue;

        @SerializedName("review_count")
        private int reviewCount;

        @SerializedName("vs_label")
        private String vsLabel;

        public double getRating() {
            return rating;
        }

        public double getChangeValue() {
            return changeValue;
        }

        public int getReviewCount() {
            return reviewCount;
        }

        public String getVsLabel() {
            return vsLabel;
        }
    }
}
