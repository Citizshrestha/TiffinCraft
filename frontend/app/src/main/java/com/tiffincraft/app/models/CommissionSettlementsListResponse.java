package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Response for GET /commission/settlements/mine */
public class CommissionSettlementsListResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("settlements")
    private List<CommissionSettlement> settlements;

    public boolean isSuccess() { return success; }
    public List<CommissionSettlement> getSettlements() { return settlements; }
}
