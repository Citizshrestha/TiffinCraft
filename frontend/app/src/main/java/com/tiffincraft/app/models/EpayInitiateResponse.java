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

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getFormUrl() { return formUrl; }
    public Map<String, String> getFields() { return fields; }
    public String getTransactionUuid() { return transactionUuid; }
}
