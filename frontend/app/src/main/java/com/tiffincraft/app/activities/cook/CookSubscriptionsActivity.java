package com.tiffincraft.app.activities.cook;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lets a cook turn on weekly/monthly subscription pricing for individual meals.
 * Plan pricing is a discount off the per-meal price (6 meals/week for weekly,
 * 24 meals/month for monthly). Which meals have a plan enabled is stored on-device
 * for now since there is no subscriptions backend yet — this screen is the cook-side
 * configuration surface that a future customer subscription flow will read from.
 */
public class CookSubscriptionsActivity extends AppCompatActivity {

    private static final String TAG = "CookSubscriptions";
    private static final String PREFS_NAME = "SubscriptionPlans";
    private static final double WEEKLY_MEALS = 6;
    private static final double MONTHLY_MEALS = 24;
    private static final double DISCOUNT = 0.90; // 10% off for subscribing

    private ApiService apiService;
    private SessionManager sessionManager;
    private SharedPreferences prefs;

    private LinearLayout layoutMeals;
    private TextView tvPlansEnabled, tvWeeklyPotential, tvEmptyMeals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_subscriptions);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        layoutMeals = findViewById(R.id.layoutMeals);
        tvPlansEnabled = findViewById(R.id.tvPlansEnabled);
        tvWeeklyPotential = findViewById(R.id.tvWeeklyPotential);
        tvEmptyMeals = findViewById(R.id.tvEmptyMeals);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadMeals();
    }

    private void loadMeals() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getMyMeals(token).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getMeals() != null && !response.body().getMeals().isEmpty()) {
                    renderMeals(response.body().getMeals());
                } else {
                    tvEmptyMeals.setVisibility(android.view.View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load meals", t);
                Toast.makeText(CookSubscriptionsActivity.this,
                        "Could not load meals: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderMeals(List<Meal> meals) {
        layoutMeals.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        for (Meal meal : meals) {
            String prefKey = "meal_" + meal.getId();
            boolean enabled = prefs.getBoolean(prefKey, false);

            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.topMargin = (int) (10 * density);
            card.setLayoutParams(cardLp);
            card.setRadius(16 * density);
            card.setCardElevation(0);
            card.setStrokeColor(0xFFEFEFEF);
            card.setStrokeWidth((int) density);
            card.setCardBackgroundColor(0xFFFFFFFF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(this);
            tvName.setText(meal.getName());
            tvName.setTextColor(0xFF111111);
            tvName.setTextSize(14.5f);
            tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameLp);

            SwitchCompat sw = new SwitchCompat(this);
            sw.setChecked(enabled);
            // Thumb/track tints previously used a single solid color regardless
            // of checked state, so every toggle looked "on" (green) all the time.
            int[][] switchStates = {
                    {android.R.attr.state_checked},
                    {-android.R.attr.state_checked}
            };
            sw.setThumbTintList(new android.content.res.ColorStateList(switchStates,
                    new int[]{0xFF4CAF50, 0xFFF5F5F5}));
            sw.setTrackTintList(new android.content.res.ColorStateList(switchStates,
                    new int[]{0xFFA5D6A7, 0xFFBDBDBD}));

            topRow.addView(tvName);
            topRow.addView(sw);

            TextView tvPricing = new TextView(this);
            tvPricing.setTextSize(12.5f);
            tvPricing.setTextColor(0xFF4CAF50);
            LinearLayout.LayoutParams pricingLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pricingLp.topMargin = (int) (6 * density);
            tvPricing.setLayoutParams(pricingLp);
            tvPricing.setText(buildPricingText(meal.getPrice()));
            tvPricing.setVisibility(enabled ? android.view.View.VISIBLE : android.view.View.GONE);

            row.addView(topRow);
            row.addView(tvPricing);
            card.addView(row);
            layoutMeals.addView(card);

            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(prefKey, isChecked).apply();
                tvPricing.setVisibility(isChecked ? android.view.View.VISIBLE : android.view.View.GONE);
                updateSummary(meals);
            });
        }

        updateSummary(meals);
    }

    private String buildPricingText(double mealPrice) {
        double weeklyPrice = mealPrice * WEEKLY_MEALS * DISCOUNT;
        double monthlyPrice = mealPrice * MONTHLY_MEALS * DISCOUNT;
        return "Weekly: " + CurrencyUtils.formatRupees(weeklyPrice)
                + "  ·  Monthly: " + CurrencyUtils.formatRupees(monthlyPrice)
                + "  (10% off)";
    }

    private void updateSummary(List<Meal> meals) {
        int enabledCount = 0;
        double avgWeekly = 0;
        for (Meal meal : meals) {
            if (prefs.getBoolean("meal_" + meal.getId(), false)) {
                enabledCount++;
                avgWeekly += meal.getPrice() * WEEKLY_MEALS * DISCOUNT;
            }
        }
        tvPlansEnabled.setText(String.valueOf(enabledCount));
        tvWeeklyPotential.setText(enabledCount > 0
                ? CurrencyUtils.formatRupees(avgWeekly / enabledCount)
                : "₹0");
    }
}
