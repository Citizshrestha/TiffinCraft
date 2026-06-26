package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CustomerHomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvWelcome;
    private TextView tvOrdersCount, tvFavoritesCount;
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
        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
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

    private void setupListeners() {
        // Search bar tapped → open cook/meal search
        View cardSearch = findViewById(R.id.cardSearch);
        if (cardSearch != null) {
            cardSearch.setOnClickListener(v ->
                startActivity(new Intent(CustomerHomeActivity.this, CookListActivity.class))
            );
        }

        // Profile image tapped → open profile
        View imgProfile = findViewById(R.id.imgProfile);
        if (imgProfile != null) {
            imgProfile.setOnClickListener(v ->
                startActivity(new Intent(CustomerHomeActivity.this, CustomerProfileActivity.class))
            );
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(CustomerHomeActivity.this, OrderHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(CustomerHomeActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(CustomerHomeActivity.this, CustomerProfileActivity.class));
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
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}