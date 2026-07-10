package com.tiffincraft.app.models;

public class MonthlyBreakdown {
    private String month;
    private double amount;

    public MonthlyBreakdown() {
    }

    public MonthlyBreakdown(String month, double amount) {
        this.month = month;
        this.amount = amount;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
