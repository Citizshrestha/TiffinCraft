package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GET /api/subscriptions/{id}/detail — one subscription, everything about it.
 *
 * Serves the customer's status screen AND the cook's request detail screen;
 * `viewer` says which side you are on. Both sides read the SAME `stage`,
 * `headline` and `detail` strings, which the server derives in a single
 * stageFor() — so the two apps can never describe the same row differently.
 *
 * `events` is the audit trail (newest first, capped at 30 server-side): every
 * lifecycle and money transition with its actor and timestamp, which is what a
 * dispute is actually argued from.
 */
public class SubscriptionDetailResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    /** "customer" or "cook". */
    @SerializedName("viewer")
    private String viewer;

    /** Today in Nepal Time — not the device clock. */
    @SerializedName("today")
    private String today;

    @SerializedName("subscription")
    private Subscription subscription;

    /** The plan's default meals. Empty when the cook set no items. */
    @SerializedName("meals")
    private List<Meal> meals;

    @SerializedName("events")
    private List<Event> events;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getViewer() { return viewer; }
    public boolean isCookView() { return "cook".equals(viewer); }
    public String getToday() { return today; }
    public Subscription getSubscription() { return subscription; }
    public List<Meal> getMeals() { return meals; }
    public List<Event> getEvents() { return events; }

    public static class Subscription {
        @SerializedName("id")
        private int id;

        @SerializedName("customer_id")
        private int customerId;

        @SerializedName("cook_id")
        private int cookId;

        @SerializedName("plan_id")
        private int planId;

        /** requested | accepted | rejected | pending_payment | pending_verification
         *  | verified | scheduled | active | paused | completed | cancelled */
        @SerializedName("status")
        private String status;

        @SerializedName("payment_status")
        private String paymentStatus;

        @SerializedName("delivery_address")
        private String deliveryAddress;

        @SerializedName("request_note")
        private String requestNote;

        @SerializedName("response_note")
        private String responseNote;

        /** Kept even after a cook rejects it, so a dispute still has the image. */
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

        @SerializedName("next_delivery_date")
        private String nextDeliveryDate;

        @SerializedName("requested_at")
        private String requestedAt;

        @SerializedName("responded_at")
        private String respondedAt;

        @SerializedName("payment_submitted_at")
        private String paymentSubmittedAt;

        @SerializedName("verified_at")
        private String verifiedAt;

        @SerializedName("plan_name")
        private String planName;

        @SerializedName("duration")
        private String duration;

        /** Per-delivery price. Nullable on a plan with no price set. */
        @SerializedName("amount")
        private Double amount;

        @SerializedName("description")
        private String description;

        @SerializedName("customer_name")
        private String customerName;

        @SerializedName("customer_phone")
        private String customerPhone;

        @SerializedName("cook_name")
        private String cookName;

        @SerializedName("cook_phone")
        private String cookPhone;

        /**
         * The cook's eSewa QR, or null when they never set one up. Pulled out of
         * their bank_details blob server-side, same field name the order detail
         * endpoint uses, so the manual-pay screens share one contract. Null is a
         * real case: the customer pays through the eSewa app instead.
         */
        @SerializedName("cook_esewa_qr_url")
        private String cookEsewaQrUrl;

        /** Null when the two have never had a chat thread. */
        @SerializedName("conversation_id")
        private Integer conversationId;

        /** 7, 14 or 30 calendar days — resolved server-side from `duration`. */
        @SerializedName("duration_days")
        private int durationDays;

        /**
         * Calendar days consumed so far, clamped to durationDays. Skipping a day
         * still consumes it: the window is fixed at verification and never moves.
         */
        @SerializedName("days_elapsed")
        private int daysElapsed;

        @SerializedName("stage")
        private String stage;

        /** Display verbatim — do not re-word per status on the client. */
        @SerializedName("headline")
        private String headline;

        @SerializedName("detail")
        private String detail;

        public int getId() { return id; }
        public int getCustomerId() { return customerId; }
        public int getCookId() { return cookId; }
        public int getPlanId() { return planId; }
        public String getStatus() { return status; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public String getRequestNote() { return requestNote; }
        public String getResponseNote() { return responseNote; }
        public String getPaymentScreenshotUrl() { return paymentScreenshotUrl; }
        public String getPaymentRejectionReason() { return paymentRejectionReason; }
        public int getPaymentProofAttempts() { return paymentProofAttempts; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getNextDeliveryDate() { return nextDeliveryDate; }
        public String getRequestedAt() { return requestedAt; }
        public String getRespondedAt() { return respondedAt; }
        public String getPaymentSubmittedAt() { return paymentSubmittedAt; }
        public String getVerifiedAt() { return verifiedAt; }
        public String getPlanName() { return planName; }
        public String getDuration() { return duration; }
        public Double getAmount() { return amount; }
        public String getDescription() { return description; }
        public String getCustomerName() { return customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public String getCookName() { return cookName; }
        public String getCookPhone() { return cookPhone; }
        public String getCookEsewaQrUrl() { return cookEsewaQrUrl; }
        public Integer getConversationId() { return conversationId; }
        public int getDurationDays() { return durationDays; }
        public int getDaysElapsed() { return daysElapsed; }
        public String getStage() { return stage; }
        public String getHeadline() { return headline; }
        public String getDetail() { return detail; }

        /**
         * What the customer pays, once, for the whole plan.
         *
         * `amount` is already that total, not a per-day rate — one meal goes out
         * per day and the price does not scale with the number of days.
         */
        public Double getTotalAmount() {
            return amount;
        }

        /** The four customer-facing stages the status screen switches on. */
        public boolean isWaitingAccept() { return "waiting_accept".equals(stage); }
        public boolean isAwaitingPayment() { return "awaiting_payment".equals(stage); }
        public boolean isVerifying() { return "verifying".equals(stage); }
        public boolean isRunning() {
            return "scheduled".equals(stage) || "active".equals(stage) || "paused".equals(stage);
        }

        /** Over for good — nothing left for either side to do. */
        public boolean isClosed() {
            return "rejected".equals(stage) || "completed".equals(stage) || "cancelled".equals(stage);
        }

        /** The customer can upload (or re-upload after a rejection) right now. */
        public boolean canUploadProof() { return isAwaitingPayment(); }

        /** A resubmission after a rejection — worth flagging to the cook. */
        public boolean isRetriedProof() { return paymentProofAttempts > 1; }
    }

    /** One meal in the plan's default line-up. */
    public static class Meal {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("image_url")
        private String imageUrl;

        @SerializedName("quantity")
        private int quantity;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getImageUrl() { return imageUrl; }
        public int getQuantity() { return quantity; }
    }

    /**
     * One row of the audit trail. Both lifecycle events and money events land in
     * the same table, so this is a single timeline rather than two lists to merge.
     */
    public static class Event {
        /** e.g. requested, accepted, proof_submitted, proof_rejected, verified. */
        @SerializedName("event")
        private String event;

        /** Only set on money events. */
        @SerializedName("amount")
        private Double amount;

        /** Human-readable line including the actor. Display verbatim. */
        @SerializedName("detail")
        private String detail;

        @SerializedName("created_at")
        private String createdAt;

        public String getEvent() { return event; }
        public Double getAmount() { return amount; }
        public String getDetail() { return detail; }
        public String getCreatedAt() { return createdAt; }
    }
}
