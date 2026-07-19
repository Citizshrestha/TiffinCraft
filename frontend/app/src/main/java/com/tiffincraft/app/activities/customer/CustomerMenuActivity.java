package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.CartActivity;
import com.tiffincraft.app.activities.meal.CookDetailsActivity;
import com.tiffincraft.app.adapters.RecommendedMealAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerMenuActivity extends AppCompatActivity {

    private static final String TAG = "CustomerMenuActivity";

    /** Which chip to pre-select when arriving via a home-screen "View All" button. */
    public static final String EXTRA_FILTER = "extra_filter";
    public static final String FILTER_POPULAR = "popular";
    public static final String FILTER_RECOMMENDED = "recommended";

    private MaterialCardView searchBar;
    // FrameLayout in the layout — a MaterialCardView declaration here throws
    // ClassCastException on findViewById and crashes this screen on open.
    private View btnFilter;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvMeals;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;

    private RecommendedMealAdapter mealAdapter;
    private final List<Meal> allMeals = new ArrayList<>();   // master list from the API
    private final List<Meal> displayedMeals = new ArrayList<>(); // filtered list bound to the adapter
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
        preselectFilterFromIntent();
        loadMeals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_menu);
        }
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
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvMeals.setLayoutManager(gridLayoutManager);

        mealAdapter = new RecommendedMealAdapter(displayedMeals, new RecommendedMealAdapter.OnMealActionListener() {
            @Override
            public void onMealClick(Meal meal) {
                openCookForMeal(meal);
            }

            @Override
            public void onFavoriteClick(Meal meal, int position) {
                openCookForMeal(meal);
            }

            @Override
            public void onAddToCartClick(Meal meal) {
                addMealToCart(meal);
            }
        });

        rvMeals.setAdapter(mealAdapter);
    }

    private void openCookForMeal(Meal meal) {
        if (meal == null || meal.getCookId() <= 0) {
            Toast.makeText(this, "Cook not available for this meal", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, CookDetailsActivity.class)
                .putExtra(CookDetailsActivity.EXTRA_COOK_ID, meal.getCookId()));
    }

    private void addMealToCart(Meal meal) {
        if (meal == null) return;
        String token = "Bearer " + sessionManager.getToken();
        apiService.addToCart(token, new AddToCartRequest(meal.getId(), 1))
                .enqueue(new Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(CustomerMenuActivity.this,
                                    meal.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CustomerMenuActivity.this,
                                    "Failed to add to cart", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CartResponse> call, Throwable t) {
                        Toast.makeText(CustomerMenuActivity.this,
                                "Network error adding to cart", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupListeners() {
        searchBar.setOnClickListener(v -> {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
        });

        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Advanced filters coming soon", Toast.LENGTH_SHORT).show();
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedId = checkedIds.get(0);
                filterMeals(selectedId);
            }
        });
    }

    /** "View All" from the home screen arrives with a filter hint — pre-check the matching chip. */
    private void preselectFilterFromIntent() {
        String filter = getIntent().getStringExtra(EXTRA_FILTER);
        if (FILTER_POPULAR.equals(filter) || FILTER_RECOMMENDED.equals(filter)) {
            Chip chipPopular = findViewById(R.id.chipPopular);
            if (chipPopular != null) {
                chipPopular.setChecked(true);
            }
        }
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
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                return false;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, com.tiffincraft.app.activities.order.OrderHistoryActivity.class));
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

        apiService.getAllMeals().enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allMeals.clear();
                    allMeals.addAll(response.body().getMeals());
                    applyCurrentFilter();
                } else {
                    Log.e(TAG, "Failed to load meals: " + response.code());
                    showEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading meals", t);
                Toast.makeText(CustomerMenuActivity.this, "Error loading meals", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private void applyCurrentFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        filterMeals(checkedId);
    }

    private void filterMeals(int chipId) {
        displayedMeals.clear();

        if (chipId == R.id.chipVeg) {
            for (Meal meal : allMeals) {
                if (meal.isVegetarian()) displayedMeals.add(meal);
            }
        } else if (chipId == R.id.chipNonVeg) {
            for (Meal meal : allMeals) {
                if (!meal.isVegetarian() && !meal.isVegan()) displayedMeals.add(meal);
            }
        } else if (chipId == R.id.chipPopular) {
            displayedMeals.addAll(allMeals);
            Collections.sort(displayedMeals, (a, b) -> {
                double ratingA = a.getCookRating() != null ? a.getCookRating() : 0;
                double ratingB = b.getCookRating() != null ? b.getCookRating() : 0;
                return Double.compare(ratingB, ratingA);
            });
        } else if (chipId == R.id.chipNearby) {
            displayedMeals.addAll(allMeals);
            Toast.makeText(this, "Nearby filtering coming soon — showing all meals", Toast.LENGTH_SHORT).show();
        } else {
            displayedMeals.addAll(allMeals);
        }

        mealAdapter.notifyDataSetChanged();
        showEmptyState(displayedMeals.isEmpty());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMeals.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMeals.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
