package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    private String email;
    private String otp;
    @SerializedName("newPassword")
    private String newPassword;

    public ResetPasswordRequest(String email, String otp, String newPassword) {
        this.email = email;
        this.otp = otp;
        this.newPassword = newPassword;
    }

    public String getEmail() { return email; }
    public String getOtp() { return otp; }
    public String getNewPassword() { return newPassword; }
}
