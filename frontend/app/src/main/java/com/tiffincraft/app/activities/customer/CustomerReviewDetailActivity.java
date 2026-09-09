package com.tiffincraft.app.activities.customer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Review;
import com.tiffincraft.app.models.ReviewResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Displays one review owned by the signed-in customer. Used by review alerts. */
public class CustomerReviewDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REVIEW_ID = "review_id";

    private int reviewId;
    private ProgressBar progressBar;
    private View content;
    private View unavailable;
    private View replySection;
    private TextView kitchenName;
    private TextView rating;
    private TextView comment;
    private TextView reply;

    public static Intent intentFor(Context context, int reviewId) {
        Intent intent = new Intent(context, CustomerReviewDetailActivity.class);
        intent.putExtra(EXTRA_REVIEW_ID, reviewId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_review_detail);

        reviewId = getIntent().getIntExtra(EXTRA_REVIEW_ID, -1);
        if (reviewId <= 0) {
            Toast.makeText(this, "Review not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        progressBar = findViewById(R.id.progressBar);
        content = findViewById(R.id.reviewContent);
        unavailable = findViewById(R.id.reviewUnavailable);
        replySection = findViewById(R.id.replySection);
        kitchenName = findViewById(R.id.tvKitchenName);
        rating = findViewById(R.id.tvReviewRating);
        comment = findViewById(R.id.tvReviewComment);
        reply = findViewById(R.id.tvCookReply);

        loadReview();
    }

    private void loadReview() {
        progressBar.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
        unavailable.setVisibility(View.GONE);

        String token = "Bearer " + new SessionManager(this).getToken();
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getMyReviews(token).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call,
                                   @NonNull Response<ReviewResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    showUnavailable();
                    return;
                }

                Review matched = findReview(response.body().getReviews());
                if (matched == null) {
                    showUnavailable();
                    return;
                }
                showReview(matched);
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                showUnavailable();
            }
        });
    }

    private Review findReview(List<Review> reviews) {
        if (reviews == null) return null;
        for (Review review : reviews) {
            if (review.getId() == reviewId) return review;
        }
        return null;
    }

    private void showReview(Review review) {
        kitchenName.setText(nonEmpty(review.getKitchenName(), "Kitchen"));
        rating.setText(review.getRating() + ".0  " + stars(review.getRating()));
        comment.setText(nonEmpty(review.getComment(), "You did not leave a written comment."));

        String cookReply = review.getCookReply();
        if (cookReply == null || cookReply.trim().isEmpty()) {
            replySection.setVisibility(View.GONE);
        } else {
            reply.setText(cookReply.trim());
            replySection.setVisibility(View.VISIBLE);
        }

        content.setVisibility(View.VISIBLE);
    }

    private void showUnavailable() {
        unavailable.setVisibility(View.VISIBLE);
    }

    private String stars(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.max(0, Math.min(count, 5)); i++) result.append('★');
        return result.toString();
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
