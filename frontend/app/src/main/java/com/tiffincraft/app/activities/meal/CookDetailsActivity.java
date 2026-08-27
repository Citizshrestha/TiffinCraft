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
import com.tiffincraft.app.models.CreateCustomerSubscriptionRequest;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.Review;
import com.tiffincraft.app.models.ReviewResponse;
import com.tiffincraft.app.models.SubscriptionPlanResponse;
import com.tiffincraft.app.adapters.ReviewAdapter;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_COOK_ID = "cook_id";
    private static final int REQUEST_STORAGE_PERMISSION = 2001;
    private static final int REQUEST_EDIT_REVIEW = 3001;

    private ImageButton btnBack;
    private FrameLayout btnFavorite;
    private FrameLayout btnMessage;
    private ImageView imgFavoriteIcon;
    private ImageView ivCookImage;
    private View layoutVerifiedBadge;
    private TextView tvKitchenName, tvRating, tvReviewCount, tvFoodType, tvTotalPrice;
    private TextView tvDishesStat, tvOrdersStat, tvReviewsStat;
    private TextView tvAboutDescription, tvLocation, tvAvailability, tvMenuItemCount;
    private View layoutLocation, layoutAvailability;
    private LinearLayout layoutMenuItems;
    private View layoutSubscriptionPlansHeader;
    private LinearLayout layoutSubscriptionPlans;
    private View layoutComboDealsHeader;
    private LinearLayout layoutComboDeals;
    private androidx.cardview.widget.CardView layoutPaymentQr;
    private MaterialButton btnOrderNow;
    private RecyclerView rvReviews;
    private TextView tvNoReviews;
    private ReviewAdapter reviewAdapter;

    private int cookId = -1;
    private int cookUserId = -1;
    private String cookDisplayName;
    private String cookAvatarUrl;
    private String cookPhone;
    private String cookEsewaQrUrl;
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
        loadCookSubscriptionPlans();
        loadCookCombos();
        loadCookReviews();
        checkFavoriteStatus();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnMessage = findViewById(R.id.btnMessage);
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
        layoutSubscriptionPlansHeader = findViewById(R.id.layoutSubscriptionPlansHeader);
        layoutSubscriptionPlans = findViewById(R.id.layoutSubscriptionPlans);
        layoutComboDealsHeader = findViewById(R.id.layoutComboDealsHeader);
        layoutComboDeals = findViewById(R.id.layoutComboDeals);
        btnOrderNow = findViewById(R.id.btnOrderNow);
        layoutPaymentQr = findViewById(R.id.layoutPaymentQr);
        rvReviews = findViewById(R.id.rvReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);

        int myUserId = -1;
        try {
            myUserId = Integer.parseInt(sessionManager.getUserId());
        } catch (Exception ignored) {}

        reviewAdapter = new ReviewAdapter(this, new ArrayList<>(), new ReviewAdapter.OnReviewActionListener() {
            @Override
            public void onReplyClick(Review review) {}

            @Override
            public void onEditClick(Review review) {
                Intent intent = new Intent(CookDetailsActivity.this, com.tiffincraft.app.activities.common.RateReviewActivity.class);
                intent.putExtra(com.tiffincraft.app.activities.common.RateReviewActivity.EXTRA_REVIEW_ID, review.getId());
                intent.putExtra(com.tiffincraft.app.activities.common.RateReviewActivity.EXTRA_COOK_ID, cookId);
                intent.putExtra(com.tiffincraft.app.activities.common.RateReviewActivity.EXTRA_EXISTING_RATING, review.getRating());
                intent.putExtra(com.tiffincraft.app.activities.common.RateReviewActivity.EXTRA_EXISTING_COMMENT, review.getComment());
                startActivityForResult(intent, REQUEST_EDIT_REVIEW);
            }

            @Override
            public void onDeleteClick(Review review) {
                new AlertDialog.Builder(CookDetailsActivity.this)
                        .setTitle("Delete review?")
                        .setMessage("This can't be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> deleteReview(review))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }, true, myUserId);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnMessage.setOnClickListener(v -> openChatWithCook());

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

        cookUserId = cook.getUserId();
        cookAvatarUrl = cook.getProfileImage();
        cookPhone = cook.getPhone();

        String kitchenName = cook.getKitchenName();
        cookDisplayName = kitchenName != null ? kitchenName : cook.getFullName();
        tvKitchenName.setText(cookDisplayName);
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
                    cookEsewaQrUrl = bankDetails.getEsewaQrUrl();
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
    // SUBSCRIPTION PLANS — automatically shown for this cook, no extra config needed
    // ─────────────────────────────────────────────
    private void loadCookSubscriptionPlans() {
        apiService.getSubscriptionPlansByCook(cookId).enqueue(new Callback<SubscriptionPlanResponse>() {
            @Override
            public void onResponse(Call<SubscriptionPlanResponse> call, Response<SubscriptionPlanResponse> response) {
                List<SubscriptionPlanResponse.Plan> plans = response.isSuccessful() && response.body() != null
                        ? response.body().getPlans() : null;
                renderSubscriptionPlans(plans);
            }

            @Override
            public void onFailure(Call<SubscriptionPlanResponse> call, Throwable t) {
                renderSubscriptionPlans(null);
            }
        });
    }

    private void renderSubscriptionPlans(List<SubscriptionPlanResponse.Plan> plans) {
        layoutSubscriptionPlans.removeAllViews();

        if (plans == null || plans.isEmpty()) {
            layoutSubscriptionPlansHeader.setVisibility(View.GONE);
            layoutSubscriptionPlans.setVisibility(View.GONE);
            return;
        }

        layoutSubscriptionPlansHeader.setVisibility(View.VISIBLE);
        layoutSubscriptionPlans.setVisibility(View.VISIBLE);

        int bestValueIndex = pickBestValuePlan(plans);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < plans.size(); index++) {
            SubscriptionPlanResponse.Plan plan = plans.get(index);
            View row = inflater.inflate(R.layout.item_subscription_plan_public, layoutSubscriptionPlans, false);

            com.google.android.material.card.MaterialCardView card = row.findViewById(R.id.cardPlan);
            TextView tvRibbon = row.findViewById(R.id.tvBestValueRibbon);
            TextView tvName = row.findViewById(R.id.tvPlanName);
            TextView tvDuration = row.findViewById(R.id.tvPlanDuration);
            TextView tvDescription = row.findViewById(R.id.tvPlanDescription);
            TextView tvItems = row.findViewById(R.id.tvPlanItems);
            TextView tvPrice = row.findViewById(R.id.tvPlanPrice);
            TextView tvPerMeal = row.findViewById(R.id.tvPlanPerMeal);
            TextView tvOriginalPrice = row.findViewById(R.id.tvPlanOriginalPrice);
            TextView tvSavings = row.findViewById(R.id.tvPlanSavings);
            MaterialButton btnSubscribe = row.findViewById(R.id.btnSubscribePlan);
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

            tvName.setText(plan.getName());
            tvDuration.setText(plan.getDurationLabel());

            if (plan.getDescription() != null && !plan.getDescription().isEmpty()) {
                tvDescription.setText(plan.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            StringBuilder itemsSummary = new StringBuilder();
            if (plan.getItems() != null) {
                for (int i = 0; i < plan.getItems().size(); i++) {
                    SubscriptionPlanResponse.PlanItem item = plan.getItems().get(i);
                    if (i > 0) itemsSummary.append(", ");
                    itemsSummary.append(item.getQuantity()).append("x ").append(item.getName());
                    if (!item.isAvailable()) itemsSummary.append(" (unavailable)");
                }
            }
            tvItems.setText(itemsSummary.toString());

            tvPrice.setText(formatRupees(plan.getPricePerDelivery()));

            // Per-meal price is what actually makes two plans comparable — a
            // ₹1,400 plan and a ₹900 plan say nothing until you know how many
            // meals each one buys.
            int mealCount = countMeals(plan);
            if (mealCount > 1) {
                tvPerMeal.setText(String.format(Locale.getDefault(), "%s/meal · %d meals per cycle",
                        formatRupees(plan.getPricePerDelivery() / mealCount), mealCount));
            } else {
                tvPerMeal.setText("Per delivery cycle");
            }

            if (plan.getSavings() > 0) {
                tvOriginalPrice.setText(formatRupees(plan.getIndividualTotal()));
                tvOriginalPrice.setVisibility(View.VISIBLE);
                tvSavings.setText(String.format(Locale.getDefault(), "SAVE %s", formatRupees(plan.getSavings())));
                tvSavings.setVisibility(View.VISIBLE);
            } else {
                tvOriginalPrice.setVisibility(View.GONE);
                tvSavings.setVisibility(View.GONE);
            }

            // Recommended plan: amber stroke + ribbon, set here rather than in a
            // second layout file so there is only one card layout to maintain.
            if (index == bestValueIndex) {
                card.setStrokeWidth(dpToPx(2));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.sub_accent_amber_deep));
                tvRibbon.setVisibility(View.VISIBLE);
            } else {
                card.setStrokeWidth(dpToPx(1));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.sub_card_border));
                tvRibbon.setVisibility(View.GONE);
            }

            if (plan.isAvailable()) {
                row.setAlpha(1f);
                btnSubscribe.setEnabled(true);
                btnSubscribe.setText("Subscribe");
                btnSubscribe.setOnClickListener(v -> showSubscribeDialog(plan));
            } else {
                // Paused, or a meal in the plan is currently unavailable — matches
                // the same block already enforced server-side in createSubscription.
                row.setAlpha(0.6f);
                btnSubscribe.setEnabled(false);
                btnSubscribe.setText("Unavailable");
                btnSubscribe.setOnClickListener(null);
            }

            layoutSubscriptionPlans.addView(row);
        }
    }

    /**
     * Index of the plan to mark "Best Value", or -1 for none.
     *
     * Highest absolute savings wins; ties break to the cheaper plan so the badge
     * points at the lower commitment. Requires real savings and more than one
     * plan — badging the only plan on screen, or a plan that saves nothing, is
     * noise that trains people to ignore the badge.
     */
    private int pickBestValuePlan(List<SubscriptionPlanResponse.Plan> plans) {
        if (plans.size() < 2) return -1;

        int best = -1;
        for (int i = 0; i < plans.size(); i++) {
            SubscriptionPlanResponse.Plan p = plans.get(i);
            if (!p.isAvailable() || p.getSavings() <= 0) continue;
            if (best == -1) {
                best = i;
                continue;
            }
            SubscriptionPlanResponse.Plan current = plans.get(best);
            if (p.getSavings() > current.getSavings()
                    || (p.getSavings() == current.getSavings()
                        && p.getPricePerDelivery() < current.getPricePerDelivery())) {
                best = i;
            }
        }
        return best;
    }

    /** Total meals in one delivery cycle (quantities summed, not distinct dishes). */
    private int countMeals(SubscriptionPlanResponse.Plan plan) {
        if (plan.getItems() == null) return 0;
        int total = 0;
        for (SubscriptionPlanResponse.PlanItem item : plan.getItems()) {
            total += Math.max(1, item.getQuantity());
        }
        return total;
    }

    private String formatRupees(double amount) {
        return "₹" + String.format(Locale.getDefault(), "%,.0f", amount);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Explicit confirmation step before any money moves.
     *
     * Nothing is created on the server from here — that only happens once the
     * customer confirms, and even then the record is created as
     * pending_payment by /api/subscriptions/initiate.
     */
    private void showSubscribeDialog(SubscriptionPlanResponse.Plan plan) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_subscribe_plan, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView tvName = view.findViewById(R.id.tvDialogPlanName);
        TextView tvCookName = view.findViewById(R.id.tvDialogCookName);
        TextView tvMealCount = view.findViewById(R.id.tvDialogMealCount);
        TextView tvDialogItems = view.findViewById(R.id.tvDialogItems);
        TextView tvPrice = view.findViewById(R.id.tvDialogPrice);
        TextView tvOriginalPrice = view.findViewById(R.id.tvDialogOriginalPrice);
        tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        TextView tvDuration = view.findViewById(R.id.tvDialogDuration);
        TextView tvSavings = view.findViewById(R.id.tvDialogSavings);
        com.google.android.material.textfield.TextInputEditText etAddress = view.findViewById(R.id.etDialogAddress);
        MaterialButton btnCancel = view.findViewById(R.id.btnDialogCancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btnDialogConfirm);

        tvName.setText(plan.getName());

        String cookLabel = plan.getKitchenName() != null && !plan.getKitchenName().isEmpty()
                ? plan.getKitchenName()
                : (plan.getCookName() != null && !plan.getCookName().isEmpty()
                    ? plan.getCookName() : cookDisplayName);
        tvCookName.setText(cookLabel != null && !cookLabel.isEmpty() ? "from " + cookLabel : "");

        int mealCount = countMeals(plan);
        tvMealCount.setText(mealCount == 1 ? "1 meal" : mealCount + " meals");

        StringBuilder itemsSummary = new StringBuilder();
        if (plan.getItems() != null) {
            for (int i = 0; i < plan.getItems().size(); i++) {
                SubscriptionPlanResponse.PlanItem item = plan.getItems().get(i);
                if (i > 0) itemsSummary.append(", ");
                itemsSummary.append(item.getQuantity()).append("x ").append(item.getName());
            }
        }
        tvDialogItems.setText(itemsSummary.toString());

        tvPrice.setText(formatRupees(plan.getPricePerDelivery()));
        tvDuration.setText(plan.getDurationLabel());
        if (plan.getSavings() > 0) {
            tvOriginalPrice.setText(formatRupees(plan.getIndividualTotal()));
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvSavings.setText(String.format(Locale.getDefault(), "SAVE %s", formatRupees(plan.getSavings())));
            tvSavings.setVisibility(View.VISIBLE);
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
            tvSavings.setVisibility(View.GONE);
        }

        // Prefill with the customer's saved address, if any.
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(Call<CustomerProfileResponse> call, Response<CustomerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getProfile() != null
                        && response.body().getProfile().getAddress() != null) {
                    etAddress.setText(response.body().getProfile().getAddress());
                }
            }

            @Override
            public void onFailure(Call<CustomerProfileResponse> call, Throwable t) { /* leave blank */ }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            if (address.isEmpty()) {
                Toast.makeText(this, "Delivery address is required", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            startSubscriptionCheckout(plan, address);
        });

        dialog.show();
    }

    /**
     * Asks when deliveries should start, then starts the manual eSewa payment
     * flow. The subscription stays pending_payment until the customer uploads
     * proof and the cook verifies it.
     *
     * The date is the customer's to choose, and it survives verification — the
     * cook confirming payment today for a start date next week produces a
     * 'scheduled' subscription that activates on the day, not one that starts
     * immediately. Before this, the start date was hardcoded to *today*, which
     * silently discarded the choice and promised a meal for a day whose kitchen
     * cutoff had already passed.
     */
    private void startSubscriptionCheckout(SubscriptionPlanResponse.Plan plan, String deliveryAddress) {
        if (sessionManager.getToken() == null || sessionManager.getToken().isEmpty()) {
            Toast.makeText(this, "Your session expired. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }
        promptForStartDate(plan, deliveryAddress);
    }

    /**
     * Tomorrow by default, and never earlier: today's kitchen cutoff has already
     * passed by the time anyone is looking at this screen, so a same-day start
     * would commit the cook to a meal they can no longer be told about.
     *
     * Bounds are the device's calendar, which is only a UI convenience — the
     * server re-validates the date against Nepal Time and rejects anything past
     * +30 days or in the past, so a wrong device clock can't smuggle a bad date
     * through.
     */
    private void promptForStartDate(SubscriptionPlanResponse.Plan plan, String deliveryAddress) {
        long tomorrowMillis = com.tiffincraft.app.utils.DeliveryDateUtils.deviceMillisPlusDays(1);

        java.util.Calendar initial = java.util.Calendar.getInstance();
        initial.setTimeInMillis(tomorrowMillis);

        android.app.DatePickerDialog picker = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String chosen = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    submitSubscription(plan, deliveryAddress, chosen);
                },
                initial.get(java.util.Calendar.YEAR),
                initial.get(java.util.Calendar.MONTH),
                initial.get(java.util.Calendar.DAY_OF_MONTH));

        picker.setTitle("First delivery day");
        picker.getDatePicker().setMinDate(tomorrowMillis);
        picker.getDatePicker().setMaxDate(com.tiffincraft.app.utils.DeliveryDateUtils.deviceMillisPlusDays(30));
        picker.show();
    }

    private void submitSubscription(SubscriptionPlanResponse.Plan plan, String deliveryAddress, String startDate) {
        String token = "Bearer " + sessionManager.getToken();
        CreateCustomerSubscriptionRequest request = new CreateCustomerSubscriptionRequest(
                plan.getId(), deliveryAddress, startDate);

        apiService.createSubscription(token, request).enqueue(new Callback<com.tiffincraft.app.models.CreateSubscriptionResponse>() {
            @Override
            public void onResponse(Call<com.tiffincraft.app.models.CreateSubscriptionResponse> call,
                                   Response<com.tiffincraft.app.models.CreateSubscriptionResponse> response) {
                com.tiffincraft.app.models.CreateSubscriptionResponse body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getSubscriptionId() > 0) {
                    Intent intent = new Intent(CookDetailsActivity.this,
                            com.tiffincraft.app.activities.customer.SubscriptionPaymentActivity.class);
                    intent.putExtra(com.tiffincraft.app.activities.customer.SubscriptionPaymentActivity.EXTRA_SUBSCRIPTION_ID,
                            body.getSubscriptionId());
                    intent.putExtra(com.tiffincraft.app.activities.customer.SubscriptionPaymentActivity.EXTRA_PLAN_NAME,
                            plan.getName());
                    intent.putExtra(com.tiffincraft.app.activities.customer.SubscriptionPaymentActivity.EXTRA_PLAN_PRICE,
                            plan.getPricePerDelivery());
                    intent.putExtra(com.tiffincraft.app.activities.customer.SubscriptionPaymentActivity.EXTRA_PLAN_DURATION,
                            plan.getDuration());
                    intent.putExtra(com.tiffincraft.app.activities.customer.SubscriptionPaymentActivity.EXTRA_COOK_ESEWA_QR_URL,
                            cookEsewaQrUrl);
                    startActivity(intent);
                    return;
                }

                String message = body != null && body.getMessage() != null
                        ? body.getMessage() : "Could not start subscription payment. Please try again.";
                Toast.makeText(CookDetailsActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<com.tiffincraft.app.models.CreateSubscriptionResponse> call, Throwable t) {
                Toast.makeText(CookDetailsActivity.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─────────────────────────────────────────────
    // COMBO DEALS — one-time bundles at a fixed price, distinct from the
    // recurring Subscription Plans above (see comboController.js).
    // ─────────────────────────────────────────────
    private void loadCookCombos() {
        apiService.getCombosByCook(cookId).enqueue(new Callback<com.tiffincraft.app.models.ComboResponse>() {
            @Override
            public void onResponse(Call<com.tiffincraft.app.models.ComboResponse> call, Response<com.tiffincraft.app.models.ComboResponse> response) {
                List<com.tiffincraft.app.models.ComboResponse.Combo> combos = response.isSuccessful() && response.body() != null
                        ? response.body().getCombos() : null;
                renderCombos(combos);
            }

            @Override
            public void onFailure(Call<com.tiffincraft.app.models.ComboResponse> call, Throwable t) {
                renderCombos(null);
            }
        });
    }

    private void renderCombos(List<com.tiffincraft.app.models.ComboResponse.Combo> combos) {
        layoutComboDeals.removeAllViews();

        if (combos == null || combos.isEmpty()) {
            layoutComboDealsHeader.setVisibility(View.GONE);
            layoutComboDeals.setVisibility(View.GONE);
            return;
        }

        layoutComboDealsHeader.setVisibility(View.VISIBLE);
        layoutComboDeals.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (com.tiffincraft.app.models.ComboResponse.Combo combo : combos) {
            View row = inflater.inflate(R.layout.item_combo_deal_public, layoutComboDeals, false);

            ImageView ivCombo = row.findViewById(R.id.ivComboImage);
            TextView tvName = row.findViewById(R.id.tvComboName);
            TextView tvSavings = row.findViewById(R.id.tvComboSavings);
            TextView tvDescription = row.findViewById(R.id.tvComboDescription);
            TextView tvItems = row.findViewById(R.id.tvComboItems);
            TextView tvPrice = row.findViewById(R.id.tvComboPrice);
            MaterialButton btnBuy = row.findViewById(R.id.btnBuyCombo);

            tvName.setText(combo.getName());

            // No dedicated combo image upload — reuses its first item's real
            // meal photo as the thumbnail, same data already shown for that
            // meal elsewhere, no new upload flow needed.
            String thumbnailUrl = combo.getItems() != null && !combo.getItems().isEmpty()
                    ? combo.getItems().get(0).getImageUrl() : null;
            com.tiffincraft.app.utils.ImageUrlHelper.load(ivCombo, thumbnailUrl, R.drawable.ic_food);

            if (combo.getSavings() > 0) {
                tvSavings.setText(String.format(Locale.getDefault(), "Save ₹%.0f", combo.getSavings()));
                tvSavings.setVisibility(View.VISIBLE);
            }

            if (combo.getDescription() != null && !combo.getDescription().isEmpty()) {
                tvDescription.setText(combo.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            }

            StringBuilder itemsSummary = new StringBuilder();
            if (combo.getItems() != null) {
                for (int i = 0; i < combo.getItems().size(); i++) {
                    com.tiffincraft.app.models.ComboResponse.ComboItem item = combo.getItems().get(i);
                    if (i > 0) itemsSummary.append(", ");
                    itemsSummary.append(item.getQuantity()).append("x ").append(item.getName());
                    if (!item.isAvailable()) itemsSummary.append(" (unavailable)");
                }
            }
            tvItems.setText(itemsSummary.toString());

            tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f", combo.getPrice()));

            if (combo.isAvailable()) {
                row.setAlpha(1f);
                btnBuy.setEnabled(true);
                btnBuy.setText("Buy Now");
                btnBuy.setOnClickListener(v -> showBuyComboDialog(combo));
            } else {
                // Paused, or one of its meals is 86'd — matches the same block
                // buyCombo() already enforces server-side.
                row.setAlpha(0.6f);
                btnBuy.setEnabled(false);
                btnBuy.setText("Unavailable");
                btnBuy.setOnClickListener(null);
            }

            layoutComboDeals.addView(row);
        }
    }

    private void showBuyComboDialog(com.tiffincraft.app.models.ComboResponse.Combo combo) {
        android.widget.EditText etAddress = new android.widget.EditText(this);
        etAddress.setHint("Delivery address");
        etAddress.setMinLines(2);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etAddress.setPadding(pad, pad, pad, pad);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(Call<CustomerProfileResponse> call, Response<CustomerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getProfile() != null
                        && response.body().getProfile().getAddress() != null) {
                    etAddress.setText(response.body().getProfile().getAddress());
                }
            }

            @Override
            public void onFailure(Call<CustomerProfileResponse> call, Throwable t) { /* leave blank */ }
        });

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(etAddress);

        new AlertDialog.Builder(this)
                .setTitle("Buy \"" + combo.getName() + "\"")
                .setMessage(String.format(Locale.getDefault(),
                        "₹%.0f · one-time order\n\nAfter continuing, pay by eSewa, Khalti, Fonepay, or bank transfer and upload your payment screenshot. The cook confirms the order after verifying it.",
                        combo.getPrice()))
                .setView(container)
                .setPositiveButton("Continue to Payment", (dialog, which) -> {
                    String address = etAddress.getText().toString().trim();
                    if (address.isEmpty()) {
                        Toast.makeText(this, "Delivery address is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    buyCombo(combo, address, "online");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void buyCombo(com.tiffincraft.app.models.ComboResponse.Combo combo, String deliveryAddress, String paymentMethod) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.buyCombo(token, combo.getId(),
                new com.tiffincraft.app.models.BuyComboRequest(deliveryAddress, paymentMethod, null)
        ).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    if ("online".equals(paymentMethod)) {
                        if (response.body().getOrderId() <= 0) {
                            Toast.makeText(CookDetailsActivity.this,
                                    "Could not start payment. Please open the order from My Orders.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent intent = new Intent(CookDetailsActivity.this,
                                com.tiffincraft.app.activities.order.OrderDetailsCustomerActivity.class);
                        intent.putExtra("order_id", response.body().getOrderId());
                        startActivity(intent);
                        return;
                    }
                    Toast.makeText(CookDetailsActivity.this,
                            "Order placed for \"" + combo.getName() + "\"!", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Failed to place combo order";
                    Toast.makeText(CookDetailsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(CookDetailsActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────
    // LOAD REVIEWS
    // ─────────────────────────────────────────────
    private void loadCookReviews() {
        apiService.getCookReviews(cookId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                List<Review> reviews = response.isSuccessful() && response.body() != null
                        ? response.body().getReviews() : null;
                if (reviews == null || reviews.isEmpty()) {
                    tvNoReviews.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.GONE);
                } else {
                    tvNoReviews.setVisibility(View.GONE);
                    rvReviews.setVisibility(View.VISIBLE);
                    reviewAdapter.updateReviews(reviews);
                }
            }

            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                tvNoReviews.setVisibility(View.VISIBLE);
                rvReviews.setVisibility(View.GONE);
            }
        });
    }

    private void deleteReview(Review review) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteReview(token, review.getId()).enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
            @Override
            public void onResponse(Call<com.tiffincraft.app.models.RegisterResponse> call,
                                    Response<com.tiffincraft.app.models.RegisterResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CookDetailsActivity.this, "Review deleted", Toast.LENGTH_SHORT).show();
                    loadCookReviews();
                    loadCookProfile(); // refresh the cook's average rating
                } else {
                    Toast.makeText(CookDetailsActivity.this, "Failed to delete review", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.tiffincraft.app.models.RegisterResponse> call, Throwable t) {
                Toast.makeText(CookDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_REVIEW && resultCode == RESULT_OK) {
            loadCookReviews();
        }
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

    // ─────────────────────────────────────────────
    // MESSAGE COOK
    // ─────────────────────────────────────────────
    private void openChatWithCook() {
        if (cookUserId <= 0) {
            Toast.makeText(this, "Cook details still loading — try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + sessionManager.getToken();
        apiService.createChatConversation(token,
                        new com.tiffincraft.app.models.CreateConversationRequest(cookUserId))
                .enqueue(new Callback<com.tiffincraft.app.models.CreateConversationResponse>() {
                    @Override
                    public void onResponse(Call<com.tiffincraft.app.models.CreateConversationResponse> call,
                                            Response<com.tiffincraft.app.models.CreateConversationResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getConversation() != null) {
                            startChatActivity(response.body().getConversation().getId());
                        } else {
                            Toast.makeText(CookDetailsActivity.this, "Could not start conversation.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.tiffincraft.app.models.CreateConversationResponse> call, Throwable t) {
                        Toast.makeText(CookDetailsActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startChatActivity(int conversationId) {
        Intent intent = new Intent(this, com.tiffincraft.app.activities.common.ChatActivity.class);
        intent.putExtra(com.tiffincraft.app.activities.common.ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(com.tiffincraft.app.activities.common.ChatActivity.EXTRA_CONTACT_NAME, cookDisplayName);
        intent.putExtra(com.tiffincraft.app.activities.common.ChatActivity.EXTRA_CONTACT_ID, cookUserId);
        intent.putExtra(com.tiffincraft.app.activities.common.ChatActivity.EXTRA_CONTACT_ROLE, "cook");
        intent.putExtra(com.tiffincraft.app.activities.common.ChatActivity.EXTRA_CONTACT_PHONE, cookPhone);
        intent.putExtra(com.tiffincraft.app.activities.common.ChatActivity.EXTRA_CONTACT_AVATAR, cookAvatarUrl);
        startActivity(intent);
    }

    private void updateFavoriteIcon() {
        imgFavoriteIcon.setImageResource(R.drawable.ic_favorite);
        imgFavoriteIcon.setColorFilter(ContextCompat.getColor(this,
                isFavorite ? R.color.red : android.R.color.darker_gray));
    }
}
