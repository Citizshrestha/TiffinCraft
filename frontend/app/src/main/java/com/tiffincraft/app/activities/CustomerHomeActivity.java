package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CustomerHomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvWelcome, tvSeeAllCooks;
    private TextView tvOrdersCount, tvFavoritesCount;
    private View cardSearch;
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
        setupBottomNavigation();
        setupClickListeners();
        applyEntranceAnimations();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
        tvSeeAllCooks = findViewById(R.id.tvSeeAllCooks);
        cardSearch = findViewById(R.id.cardSearch);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupClickListeners() {
        if (cardSearch != null) {
            cardSearch.setOnClickListener(v -> {
                startActivity(new Intent(CustomerHomeActivity.this, SearchFilterActivity.class));
            });
        }

        if (tvSeeAllCooks != null) {
            tvSeeAllCooks.setOnClickListener(v -> {
                startActivity(new Intent(CustomerHomeActivity.this, CookListActivity.class));
            });
        }
    }

    private void loadUserData() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvUserName.setText(fullName);
        } else {
            tvUserName.setText("Customer");
        }

        tvOrdersCount.setText("0");
        tvFavoritesCount.setText("0");
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, OrderHistoryActivity.class));
                return false;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return false;
            } else if (itemId == R.id.nav_profile) {
                showLogoutDialog();
                return false;
            }

            return false;
        });
    }

    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> {
                performLogout();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performLogout() {
        sessionManager.logout();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(CustomerHomeActivity.this, SelectRoleActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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