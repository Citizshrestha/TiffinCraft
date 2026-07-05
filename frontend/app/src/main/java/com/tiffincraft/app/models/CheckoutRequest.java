package com.tiffincraft.app.models;

public class CheckoutRequest {
    private String delivery_address;
    private Double delivery_latitude;
    private Double delivery_longitude;
    private String special_instructions;

    public CheckoutRequest(String deliveryAddress, Double lat, Double lng, String specialInstructions) {
        this.delivery_address = deliveryAddress;
        this.delivery_latitude = lat;
        this.delivery_longitude = lng;
        this.special_instructions = specialInstructions;
    }
}