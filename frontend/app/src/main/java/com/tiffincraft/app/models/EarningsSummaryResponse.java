package com.tiffincraft.app.models;

public class EarningsSummaryResponse {
    private boolean success;
    private EarningsSummary earnings;
    private String message;

    public EarningsSummaryResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public EarningsSummary getEarnings() {
        return earnings;
    }

    public void setEarnings(EarningsSummary earnings) {
        this.earnings = earnings;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
