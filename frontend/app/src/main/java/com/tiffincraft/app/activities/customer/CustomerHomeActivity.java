package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.CartActivity;
import com.tiffincraft.app.activities.common.NotificationActivity;
import com.tiffincraft.app.activities.meal.CookDetailsActivity;
import com.tiffincraft.app.adapters.CategoryAdapter;
import com.tiffincraft.app.adapters.CookSearchResultAdapter;
import com.tiffincraft.app.adapters.PopularCookAdapter;
import com.tiffincraft.app.adapters.PopularMealAdapter;
import com.tiffincraft.app.adapters.RecommendedMealAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.Category;
import com.tiffincraft.app.models.CookProfile;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.CustomerProfile;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ChatPanelManager;
import com.tiffincraft.app.utils.ImageUrlHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerHomeActivity extends AppCompatActivity {

    private static final String TAG = "CustomerHomeActivity";

    // UI Components
    private ImageView imgCustomerProfile;
    private TextView tvCustomerName;
    private TextView tvDeliveryAddress;
    private ImageView btnNotifications;
    private TextView tvNotificationBadge;
    private View btnCart;
    private TextView tvCartBadge;
    private View cardViewCartBar;
    private TextView tvCartBarCount;
    private TextView tvWelcome;
    private MaterialCardView searchBar;
    // btnFilter is a FrameLayout in the layout — declaring it as MaterialCardView
    // makes findViewById throw ClassCastException, which silently aborts view
    // init and leaves the bottom navigation without a listener attached.
    private View btnFilter;
    private MaterialCardView promoBanner;
    private EditText etSearchHome;
    private MaterialCardView cardSearchResults;
    private RecyclerView rvSearchResults;
    private TextView tvSearchNoResults;

    // RecyclerViews
    private RecyclerView rvCategories;
    private RecyclerView rvPopularMeals;
    private RecyclerView rvRecommendedMeals;
    private RecyclerView rvNearbyCooks;

    // View All Buttons
    private TextView tvViewAllPopular;
    private TextView tvViewAllRecommended;
    private TextView tvViewAllCooks;

    // Bottom Navigation
    private BottomNavigationView bottomNavigation;

    // Adapters
    private CategoryAdapter categoryAdapter;
    private PopularMealAdapter popularMealAdapter;
    private RecommendedMealAdapter recommendedMealAdapter;
    private PopularCookAdapter popularCookAdapter;
    private CookSearchResultAdapter cookSearchResultAdapter;

    private static final int SEARCH_DEBOUNCE_MS = 350;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    // Data
    private List<Category> categories;
    private List<Meal> popularMeals;
    private List<Meal> recommendedMeals;
    private List<CookProfile> popularCooks;

    // Session and API
    private SessionManager sessionManager;
    private ApiService apiService;
    private ChatPanelManager chatPanelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_customer_home);

            // Deep-green status bar to blend with the immersive header
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.forest_deep));
            getWindow().getDecorView().setSystemUiVisibility(0);

            sessionManager = new SessionManager(this);
            apiService = RetrofitClient.getInstance(this).getApiService();

            initViews();
            setupRecyclerViews();
            setupListeners();
            setupBottomNavigation();
            loadUserData();
            loadCategories();
            loadMeals();
            loadPopularCooks();
            setupBackPressHandler();

            // Chat: this screen has no dedicated chat button in its layout,
            // so let the panel manager inflate its own floating chat button.
            chatPanelManager = ChatPanelManager.attach(this);

            requestNotificationPermissionIfNeeded();

            Log.d(TAG, "CustomerHomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
        if (chatPanelManager != null) {
            chatPanelManager.refreshUnreadBadge();
        }
        fetchUnreadNotifications();
        refreshCartBadge();
    }

    /** Bell badge: count of unread rows in the notifications table (incl. chat messages). */
    private void fetchUnreadNotifications() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getUnreadNotificationCount(token).enqueue(new Callback<com.tiffincraft.app.models.NotificationResponse>() {
            @Override
            public void onResponse(Call<com.tiffincraft.app.models.NotificationResponse> call,
                                   Response<com.tiffincraft.app.models.NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && tvNotificationBadge != null) {
                    int count = response.body().getUnreadCount();
                    if (count > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<com.tiffincraft.app.models.NotificationResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching unread notification count", t);
            }
        });
    }

    /** Android 13+ requires runtime consent before chat notifications can be shown. */
    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                   != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 9001);
        }
    }

    private void initViews() {
        try {
            // Top bar
            imgCustomerProfile = findViewById(R.id.imgCustomerProfile);
            tvCustomerName = findViewById(R.id.tvCustomerName);
            tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
            btnNotifications = findViewById(R.id.btnNotifications);
            tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
            btnCart = findViewById(R.id.btnCart);
            tvCartBadge = findViewById(R.id.tvCartBadge);
            cardViewCartBar = findViewById(R.id.cardViewCartBar);
            tvCartBarCount = findViewById(R.id.tvCartBarCount);

            // Greeting
            tvWelcome = findViewById(R.id.tvWelcome);

            // Search and Filter
            searchBar = findViewById(R.id.searchBar);
            btnFilter = findViewById(R.id.btnFilter);
            etSearchHome = findViewById(R.id.etSearchHome);
            cardSearchResults = findViewById(R.id.cardSearchResults);
            rvSearchResults = findViewById(R.id.rvSearchResults);
            tvSearchNoResults = findViewById(R.id.tvSearchNoResults);

            // Promo Banner
            promoBanner = findViewById(R.id.promoBanner);

            // RecyclerViews
            rvCategories = findViewById(R.id.rvCategories);
            rvPopularMeals = findViewById(R.id.rvPopularMeals);
            rvRecommendedMeals = findViewById(R.id.rvRecommendedMeals);
            rvNearbyCooks = findViewById(R.id.rvNearbyCooks);

            // View All buttons
            tvViewAllPopular = findViewById(R.id.tvViewAllPopular);
            tvViewAllRecommended = findViewById(R.id.tvViewAllRecommended);
            tvViewAllCooks = findViewById(R.id.tvViewAllCooks);

            // Bottom Navigation
            bottomNavigation = findViewById(R.id.bottomNavigation);

            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void loadUserData() {
        try {
            String fullName = sessionManager.getFullName();

            // Set customer name (from cached session first, refreshed from API below)
            if (fullName != null && !fullName.isEmpty()) {
                tvCustomerName.setText(fullName);
                String firstName = fullName.split(" ")[0];
                updateGreeting(firstName);
            } else {
                tvCustomerName.setText("Guest");
                updateGreeting("Guest");
            }

            tvDeliveryAddress.setText("Kathmandu, Nepal");
            loadProfileImage(sessionManager.getProfileImage());
            fetchCustomerProfile();

        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
        }
    }

    private void fetchCustomerProfile() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(Call<CustomerProfileResponse> call, Response<CustomerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getProfile() != null) {
                    CustomerProfile profile = response.body().getProfile();

                    if (profile.getFullName() != null && !profile.getFullName().isEmpty()) {
                        tvCustomerName.setText(profile.getFullName());
                        updateGreeting(profile.getFullName().split(" ")[0]);
                        sessionManager.saveFullName(profile.getFullName());
                    }

                    if (profile.getAddress() != null && !profile.getAddress().isEmpty()) {
                        tvDeliveryAddress.setText(profile.getAddress());
                    } else {
                        tvDeliveryAddress.setText("Add delivery address");
                    }

                    if (profile.getProfileImage() != null && !profile.getProfileImage().isEmpty()) {
                        sessionManager.saveProfileImage(profile.getProfileImage());
                        loadProfileImage(profile.getProfileImage());
                    }
                } else {
                    Log.e(TAG, "Failed to load customer profile: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CustomerProfileResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading customer profile", t);
            }
        });
    }

    private void loadProfileImage(String imageUrl) {
        String fullUrl = getFullImageUrl(imageUrl);
        if (fullUrl != null) {
            // The XML tint/padding style the placeholder icon; left in place they
            // render the loaded photo as a solid green silhouette.
            imgCustomerProfile.setImageTintList(null);
            imgCustomerProfile.setPadding(0, 0, 0, 0);
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgCustomerProfile);
        } else {
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            imgCustomerProfile.setPadding(pad, pad, pad, pad);
            imgCustomerProfile.setImageTintList(
                    android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(this, R.color.forest)));
            imgCustomerProfile.setImageResource(R.drawable.ic_person);
        }
    }

    /** Backend returns a relative path (e.g. /uploads/profiles/abc.jpg); prepend the server base. */
    private String getFullImageUrl(String imageUrl) {
        return ImageUrlHelper.resolve(imageUrl);
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void refreshCartBadge() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCart(token).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CartResponse body = response.body();
                    updateCartUi(body.getItemCount(), body.getGrandTotal());
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Log.e(TAG, "Failed to refresh cart badge", t);
            }
        });
    }

    private void updateCartUi(int count) {
        updateCartUi(count, -1);
    }

    private void updateCartUi(int count, double grandTotal) {
        if (tvCartBadge != null) {
            tvCartBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            tvCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        }
        if (cardViewCartBar != null) {
            if (count > 0) {
                cardViewCartBar.setVisibility(View.VISIBLE);
                if (tvCartBarCount != null) {
                    String label = count == 1 ? "1 item in cart" : count + " items in cart";
                    if (grandTotal >= 0) {
                        label += " · ₹" + String.format("%.0f", grandTotal);
                    }
                    tvCartBarCount.setText(label);
                }
            } else {
                cardViewCartBar.setVisibility(View.GONE);
            }
        }
    }

    private void addMealToCart(Meal meal) {
        if (meal == null) return;
        String token = "Bearer " + sessionManager.getToken();
        apiService.addToCart(token, new AddToCartRequest(meal.getId(), 1))
                .enqueue(new Callback<CartResponse>() {
                    @Override
                    public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(CustomerHomeActivity.this,
                                    meal.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                            refreshCartBadge();
                        } else {
                            Toast.makeText(CustomerHomeActivity.this,
                                    "Failed to add to cart", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CartResponse> call, Throwable t) {
                        Toast.makeText(CustomerHomeActivity.this,
                                "Network error adding to cart", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openCookForMeal(Meal meal) {
        if (meal == null || meal.getCookId() <= 0) {
            Toast.makeText(this, "Cook not available for this meal", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, CookDetailsActivity.class)
                .putExtra(CookDetailsActivity.EXTRA_COOK_ID, meal.getCookId()));
    }

    private void updateGreeting(String name) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "Good Morning, " + name + "! 👋";
        } else if (hour < 17) {
            greeting = "Good Afternoon, " + name + "! 👋";
        } else {
            greeting = "Good Evening, " + name + "! 👋";
        }

        tvWelcome.setText(greeting);
    }

    private void setupRecyclerViews() {
        try {
            // Categories RecyclerView
            rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            // Popular Meals RecyclerView
            rvPopularMeals.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            // Recommended Meals RecyclerView (horizontal cards, like Popular Near You)
            rvRecommendedMeals.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            // Popular Near You RecyclerView (horizontal cards)
            rvNearbyCooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            // Search results RecyclerView (vertical dropdown under the search bar)
            rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
            cookSearchResultAdapter = new CookSearchResultAdapter(cook ->
                    startActivity(new Intent(CustomerHomeActivity.this, CookDetailsActivity.class)
                            .putExtra(CookDetailsActivity.EXTRA_COOK_ID, cook.getId())));
            rvSearchResults.setAdapter(cookSearchResultAdapter);

            Log.d(TAG, "RecyclerViews setup successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerViews", e);
        }
    }

    private void loadCategories() {
        categories = new ArrayList<>();
        categories.add(new Category("🍛", "Nepali"));
        categories.add(new Category("🍕", "Fast Food"));
        categories.add(new Category("🥗", "Healthy"));
        categories.add(new Category("🥟", "Snacks"));
        categories.add(new Category("🥘", "Lunch"));
        categories.add(new Category("🍰", "Desserts"));

        categoryAdapter = new CategoryAdapter(categories, (category, position) -> {
            Toast.makeText(this, "Category: " + category.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Filter meals by category
        });

        rvCategories.setAdapter(categoryAdapter);
    }

    private void loadMeals() {
        popularMeals = new ArrayList<>();
        popularMealAdapter = new PopularMealAdapter(popularMeals, new PopularMealAdapter.OnMealClickListener() {
            @Override
            public void onMealClick(Meal meal) {
                openCookForMeal(meal);
            }

            @Override
            public void onFavoriteClick(Meal meal, int position) {
                Toast.makeText(CustomerHomeActivity.this, "Open cook to save favorites", Toast.LENGTH_SHORT).show();
                openCookForMeal(meal);
            }
        });
        rvPopularMeals.setAdapter(popularMealAdapter);

        recommendedMeals = new ArrayList<>();
        recommendedMealAdapter = new RecommendedMealAdapter(recommendedMeals, new RecommendedMealAdapter.OnMealActionListener() {
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
        }, true);
        rvRecommendedMeals.setAdapter(recommendedMealAdapter);

        apiService.getAllMeals().enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Meal> meals = response.body().getMeals();

                    // "Popular Near You": highest-rated cooks' meals first.
                    List<Meal> byRating = new ArrayList<>(meals);
                    Collections.sort(byRating, (a, b) -> {
                        double ratingA = a.getCookRating() != null ? a.getCookRating() : 0;
                        double ratingB = b.getCookRating() != null ? b.getCookRating() : 0;
                        return Double.compare(ratingB, ratingA);
                    });
                    popularMeals.clear();
                    popularMeals.addAll(byRating);
                    popularMealAdapter.notifyDataSetChanged();

                    // "Recommended For You": top 5 only on the home screen; "View All" shows the rest.
                    recommendedMeals.clear();
                    recommendedMeals.addAll(meals.subList(0, Math.min(5, meals.size())));
                    recommendedMealAdapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Failed to load meals: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading meals", t);
            }
        });
    }

    private void loadPopularCooks() {
        popularCooks = new ArrayList<>();
        popularCookAdapter = new PopularCookAdapter(popularCooks, cook ->
                startActivity(new Intent(CustomerHomeActivity.this, CookDetailsActivity.class)
                        .putExtra(CookDetailsActivity.EXTRA_COOK_ID, cook.getId())));
        rvNearbyCooks.setAdapter(popularCookAdapter);

        apiService.getAllCooks().enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(Call<CookProfileResponse> call, Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getCooks() != null) {
                    popularCooks.clear();
                    popularCooks.addAll(response.body().getCooks());
                    popularCookAdapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Failed to load popular cooks: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CookProfileResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading popular cooks", t);
            }
        });
    }

    private void scheduleSearch(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        if (trimmed.isEmpty()) {
            cardSearchResults.setVisibility(View.GONE);
            return;
        }
        pendingSearch = () -> searchCooks(trimmed);
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void searchCooks(String query) {
        apiService.getAllCooks(query).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(Call<CookProfileResponse> call, Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CookProfile> results = response.body().getCooks() != null
                            ? response.body().getCooks() : new ArrayList<>();
                    cookSearchResultAdapter.setCooks(results);
                    cardSearchResults.setVisibility(View.VISIBLE);
                    tvSearchNoResults.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    rvSearchResults.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    Log.e(TAG, "Cook search failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CookProfileResponse> call, Throwable t) {
                Log.e(TAG, "Cook search network error", t);
            }
        });
    }

    private void setupListeners() {
        // Search cooks by name/address as the user types (debounced)
        etSearchHome.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter button click
        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Show filter bottom sheet
        });

        // Notification button click
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        // Cart — header icon (always visible) + sticky bottom strip (shown once cart has items)
        if (btnCart != null) {
            btnCart.setOnClickListener(v -> openCart());
        }
        if (cardViewCartBar != null) {
            cardViewCartBar.setOnClickListener(v -> openCart());
        }

        // Cooks Near Me — opens the customer discovery map
        View cardNearbyCooks = findViewById(R.id.cardNearbyCooks);
        if (cardNearbyCooks != null) {
            cardNearbyCooks.setOnClickListener(v ->
                    startActivity(new Intent(this, NearbyCooksMapActivity.class)));
        }

        // Promo banner click
        promoBanner.setOnClickListener(v -> {
            Toast.makeText(this, "Promo details coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Show promo details
        });

        // View All buttons
        tvViewAllPopular.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerMenuActivity.class)
                    .putExtra(CustomerMenuActivity.EXTRA_FILTER, CustomerMenuActivity.FILTER_POPULAR));
        });

        tvViewAllRecommended.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerMenuActivity.class)
                    .putExtra(CustomerMenuActivity.EXTRA_FILTER, CustomerMenuActivity.FILTER_RECOMMENDED));
        });

        tvViewAllCooks.setOnClickListener(v -> {
            Toast.makeText(this, "View all cooks", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to all cooks
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_menu) {
                startActivity(new Intent(this, CustomerMenuActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_cart) {
                openCart();
                return false; // stay on home; cart is a stack activity
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, com.tiffincraft.app.activities.order.OrderHistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, CustomerProfileActivity.class));
                return false;
            }

            return false;
        });
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }
}
