package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

public class CookProfile {
    private int id;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("kitchen_name")
    private String kitchenName;
    @SerializedName("food_type")
    private String foodType;
    private String description;
    @SerializedName("capacity_per_day")
    private int capacityPerDay;
    private String bio;
    private String specialties;
    private double rating;
    @SerializedName("total_orders")
    private int totalOrders;
    // Backend sends these as MySQL tinyint (0/1 NUMBER), not JSON booleans.
    // Kept as Object so Gson never throws "Expected a boolean but was NUMBER";
    // isVerified()/isApproved() coerce them below.
    @SerializedName("is_verified")
    private Object isVerifiedRaw;
    @SerializedName("is_approved")
    private Object isApprovedRaw;
    @SerializedName("bank_details")
    private String bankDetails;

    @SerializedName("total_reviews")
    private int totalReviews;

    @SerializedName("review_count")
    private int reviewCount;

    @SerializedName("dish_count")
    private int dishCount;

    @SerializedName("user_created_at")
    private String userCreatedAt;

    /** Accepts MySQL 0/1 or JSON true/false. */
    @SerializedName("is_holiday_mode")
    private Object isHolidayModeRaw;

    /** {"monday":{"open":"09:00","close":"21:00","isOpen":true}, ...} or null */
    @SerializedName("operating_hours")
    private java.util.Map<String, DayHours> operatingHours;

    // User info (from JOIN)
    @SerializedName("full_name")
    private String fullName;
    @SerializedName("profile_image")
    private String profileImage;
    private String address;
    private String email;
    private String phone;

    // Nearby-cook discovery (customer map). Boxed so a missing field
    // deserializes to null rather than 0.0 — (0,0) is a real coordinate
    // (Gulf of Guinea), not a safe "no location" sentinel.
    private Double latitude;
    private Double longitude;
    @SerializedName("distance_km")
    private Double distanceKm;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getKitchenName() {
        return kitchenName;
    }

    public void setKitchenName(String kitchenName) {
        this.kitchenName = kitchenName;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacityPerDay() {
        return capacityPerDay;
    }

    public void setCapacityPerDay(int capacityPerDay) {
        this.capacityPerDay = capacityPerDay;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSpecialties() {
        return specialties;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public boolean isVerified() {
        return coerceBoolean(isVerifiedRaw);
    }

    public void setVerified(boolean verified) {
        isVerifiedRaw = verified;
    }

    public boolean isApproved() {
        return coerceBoolean(isApprovedRaw);
    }

    public void setApproved(boolean approved) {
        isApprovedRaw = approved;
    }

    /** Accepts MySQL 0/1 (Number), JSON true/false (Boolean), or "0"/"1"/"true" (String). */
    private static boolean coerceBoolean(Object raw) {
        if (raw == null) return false;
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(String bankDetails) {
        this.bankDetails = bankDetails;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getDishCount() {
        return dishCount;
    }

    public String getUserCreatedAt() {
        return userCreatedAt;
    }

    public boolean isHolidayMode() {
        return coerceBoolean(isHolidayModeRaw);
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public java.util.Map<String, DayHours> getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(java.util.Map<String, DayHours> operatingHours) {
        this.operatingHours = operatingHours;
    }

    public static class DayHours {
        private String open;
        private String close;
        @SerializedName("isOpen")
        private boolean isOpen;

        public DayHours() {
        }

        public DayHours(String open, String close, boolean isOpen) {
            this.open = open;
            this.close = close;
            this.isOpen = isOpen;
        }

        public String getOpen() {
            return open;
        }

        public void setOpen(String open) {
            this.open = open;
        }

        public String getClose() {
            return close;
        }

        public void setClose(String close) {
            this.close = close;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public void setOpen(boolean open) {
            isOpen = open;
        }
    }
}
