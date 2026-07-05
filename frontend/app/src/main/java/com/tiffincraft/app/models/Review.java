package com.tiffincraft.app.models;

public class Review {
    private int id;
    private int order_id;
    private int customer_id;
    private int cook_id;
    private int meal_id;
    private int rating;
    private String comment;
    private String cook_reply;
    private String cook_reply_at;
    private String created_at;
    private String updated_at;

    // Additional fields from JOIN queries
    private String customer_name;
    private String customer_image;
    private String meal_name;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return order_id;
    }

    public void setOrderId(int order_id) {
        this.order_id = order_id;
    }

    public int getCustomerId() {
        return customer_id;
    }

    public void setCustomerId(int customer_id) {
        this.customer_id = customer_id;
    }

    public int getCookId() {
        return cook_id;
    }

    public void setCookId(int cook_id) {
        this.cook_id = cook_id;
    }

    public int getMealId() {
        return meal_id;
    }

    public void setMealId(int meal_id) {
        this.meal_id = meal_id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCookReply() {
        return cook_reply;
    }

    public void setCookReply(String cook_reply) {
        this.cook_reply = cook_reply;
    }

    public String getCookReplyAt() {
        return cook_reply_at;
    }

    public void setCookReplyAt(String cook_reply_at) {
        this.cook_reply_at = cook_reply_at;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdatedAt() {
        return updated_at;
    }

    public void setUpdatedAt(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getCustomerName() {
        return customer_name;
    }

    public void setCustomerName(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getCustomerImage() {
        return customer_image;
    }

    public void setCustomerImage(String customer_image) {
        this.customer_image = customer_image;
    }

    public String getMealName() {
        return meal_name;
    }

    public void setMealName(String meal_name) {
        this.meal_name = meal_name;
    }
}
