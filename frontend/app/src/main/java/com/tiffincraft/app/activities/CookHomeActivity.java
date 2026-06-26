package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CookHomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvWelcome;
    private TextView tvTotalOrders, tvRevenue, tvActiveMeals, tvRating;
    private MaterialButton btnAddMeal;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvRevenue = findViewById(R.id.tvRevenue);
        tvActiveMeals = findViewById(R.id.tvActiveMeals);
        tvRating = findViewById(R.id.tvRating);
        btnAddMeal = findViewById(R.id.btnAddMeal);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void loadUserData() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvUserName.setText(fullName);
        } else {
            tvUserName.setText("Home Cook");
        }

        tvTotalOrders.setText("0");
        tvRevenue.setText("₹0");
        tvActiveMeals.setText("0");
        tvRating.setText("0.0");
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

        // Profile avatar tapped → go to cook profile
        View imgProfile = findViewById(R.id.imgProfile);
        if (imgProfile != null) {
            imgProfile.setOnClickListener(v ->
                startActivity(new Intent(CookHomeActivity.this, CookProfileActivity.class))
            );
        }

        // Revenue card tapped → go to earnings
        View tvRevenueCard = tvRevenue != null ? (View) tvRevenue.getParent() : null;
        if (tvRevenueCard != null) {
            tvRevenueCard.setOnClickListener(v ->
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
                startActivity(new Intent(CookHomeActivity.this, AddMenuActivity.class));
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(CookHomeActivity.this, ManageOrdersActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(CookHomeActivity.this, CookProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    private void applyEntranceAnimations() {
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
        }
    }

    @Override
    public void onBackPressed() {
        // Show exit confirmation or minimize app
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}