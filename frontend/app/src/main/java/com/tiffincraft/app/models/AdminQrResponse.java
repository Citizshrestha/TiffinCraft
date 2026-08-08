package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/** Response for GET /commission/admin-qr — the platform's own payment QR, shown to cooks. */
public class AdminQrResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("bank_details")
    private BankDetails bankDetails;

    public boolean isSuccess() { return success; }
    public BankDetails getBankDetails() { return bankDetails; }
}
