package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Holds payment QR code URLs from the cook's bank_details field.
 * The backend stores this as a JSON string in cook_profiles.bank_details
 * with keys: esewa_qr_url, khalti_qr_url, bank_qr_url.
 */
public class BankDetails {

    @SerializedName("esewa_qr_url")
    private String esewaQrUrl;

    @SerializedName("khalti_qr_url")
    private String khaltiQrUrl;

    @SerializedName("bank_qr_url")
    private String bankQrUrl;

    // Getters
    public String getEsewaQrUrl() { return esewaQrUrl; }
    public String getKhaltiQrUrl() { return khaltiQrUrl; }
    public String getBankQrUrl() { return bankQrUrl; }

    // Setters
    public void setEsewaQrUrl(String esewaQrUrl) { this.esewaQrUrl = esewaQrUrl; }
    public void setKhaltiQrUrl(String khaltiQrUrl) { this.khaltiQrUrl = khaltiQrUrl; }
    public void setBankQrUrl(String bankQrUrl) { this.bankQrUrl = bankQrUrl; }
}
