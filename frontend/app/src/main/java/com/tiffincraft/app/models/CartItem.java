package com.tiffincraft.app.models;

public class CartItem {
    private int cart_item_id;
    private int meal_id;
    private String meal_name;
    private String description;
    private double price;
    private String image_url;
    private int quantity;
    private boolean available;
    private int cook_id;
    private String cook_name;
    private String created_at;

    public int getCartItemId() { return cart_item_id; }
    public int getMealId() { return meal_id; }
    public String getMealName() { return meal_name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getImageUrl() { return image_url; }
    public int getQuantity() { return quantity; }
    public boolean isAvailable() { return available; }
    public int getCookId() { return cook_id; }
    public String getCookName() { return cook_name; }
    public String getCreatedAt() { return created_at; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}
