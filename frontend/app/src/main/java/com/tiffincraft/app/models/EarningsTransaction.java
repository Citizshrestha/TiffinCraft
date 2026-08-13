package com.tiffincraft.app.models;

public class EarningsTransaction {
    private int orderId;
    private String customerName;
    private double amount;
    private String paymentMethod;
    private String time;
    private String date;
    private String imageUrl;  // First meal image from the order

    public EarningsTransaction() {
    }

    public EarningsTransaction(int orderId, String customerName, double amount, String time, String date) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.amount = amount;
        this.time = time;
        this.date = date;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /** True for online/eSewa/QR payments; false for Cash on Delivery. */
    public boolean isOnlinePayment() {
        return paymentMethod != null && !"cod".equalsIgnoreCase(paymentMethod.trim());
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
