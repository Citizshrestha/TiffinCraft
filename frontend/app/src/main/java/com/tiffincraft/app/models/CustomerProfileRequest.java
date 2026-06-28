package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CustomerProfileRequest {
    @SerializedName("full_name")
    private String fullName;
    
    private String phone;
    private String address;

    public CustomerProfileRequest(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
