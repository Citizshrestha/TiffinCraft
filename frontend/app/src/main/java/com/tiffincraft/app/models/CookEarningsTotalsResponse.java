package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Response for GET /orders/cook/earnings — all-time totals for the profile stats row. */
public class CookEarningsTotalsResponse {

    public static class Totals {
        @SerializedName("total_orders")
        private int totalOrders;

        @SerializedName("total_earned")
        private double totalEarned;

        @SerializedName("today_earned")
        private double todayEarned;

        @SerializedName("this_month_earned")
        private double thisMonthEarned;

        @SerializedName("pending_orders")
        private int pendingOrders;

        public int getTotalOrders() { return totalOrders; }
        public double getTotalEarned() { return totalEarned; }
        public double getTodayEarned() { return todayEarned; }
        public double getThisMonthEarned() { return thisMonthEarned; }
        public int getPendingOrders() { return pendingOrders; }
    }

    @SerializedName("success")
    private boolean success;

    @SerializedName("earnings")
    private Totals earnings;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public Totals getEarnings() { return earnings; }
    public String getMessage() { return message; }
}
