package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.models.PopularCook;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CustomerHomeActivity extends AppCompatActivity {
    
    private static final String TAG = "CustomerHomeActivity";

    private TextView tvGreeting, tvSubtitle;
    private FrameLayout notificationButton, filterButton;
    private View searchBar, heroBanner, notificationDot;
    private TextView cartBadge, tvViewAll;
    private FloatingActionButton fabCart;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        sessionManager = new SessionManager(this);

        initViews();
        loadUserData();
        setupListeners();
        setupBottomNavigation();
        applyEntranceAnimations();
    }

    private void initViews() {
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
    }

    private void loadUserData() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            String firstName = fullName.split(" ")[0];
            tvGreeting.setText("Hello, " + firstName + " 👋");
        } else {
            tvGreeting.setText("Hello, User 👋");
        }

        // Set cart badge visibility and count
        cartBadge.setText("0");
        cartBadge.setVisibility(View.GONE); // Hide when cart is empty
    }

    private void setupListeners() {
        // Search bar tapped → open search activity
        searchBar.setOnClickListener(v -> {
            // startActivity(new Intent(this, SearchActivity.class));
        });

        // Notification bell tapped
        notificationButton.setOnClickListener(v -> {
            // startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Filter button tapped
        if (findViewById(R.id.filterButton) != null) {
            findViewById(R.id.filterButton).setOnClickListener(v -> {
                // Show filter dialog or open filter activity
            });
        }

        // Hero banner tapped
        heroBanner.setOnClickListener(v -> {
            // Show promo details or navigate to featured section
        });

        // View All tapped
        tvViewAll.setOnClickListener(v -> {
            // startActivity(new Intent(this, AllCooksActivity.class));
        });

        // Floating cart button tapped
        fabCart.setOnClickListener(v -> {
            // startActivity(new Intent(this, CartActivity.class));
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
                // startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                // startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    private void applyEntranceAnimations() {
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
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
