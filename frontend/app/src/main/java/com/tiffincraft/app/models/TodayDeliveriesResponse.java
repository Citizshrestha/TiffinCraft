package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GET /api/cook/today-deliveries — who the cook is actually cooking for on a
 * given date, and the one date they can still change.
 *
 * `nextChangeable` is not the same date as `date`: today's cutoff has already
 * passed by definition, so the bulk "kitchen closed" action always targets
 * TOMORROW. Reading the target off this object rather than computing it locally
 * is what keeps the button honest when the cutoff hour is reconfigured.
 */
public class TodayDeliveriesResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    /** The date being viewed, 'YYYY-MM-DD' in Nepal Time. */
    @SerializedName("date")
    private String date;

    @SerializedName("is_today")
    private boolean isToday;

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("next_changeable")
    private NextChangeable nextChangeable;

    @SerializedName("cutoff")
    private Cutoff cutoff;

    /** True when the viewed date itself is one the cook already closed. */
    @SerializedName("viewed_date_unavailable")
    private boolean viewedDateUnavailable;

    @SerializedName("deliveries")
    private List<Delivery> deliveries;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getDate() { return date; }
    public boolean isToday() { return isToday; }
    public Summary getSummary() { return summary; }
    public NextChangeable getNextChangeable() { return nextChangeable; }
    public Cutoff getCutoff() { return cutoff; }
    public boolean isViewedDateUnavailable() { return viewedDateUnavailable; }
    public List<Delivery> getDeliveries() { return deliveries; }

    public static class Summary {
        @SerializedName("total")
        private int total;

        /** Meals to actually prepare — scheduled plus already delivered. */
        @SerializedName("cooking")
        private int cooking;

        @SerializedName("customer_skipped")
        private int customerSkipped;

        @SerializedName("cook_unavailable")
        private int cookUnavailable;

        @SerializedName("delivered")
        private int delivered;

        @SerializedName("missed")
        private int missed;

        public int getTotal() { return total; }
        public int getCooking() { return cooking; }
        public int getCustomerSkipped() { return customerSkipped; }
        public int getCookUnavailable() { return cookUnavailable; }
        public int getDelivered() { return delivered; }
        public int getMissed() { return missed; }
    }

    /** The only date the cook can still open or close, plus how long they have. */
    public static class NextChangeable {
        @SerializedName("date")
        private String date;

        @SerializedName("is_marked_unavailable")
        private boolean isMarkedUnavailable;

        @SerializedName("reason")
        private String reason;

        @SerializedName("ms_until_cutoff")
        private long msUntilCutoff;

        @SerializedName("is_locked")
        private boolean isLocked;

        public String getDate() { return date; }
        public boolean isMarkedUnavailable() { return isMarkedUnavailable; }
        public String getReason() { return reason; }
        public long getMsUntilCutoff() { return msUntilCutoff; }
        public boolean isLocked() { return isLocked; }
    }

    public static class Cutoff {
        @SerializedName("hour")
        private int hour;

        @SerializedName("label")
        private String label;

        public int getHour() { return hour; }
        public String getLabel() { return label; }
    }

    /** One subscriber's meal for the viewed date. */
    public static class Delivery {
        @SerializedName("subscription_id")
        private int subscriptionId;

        @SerializedName("customer_id")
        private int customerId;

        @SerializedName("customer_name")
        private String customerName;

        @SerializedName("customer_phone")
        private String customerPhone;

        @SerializedName("plan_name")
        private String planName;

        @SerializedName("duration")
        private String duration;

        @SerializedName("delivery_address")
        private String deliveryAddress;

        /** Nullable on legacy subscriptions with no meal-credit accounting. */
        @SerializedName("meals_remaining")
        private Integer mealsRemaining;

        @SerializedName("status")
        private String status;

        @SerializedName("label")
        private String label;

        @SerializedName("toggled_by")
        private String toggledBy;

        @SerializedName("reason")
        private String reason;

        @SerializedName("credit_deducted")
        private boolean creditDeducted;

        @SerializedName("order_id")
        private Integer orderId;

        public int getSubscriptionId() { return subscriptionId; }
        public int getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public String getPlanName() { return planName; }
        public String getDuration() { return duration; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public Integer getMealsRemaining() { return mealsRemaining; }
        public String getStatus() { return status; }
        public String getLabel() { return label; }
        public String getToggledBy() { return toggledBy; }
        public String getReason() { return reason; }
        public boolean isCreditDeducted() { return creditDeducted; }
        public Integer getOrderId() { return orderId; }

        public boolean isScheduled() { return "scheduled".equals(status); }
        public boolean isCustomerSkipped() { return "customer_skipped".equals(status); }
        public boolean isCookUnavailable() { return "cook_unavailable".equals(status); }
        public boolean isDelivered() { return "delivered".equals(status); }
        public boolean isMissed() { return "missed".equals(status); }

        /** A meal that still has to be made for this date. */
        public boolean needsCooking() { return isScheduled(); }
    }
}
