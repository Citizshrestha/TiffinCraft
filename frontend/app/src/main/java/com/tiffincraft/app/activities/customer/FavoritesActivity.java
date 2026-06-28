package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.FavoritesAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.FavoriteResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity implements FavoritesAdapter.OnFavoriteActionListener {

    private RecyclerView rvFavorites;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private FavoritesAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadFavorites();
    }

    private void initViews() {
        rvFavorites = findViewById(R.id.rvFavorites);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
    }

    private void setupRecyclerView() {
        adapter = new FavoritesAdapter(this, this);
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadFavorites() {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getFavorites(token).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    FavoriteResponse favoriteResponse = response.body();

                    if (favoriteResponse.isSuccess()) {
                        if (favoriteResponse.getFavorites() != null && !favoriteResponse.getFavorites().isEmpty()) {
                            adapter.setFavorites(favoriteResponse.getFavorites());
                            showEmptyState(false);
                        } else {
                            showEmptyState(true);
                        }
                    } else {
                        Toast.makeText(FavoritesActivity.this,
                                favoriteResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    }
                } else {
                    handleError(response.code());
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(FavoritesActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    @Override
    public void onRemoveFavorite(int cookId, int position) {
        String token = "Bearer " + sessionManager.getToken();

        apiService.removeFromFavorites(token, cookId).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FavoriteResponse favoriteResponse = response.body();

                    if (favoriteResponse.isSuccess()) {
                        adapter.removeFavorite(position);
                        Toast.makeText(FavoritesActivity.this,
                                "Removed from favorites", Toast.LENGTH_SHORT).show();

                        // Show empty state if no more favorites
                        if (adapter.getItemCount() == 0) {
                            showEmptyState(true);
                        }
                    } else {
                        Toast.makeText(FavoritesActivity.this,
                                favoriteResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    handleError(response.code());
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                Toast.makeText(FavoritesActivity.this,
                        "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewMenu(int cookId) {
        // Navigate to Cook Details Activity
        Intent intent = new Intent(this, com.tiffincraft.app.activities.meal.CookDetailsActivity.class);
        intent.putExtra("COOK_ID", cookId);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFavorites.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFavorites.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void handleError(int code) {
        String message;
        switch (code) {
            case 401:
                message = "Session expired. Please login again.";
                sessionManager.logout();
                // Navigate to login
                break;
            case 404:
                message = "No favorites found";
                showEmptyState(true);
                return;
            case 500:
                message = "Server error. Please try again later.";
                break;
            default:
                message = "Error loading favorites";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload favorites when returning to this activity
        loadFavorites();
    }
}
