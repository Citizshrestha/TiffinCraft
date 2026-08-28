package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GET /api/subscriptions/{id}/calendar — the per-day delivery schedule.
 *
 * Every date field here is already a plain 'YYYY-MM-DD' string: the server
 * formats date columns in SQL precisely so an ISO timestamp
 * ("2026-09-03T00:00:00.000Z") can never reach this model and get displayed
 * verbatim, which is what the old flat "Active" card did.
 *
 * The client deliberately does NOT recompute the 8pm-NPT cutoff. `isLocked`,
 * `canSkip` and `lockedMessage` are decided server-side against Nepal time and
 * the configurable platform_settings hour; duplicating that arithmetic in Java
 * would mean two implementations that disagree on the boundary.
 */
public class SubscriptionCalendarResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    /** "customer" or "cook" — the same endpoint serves both, read-only for cooks. */
    @SerializedName("viewer")
    private String viewer;

    /** Today's date in Nepal Time, not the device's timezone. */
    @SerializedName("today")
    private String today;

    @SerializedName("subscription")
    private Info subscription;

    @SerializedName("cutoff")
    private Cutoff cutoff;

    @SerializedName("window")
    private Window window;

    @SerializedName("days")
    private List<Day> days;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getViewer() { return viewer; }
    public boolean isCustomerView() { return "customer".equals(viewer); }
    public String getToday() { return today; }
    public Info getSubscription() { return subscription; }
    public Cutoff getCutoff() { return cutoff; }
    public Window getWindow() { return window; }
    public List<Day> getDays() { return days; }

    /** Subscription-level context, so the calendar screen needs only this one call. */
    public static class Info {
        @SerializedName("id")
        private int id;

        @SerializedName("status")
        private String status;

        @SerializedName("payment_status")
        private String paymentStatus;

        @SerializedName("plan_name")
        private String planName;

        @SerializedName("duration")
        private String duration;

        @SerializedName("customer_name")
        private String customerName;

        @SerializedName("cook_name")
        private String cookName;

        @SerializedName("start_date")
        private String startDate;

        @SerializedName("end_date")
        private String endDate;

        /** Nullable — a completed/cancelled subscription has no next delivery. */
        @SerializedName("next_delivery_date")
        private String nextDeliveryDate;

        /** Nullable on legacy rows created before meal credits existed. */
        @SerializedName("meals_total")
        private Integer mealsTotal;

        @SerializedName("meals_remaining")
        private Integer mealsRemaining;

        /** 0 once started; drives "Starts in 3 days" on a scheduled subscription. */
        @SerializedName("days_until_start")
        private Integer daysUntilStart;

        public int getId() { return id; }
        public String getStatus() { return status; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getPlanName() { return planName; }
        public String getDuration() { return duration; }
        public String getCustomerName() { return customerName; }
        public String getCookName() { return cookName; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getNextDeliveryDate() { return nextDeliveryDate; }
        public Integer getMealsTotal() { return mealsTotal; }
        public Integer getMealsRemaining() { return mealsRemaining; }
        public Integer getDaysUntilStart() { return daysUntilStart; }

        /** True while the subscription is waiting for its chosen start date. */
        public boolean isScheduled() { return "scheduled".equals(status); }
    }

    /** The change deadline, as the server computed it in Nepal Time. */
    public static class Cutoff {
        @SerializedName("hour")
        private int hour;

        /** Pre-formatted, e.g. "8:00 PM" — never rebuilt from `hour` on the client. */
        @SerializedName("label")
        private String label;

        @SerializedName("next_editable_date")
        private String nextEditableDate;

        /** Milliseconds until the next cutoff; 0 once it has already passed. */
        @SerializedName("ms_until_cutoff")
        private long msUntilCutoff;

        public int getHour() { return hour; }
        public String getLabel() { return label; }
        public String getNextEditableDate() { return nextEditableDate; }
        public long getMsUntilCutoff() { return msUntilCutoff; }
    }

    public static class Window {
        @SerializedName("from")
        private String from;

        @SerializedName("to")
        private String to;

        public String getFrom() { return from; }
        public String getTo() { return to; }
    }

    /** One delivery date. */
    public static class Day {
        @SerializedName("date")
        private String date;

        /** scheduled | customer_skipped | cook_unavailable | delivered | missed */
        @SerializedName("status")
        private String status;

        /** Server-supplied wording ("You skipped", "Kitchen closed") — kept
         *  server-side so app and any future web client can't drift apart. */
        @SerializedName("label")
        private String label;

        /** customer | cook | system — who last changed this day. */
        @SerializedName("toggled_by")
        private String toggledBy;

        @SerializedName("reason")
        private String reason;

        @SerializedName("credit_deducted")
        private boolean creditDeducted;

        @SerializedName("order_id")
        private Integer orderId;

        @SerializedName("is_today")
        private boolean isToday;

        @SerializedName("is_locked")
        private boolean isLocked;

        @SerializedName("can_skip")
        private boolean canSkip;

        /** Why this day can't be changed. Non-null exactly when isLocked. */
        @SerializedName("locked_message")
        private String lockedMessage;

        /**
         * The meal swap already asked for on this day, or null. Present so the
         * calendar can show a request the customer already sent instead of
         * offering the button again and hitting the UNIQUE (subscription, date)
         * key on the server.
         */
        @SerializedName("custom_meal")
        private CustomMeal customMeal;

        /**
         * Server-decided: true only when a swap request would actually be
         * accepted (day still scheduled, before cutoff, nothing asked for yet).
         * Same conditions the create endpoint enforces, so the UI never shows a
         * button the server will refuse.
         */
        @SerializedName("can_request_custom")
        private boolean canRequestCustom;

        public String getDate() { return date; }
        public String getStatus() { return status; }
        public String getLabel() { return label; }
        public String getToggledBy() { return toggledBy; }
        public String getReason() { return reason; }
        public boolean isCreditDeducted() { return creditDeducted; }
        public Integer getOrderId() { return orderId; }
        public boolean isToday() { return isToday; }
        public boolean isLocked() { return isLocked; }
        public boolean canSkip() { return canSkip; }
        public String getLockedMessage() { return lockedMessage; }
        public CustomMeal getCustomMeal() { return customMeal; }
        public boolean canRequestCustom() { return canRequestCustom; }

        public boolean isScheduled() { return "scheduled".equals(status); }
        public boolean isCustomerSkipped() { return "customer_skipped".equals(status); }
        public boolean isCookUnavailable() { return "cook_unavailable".equals(status); }
        public boolean isDelivered() { return "delivered".equals(status); }
        public boolean isMissed() { return "missed".equals(status); }
    }
}
