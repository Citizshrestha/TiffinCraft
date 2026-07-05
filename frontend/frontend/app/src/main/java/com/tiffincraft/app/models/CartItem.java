package com.tiffincraft.app.models;

public class CartItem {
    private int cart_item_id;
    private int meal_id;
    private String meal_name;
    private double price;
    private String image_url;
    private int quantity;
    private boolean is_available;
    private double line_total;

    public int getCartItemId() { return cart_item_id; }
    public int getMealId() { return meal_id; }
    public String getMealName() { return meal_name; }
    public double getPrice() { return price; }
    public String getImageUrl() { return image_url; }
    public int getQuantity() { return quantity; }
    public boolean isAvailable() { return is_available; }
    public double getLineTotal() { return line_total; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}