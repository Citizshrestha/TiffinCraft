package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityReplyToReviewBinding;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.JsonObject;

public class ReplyToReviewActivity extends AppCompatActivity {

    private static final String TAG = "ReplyToReviewActivity";

    private ActivityReplyToReviewBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private int reviewId;
    private String existingReply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReplyToReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        readIntent();
        setupUI();
        setupClickListeners();
    }

    private void readIntent() {
        reviewId = getIntent().getIntExtra("review_id", -1);
        String customerName = getIntent().getStringExtra("customer_name");
        int rating = getIntent().getIntExtra("rating", 0);
        String comment = getIntent().getStringExtra("comment");
        existingReply = getIntent().getStringExtra("existing_reply");

        if (reviewId == -1) {
            Toast.makeText(this, "Invalid review data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Display review details
        binding.tvCustomerName.setText(customerName != null ? customerName : "Customer");
        binding.ratingBar.setRating(rating);
        binding.tvRating.setText(rating + ".0");
        binding.tvComment.setText(comment != null ? comment : "No comment");
    }

    private void setupUI() {
        if (existingReply != null && !existingReply.isEmpty()) {
            // Pre-fill existing reply
            binding.etReply.setText(existingReply);
            binding.tvTitle.setText("Edit Your Reply");
            binding.btnSubmit.setText("Update Reply");
            binding.btnDeleteReply.setVisibility(View.VISIBLE);
        } else {
            binding.tvTitle.setText("Reply to Review");
            binding.btnSubmit.setText("Post Reply");
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSubmit.setOnClickListener(v -> {
            String reply = binding.etReply.getText().toString().trim();

            if (reply.isEmpty()) {
                binding.etReply.setError("Reply cannot be empty");
                binding.etReply.requestFocus();
                return;
            }

            if (reply.length() < 10) {
                binding.etReply.setError("Reply should be at least 10 characters");
                binding.etReply.requestFocus();
                return;
            }

            submitReply(reply);
        });

        binding.btnDeleteReply.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete your reply?")
                .setMessage("The customer will no longer see this response.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteReply())
                .show());
    }

    private void deleteReply() {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteReviewReply(token, reviewId).enqueue(new Callback<RegisterResponse>() {
            @Override public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ReplyToReviewActivity.this, "Reply deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else Toast.makeText(ReplyToReviewActivity.this, "Could not delete reply", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ReplyToReviewActivity.this, "Could not delete reply", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReply(String replyText) {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();

        // Create JSON body with cook_reply field
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("cook_reply", replyText);

        apiService.replyToReview(token, reviewId, jsonBody).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call,
                                   @NonNull Response<RegisterResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ReplyToReviewActivity.this,
                            "Reply posted successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String errorMsg = "Failed to post reply";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(ReplyToReviewActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error posting reply", t);
                Toast.makeText(ReplyToReviewActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!show);
        binding.etReply.setEnabled(!show);
        binding.btnDeleteReply.setEnabled(!show);
    }
}
