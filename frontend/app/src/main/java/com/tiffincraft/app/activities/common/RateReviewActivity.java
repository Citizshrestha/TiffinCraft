package com.tiffincraft.app.activities.common;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityRateReviewBinding;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.ReviewResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUrlHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RateReviewActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_COOK_ID = "cook_id";
    public static final String EXTRA_KITCHEN_NAME = "kitchen_name";
    public static final String EXTRA_REVIEW_ID = "review_id";
    public static final String EXTRA_EXISTING_RATING = "existing_rating";
    public static final String EXTRA_EXISTING_COMMENT = "existing_comment";

    private static final String[] RATING_LABELS = {
            "Tap a star to rate", "Poor", "Fair", "Good", "Great", "Excellent!"
    };

    private ActivityRateReviewBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int orderId = -1;
    private int cookId = -1;
    private int reviewId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRateReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        orderId = getIntent().getIntExtra(EXTRA_ORDER_ID, -1);
        cookId = getIntent().getIntExtra(EXTRA_COOK_ID, -1);
        reviewId = getIntent().getIntExtra(EXTRA_REVIEW_ID, -1);
        String kitchenName = getIntent().getStringExtra(EXTRA_KITCHEN_NAME);
        boolean isEditMode = reviewId > 0;

        if (cookId <= 0 || (!isEditMode && orderId <= 0)) {
            Toast.makeText(this, "Unable to open review — missing order info.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (kitchenName != null && !kitchenName.isEmpty()) {
            binding.tvKitchenName.setText(kitchenName);
        }

        loadCookAvatar();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.ratingBar.setOnRatingBarChangeListener(this::onRatingChanged);

        binding.etReview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvCharCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (isEditMode) {
            binding.tvScreenTitle.setText("Edit Your Review");
            binding.btnSubmitReview.setText("Update Review");
            int existingRating = getIntent().getIntExtra(EXTRA_EXISTING_RATING, 0);
            String existingComment = getIntent().getStringExtra(EXTRA_EXISTING_COMMENT);
            if (existingRating > 0) {
                binding.ratingBar.setRating(existingRating);
            }
            if (existingComment != null) {
                binding.etReview.setText(existingComment);
            }
        }

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        int stars = Math.round(rating);
        binding.tvRatingLabel.setText(RATING_LABELS[Math.max(0, Math.min(stars, 5))]);

        boolean canSubmit = stars >= 1;
        binding.btnSubmitReview.setEnabled(canSubmit);
        binding.btnSubmitReview.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, canSubmit ? R.color.green_primary : R.color.text_disabled)));
    }

    private void loadCookAvatar() {
        apiService.getCookById(cookId).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call, @NonNull Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCook() != null) {
                    String image = response.body().getCook().getProfileImage();
                    ImageUrlHelper.load(binding.imgCookAvatar, image, R.drawable.avatar_cook);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                // Avatar is cosmetic — keep the default placeholder on failure.
            }
        });
    }

    private void submitReview() {
        int rating = Math.round(binding.ratingBar.getRating());
        if (rating < 1) {
            Toast.makeText(this, "Please select a star rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = binding.etReview.getText().toString().trim();
        boolean isEditMode = reviewId > 0;

        JsonObject body = new JsonObject();
        body.addProperty("rating", rating);
        if (!comment.isEmpty()) {
            body.addProperty("comment", comment);
        }
        if (!isEditMode) {
            body.addProperty("order_id", orderId);
            body.addProperty("cook_id", cookId);
        }

        binding.btnSubmitReview.setEnabled(false);
        binding.btnSubmitReview.setText(isEditMode ? "Updating…" : "Submitting…");

        String token = "Bearer " + sessionManager.getToken();
        Callback<ReviewResponse> callback = new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(RateReviewActivity.this,
                            isEditMode ? "Review updated!" : "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String message = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Could not save review.";
                    Toast.makeText(RateReviewActivity.this, message, Toast.LENGTH_LONG).show();
                    resetSubmitButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                Toast.makeText(RateReviewActivity.this, "Network error — please try again.", Toast.LENGTH_SHORT).show();
                resetSubmitButton();
            }
        };

        if (isEditMode) {
            apiService.updateReview(token, reviewId, body).enqueue(callback);
        } else {
            apiService.submitReview(token, body).enqueue(callback);
        }
    }

    private void resetSubmitButton() {
        binding.btnSubmitReview.setEnabled(true);
        binding.btnSubmitReview.setText(reviewId > 0 ? "Update Review" : getString(R.string.submit_review));
    }
}
