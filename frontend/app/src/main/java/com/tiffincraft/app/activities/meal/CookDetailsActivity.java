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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.CartActivity;
import com.tiffincraft.app.utils.ImageUrlHelper;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.AddToCartRequest;
import com.tiffincraft.app.models.BankDetails;
import com.tiffincraft.app.models.CartResponse;
import com.tiffincraft.app.models.CookProfile;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.FavoriteResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_COOK_ID = "cook_id";
    private static final int REQUEST_STORAGE_PERMISSION = 2001;

    private ImageButton btnBack;
    private FrameLayout btnFavorite;
    private ImageView imgFavoriteIcon;
    private ImageView ivCookImage;
    private View layoutVerifiedBadge;
    private TextView tvKitchenName, tvRating, tvReviewCount, tvFoodType, tvTotalPrice;
    private TextView tvDishesStat, tvOrdersStat, tvReviewsStat;
    private TextView tvAboutDescription, tvLocation, tvAvailability, tvMenuItemCount;
    private View layoutLocation, layoutAvailability;
    private LinearLayout layoutMenuItems;
    private androidx.cardview.widget.CardView layoutPaymentQr;
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
        ivCookImage = findViewById(R.id.ivCookImage);
        layoutVerifiedBadge = findViewById(R.id.layoutVerifiedBadge);
        tvKitchenName = findViewById(R.id.tvKitchenName);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvFoodType = findViewById(R.id.tvFoodType);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvDishesStat = findViewById(R.id.tvDishesStat);
        tvOrdersStat = findViewById(R.id.tvOrdersStat);
        tvReviewsStat = findViewById(R.id.tvReviewsStat);
        tvAboutDescription = findViewById(R.id.tvAboutDescription);
        layoutLocation = findViewById(R.id.layoutLocation);
        tvLocation = findViewById(R.id.tvLocation);
        layoutAvailability = findViewById(R.id.layoutAvailability);
        tvAvailability = findViewById(R.id.tvAvailability);
        tvMenuItemCount = findViewById(R.id.tvMenuItemCount);
        layoutMenuItems = findViewById(R.id.layoutMenuItems);
        btnOrderNow = findViewById(R.id.btnOrderNow);
        layoutPaymentQr = findViewById(R.id.layoutPaymentQr);

        btnBack.setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnOrderNow.setText("View Cart");
        btnOrderNow.setOnClickListener(v -> {
            startActivity(new Intent(CookDetailsActivity.this, CartActivity.class));
        });

        tvTotalPrice.setText("₹0");
        refreshCartTotal();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartTotal();
    }

    /** Show cart total (all cooks) so multi-cook orders stay clear. */
    private void refreshCartTotal() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCart(token).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    runningTotal = response.body().getGrandTotal();
                    int count = response.body().getItemCount();
                    tvTotalPrice.setText(String.format(Locale.getDefault(), "₹%.0f", runningTotal));
                    if (count > 0) {
                        btnOrderNow.setText("View Cart (" + count + ")");
                    } else {
                        btnOrderNow.setText("View Cart");
                    }
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                // keep existing total
            }
        });
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
        CookProfile cook = response.getCook();
        if (cook == null) return;

        String kitchenName = cook.getKitchenName();
        tvKitchenName.setText(kitchenName != null ? kitchenName : cook.getFullName());
        tvRating.setText(String.format(Locale.getDefault(), "%.1f", cook.getRating()));
        tvReviewCount.setText(String.format(Locale.getDefault(), "(%d reviews)", cook.getTotalReviews()));
        tvFoodType.setText(cook.getFoodType() != null ? cook.getFoodType() : "Home-cooked meals");

        tvDishesStat.setText(String.valueOf(cook.getDishCount()));
        tvOrdersStat.setText(String.valueOf(cook.getTotalOrders()));
        tvReviewsStat.setText(String.valueOf(cook.getTotalReviews()));

        layoutVerifiedBadge.setVisibility(cook.isVerified() ? View.VISIBLE : View.GONE);

        ImageUrlHelper.load(ivCookImage, cook.getProfileImage(), R.drawable.avatar_cook);

        String about = cook.getDescription() != null && !cook.getDescription().isEmpty()
                ? cook.getDescription() : cook.getBio();
        if (about != null && !about.isEmpty()) {
            tvAboutDescription.setText(about);
            tvAboutDescription.setVisibility(View.VISIBLE);
        }

        if (cook.getAddress() != null && !cook.getAddress().isEmpty()) {
            tvLocation.setText(cook.getAddress());
            layoutLocation.setVisibility(View.VISIBLE);
        }

        String todayHours = getTodayOperatingHours(cook);
        if (todayHours != null) {
            tvAvailability.setText(todayHours);
            layoutAvailability.setVisibility(View.VISIBLE);
        }

        // ── Payment QR Section ─────────────────────────────────
        // Parse bank_details JSON and show QR codes for eSewa, Khalti, Bank
        String bankDetailsJson = cook.getBankDetails();
        if (bankDetailsJson != null && !bankDetailsJson.isEmpty() && !bankDetailsJson.equals("null")) {
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                BankDetails bankDetails = gson.fromJson(bankDetailsJson, BankDetails.class);
                if (bankDetails != null) {
                    populatePaymentQr(bankDetails);
                }
            } catch (Exception e) {
                // Invalid JSON, just hide the section
                if (layoutPaymentQr != null) layoutPaymentQr.setVisibility(View.GONE);
            }
        } else {
            if (layoutPaymentQr != null) layoutPaymentQr.setVisibility(View.GONE);
        }
    }

    /**
     * Populates the Payment QR section with eSewa, Khalti, and Bank QR codes
     */
    private void populatePaymentQr(BankDetails bankDetails) {
        if (layoutPaymentQr == null) return;

        boolean hasAnyQr = false;

        // Clear any previous dynamically added QR rows
        LinearLayout qrContainer = layoutPaymentQr.findViewById(R.id.qrContainer);
        if (qrContainer != null) qrContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        // eSewa QR
        if (bankDetails.getEsewaQrUrl() != null && !bankDetails.getEsewaQrUrl().isEmpty()) {
            addQrRow(qrContainer != null ? qrContainer : layoutPaymentQr, inflater,
                    "Pay via eSewa", bankDetails.getEsewaQrUrl());
            hasAnyQr = true;
        }

        // Khalti QR
        if (bankDetails.getKhaltiQrUrl() != null && !bankDetails.getKhaltiQrUrl().isEmpty()) {
            addQrRow(qrContainer != null ? qrContainer : layoutPaymentQr, inflater,
                    "Pay via Khalti", bankDetails.getKhaltiQrUrl());
            hasAnyQr = true;
        }

        // Bank QR
        if (bankDetails.getBankQrUrl() != null && !bankDetails.getBankQrUrl().isEmpty()) {
            addQrRow(qrContainer != null ? qrContainer : layoutPaymentQr, inflater,
                    "Pay via Bank Transfer", bankDetails.getBankQrUrl());
            hasAnyQr = true;
        }

        layoutPaymentQr.setVisibility(hasAnyQr ? View.VISIBLE : View.GONE);
    }

    private void addQrRow(android.view.ViewGroup container, LayoutInflater inflater, String label, String imageUrl) {
        View row = inflater.inflate(R.layout.item_payment_qr_row, container, false);
        if (row == null) return;

        TextView tvLabel = row.findViewById(R.id.tvQrLabel);
        ImageView ivQr = row.findViewById(R.id.ivQrImage);

        if (tvLabel != null) tvLabel.setText(label);
        if (ivQr != null && imageUrl != null && !imageUrl.isEmpty()) {
            // QR images are tall rectangles, not squares — cropping (the default for most
            // thumbnails in this app) would cut off the account name/number at the edges.
            ImageUrlHelper.loadNoCrop(ivQr, imageUrl, R.drawable.ic_image_placeholder);

            // Tap to view full-screen with a Save to Gallery option
            ivQr.setOnClickListener(v -> showQrFullScreen(label, imageUrl));
        }

        container.addView(row);
    }

    /**
     * Fetches the QR fresh at full resolution (not the downscaled thumbnail) so both the
     * full-screen preview and the gallery-saved file are sharp and scannable, uncropped.
     */
    private void showQrFullScreen(String label, String imageUrl) {
        ImageView qrView = new ImageView(this);
        qrView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        qrView.setAdjustViewBounds(true);
        qrView.setPadding(32, 32, 32, 32);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(label)
                .setView(qrView)
                .setPositiveButton("Save to Gallery", null)
                .setNegativeButton("Close", null)
                .show();

        final android.graphics.Bitmap[] loadedBitmap = new android.graphics.Bitmap[1];
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (loadedBitmap[0] != null) {
                saveQrToGallery(loadedBitmap[0], label);
            } else {
                Toast.makeText(this, "QR code is still loading — please try again in a moment.", Toast.LENGTH_SHORT).show();
            }
        });

        ImageUrlHelper.loadBitmap(this, imageUrl, new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
            @Override
            public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource,
                                         @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                loadedBitmap[0] = resource;
                qrView.setImageBitmap(resource);
            }

            @Override
            public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}

            @Override
            public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                Toast.makeText(CookDetailsActivity.this, "Failed to load QR code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveQrToGallery(android.graphics.Bitmap bitmap, String label) {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            Toast.makeText(this, "Storage permission needed — tap Save to Gallery again after allowing.", Toast.LENGTH_LONG).show();
            return;
        }

        String safeLabel = label.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "TiffinCraft_QR_" + safeLabel + "_" + System.currentTimeMillis() + ".png";
        boolean saved = com.tiffincraft.app.utils.ImageUtils.saveBitmapToGallery(this, bitmap, fileName);
        Toast.makeText(this, saved ? "QR code saved to Gallery" : "Failed to save QR code", Toast.LENGTH_SHORT).show();
    }

    private String getTodayOperatingHours(CookProfile cook) {
        if (cook.getOperatingHours() == null) return null;

        String[] dayKeys = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        int dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        CookProfile.DayHours today = cook.getOperatingHours().get(dayKeys[dayIndex]);
        if (today == null) return null;

        if (!today.isOpen()) return "Closed today";
        if (today.getOpen() == null || today.getClose() == null) return null;
        return "Available: " + today.getOpen() + " - " + today.getClose();
    }

    // ─────────────────────────────────────────────
    // LOAD MEALS
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
        tvMenuItemCount.setText(meals.size() + " items");

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

            ImageUrlHelper.load(imgMeal, meal.getImageUrl(), R.drawable.ic_food);

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
                    Toast.makeText(CookDetailsActivity.this,
                            meal.getName() + " added — you can also add meals from other cooks",
                            Toast.LENGTH_SHORT).show();
                    refreshCartTotal();
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
                // Silent fail
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
