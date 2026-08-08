package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response from GET /api/payments/esewa/status/{transactionUuid}.
 * status is one of: BOOKED, PENDING, SUCCESS, FAILED, CANCELED, REVERTED —
 * always the server-confirmed value, never anything read off the eSewa
 * redirect URI directly (see PaymentResultActivity).
 */
public class EsewaStatusResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private String status;

    @SerializedName("order_id")
    private int orderId;

    @SerializedName("transaction_uuid")
    private String transactionUuid;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public int getOrderId() { return orderId; }
    public String getTransactionUuid() { return transactionUuid; }
}
