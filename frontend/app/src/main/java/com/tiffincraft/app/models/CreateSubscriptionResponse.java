package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Response for POST /subscriptions — creating a subscription no longer
 *  activates it, so the caller needs the new subscriptionId to hand off
 *  to SubscriptionPaymentActivity. */
public class CreateSubscriptionResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("subscriptionId")
    private int subscriptionId;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }
}
