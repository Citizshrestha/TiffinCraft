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

        @SerializedName("plan_id")
        private int planId;

        /**
         * pending_payment | pending_verification | verified | scheduled |
         * active | paused | completed | cancelled.
         *
         * 'scheduled' is a real, paid state — the cook has confirmed the money
         * and the customer's chosen start date simply hasn't arrived yet. It
         * must never be rendered as "Active" (nothing is being delivered) nor
         * lumped in with the unpaid states (they have already paid).
         */
        @SerializedName("status")
        private String status;

        @SerializedName("payment_status")
        private String paymentStatus; // pending | submitted | verified | rejected | failed

        /**
         * "esewa" or "manual_qr". Decides how an unpaid subscription is retried:
         * eSewa rows go back through the gateway, manual_qr rows go back to the
         * upload-proof screen. Never inferred from payment_status.
         */
        @SerializedName("payment_method")
        private String paymentMethod;

        /**
         * The customer's chosen first delivery day, 'YYYY-MM-DD'. Survives all
         * the way to activation — it is no longer a placeholder overwritten
         * with the payment date, so a cook verifying today for a start date
         * next week must show THIS, not "starts now".
         */
        @SerializedName("start_date")
        private String startDate;

        /** Last day this paid cycle covers; null for open-ended legacy rows. */
        @SerializedName("end_date")
        private String endDate;

        /** When the cook confirmed payment (UTC ISO); null if never verified. */
        @SerializedName("verified_at")
        private String verifiedAt;

        /** users.id of the cook/admin who verified; null for eSewa auto-verification. */
        @SerializedName("verified_by")
        private Integer verifiedBy;

        /** Paid deliveries in this cycle. Null on legacy rows predating credits. */
        @SerializedName("meals_total")
        private Integer mealsTotal;

        /** Deliveries still owed. Reaching 0 auto-completes the subscription. */
        @SerializedName("meals_remaining")
        private Integer mealsRemaining;

        @SerializedName("payment_screenshot_url")
        private String paymentScreenshotUrl;

        @SerializedName("verification_notes")
        private String verificationNotes;

        @SerializedName("delivery_address")
        private String deliveryAddress;

        /**
         * Next date a meal is expected, 'YYYY-MM-DD'.
         *
         * NULLABLE. A completed or cancelled subscription has none, and neither
         * does one whose every remaining day has been skipped — so every display
         * site must handle null rather than assuming a date is always present.
         */
        @SerializedName("next_delivery_date")
        private String nextDeliveryDate;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("payment_submitted_at")
        private String paymentSubmittedAt;

        @SerializedName("plan")
        private SubscriptionPlanResponse.Plan plan;

        /**
         * Cook's eSewa QR, for the manual-pay screen. Supplied by
         * GET /subscriptions/customer/my; null when the cook never uploaded
         * one, so callers must treat absence as normal rather than an error.
         */
        @SerializedName("cook_esewa_qr_url")
        private String cookEsewaQrUrl;

        // Flat fields present only on GET /subscriptions/cook/my (getCookSubscribers) —
        // that endpoint joins plan/customer columns directly instead of nesting a
        // full `plan` object like getMySubscriptions does. Null on the customer-side response.
        @SerializedName("plan_name")
        private String planName;

        @SerializedName("duration")
        private String duration;

        @SerializedName("customer_name")
        private String customerName;

        @SerializedName("customer_phone")
        private String customerPhone;

        public int getId() {
            return id;
        }

        public int getCookId() {
            return cookId;
        }

        public int getPlanId() {
            return planId;
        }

        public String getStatus() {
            return status;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        /** True when this subscription is (or was) paid through the eSewa gateway. */
        public boolean isEsewaPayment() {
            return "esewa".equals(paymentMethod);
        }

        public String getEndDate() {
            return endDate;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getVerifiedAt() {
            return verifiedAt;
        }

        public Integer getVerifiedBy() {
            return verifiedBy;
        }

        public Integer getMealsTotal() {
            return mealsTotal;
        }

        public Integer getMealsRemaining() {
            return mealsRemaining;
        }

        /**
         * Paid and confirmed, but the chosen start date hasn't arrived. Kept as a
         * helper because both UIs must treat it as "all set, nothing to do yet" —
         * neither "active" nor "awaiting payment".
         */
        public boolean isScheduled() {
            return "scheduled".equals(status);
        }

        /** Cook has been paid and confirmed it; deliveries are happening or pending a start date. */
        public boolean isLiveAndPaid() {
            return "active".equals(status) || "scheduled".equals(status) || "paused".equals(status);
        }

        /** Waiting on the cook to confirm a payment the customer has already made. */
        public boolean isAwaitingVerification() {
            return "pending_verification".equals(status) || "submitted".equals(paymentStatus);
        }

        public String getPaymentScreenshotUrl() {
            return paymentScreenshotUrl;
        }

        public String getVerificationNotes() {
            return verificationNotes;
        }

        public String getDeliveryAddress() {
            return deliveryAddress;
        }

        public String getNextDeliveryDate() {
            return nextDeliveryDate;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getPaymentSubmittedAt() {
            return paymentSubmittedAt;
        }

        public SubscriptionPlanResponse.Plan getPlan() {
            return plan;
        }

        public String getCookEsewaQrUrl() {
            return cookEsewaQrUrl;
        }

        public String getPlanName() {
            return plan != null ? plan.getName() : planName;
        }

        public String getDuration() {
            return plan != null ? plan.getDuration() : duration;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getCustomerPhone() {
            return customerPhone;
        }

        public String getDurationLabel() {
            String dur = getDuration();
            if ("2_weeks".equals(dur) || "biweekly".equals(dur) || "2_week".equals(dur)) return "2 Weeks";
            if ("monthly".equals(dur) || "1_month".equals(dur) || "1_months".equals(dur)) return "1 Month";
            return "1 Week";
        }
    }
}
