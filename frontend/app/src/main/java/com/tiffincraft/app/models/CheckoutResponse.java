package com.tiffincraft.app.models;

import java.util.List;

public class CheckoutResponse {
    private boolean success;
    private String message;
    private List<CreatedOrder> orders;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<CreatedOrder> getOrders() { return orders; }

    public static class CreatedOrder {
        private int order_id;
        private int cook_id;
        private double total_amount;

        public int getOrderId() { return order_id; }
        public int getCookId() { return cook_id; }
        public double getTotalAmount() { return total_amount; }
    }
}