package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FavoriteResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("favorites")
    private List<FavoriteCook> favorites;

    @SerializedName("isFavorite")
    private Boolean isFavorite;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<FavoriteCook> getFavorites() {
        return favorites;
    }

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public static class FavoriteCook {
        @SerializedName("favoriteId")
        private int favoriteId;

        @SerializedName("cookId")
        private int cookId;

        @SerializedName("cookName")
        private String cookName;

        @SerializedName("profileImage")
        private String profileImage;

        @SerializedName("address")
        private String address;

        @SerializedName("description")
        private String description;

        @SerializedName("specialties")
        private List<String> specialties;

        @SerializedName("experienceYears")
        private Integer experienceYears;

        @SerializedName("certifications")
        private List<String> certifications;

        @SerializedName("averageRating")
        private double averageRating;

        @SerializedName("totalReviews")
        private int totalReviews;

        @SerializedName("totalMeals")
        private int totalMeals;

        @SerializedName("addedAt")
        private String addedAt;

        // Getters
        public int getFavoriteId() {
            return favoriteId;
        }

        public int getCookId() {
            return cookId;
        }

        public String getCookName() {
            return cookName;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public String getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getSpecialties() {
            return specialties;
        }

        public Integer getExperienceYears() {
            return experienceYears;
        }

        public List<String> getCertifications() {
            return certifications;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }

        public int getTotalMeals() {
            return totalMeals;
        }

        public String getAddedAt() {
            return addedAt;
        }
    }
}
