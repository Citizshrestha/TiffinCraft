package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.NotificationActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.DashboardResponse;
import com.tiffincraft.app.models.DashboardStats;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.session.SessionManager;

import java.text.DecimalFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookHomeActivity extends AppCompatActivity {

    private static final String TAG = "CookHomeActivity";

    private ImageView imgCookProfilePic;
    private TextView tvKitchenName, tvWelcome;
    private TextView tvTodayOrders, tvTodayEarnings, tvActiveSubscriptions, tvAvgRating;
    private TextView tvNotificationBadge;
    private View btnAddMeal;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_cook_home);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            sessionManager = new SessionManager(this);

            initViews();
            loadUserData();
            setupListeners();
            setupBottomNavigation();
            applyEntranceAnimations();
            setupBackPressHandler();
            
            // Click on notification bell
            findViewById(R.id.btnNotifications).setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
            });
            
            Log.d(TAG, "CookHomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
        fetchUnreadNotifications();
    }

    private void initViews() {
        imgCookProfilePic = findViewById(R.id.imgCookProfilePic);
        tvKitchenName = findViewById(R.id.tvKitchenName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvTodayOrders = findViewById(R.id.tvTodayOrders);
        tvTodayEarnings = findViewById(R.id.tvTodayEarnings);
        tvActiveSubscriptions = findViewById(R.id.tvActiveSubscriptions);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        btnAddMeal = findViewById(R.id.btnAddMeal);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
    }

    private void loadUserData() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvKitchenName.setText(fullName);
            tvWelcome.setText("Hello, " + fullName.split(" ")[0] + "! 👋");
        } else {
            tvKitchenName.setText("Home Cook");
            tvWelcome.setText("Hello! 👋");
        }

        // Load profile picture using same method as CookProfileActivity
        String profileImageUrl = sessionManager.getProfileImage();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            String fullUrl = getFullImageUrl(profileImageUrl);
            if (fullUrl != null) {
                Log.d(TAG, "Loading profile image: " + fullUrl);

                GlideUrl glideUrl = new GlideUrl(fullUrl, new LazyHeaders.Builder()
                    .addHeader("Bypass-Tunnel-Reminder", "true")
                    .build());

                Glide.with(this)
                    .load(glideUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imgCookProfilePic);
            } else {
                Glide.with(this)
                    .load(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imgCookProfilePic);
            }
        } else {
            // Default avatar
            Glide.with(this)
                .load(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(imgCookProfilePic);
        }

        // Fetch real dashboard data from backend
        fetchDashboardData();
    }

    /**
     * Build full URL from relative path returned by backend
     */
    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return RetrofitClient.SERVER_URL + imageUrl;
    }

    /**
     * Fetch real dashboard statistics from backend API
     */
    private void fetchDashboardData() {
        String token = "Bearer " + sessionManager.getToken();

        Log.d(TAG, "=== Fetching Dashboard Data ===");
        Log.d(TAG, "Token exists: " + (sessionManager.getToken() != null));
        Log.d(TAG, "User ID: " + sessionManager.getUserId());
        Log.d(TAG, "User Role: " + sessionManager.getRole());

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getCookDashboard(token).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(@NonNull Call<DashboardResponse> call,
                                   @NonNull Response<DashboardResponse> response) {
                Log.d(TAG, "=== Dashboard Response Received ===");
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Success: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {

                    DashboardStats stats = response.body().getDashboard();
                    Log.d(TAG, "✅ Dashboard data received");
                    if (stats != null) {
                        Log.d(TAG, "  - Today orders: " + (stats.getTodayOrders() != null ? stats.getTodayOrders().getCount() : "null"));
                        Log.d(TAG, "  - Today earnings: ₹" + (stats.getTodayEarnings() != null ? stats.getTodayEarnings().getAmount() : "null"));
                        Log.d(TAG, "  - Active orders: " + (stats.getActiveOrders() != null ? stats.getActiveOrders().getCount() : "null"));
                        Log.d(TAG, "  - Average rating: " + (stats.getAverageRating() != null ? stats.getAverageRating().getRating() : "null"));
                    }
                    updateDashboardUI(stats);

                } else {
                    Log.e(TAG, "❌ Failed to fetch dashboard: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not read error body", e);
                        }
                    }
                    // Show default values on error
                    setDefaultDashboardValues();
                    Toast.makeText(CookHomeActivity.this,
                            "Could not load dashboard: Code " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Dashboard API call failed", t);
                Log.e(TAG, "Error message: " + t.getMessage());
                Log.e(TAG, "Error class: " + t.getClass().getName());
                Log.e(TAG, "Cause: " + (t.getCause() != null ? t.getCause().getMessage() : "null"));
                
                // Show default values on error
                setDefaultDashboardValues();
                
                String errorMessage = "Could not load dashboard";
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMessage = "Cannot connect to server";
                } else if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMessage = "Connection timeout";
                }
                
                Toast.makeText(CookHomeActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Update UI with real dashboard statistics
     */
    private void updateDashboardUI(DashboardStats stats) {
        if (stats == null) {
            setDefaultDashboardValues();
            return;
        }

        // Update today's orders
        if (stats.getTodayOrders() != null) {
            tvTodayOrders.setText(String.valueOf(stats.getTodayOrders().getCount()));
        }

        // Update today's earnings
        if (stats.getTodayEarnings() != null) {
            DecimalFormat formatter = new DecimalFormat("#,##0");
            String earnings = "₹" + formatter.format(stats.getTodayEarnings().getAmount());
            tvTodayEarnings.setText(earnings);
        }

        // Update active subscriptions (using active orders count)
        if (stats.getActiveOrders() != null) {
            tvActiveSubscriptions.setText(String.valueOf(stats.getActiveOrders().getCount()));
        }

        // Update average rating
        if (stats.getAverageRating() != null) {
            DecimalFormat ratingFormatter = new DecimalFormat("0.0");
            String rating = ratingFormatter.format(stats.getAverageRating().getRating());
            tvAvgRating.setText(rating);
        }

        Log.d(TAG, "Dashboard UI updated with real data");
    }

    /**
     * Set default dashboard values (fallback)
     */
    private void setDefaultDashboardValues() {
        tvTodayOrders.setText("0");
        tvTodayEarnings.setText("₹0");
        tvActiveSubscriptions.setText("0");
        tvAvgRating.setText("0.0");
    }

    private void setupListeners() {
        btnAddMeal.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
                })
                .start();
            startActivity(new Intent(CookHomeActivity.this, AddMenuActivity.class));
        });

        // Profile avatar tapped → go to cook profile (if exists in layout)
        // View imgProfile = findViewById(R.id.imgProfile);
        // if (imgProfile != null) {
        //     imgProfile.setOnClickListener(v ->
        //         startActivity(new Intent(CookHomeActivity.this, CookProfileActivity.class))
        //     );
        // }

        // Earnings card tapped → go to earnings detail
        View tvEarningsCard = tvTodayEarnings != null ? (View) tvTodayEarnings.getParent().getParent() : null;
        if (tvEarningsCard != null) {
            tvEarningsCard.setOnClickListener(v ->
                startActivity(new Intent(CookHomeActivity.this, CookEarningsActivity.class))
            );
        }
    }


    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_meals) {
                startActivity(new Intent(CookHomeActivity.this, CookMealActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(CookHomeActivity.this, ManageOrdersActivity.class));
                return true;
            } else if (itemId == R.id.nav_earnings) {
                startActivity(new Intent(CookHomeActivity.this, CookEarningsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(CookHomeActivity.this, CookProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void applyEntranceAnimations() {
        try {
            View appBarLayout = findViewById(R.id.appBarLayout);

            if (appBarLayout != null) {
                appBarLayout.setAlpha(0f);
                appBarLayout.setTranslationY(-50f);
                appBarLayout.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(100)
                    .start();
            } else {
                Log.w(TAG, "appBarLayout not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying entrance animations", e);
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show exit confirmation or minimize app
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    private void fetchUnreadNotifications() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getUnreadNotificationCount(token).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
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
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching unread count", t);
            }
        });
    }
}
