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

    /**
     * Total already received against this settlement, accumulated across part
     * payments. Absent on responses from older backends, where Gson leaves it 0
     * — which is the correct reading of "nothing recorded as paid".
     */
    @SerializedName("amount_paid")
    private double amountPaid;

    @SerializedName("order_count")
    private int orderCount;

    @SerializedName("status")
    private String status; // pending | submitted | verified | rejected

    @SerializedName("payment_screenshot_url")
    private String paymentScreenshotUrl;

    @SerializedName("admin_notes")
    private String adminNotes;

    /**
     * Day the payment is actually due — (1st of the month after the billed
     * period) + the 15-day grace period, set once at generation time. Null on
     * legacy rows created before the due_date column existed, so callers must
     * treat absence as "no known due date" rather than "overdue".
     */
    @SerializedName("due_date")
    private String dueDate;

    public int getId() { return id; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public double getAmountDue() { return amountDue; }
    public double getAmountPaid() { return amountPaid; }

    /** What the cook still owes. Never negative, even if an overpayment is ever recorded. */
    public double getAmountRemaining() { return Math.max(0d, amountDue - amountPaid); }

    /** True when money has come in but the settlement is not yet covered in full. */
    public boolean isPartiallyPaid() {
        // Compared in paisa — 145 - 100 - 45 is not reliably 0 in double arithmetic.
        return Math.round(amountPaid * 100) > 0 && Math.round(amountPaid * 100) < Math.round(amountDue * 100);
    }
    public int getOrderCount() { return orderCount; }
    public String getStatus() { return status; }
    public String getPaymentScreenshotUrl() { return paymentScreenshotUrl; }
    public String getAdminNotes() { return adminNotes; }
    public String getDueDate() { return dueDate; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isSubmitted() { return "submitted".equals(status); }
    public boolean isVerified() { return "verified".equals(status); }
    public boolean isRejected() { return "rejected".equals(status); }

    /**
     * Mirrors the backend's is_overdue rule (listSettlements): still unpaid and
     * past the grace period. Compared as ISO yyyy-MM-dd strings, which sort
     * lexicographically — no Date parsing, no timezone to get wrong. A null
     * due_date is never overdue.
     */
    public boolean isOverdue(String todayIso) {
        if (!isPending() || dueDate == null || dueDate.length() < 10 || todayIso == null) return false;
        return dueDate.substring(0, 10).compareTo(todayIso) < 0;
    }
}
