package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GET /api/subscriptions/cook/requests?filter=… — the cook's "Subscription
 * Requests" inbox.
 *
 * Separate from CookSubscribersResponse on purpose: that one lists people
 * already being cooked for, this one lists decisions waiting on the cook. The
 * default `pending` filter mixes both kinds of decision (a new request to answer
 * and a payment screenshot to check) because from the cook's side they are the
 * same job — something is blocked on them.
 *
 * `counts` describes the WHOLE inbox, not the filtered slice, so the filter chips
 * keep showing real totals while the list below shows one filter.
 */
public class SubscriptionRequestsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    /** Echoes the filter the server actually applied. */
    @SerializedName("filter")
    private String filter;

    @SerializedName("counts")
    private Counts counts;

    @SerializedName("requests")
    private List<Item> requests;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getFilter() { return filter; }
    public Counts getCounts() { return counts; }
    public List<Item> getRequests() { return requests; }

    public static class Counts {
        @SerializedName("requested")
        private int requested;

        @SerializedName("awaiting_proof_check")
        private int awaitingProofCheck;

        /** Accepted, waiting for the customer to upload a screenshot. */
        @SerializedName("awaiting_payment")
        private int awaitingPayment;

        public int getRequested() { return requested; }
        public int getAwaitingProofCheck() { return awaitingProofCheck; }
        public int getAwaitingPayment() { return awaitingPayment; }

        /** The badge number for the whole screen. */
        public int getActionable() { return requested + awaitingProofCheck; }
    }

    /** One subscription in the inbox. */
    public static class Item {
        @SerializedName("id")
        private int id;

        /** requested | accepted | rejected | pending_verification | verified | scheduled | active */
        @SerializedName("status")
        private String status;

        @SerializedName("payment_status")
        private String paymentStatus;

        /** What the customer wrote when subscribing. Nullable. */
        @SerializedName("request_note")
        private String requestNote;

        /** What the cook wrote when accepting or rejecting. Nullable. */
        @SerializedName("response_note")
        private String responseNote;

        /** The proof image. Present once submitted, and KEPT even after a rejection. */
        @SerializedName("payment_screenshot_url")
        private String paymentScreenshotUrl;

        @SerializedName("payment_rejection_reason")
        private String paymentRejectionReason;

        @SerializedName("payment_proof_attempts")
        private int paymentProofAttempts;

        @SerializedName("start_date")
        private String startDate;

        @SerializedName("end_date")
        private String endDate;

        @SerializedName("delivery_address")
        private String deliveryAddress;

        @SerializedName("requested_at")
        private String requestedAt;

        @SerializedName("responded_at")
        private String respondedAt;

        @SerializedName("payment_submitted_at")
        private String paymentSubmittedAt;

        @SerializedName("customer_id")
        private int customerId;

        @SerializedName("customer_name")
        private String customerName;

        @SerializedName("customer_phone")
        private String customerPhone;

        @SerializedName("customer_image")
        private String customerImage;

        @SerializedName("plan_id")
        private int planId;

        @SerializedName("plan_name")
        private String planName;

        @SerializedName("duration")
        private String duration;

        /** Per-delivery price. Nullable on a plan with no price set. */
        @SerializedName("amount")
        private Double amount;

        /** 7, 14 or 30 — resolved server-side from `duration`. */
        @SerializedName("duration_days")
        private int durationDays;

        /** Null when customer and cook have never had a chat thread. */
        @SerializedName("conversation_id")
        private Integer conversationId;

        /** True exactly when this row needs Accept/Reject. */
        @SerializedName("needs_decision")
        private boolean needsDecision;

        /** True exactly when this row needs Verify/Reject on a screenshot. */
        @SerializedName("needs_payment_check")
        private boolean needsPaymentCheck;

        /** Machine-readable stage key, from the server's single stageFor(). */
        @SerializedName("stage")
        private String stage;

        /** e.g. "Waiting for payment verification" — display verbatim. */
        @SerializedName("headline")
        private String headline;

        /** One supporting line under the headline. */
        @SerializedName("detail")
        private String detail;

        public int getId() { return id; }
        public String getStatus() { return status; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getRequestNote() { return requestNote; }
        public String getResponseNote() { return responseNote; }
        public String getPaymentScreenshotUrl() { return paymentScreenshotUrl; }
        public String getPaymentRejectionReason() { return paymentRejectionReason; }
        public int getPaymentProofAttempts() { return paymentProofAttempts; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public String getRequestedAt() { return requestedAt; }
        public String getRespondedAt() { return respondedAt; }
        public String getPaymentSubmittedAt() { return paymentSubmittedAt; }
        public int getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public String getCustomerImage() { return customerImage; }
        public int getPlanId() { return planId; }
        public String getPlanName() { return planName; }
        public String getDuration() { return duration; }
        public Double getAmount() { return amount; }
        public int getDurationDays() { return durationDays; }
        public Integer getConversationId() { return conversationId; }
        public boolean needsDecision() { return needsDecision; }
        public boolean needsPaymentCheck() { return needsPaymentCheck; }
        public String getStage() { return stage; }
        public String getHeadline() { return headline; }
        public String getDetail() { return detail; }

        /**
         * What the customer pays, once, for the whole plan.
         *
         * `amount` is already that total, not a per-day rate — so the figure the
         * cook checks the screenshot against is this number, unmultiplied.
         */
        public Double getTotalAmount() {
            return amount;
        }

        /** A resubmission after a rejection — worth flagging to the cook. */
        public boolean isRetriedProof() { return paymentProofAttempts > 1; }
    }
}
