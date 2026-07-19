package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReferralInfoResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("code")
    private String code;

    @SerializedName("credits")
    private double credits;

    @SerializedName("total_referred")
    private int totalReferred;

    @SerializedName("has_applied")
    private boolean hasApplied;

    @SerializedName("referrer_reward")
    private double referrerReward;

    @SerializedName("referred_reward")
    private double referredReward;

    @SerializedName("referrals")
    private List<ReferralEntry> referrals;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getCode() { return code; }
    public double getCredits() { return credits; }
    public int getTotalReferred() { return totalReferred; }
    public boolean hasApplied() { return hasApplied; }
    public double getReferrerReward() { return referrerReward; }
    public double getReferredReward() { return referredReward; }
    public List<ReferralEntry> getReferrals() { return referrals; }

    public static class ReferralEntry {
        @SerializedName("full_name")
        private String fullName;

        @SerializedName("reward")
        private double reward;

        @SerializedName("created_at")
        private String createdAt;

        public String getFullName() { return fullName; }
        public double getReward() { return reward; }
        public String getCreatedAt() { return createdAt; }
    }
}
