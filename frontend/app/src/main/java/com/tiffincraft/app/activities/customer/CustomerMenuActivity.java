package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android:widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.RecommendedMealAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CustomerMenuActivity extends AppCompatActivity {

    private MaterialCardView searchBar;
    private MaterialCardView btnFilter;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvMeals;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;

    private RecommendedMealAdapter mealAdapter;
    private List<Meal> allMeals;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_menu);

        init();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        loadMeals();
    }

    private void init() {
        searchBar = findViewById(R.id.searchBar);
        btnFilter = findViewById(R.id.btnFilter);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        rvMeals = findViewById(R.id.rvMeals);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);
        allMeals = new ArrayList<>();
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvMeals.setLayoutManager(gridLayoutManager);

        mealAdapter = new RecommendedMealAdapter(allMeals, new RecommendedMealAdapter.OnMealActionListener() {
            @Override
            public void onMealClick(Meal meal) {
                Toast.makeText(CustomerMenuActivity.this, "Meal: " + meal.getName(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to meal details
            }

            @Override
            public void onFavoriteClick(Meal meal, int position) {
                Toast.makeText(CustomerMenuActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                // TODO: Add to favorites API call
            }

            @Override
            public void onAddToCartClick(Meal meal) {
                Toast.makeText(CustomerMenuActivity.this, "Added to cart", Toast.LENGTH_SHORT).show();
                // TODO: Add to cart API call
            }
        });

        rvMeals.setAdapter(mealAdapter);
    }

    private void setupListeners() {
        searchBar.setOnClickListener(v -> {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Open search activity
        });

        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Advanced filters coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Show filter bottom sheet
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedId = checkedIds.get(0);
                filterMeals(selectedId);
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_menu);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, CustomerHomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                return true;
            } else if (itemId == R.id.nav_orders) {
                Toast.makeText(this, "Orders coming soon", Toast.LENGTH_SHORT).show();
                return false;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, CustomerProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void loadMeals() {
        showLoading(true);

        // TODO: Replace with actual API call
        // Mock data for now
        allMeals.clear();
        
        // Simulate empty state for testing
        showLoading(false);
        showEmptyState(true);
        
        /* 
        // Example API call structure:
        String token = "Bearer " + sessionManager.getToken();
        apiService.getAllMeals(token).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    allMeals.addAll(response.body().getMeals());
                    mealAdapter.notifyDataSetChanged();
                    showEmptyState(allMeals.isEmpty());
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CustomerMenuActivity.this, "Error loading meals", Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void filterMeals(int chipId) {
        if (chipId == R.id.chipAll) {
            mealAdapter.notifyDataSetChanged();
        } else if (chipId == R.id.chipVeg) {
            // Filter vegetarian meals
            Toast.makeText(this, "Showing vegetarian meals", Toast.LENGTH_SHORT).show();
        } else if (chipId == R.id.chipNonVeg) {
            // Filter non-vegetarian meals
            Toast.makeText(this, "Showing non-vegetarian meals", Toast.LENGTH_SHORT).show();
        } else if (chipId == R.id.chipPopular) {
            // Filter popular meals
            Toast.makeText(this, "Showing popular meals", Toast.LENGTH_SHORT).show();
        } else if (chipId == R.id.chipNearby) {
            // Filter nearby meals
            Toast.makeText(this, "Showing nearby meals", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMeals.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMeals.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_menu);
        }
    }
}
