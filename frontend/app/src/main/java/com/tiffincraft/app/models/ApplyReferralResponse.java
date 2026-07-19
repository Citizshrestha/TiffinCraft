package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class ApplyReferralResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("credits")
    private double credits;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public double getCredits() { return credits; }
}
