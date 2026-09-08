package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MealDiscoveryResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("personalized")
    private boolean personalized;

    @SerializedName("location_applied")
    private boolean locationApplied;

    @SerializedName("popular")
    private List<Meal> popular;

    @SerializedName("recommended")
    private List<Meal> recommended;

    public boolean isSuccess() {
        return success;
    }

    public boolean isPersonalized() {
        return personalized;
    }

    public boolean isLocationApplied() {
        return locationApplied;
    }

    public List<Meal> getPopular() {
        return popular;
    }

    public List<Meal> getRecommended() {
        return recommended;
    }
}
