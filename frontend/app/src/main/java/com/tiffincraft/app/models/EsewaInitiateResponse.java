package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Response from POST /api/payments/esewa/initiate. */
public class EsewaInitiateResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("deeplink")
    private String deeplink;

    /**
     * The esewa:// URI the eSewa app actually handles, resolved server-side
     * from `deeplink`'s redirect. Nullable — see launchEsewaDeeplink.
     */
    @SerializedName("app_deeplink")
    private String appDeeplink;

    @SerializedName("booking_id")
    private String bookingId;

    @SerializedName("transaction_uuid")
    private String transactionUuid;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getDeeplink() { return deeplink; }
    public String getAppDeeplink() { return appDeeplink; }
    public String getBookingId() { return bookingId; }
    public String getTransactionUuid() { return transactionUuid; }
}
