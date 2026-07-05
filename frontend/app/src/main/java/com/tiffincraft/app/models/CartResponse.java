package com.tiffincraft.app.models;

import java.util.List;

public class CartResponse {
    private boolean success;
    private String message;
    private List<CartGroup> cart_groups;
    private double grand_total;
    private int item_count;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<CartGroup> getCartGroups() { return cart_groups; }
    public double getGrandTotal() { return grand_total; }
    public int getItemCount() { return item_count; }
}