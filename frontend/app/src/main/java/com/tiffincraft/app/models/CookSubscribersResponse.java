package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CookSubscribersResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("activeSubscriberCount")
    private int activeSubscriberCount;

    @SerializedName("scheduledSubscriberCount")
    private int scheduledSubscriberCount;

    @SerializedName("subscriptions")
    private List<SubscriptionResponse.Subscription> subscriptions;

    public boolean isSuccess() {
        return success;
    }

    public int getActiveSubscriberCount() {
        return activeSubscriberCount;
    }

    /** Verified and paid for, but its customer-chosen start date hasn't arrived yet. */
    public int getScheduledSubscriberCount() {
        return scheduledSubscriberCount;
    }

    /**
     * Everyone the cook has been paid by and owes meals to. 'scheduled' counts:
     * the cook verified the payment, so the subscription is live even though the
     * first meal hasn't gone out. Counting only 'active' made the number sit
     * still until the nightly job flipped the row on the start date.
     */
    public int getPaidSubscriberCount() {
        return activeSubscriberCount + scheduledSubscriberCount;
    }

    public List<SubscriptionResponse.Subscription> getSubscriptions() {
        return subscriptions;
    }
}
