package com.tiffincraft.app.activities.meal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.CartActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.FavoriteResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_COOK_ID = "cook_id";

    private ImageButton btnBack;
    private FrameLayout btnFavorite;
    private ImageView imgFavoriteIcon;
    private TextView tvKitchenName, tvRating, tvFoodType, tvTotalPrice;
    private LinearLayout layoutMenuItems;
    private MaterialButton btnOrderNow;

    private int cookId = -1;
    private boolean isFavorite = false;
    private double runningTotal = 0.0;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_details);

        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.green_primary));
            getWindow().getDecorView().setSystemUiVisibility(0);
        } catch (Exception e) {
            getWindow().setStatusBarColor(0xFF4CAF50);
        }

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        cookId = getIntent().getIntExtra(EXTRA_COOK_ID, -1);
        if (cookId == -1) {
            Toast.makeText(this, "Invalid cook", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadCookProfile();
        loadCookMeals();
        checkFavoriteStatus();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        imgFavoriteIcon = (ImageView) btnFavorite.getChildAt(0);
        tvKitchenName = findViewById(R.id.tvKitchenName);
        tvRating = findViewById(R.id.tvRating);
        tvFoodType = findViewById(R.id.tvFoodType);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        layoutMenuItems = findViewById(R.id.layoutMenuItems);
        btnOrderNow = findViewById(R.id.btnOrderNow);

        btnBack.setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // "Order Now" jumps straight to cart, since items were added inline via the menu
        btnOrderNow.setOnClickListener(v -> {
            startActivity(new Intent(CookDetailsActivity.this, CartActivity.class));
        });

        tvTotalPrice.setText("₹0");
    }

    // ─────────────────────────────────────────────
    // LOAD COOK PROFILE
    // ─────────────────────────────────────────────
    private void loadCookProfile() {
        apiService.getCookById(cookId).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(Call<CookProfileResponse> call, Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    populateCookInfo(response.body());
                } else {
                    Toast.makeText(CookDetailsActivity.this, "Failed to load cook profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CookProfileResponse> call, Throwable t) {
                Toast.makeText(CookDetailsActivity.this, "Network error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateCookInfo(CookProfileResponse response) {
        // NOTE: adjust getters below to match your actual CookProfileResponse/CookProfile model.
        // I'm assuming a nested getCook() based on your earlier masterprompt's CookPublicProfile pattern.
        if (response.getCook() == null) return;

        String kitchenName = response.getCook().getKitchenName();
        tvKitchenName.setText(kitchenName != null ? kitchenName : response.getCook().getFullName());
        tvRating.setText(String.valueOf(response.getCook().getRating()));
        tvFoodType.setText(response.getCook().getFoodType() != null
                ? response.getCook().getFoodType() : "Home-cooked meals");
    }

    // ─────────────────────────────────────────────
    // LOAD MEALS — inflated as rows into layoutMenuItems
    // ─────────────────────────────────────────────
    private void loadCookMeals() {
        apiService.getMealsByCook(cookId).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getMeals() != null) {
                    renderMeals(response.body().getMeals());
                } else {
                    Toast.makeText(CookDetailsActivity.this, "No meals available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                Toast.makeText(CookDetailsActivity.this, "Failed to load meals", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderMeals(List<Meal> meals) {
        layoutMenuItems.removeAllViews();

        if (meals.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No meals available right now");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_on_light));
            empty.setPadding(0, 24, 0, 24);
            layoutMenuItems.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Meal meal : meals) {
            View row = inflater.inflate(R.layout.item_meal_row_simple, layoutMenuItems, false);

            ImageView imgMeal = row.findViewById(R.id.imgMeal);
            TextView tvName = row.findViewById(R.id.tvMealName);
            TextView tvPrice = row.findViewById(R.id.tvPrice);
            MaterialButton btnAdd = row.findViewById(R.id.btnAdd);

            tvName.setText(meal.getName());
            tvPrice.setText(String.format("₹%.0f", meal.getPrice()));

            Glide.with(this)
                    .load(meal.getImageUrl())
                    .placeholder(R.drawable.ic_food)
                    .centerCrop()
                    .into(imgMeal);

            if (!meal.isAvailable()) {
                btnAdd.setEnabled(false);
                btnAdd.setText("Unavailable");
            } else {
                btnAdd.setOnClickListener(v -> addMealToCart(meal, btnAdd));
            }

            layoutMenuItems.addView(row);
        }
    }

    private void addMealToCart(Meal meal, MaterialButton btnAdd) {
        btnAdd.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();

        apiService.addToCart(token, new AddToCartRequest(meal.getId(), 1)).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                btnAdd.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    runningTotal += meal.getPrice();
                    tvTotalPrice.setText(String.format("₹%.0f", runningTotal));
                    Toast.makeText(CookDetailsActivity.this, meal.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CookDetailsActivity.this, "Failed to add to cart", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                btnAdd.setEnabled(true);
                Toast.makeText(CookDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────
    // FAVORITES
    // ─────────────────────────────────────────────
    private void checkFavoriteStatus() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.checkFavoriteStatus(token, cookId).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFavorite = response.body().getIsFavorite() != null && response.body().getIsFavorite();
                    updateFavoriteIcon();
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                // Silent fail — favorite status just won't pre-populate
            }
        });
    }

    private void toggleFavorite() {
        String token = "Bearer " + sessionManager.getToken();

        if (isFavorite) {
            apiService.removeFromFavorites(token, cookId).enqueue(new Callback<FavoriteResponse>() {
                @Override
                public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                    isFavorite = false;
                    updateFavoriteIcon();
                    Toast.makeText(CookDetailsActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                    Toast.makeText(CookDetailsActivity.this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            com.google.gson.JsonObject body = new com.google.gson.JsonObject();
            body.addProperty("cook_id", cookId);

            apiService.addToFavorites(token, body).enqueue(new Callback<FavoriteResponse>() {
                @Override
                public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                    isFavorite = true;
                    updateFavoriteIcon();
                    Toast.makeText(CookDetailsActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                    Toast.makeText(CookDetailsActivity.this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFavoriteIcon() {
        imgFavoriteIcon.setImageResource(R.drawable.ic_favorite);
        imgFavoriteIcon.setColorFilter(ContextCompat.getColor(this,
                isFavorite ? R.color.red : android.R.color.darker_gray));
    }
}