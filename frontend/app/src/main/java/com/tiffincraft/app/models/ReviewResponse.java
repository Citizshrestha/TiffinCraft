package com.tiffincraft.app.models;

import java.util.List;

public class ReviewResponse {
    private boolean success;
    private String message;
    private ReviewSummary summary;
    private List<Review> reviews;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReviewSummary getSummary() {
        return summary;
    }

    public void setSummary(ReviewSummary summary) {
        this.summary = summary;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
}
