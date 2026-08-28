package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * The reply from every WRITE in the subscription request→active flow and the
 * custom-meal flow: request, respond, payment-proof, verify-proof, custom-meal
 * create/respond/cancel.
 *
 * One model for all of them because they are all "here is the new state, here is
 * what to tell the user" — a flat acknowledgement, deliberately NOT a whole
 * subscription. The screen that needs the full picture re-fetches
 * getSubscriptionDetail() afterwards, so there is one source of truth for
 * stage/headline/detail instead of two payloads that can disagree.
 *
 * Each endpoint fills in only the fields that mean something for it; Gson leaves
 * the rest null or 0. `success` and `message` are always present, and `message`
 * is written to be shown to the user verbatim.
 */
public class SubscriptionActionResponse {

    @SerializedName("success")
    private boolean success;

    /** Server-authored, user-facing. Show this rather than inventing wording. */
    @SerializedName("message")
    private String message;

    /**
     * The subscription's new lifecycle status, or — on the custom-meal
     * endpoints — the request's new status ('pending', 'accepted', 'declined').
     */
    @SerializedName("status")
    private String status;

    /** Set by createSubscriptionRequest only. */
    @SerializedName("subscription_id")
    private Integer subscriptionId;

    /** Set by createCustomMealRequest only. */
    @SerializedName("request_id")
    private Integer requestId;

    /**
     * The chat thread the announcement was posted into. Lets the app jump
     * straight to the conversation that now holds the request card.
     */
    @SerializedName("conversation_id")
    private Integer conversationId;

    @SerializedName("start_date")
    private String startDate;

    /** The swap's date, from createCustomMealRequest. */
    @SerializedName("delivery_date")
    private String deliveryDate;

    @SerializedName("duration_days")
    private int durationDays;

    /** Per-delivery price. Nullable on a plan with no price set. */
    @SerializedName("amount")
    private Double amount;

    @SerializedName("meal_id")
    private Integer mealId;

    @SerializedName("meal_name")
    private String mealName;

    /**
     * cancelSubscription only: what the customer still owes. The FULL plan amount
     * once the cook had confirmed the payment — however early the cancellation and
     * however many days were skipped — and 0 before that confirmation.
     */
    @SerializedName("amount_owed")
    private Double amountOwed;

    /**
     * cancelSubscription only: money already transferred for a subscription the
     * cook never confirmed, so nothing was owed and it has to come back. Settling
     * it is an admin action — there is no refund_requests row for it.
     */
    @SerializedName("refund_due")
    private Double refundDue;

    /** The stored image, returned by submitPaymentProof. */
    @SerializedName("payment_screenshot_url")
    private String paymentScreenshotUrl;

    /** Only verifySubscriptionProof's verify path sends this. */
    @SerializedName("subscription")
    private Verified subscription;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public Integer getSubscriptionId() { return subscriptionId; }
    public Integer getRequestId() { return requestId; }
    public Integer getConversationId() { return conversationId; }
    public String getStartDate() { return startDate; }
    public String getDeliveryDate() { return deliveryDate; }
    public int getDurationDays() { return durationDays; }
    public Double getAmount() { return amount; }
    public Integer getMealId() { return mealId; }
    public String getMealName() { return mealName; }
    public String getPaymentScreenshotUrl() { return paymentScreenshotUrl; }
    public Double getAmountOwed() { return amountOwed; }
    public Double getRefundDue() { return refundDue; }
    public Verified getSubscription() { return subscription; }

    /** Total for the whole plan — per-day price × calendar days. */
    public Double getTotalAmount() {
        if (amount == null || durationDays <= 0) return null;
        return amount * durationDays;
    }

    /**
     * The window the server actually fixed at verification.
     *
     * `startDateClamped` is true when the customer's chosen start date had
     * already gone past by the time the cook verified — the subscription then
     * starts today instead, and the cook's screen should say so rather than
     * showing a date in the past.
     */
    public static class Verified {
        @SerializedName("id")
        private int id;

        /** 'active' if it starts today, otherwise 'scheduled'. */
        @SerializedName("status")
        private String status;

        @SerializedName("start_date")
        private String startDate;

        /** start_date + duration_days - 1. Fixed here and never moved again. */
        @SerializedName("end_date")
        private String endDate;

        @SerializedName("duration_days")
        private int durationDays;

        @SerializedName("start_date_clamped")
        private boolean startDateClamped;

        public int getId() { return id; }
        public String getStatus() { return status; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public int getDurationDays() { return durationDays; }
        public boolean isStartDateClamped() { return startDateClamped; }
    }
}
