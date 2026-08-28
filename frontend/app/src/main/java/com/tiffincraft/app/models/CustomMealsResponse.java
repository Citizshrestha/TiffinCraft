package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GET /api/subscriptions/{id}/custom-meals — every swap ever asked for on one
 * subscription, newest delivery day first.
 *
 * Served to the owning customer AND the owning cook; `viewer` says which one you
 * are, so a single screen can render the cook's Accept/Decline affordances or the
 * customer's Withdraw one without a second endpoint.
 */
public class CustomMealsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    /** "customer" or "cook". */
    @SerializedName("viewer")
    private String viewer;

    /** Today in Nepal Time — not the device clock. */
    @SerializedName("today")
    private String today;

    @SerializedName("requests")
    private List<CustomMeal> requests;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getViewer() { return viewer; }
    public boolean isCookView() { return "cook".equals(viewer); }
    public String getToday() { return today; }
    public List<CustomMeal> getRequests() { return requests; }
}
