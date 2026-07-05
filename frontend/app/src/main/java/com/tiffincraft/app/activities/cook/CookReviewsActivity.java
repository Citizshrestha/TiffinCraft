package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.ReviewAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookReviewsBinding;
import com.tiffincraft.app.models.Review;
import com.tiffincraft.app.models.ReviewResponse;
import com.tiffincraft.app.models.ReviewSummary;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookReviewsActivity extends AppCompatActivity implements ReviewAdapter.OnReviewActionListener {

    private static final String TAG = "CookReviewsActivity";

    private ActivityCookReviewsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private ReviewAdapter adapter;
    private List<Review> allReviews = new ArrayList<>();
    private String currentFilter = null; // null = all, "5", "4", "3", "2", "1"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        setupRecyclerView();
        setupClickListeners();
        loadReviews();
    }

    private void setupRecyclerView() {
        adapter = new ReviewAdapter(this, new ArrayList<>(), this);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.swipeRefresh.setOnRefreshListener(this::loadReviews);

        // Filter buttons
        binding.btnFilterAll.setOnClickListener(v -> {
            currentFilter = null;
            updateFilterButtons();
            applyFilter();
        });

        binding.btnFilter5Star.setOnClickListener(v -> {
            currentFilter = "5";
            updateFilterButtons();
            applyFilter();
        });

        binding.btnFilter4Star.setOnClickListener(v -> {
            currentFilter = "4";
            updateFilterButtons();
            applyFilter();
        });

        binding.btnFilter3Star.setOnClickListener(v -> {
            currentFilter = "3";
            updateFilterButtons();
            applyFilter();
        });

        binding.btnFilter2Star.setOnClickListener(v -> {
            currentFilter = "2";
            updateFilterButtons();
            applyFilter();
        });

        binding.btnFilter1Star.setOnClickListener(v -> {
            currentFilter = "1";
            updateFilterButtons();
            applyFilter();
        });
    }

    private void updateFilterButtons() {
        // Reset all to default style
        binding.btnFilterAll.setBackgroundResource(R.drawable.chip_unselected);
        binding.btnFilter5Star.setBackgroundResource(R.drawable.chip_unselected);
        binding.btnFilter4Star.setBackgroundResource(R.drawable.chip_unselected);
        binding.btnFilter3Star.setBackgroundResource(R.drawable.chip_unselected);
        binding.btnFilter2Star.setBackgroundResource(R.drawable.chip_unselected);
        binding.btnFilter1Star.setBackgroundResource(R.drawable.chip_unselected);

        // Highlight selected
        if (currentFilter == null) {
            binding.btnFilterAll.setBackgroundResource(R.drawable.chip_selected_green);
        } else if (currentFilter.equals("5")) {
            binding.btnFilter5Star.setBackgroundResource(R.drawable.chip_selected_green);
        } else if (currentFilter.equals("4")) {
            binding.btnFilter4Star.setBackgroundResource(R.drawable.chip_selected_green);
        } else if (currentFilter.equals("3")) {
            binding.btnFilter3Star.setBackgroundResource(R.drawable.chip_selected_green);
        } else if (currentFilter.equals("2")) {
            binding.btnFilter2Star.setBackgroundResource(R.drawable.chip_selected_green);
        } else if (currentFilter.equals("1")) {
            binding.btnFilter1Star.setBackgroundResource(R.drawable.chip_selected_green);
        }
    }

    private void loadReviews() {
        String token = "Bearer " + sessionManager.getToken();
        showLoading(true);

        apiService.getMyCookReviews(token).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call,
                                   @NonNull Response<ReviewResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allReviews = response.body().getReviews();
                    if (allReviews == null) allReviews = new ArrayList<>();

                    ReviewSummary summary = response.body().getSummary();
                    updateSummaryUI(summary);
                    applyFilter();
                } else {
                    String errorMsg = "Failed to load reviews";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(CookReviewsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    showEmpty(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading reviews", t);
                Toast.makeText(CookReviewsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty(true);
            }
        });
    }

    private void updateSummaryUI(ReviewSummary summary) {
        if (summary == null) return;

        binding.tvAverageRating.setText(String.format("%.1f", summary.getAverageRating()));
        binding.tvTotalReviews.setText(summary.getTotalReviews() + " reviews");
        binding.ratingBar.setRating((float) summary.getAverageRating());

        // Update star breakdown
        int total = summary.getTotalReviews();
        if (total > 0) {
            binding.progressBar5Star.setProgress((summary.getFiveStar() * 100) / total);
            binding.progressBar4Star.setProgress((summary.getFourStar() * 100) / total);
            binding.progressBar3Star.setProgress((summary.getThreeStar() * 100) / total);
            binding.progressBar2Star.setProgress((summary.getTwoStar() * 100) / total);
            binding.progressBar1Star.setProgress((summary.getOneStar() * 100) / total);

            binding.tvCount5Star.setText(String.valueOf(summary.getFiveStar()));
            binding.tvCount4Star.setText(String.valueOf(summary.getFourStar()));
            binding.tvCount3Star.setText(String.valueOf(summary.getThreeStar()));
            binding.tvCount2Star.setText(String.valueOf(summary.getTwoStar()));
            binding.tvCount1Star.setText(String.valueOf(summary.getOneStar()));
        }
    }

    private void applyFilter() {
        List<Review> filtered = new ArrayList<>();
        for (Review review : allReviews) {
            if (currentFilter == null || String.valueOf(review.getRating()).equals(currentFilter)) {
                filtered.add(review);
            }
        }
        adapter.updateReviews(filtered);
        showEmpty(filtered.isEmpty());
    }

    private void showLoading(boolean show) {
        binding.swipeRefresh.setRefreshing(show);
        if (!show) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        binding.layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvReviews.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onReplyClick(Review review) {
        // Navigate to reply activity
        Intent intent = new Intent(this, ReplyToReviewActivity.class);
        intent.putExtra("review_id", review.getId());
        intent.putExtra("customer_name", review.getCustomerName());
        intent.putExtra("rating", review.getRating());
        intent.putExtra("comment", review.getComment());
        intent.putExtra("existing_reply", review.getCookReply());
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh reviews after reply
            loadReviews();
        }
    }
}
