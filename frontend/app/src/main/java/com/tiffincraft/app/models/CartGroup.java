package com.tiffincraft.app.models;

import java.util.List;

public class CartGroup {
    private int cook_id;
    private String cook_name;
    private String kitchen_name;
    private List<CartItem> items;
    private double subtotal;

    public int getCookId() { return cook_id; }
    public String getCookName() { return cook_name; }
    public String getKitchenName() { return kitchen_name; }
    public List<CartItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
}