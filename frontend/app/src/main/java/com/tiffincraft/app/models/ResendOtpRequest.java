package com.tiffincraft.app.models;

public class ResendOtpRequest {
    private String email;

    public ResendOtpRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
