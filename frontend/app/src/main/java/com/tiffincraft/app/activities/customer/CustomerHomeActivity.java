package com.tiffincraft.app.activities.customer;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.NotificationActivity;
import com.tiffincraft.app.adapters.CategoryAdapter;
import com.tiffincraft.app.adapters.NearbyCookAdapter;
import com.tiffincraft.app.adapters.PopularMealAdapter;
import com.tiffincraft.app.adapters.RecommendedMealAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Category;
import com.tiffincraft.app.models.Cook;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ChatPanelManager;

import java.util.ArrayList;
import java.util.Calendar;
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
    private TextView tvWelcome;
    private MaterialCardView searchBar;
    private MaterialCardView btnFilter;
    private MaterialCardView promoBanner;
    
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
    private NearbyCookAdapter nearbyCookAdapter;
    
    // Data
    private List<Category> categories;
    private List<Meal> popularMeals;
    private List<Meal> recommendedMeals;
    private List<Cook> nearbyCooks;
    
    // Session and API
    private SessionManager sessionManager;
    private ApiService apiService;
    private ChatPanelManager chatPanelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_customer_home);

            // Set white status bar
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            sessionManager = new SessionManager(this);
            apiService = RetrofitClient.getInstance(this).getApiService();

            initViews();
            setupRecyclerViews();
            setupListeners();
            setupBottomNavigation();
            loadUserData();
            loadCategories();
            loadMockData(); // Will be replaced with API calls
            setupBackPressHandler();

            // Chat: this screen has no dedicated chat button in its layout,
            // so let the panel manager inflate its own floating chat button.
            chatPanelManager = ChatPanelManager.attach(this);

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
    }

    private void initViews() {
        try {
            // Top bar
            imgCustomerProfile = findViewById(R.id.imgCustomerProfile);
            tvCustomerName = findViewById(R.id.tvCustomerName);
            tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
            btnNotifications = findViewById(R.id.btnNotifications);
            tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
            
            // Greeting
            tvWelcome = findViewById(R.id.tvWelcome);
            
            // Search and Filter
            searchBar = findViewById(R.id.searchBar);
            btnFilter = findViewById(R.id.btnFilter);
            
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
            
            // Set customer name
            if (fullName != null && !fullName.isEmpty()) {
                tvCustomerName.setText(fullName);
                String firstName = fullName.split(" ")[0];
                updateGreeting(firstName);
            } else {
                tvCustomerName.setText("Guest");
                updateGreeting("Guest");
            }
            
            // Set delivery address (placeholder, can be fetched from API)
            tvDeliveryAddress.setText("Kathmandu, Nepal");
            
            // Load profile image
            // TODO: Load from session or API
            imgCustomerProfile.setImageResource(R.drawable.ic_person);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
        }
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
            
            // Recommended Meals RecyclerView (Grid)
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            rvRecommendedMeals.setLayoutManager(gridLayoutManager);
            
            // Nearby Cooks RecyclerView
            rvNearbyCooks.setLayoutManager(new LinearLayoutManager(this));
            
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
    
    private void loadMockData() {
        // Mock Popular Meals
        popularMeals = new ArrayList<>();
        // TODO: Replace with API call
        
        popularMealAdapter = new PopularMealAdapter(popularMeals, new PopularMealAdapter.OnMealClickListener() {
            @Override
            public void onMealClick(Meal meal) {
                Toast.makeText(CustomerHomeActivity.this, "Meal: " + meal.getName(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to meal details
            }

            @Override
            public void onFavoriteClick(Meal meal, int position) {
                Toast.makeText(CustomerHomeActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                // TODO: Add to favorites API call
            }
        });
        rvPopularMeals.setAdapter(popularMealAdapter);
        
        // Mock Recommended Meals
        recommendedMeals = new ArrayList<>();
        // TODO: Replace with API call
        
        recommendedMealAdapter = new RecommendedMealAdapter(recommendedMeals, new RecommendedMealAdapter.OnMealActionListener() {
            @Override
            public void onMealClick(Meal meal) {
                Toast.makeText(CustomerHomeActivity.this, "Meal: " + meal.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavoriteClick(Meal meal, int position) {
                Toast.makeText(CustomerHomeActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddToCartClick(Meal meal) {
                Toast.makeText(CustomerHomeActivity.this, "Added to cart", Toast.LENGTH_SHORT).show();
                // TODO: Add to cart API call
            }
        });
        rvRecommendedMeals.setAdapter(recommendedMealAdapter);
        
        // Mock Nearby Cooks
        nearbyCooks = new ArrayList<>();
        // TODO: Replace with API call
        
        nearbyCookAdapter = new NearbyCookAdapter(nearbyCooks, new NearbyCookAdapter.OnCookClickListener() {
            @Override
            public void onCookClick(Cook cook) {
                Toast.makeText(CustomerHomeActivity.this, "Cook: " + cook.getName(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to cook details
            }

            @Override
            public void onViewMenuClick(Cook cook) {
                Toast.makeText(CustomerHomeActivity.this, "View menu of " + cook.getName(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to cook menu
            }
        });
        rvNearbyCooks.setAdapter(nearbyCookAdapter);
    }
    
    private void setupListeners() {
        // Search bar click
        searchBar.setOnClickListener(v -> {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Open search activity
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
        
        // Promo banner click
        promoBanner.setOnClickListener(v -> {
            Toast.makeText(this, "Promo details coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Show promo details
        });
        
        // View All buttons
        tvViewAllPopular.setOnClickListener(v -> {
            Toast.makeText(this, "View all popular meals", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to all popular meals
        });
        
        tvViewAllRecommended.setOnClickListener(v -> {
            Toast.makeText(this, "View all recommended meals", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to all recommended meals
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
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, com.tiffincraft.app.activities.order.OrderHistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return false;
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
