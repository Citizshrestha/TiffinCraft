package com.tiffincraft.app.activities.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.CartActivity;
import com.tiffincraft.app.activities.meal.CookDetailsActivity;
import com.tiffincraft.app.adapters.RecommendedMealAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.FavoriteResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealDiscoveryResponse;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.MealCategoryCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerMenuActivity extends AppCompatActivity {

    private static final String TAG = "CustomerMenuActivity";

    /** Which chip to pre-select when arriving via a home-screen "View All" button. */
    public static final String EXTRA_FILTER = "extra_filter";
    public static final String EXTRA_CATEGORY_SLUGS = "extra_category_slugs";
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
    private final List<Meal> nearbyMeals = new ArrayList<>();
    private final List<Meal> displayedMeals = new ArrayList<>(); // filtered list bound to the adapter
    private final Set<String> categoryFilterSlugs = new HashSet<>();
    private final Set<Integer> favoriteUpdatesInFlight = new HashSet<>();
    private ApiService apiService;
    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean nearbyRequestInFlight = false;
    private boolean rankedPopularSource = false;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    if (chipGroupFilter.getCheckedChipId() == R.id.chipNearby) {
                        requestNearbyMeals();
                    } else {
                        loadMealsForCurrentLocation();
                    }
                } else {
                    if (chipGroupFilter.getCheckedChipId() == R.id.chipNearby) {
                        clearNearbyFilter("Location permission is needed to show nearby meals");
                    } else {
                        loadMeals();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_menu);

        init();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        readCategoryFiltersFromIntent();
        preselectFilterFromIntent();
        loadMealsForCurrentLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_menu);
        }
        if (!allMeals.isEmpty() || !nearbyMeals.isEmpty()) {
            loadFavoriteStates();
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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
                toggleFavoriteCook(meal);
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
        if (FILTER_POPULAR.equals(filter)) {
            Chip chipPopular = findViewById(R.id.chipPopular);
            if (chipPopular != null) {
                chipPopular.setChecked(true);
            }
        }
    }

    private void readCategoryFiltersFromIntent() {
        ArrayList<String> slugs = getIntent().getStringArrayListExtra(EXTRA_CATEGORY_SLUGS);
        if (slugs != null) categoryFilterSlugs.addAll(slugs);
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

        String requestedFilter = getIntent().getStringExtra(EXTRA_FILTER);
        if (FILTER_POPULAR.equals(requestedFilter) || FILTER_RECOMMENDED.equals(requestedFilter)) {
            loadRankedMeals(requestedFilter, null, null);
        } else {
            loadAllMeals();
        }
    }

    /** Loads normal browse results with device distances when location is available. */
    private void loadMealsForCurrentLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        loadMeals();
                        return;
                    }
                    String requestedFilter = getIntent().getStringExtra(EXTRA_FILTER);
                    if (FILTER_POPULAR.equals(requestedFilter) || FILTER_RECOMMENDED.equals(requestedFilter)) {
                        loadRankedMeals(requestedFilter, location.getLatitude(), location.getLongitude());
                    } else {
                        loadMealsWithDistance(location.getLatitude(), location.getLongitude());
                    }
                })
                .addOnFailureListener(error -> loadMeals());
    }

    private void loadMealsWithDistance(double latitude, double longitude) {
        showLoading(true);
        apiService.getAllMealsWithDistance(latitude, longitude).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allMeals.clear();
                    allMeals.addAll(response.body().getMeals());
                    applyCurrentFilter();
                    loadFavoriteStates();
                } else {
                    loadMeals();
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                loadMeals();
            }
        });
    }

    private void loadRankedMeals(String requestedFilter, Double latitude, Double longitude) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getMealDiscovery(token, latitude, longitude).enqueue(new Callback<MealDiscoveryResponse>() {
            @Override
            public void onResponse(Call<MealDiscoveryResponse> call,
                                   Response<MealDiscoveryResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Meal> rankedMeals = FILTER_POPULAR.equals(requestedFilter)
                            ? response.body().getPopular() : response.body().getRecommended();
                    rankedPopularSource = FILTER_POPULAR.equals(requestedFilter);
                    allMeals.clear();
                    if (rankedMeals != null) allMeals.addAll(rankedMeals);
                    applyCurrentFilter();
                } else {
                    Log.e(TAG, "Failed to load ranked meals: " + response.code());
                    loadAllMeals();
                }
            }

            @Override
            public void onFailure(Call<MealDiscoveryResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading ranked meals", t);
                loadAllMeals();
            }
        });
    }

    private void loadAllMeals() {
        rankedPopularSource = false;

        apiService.getAllMeals().enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allMeals.clear();
                    allMeals.addAll(response.body().getMeals());
                    applyCurrentFilter();
                    loadFavoriteStates();
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

        List<Meal> categoryMatches = new ArrayList<>();
        for (Meal meal : allMeals) {
            if (categoryFilterSlugs.isEmpty() || matchesAnyCategory(meal)) {
                categoryMatches.add(meal);
            }
        }

        if (chipId == R.id.chipVeg) {
            for (Meal meal : categoryMatches) {
                if (meal.isVegetarian()) displayedMeals.add(meal);
            }
        } else if (chipId == R.id.chipNonVeg) {
            for (Meal meal : categoryMatches) {
                if (!meal.isVegetarian() && !meal.isVegan()) displayedMeals.add(meal);
            }
        } else if (chipId == R.id.chipPopular) {
            if (rankedPopularSource) {
                displayedMeals.addAll(categoryMatches);
                mealAdapter.notifyDataSetChanged();
                showEmptyState(displayedMeals.isEmpty());
                return;
            }
            // Popular: Only show meals with cook rating >= 4.0 AND that have reviews
            for (Meal meal : categoryMatches) {
                double rating = meal.getCookRating() != null ? meal.getCookRating() : 0;
                // Assuming cook has reviews if rating > 0 (in real app, we'd need a review_count field)
                if (rating >= 4.0 && rating > 0) {
                    displayedMeals.add(meal);
                }
            }
            // Sort by rating descending
            Collections.sort(displayedMeals, (a, b) -> {
                double ratingA = a.getCookRating() != null ? a.getCookRating() : 0;
                double ratingB = b.getCookRating() != null ? b.getCookRating() : 0;
                return Double.compare(ratingB, ratingA);
            });
        } else if (chipId == R.id.chipNearby) {
            requestNearbyMeals();
            return;
        } else {
            displayedMeals.addAll(categoryMatches);
        }

        mealAdapter.notifyDataSetChanged();
        showEmptyState(displayedMeals.isEmpty());
    }

    /** Requests the user's location only after they explicitly choose Nearby. */
    private void requestNearbyMeals() {
        if (nearbyRequestInFlight) return;
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        nearbyRequestInFlight = true;
        showLoading(true);
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    nearbyRequestInFlight = false;
                    if (location == null) {
                        showLoading(false);
                        clearNearbyFilter("Couldn't get your location. Turn on location services and try again.");
                        return;
                    }
                    loadNearbyMeals(location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(error -> {
                    nearbyRequestInFlight = false;
                    showLoading(false);
                    clearNearbyFilter("Couldn't get your location. Please try again.");
                });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void loadNearbyMeals(double latitude, double longitude) {
        apiService.getNearbyMeals(latitude, longitude, 10).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    nearbyMeals.clear();
                    nearbyMeals.addAll(response.body().getMeals());
                    loadFavoriteStates();
                    if (chipGroupFilter.getCheckedChipId() == R.id.chipNearby) {
                        filterNearbyMeals();
                    } else {
                        applyCurrentFilter();
                    }
                } else {
                    clearNearbyFilter("Couldn't load nearby meals. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading nearby meals", t);
                clearNearbyFilter("Couldn't load nearby meals. Please try again.");
            }
        });
    }

    private void loadFavoriteStates() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getFavorites(token).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) return;
                Set<Integer> favoriteCookIds = new HashSet<>();
                if (response.body().getFavorites() != null) {
                    for (FavoriteResponse.FavoriteCook cook : response.body().getFavorites()) {
                        favoriteCookIds.add(cook.getCookId());
                    }
                }
                for (Meal meal : allMeals) {
                    meal.setFavoriteCook(favoriteCookIds.contains(meal.getCookId()));
                }
                for (Meal meal : nearbyMeals) {
                    meal.setFavoriteCook(favoriteCookIds.contains(meal.getCookId()));
                }
                mealAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                Log.e(TAG, "Unable to refresh favorite cooks", t);
            }
        });
    }

    private void filterNearbyMeals() {
        displayedMeals.clear();
        for (Meal meal : nearbyMeals) {
            if (categoryFilterSlugs.isEmpty() || matchesAnyCategory(meal)) {
                displayedMeals.add(meal);
            }
        }
        mealAdapter.notifyDataSetChanged();
        showEmptyState(displayedMeals.isEmpty());
    }

    private void clearNearbyFilter(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        chipGroupFilter.clearCheck();
        filterMeals(View.NO_ID);
    }

    private void toggleFavoriteCook(Meal meal) {
        if (meal == null || meal.getCookId() <= 0 || favoriteUpdatesInFlight.contains(meal.getCookId())) {
            return;
        }
        final int cookId = meal.getCookId();
        final boolean removing = meal.isFavoriteCook();
        favoriteUpdatesInFlight.add(cookId);
        String token = "Bearer " + sessionManager.getToken();
        Call<FavoriteResponse> request;
        if (removing) {
            request = apiService.removeFromFavorites(token, cookId);
        } else {
            JsonObject body = new JsonObject();
            body.addProperty("cook_id", cookId);
            request = apiService.addToFavorites(token, body);
        }

        request.enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                favoriteUpdatesInFlight.remove(cookId);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    updateFavoriteCookState(cookId, !removing);
                    Toast.makeText(CustomerMenuActivity.this,
                            removing ? "Removed cook from favorites" : "Cook added to favorites",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CustomerMenuActivity.this,
                            "Could not update favorites", Toast.LENGTH_SHORT).show();
                    loadFavoriteStates();
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                favoriteUpdatesInFlight.remove(cookId);
                Toast.makeText(CustomerMenuActivity.this,
                        "Could not update favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteCookState(int cookId, boolean favorite) {
        for (Meal candidate : allMeals) {
            if (candidate.getCookId() == cookId) candidate.setFavoriteCook(favorite);
        }
        for (Meal candidate : nearbyMeals) {
            if (candidate.getCookId() == cookId) candidate.setFavoriteCook(favorite);
        }
        mealAdapter.notifyDataSetChanged();
    }

    private boolean matchesAnyCategory(Meal meal) {
        Set<String> mealCategories = new HashSet<>(meal.getCategorySlugs());
        if (mealCategories.isEmpty()) {
            mealCategories.addAll(MealCategoryCatalog.fromLegacy(
                    meal.getCategory(), meal.getCuisineType()));
        }
        for (String slug : categoryFilterSlugs) {
            if (mealCategories.contains(slug)) return true;
        }
        return false;
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
