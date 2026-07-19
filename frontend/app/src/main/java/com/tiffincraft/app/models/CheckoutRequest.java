package com.tiffincraft.app.models;

public class CheckoutRequest {
    private String delivery_address;
    private Double delivery_latitude;
    private Double delivery_longitude;
    private String special_instructions;
    private String payment_method;

    public CheckoutRequest(String deliveryAddress, Double lat, Double lng, String specialInstructions) {
        this(deliveryAddress, lat, lng, specialInstructions, "cod");
    }

    public CheckoutRequest(String deliveryAddress, Double lat, Double lng,
                           String specialInstructions, String paymentMethod) {
        this.delivery_address = deliveryAddress;
        this.delivery_latitude = lat;
        this.delivery_longitude = lng;
        this.special_instructions = specialInstructions;
        this.payment_method = paymentMethod != null ? paymentMethod : "cod";
    }
}
