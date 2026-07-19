package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("orders")
    private List<Order> orders;

    // GET /orders/{orderId} (getOrderById) returns a single "order" object,
    // not an "orders" array — this field backs that shape specifically.
    @SerializedName("order")
    private Order order;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Order> getOrders() { return orders; }
    public Order getOrder() { return order; }
}
