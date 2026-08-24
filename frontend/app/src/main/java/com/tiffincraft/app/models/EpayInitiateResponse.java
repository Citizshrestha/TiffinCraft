package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Response from POST /api/payments/esewa-epay/initiate (the ePay v2
 * fallback — see EsewaEpayCheckoutActivity). Unlike Intent Payment's
 * response, this carries the full signed form field set to be POSTed to
 * form_url from a WebView, not a native-app deeplink.
 */
public class EpayInitiateResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("form_url")
    private String formUrl;

    @SerializedName("fields")
    private Map<String, String> fields;

    @SerializedName("transaction_uuid")
    private String transactionUuid;

    /**
     * Only present when the response came from POST /api/subscriptions/initiate.
     * The subscription row exists at this point but is pending_payment — it is
     * NOT active until the backend verifies the payment with eSewa.
     */
    @SerializedName("subscription_id")
    private Integer subscriptionId;

    /** Amount the SERVER decided to charge, from the plan price in the DB. */
    @SerializedName("amount")
    private Double amount;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getFormUrl() { return formUrl; }
    public Map<String, String> getFields() { return fields; }
    public String getTransactionUuid() { return transactionUuid; }
    public int getSubscriptionId() { return subscriptionId != null ? subscriptionId : -1; }
    public double getAmount() { return amount != null ? amount : 0.0; }
}
