package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CartItem {
    @SerializedName("cart_item_id")
    private int cart_item_id;

    @SerializedName("meal_id")
    private int meal_id;

    @SerializedName("meal_name")
    private String meal_name;

    private String description;

    private double price;

    @SerializedName("image_url")
    private String image_url;

    private int quantity;

    /** Backend sends is_available (boolean or 0/1). */
    @SerializedName("is_available")
    private Boolean is_available;

    @SerializedName("line_total")
    private double line_total;

    @SerializedName("cook_id")
    private int cook_id;

    @SerializedName("cook_name")
    private String cook_name;

    @SerializedName("created_at")
    private String created_at;

    public int getCartItemId() { return cart_item_id; }
    public int getMealId() { return meal_id; }
    public String getMealName() { return meal_name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getImageUrl() { return image_url; }
    public int getQuantity() { return quantity; }

    public boolean isAvailable() {
        // Default true when field missing so cart items don't show as sold out incorrectly
        return is_available == null || Boolean.TRUE.equals(is_available);
    }

    public double getLineTotal() { return line_total; }
    public int getCookId() { return cook_id; }
    public String getCookName() { return cook_name; }
    public String getCreatedAt() { return created_at; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}
