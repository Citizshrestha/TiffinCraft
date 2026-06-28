package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CookHomeActivity extends AppCompatActivity {

    private static final String TAG = "CookHomeActivity";

    private ImageView imgCookProfilePic;
    private TextView tvKitchenName, tvWelcome;
    private TextView tvTodayOrders, tvTodayEarnings, tvActiveSubscriptions, tvAvgRating;
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
            
            Log.d(TAG, "CookHomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            finish();
        }
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

        // Load profile picture
        String profileImageUrl = sessionManager.getProfileImage();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            String fullImageUrl = "http://192.168.100.115:5000" + profileImageUrl;
            
            RequestOptions options = new RequestOptions()
                .centerCrop()
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person);
            
            Glide.with(this)
                .load(fullImageUrl)
                .apply(options)
                .into(imgCookProfilePic);
        } else {
            // Default avatar
            imgCookProfilePic.setImageResource(R.drawable.ic_person);
        }

        tvTodayOrders.setText("18");
        tvTodayEarnings.setText("₹4,250");
        tvActiveSubscriptions.setText("32");
        tvAvgRating.setText("4.8");
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
}
