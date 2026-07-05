package com.tiffincraft.app.models;

public class ReviewSummary {
    private double average_rating;
    private int total_reviews;
    private int five_star;
    private int four_star;
    private int three_star;
    private int two_star;
    private int one_star;

    public double getAverageRating() {
        return average_rating;
    }

    public void setAverageRating(double average_rating) {
        this.average_rating = average_rating;
    }

    public int getTotalReviews() {
        return total_reviews;
    }

    public void setTotalReviews(int total_reviews) {
        this.total_reviews = total_reviews;
    }

    public int getFiveStar() {
        return five_star;
    }

    public void setFiveStar(int five_star) {
        this.five_star = five_star;
    }

    public int getFourStar() {
        return four_star;
    }

    public void setFourStar(int four_star) {
        this.four_star = four_star;
    }

    public int getThreeStar() {
        return three_star;
    }

    public void setThreeStar(int three_star) {
        this.three_star = three_star;
    }

    public int getTwoStar() {
        return two_star;
    }

    public void setTwoStar(int two_star) {
        this.two_star = two_star;
    }

    public int getOneStar() {
        return one_star;
    }

    public void setOneStar(int one_star) {
        this.one_star = one_star;
    }
}
