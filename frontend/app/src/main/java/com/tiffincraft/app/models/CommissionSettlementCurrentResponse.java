package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Response for GET /commission/settlements/current */
public class CommissionSettlementCurrentResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("current")
    private CommissionSettlement current;

    @SerializedName("past_due")
    private CommissionSettlement pastDue;

    /**
     * Live, not-yet-billed accrual for the month currently in progress.
     * Settlement rows only exist once a month closes, so `current` is
     * structurally null mid-month — this is what a cook can actually see
     * about what they're building up. It is NOT a bill: there's no
     * settlement id yet — POST /commission/settlements/settle-now creates one
     * so the cook can pay it early (see {@code payable_now}).
     */
    @SerializedName("accruing")
    private Accruing accruing;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public CommissionSettlement getCurrent() { return current; }
    public CommissionSettlement getPastDue() { return pastDue; }
    public Accruing getAccruing() { return accruing; }
    public String getMessage() { return message; }

    public static class Accruing {
        @SerializedName("amount")
        private double amount;

        @SerializedName("order_count")
        private int orderCount;

        @SerializedName("month")
        private int month;

        @SerializedName("year")
        private int year;

        /** True when the cook may settle this accrual now instead of waiting for month close. */
        @SerializedName("payable_now")
        private boolean payableNow;

        public double getAmount() { return amount; }
        public boolean isPayableNow() { return payableNow; }
        public int getOrderCount() { return orderCount; }
        public int getMonth() { return month; }
        public int getYear() { return year; }
    }
}
