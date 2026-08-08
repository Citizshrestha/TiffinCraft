package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Response for GET /commission/settlements/current */
public class CommissionSettlementCurrentResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("current")
    private CommissionSettlement current;

    @SerializedName("past_due")
    private CommissionSettlement pastDue;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public CommissionSettlement getCurrent() { return current; }
    public CommissionSettlement getPastDue() { return pastDue; }
    public String getMessage() { return message; }
}
