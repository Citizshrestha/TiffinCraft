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

        @SerializedName("status")
        private String status; // pending_payment | active | paused | cancelled

        @SerializedName("payment_status")
        private String paymentStatus; // pending | submitted | verified | rejected

        @SerializedName("payment_screenshot_url")
        private String paymentScreenshotUrl;

        @SerializedName("verification_notes")
        private String verificationNotes;

        @SerializedName("delivery_address")
        private String deliveryAddress;

        @SerializedName("next_delivery_date")
        private String nextDeliveryDate;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("plan")
        private SubscriptionPlanResponse.Plan plan;

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

        public SubscriptionPlanResponse.Plan getPlan() {
            return plan;
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
    }
}
