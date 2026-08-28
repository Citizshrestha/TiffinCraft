package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * One "give me this meal instead on this day" request.
 *
 * Shared by three payloads rather than duplicated into each, because all three
 * describe the same row:
 *   • SubscriptionCalendarResponse.Day.customMeal  — the customer's calendar
 *   • TodayDeliveriesResponse.Delivery.customMeal  — the cook's daily list
 *   • CustomMealsResponse.requests                 — the full per-subscription list
 *
 * Each of those endpoints sends a subset of these fields; Gson leaves the rest
 * null, so read only what the screen you're on actually supplies. `status` is
 * always present and is the field to branch on.
 */
public class CustomMeal {

    @SerializedName(value = "request_id", alternate = { "id" })
    private int requestId;

    /** Only sent by the full list endpoint; null inside a calendar day. */
    @SerializedName("delivery_date")
    private String deliveryDate;

    /** pending | accepted | declined | cancelled | expired */
    @SerializedName("status")
    private String status;

    /** Null when the customer described the swap in `note` instead of picking a meal. */
    @SerializedName("meal_id")
    private Integer mealId;

    @SerializedName("meal_name")
    private String mealName;

    @SerializedName("meal_image")
    private String mealImage;

    @SerializedName("note")
    private String note;

    /** The cook's reason, set when they accepted or declined. */
    @SerializedName("response_note")
    private String responseNote;

    @SerializedName("responded_at")
    private String respondedAt;

    @SerializedName("created_at")
    private String createdAt;

    /**
     * Cook's daily list only: the swap is agreed, so this is what to cook.
     * Server-computed rather than derived from `status` on the client so both
     * sides can never disagree about what "confirmed" means.
     */
    @SerializedName("is_confirmed")
    private boolean isConfirmed;

    public int getRequestId() { return requestId; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getStatus() { return status; }
    public Integer getMealId() { return mealId; }
    public String getMealName() { return mealName; }
    public String getMealImage() { return mealImage; }
    public String getNote() { return note; }
    public String getResponseNote() { return responseNote; }
    public String getRespondedAt() { return respondedAt; }
    public String getCreatedAt() { return createdAt; }
    public boolean isConfirmed() { return isConfirmed; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isAccepted() { return "accepted".equals(status); }
    public boolean isDeclined() { return "declined".equals(status); }
    public boolean isCancelled() { return "cancelled".equals(status); }
    public boolean isExpired() { return "expired".equals(status); }

    /** Only a pending request can still be withdrawn — see DELETE /custom-meals/{id}. */
    public boolean canCancel() { return isPending(); }

    /** What to show on a one-line card: the meal if there is one, else the note. */
    public String describe() {
        if (mealName != null && !mealName.trim().isEmpty()) return mealName;
        if (note != null && !note.trim().isEmpty()) return note;
        return "Custom request";
    }
}
