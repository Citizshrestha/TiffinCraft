package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CookSubscribersResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("activeSubscriberCount")
    private int activeSubscriberCount;

    @SerializedName("subscriptions")
    private List<SubscriptionResponse.Subscription> subscriptions;

    public boolean isSuccess() {
        return success;
    }

    public int getActiveSubscriberCount() {
        return activeSubscriberCount;
    }

    public List<SubscriptionResponse.Subscription> getSubscriptions() {
        return subscriptions;
    }
}
