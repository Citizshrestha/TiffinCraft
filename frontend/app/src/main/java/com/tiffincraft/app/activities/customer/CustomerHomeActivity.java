package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CustomerDashboardResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerHomeActivity extends AppCompatActivity {
    
    private static final String TAG = "CustomerHomeActivity";

    private TextView tvGreeting, tvSubtitle;
    private FrameLayout notificationButton, filterButton;
    private View searchBar, heroBanner, notificationDot;
    private TextView cartBadge, tvViewAll;
    private FloatingActionButton fabCart;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;
    private LinearLayout popularCooksContainer;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_customer_home);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            sessionManager = new SessionManager(this);
            apiService = RetrofitClient.getClient().create(ApiService.class);

            initViews();
            loadUserData();
            setupListeners();
            setupBottomNavigation();
            applyEntranceAnimations();
            setupBackPressHandler();
            
            // Load dashboard data from backend
            loadDashboardData();
            
            Log.d(TAG, "CustomerHomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
    }

    private void initViews() {
        try {
            tvGreeting = findViewById(R.id.tvGreeting);
            tvSubtitle = findViewById(R.id.tvSubtitle);
            notificationButton = findViewById(R.id.notificationButton);
            searchBar = findViewById(R.id.searchBar);
            heroBanner = findViewById(R.id.heroBanner);
            notificationDot = findViewById(R.id.notificationDot);
            tvViewAll = findViewById(R.id.tvViewAll);
            fabCart = findViewById(R.id.fabCart);
            cartBadge = findViewById(R.id.cartBadge);
            bottomNavigation = findViewById(R.id.bottomNavigation);
            popularCooksContainer = findViewById(R.id.popularCooksContainer);
            
            // Apply circular clipping to banner image container
            View bannerImageContainer = findViewById(R.id.bannerImageContainer);
            if (bannerImageContainer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                bannerImageContainer.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                bannerImageContainer.setClipToOutline(true);
            }
            
            // Log missing views
            if (tvGreeting == null) Log.e(TAG, "tvGreeting is null");
            if (bottomNavigation == null) Log.e(TAG, "bottomNavigation is null");
            if (fabCart == null) Log.e(TAG, "fabCart is null");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void loadUserData() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            String firstName = fullName.split(" ")[0];
            tvGreeting.setText("Hello, " + firstName + " 👋");
        } else {
            tvGreeting.setText("Hello, User 👋");
        }

        // Cart badge will be updated from dashboard data
        cartBadge.setVisibility(View.GONE);
    }

    private void loadDashboardData() {
        String token = "Bearer " + sessionManager.getToken();
        
        Call<CustomerDashboardResponse> call = apiService.getCustomerDashboard(token);
        call.enqueue(new Callback<CustomerDashboardResponse>() {
            @Override
            public void onResponse(Call<CustomerDashboardResponse> call, Response<CustomerDashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CustomerDashboardResponse.DashboardData data = response.body().getData();
                    populateDashboard(data);
                } else {
                    Log.e(TAG, "Failed to load dashboard data: " + response.code());
                    Toast.makeText(CustomerHomeActivity.this, "Failed to load dashboard data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CustomerDashboardResponse> call, Throwable t) {
                Log.e(TAG, "Error loading dashboard data", t);
                Toast.makeText(CustomerHomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateDashboard(CustomerDashboardResponse.DashboardData data) {
        try {
            // Update cart badge
            int cartCount = data.getStats().getCartItemsCount();
            if (cartCount > 0) {
                cartBadge.setText(String.valueOf(cartCount));
                cartBadge.setVisibility(View.VISIBLE);
            } else {
                cartBadge.setVisibility(View.GONE);
            }

            // Update notification dot
            int unreadCount = data.getUnreadNotificationsCount();
            if (unreadCount > 0) {
                notificationDot.setVisibility(View.VISIBLE);
            } else {
                notificationDot.setVisibility(View.GONE);
            }

            // Update greeting with user's name
            String fullName = data.getUser().getFullName();
            if (fullName != null && !fullName.isEmpty()) {
                String firstName = fullName.split(" ")[0];
                tvGreeting.setText("Hello, " + firstName + " 👋");
            }

            // Populate popular cooks
            List<CustomerDashboardResponse.PopularCook> popularCooks = data.getPopularCooks();
            if (popularCooks != null && !popularCooks.isEmpty() && popularCooksContainer != null) {
                popularCooksContainer.removeAllViews();
                for (CustomerDashboardResponse.PopularCook cook : popularCooks) {
                    View cookCard = createPopularCookCard(cook);
                    popularCooksContainer.addView(cookCard);
                }
            }

            Log.d(TAG, "Dashboard populated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error populating dashboard", e);
        }
    }

    private View createPopularCookCard(CustomerDashboardResponse.PopularCook cook) {
        View card = getLayoutInflater().inflate(R.layout.item_popular_cook, null);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int) (140 * getResources().getDisplayMetrics().density),
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
        card.setLayoutParams(params);

        ImageView imgCook = card.findViewById(R.id.imgCook);
        TextView tvCookName = card.findViewById(R.id.tvCookName);
        TextView tvRating = card.findViewById(R.id.tvRating);
        TextView tvDeliveryTime = card.findViewById(R.id.tvDeliveryTime);

        // Set cook name
        tvCookName.setText(cook.getKitchenName() != null ? cook.getKitchenName() : cook.getCookName());

        // Set rating
        String ratingText = "⭐ " + String.format("%.1f", cook.getRating()) + " (" + cook.getReviewCount() + ")";
        tvRating.setText(ratingText);

        // Set delivery time
        tvDeliveryTime.setText(cook.getAvgDeliveryTime() + " mins");

        // Load image
        if (cook.getProfileImage() != null && !cook.getProfileImage().isEmpty()) {
            String imageUrl = RetrofitClient.BASE_URL.replace("/api/", "/") + cook.getProfileImage();
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.avatar_cook)
                .error(R.drawable.avatar_cook)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgCook);
        } else {
            imgCook.setImageResource(R.drawable.avatar_cook);
        }

        // Click listener
        card.setOnClickListener(v -> {
            // Navigate to cook details
            // Intent intent = new Intent(this, CookDetailsActivity.class);
            // intent.putExtra("cookId", cook.getCookId());
            // startActivity(intent);
            Toast.makeText(this, "Cook: " + cook.getCookName(), Toast.LENGTH_SHORT).show();
        });

        return card;
    }

    private void setupListeners() {
        // Search bar tapped → open search activity
        searchBar.setOnClickListener(v -> {
            // startActivity(new Intent(this, SearchActivity.class));
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
        });

        // Notification bell tapped → open notifications activity
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        // Filter button tapped
        if (findViewById(R.id.filterButton) != null) {
            findViewById(R.id.filterButton).setOnClickListener(v -> {
                // Show filter dialog or open filter activity
                Toast.makeText(this, "Filter coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Hero banner tapped
        heroBanner.setOnClickListener(v -> {
            // Show promo details or navigate to featured section
            Toast.makeText(this, "Promo details coming soon", Toast.LENGTH_SHORT).show();
        });

        // View All tapped
        tvViewAll.setOnClickListener(v -> {
            // startActivity(new Intent(this, AllCooksActivity.class));
            Toast.makeText(this, "All cooks coming soon", Toast.LENGTH_SHORT).show();
        });

        // Floating cart button tapped
        fabCart.setOnClickListener(v -> {
            // startActivity(new Intent(this, CartActivity.class));
            Toast.makeText(this, "Cart coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_orders) {
                // startActivity(new Intent(this, OrderHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, CustomerProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    private void applyEntranceAnimations() {
        try {
            View headerSection = findViewById(R.id.tvGreeting);

            if (headerSection != null) {
                headerSection.setAlpha(0f);
                headerSection.setTranslationY(-20f);
                headerSection.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(100)
                    .start();
            }

            if (searchBar != null) {
                searchBar.setAlpha(0f);
                searchBar.setTranslationY(-20f);
                searchBar.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(200)
                    .start();
            }

            if (heroBanner != null) {
                heroBanner.setAlpha(0f);
                heroBanner.setScaleX(0.9f);
                heroBanner.setScaleY(0.9f);
                heroBanner.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying entrance animations", e);
        }
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
