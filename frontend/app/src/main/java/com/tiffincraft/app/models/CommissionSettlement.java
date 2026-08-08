package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** A cook's monthly commission-due row — see backend commissionController.js. */
public class CommissionSettlement {

    @SerializedName("id")
    private int id;

    @SerializedName("month")
    private int month;

    @SerializedName("year")
    private int year;

    @SerializedName("amount_due")
    private double amountDue;

    @SerializedName("order_count")
    private int orderCount;

    @SerializedName("status")
    private String status; // pending | submitted | verified | rejected

    @SerializedName("payment_screenshot_url")
    private String paymentScreenshotUrl;

    @SerializedName("admin_notes")
    private String adminNotes;

    public int getId() { return id; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public double getAmountDue() { return amountDue; }
    public int getOrderCount() { return orderCount; }
    public String getStatus() { return status; }
    public String getPaymentScreenshotUrl() { return paymentScreenshotUrl; }
    public String getAdminNotes() { return adminNotes; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isSubmitted() { return "submitted".equals(status); }
    public boolean isVerified() { return "verified".equals(status); }
    public boolean isRejected() { return "rejected".equals(status); }
}
