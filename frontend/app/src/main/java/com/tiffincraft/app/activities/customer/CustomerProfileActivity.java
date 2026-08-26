package com.tiffincraft.app.activities.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCustomerProfileBinding;
import com.tiffincraft.app.models.CustomerProfile;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionPlanResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.activities.order.OrderHistoryActivity;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.ImageUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerProfileActivity extends AppCompatActivity {

    private static final String TAG = "CustomerProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST_CODE = 1002;

    private ActivityCustomerProfileBinding binding;
    private SessionManager sessionManager;
    private Uri selectedImageUri;
    private File compressedFile;
    private CustomerProfile currentProfile;
    private SubscriptionResponse.Subscription activeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Deep-green status bar to blend with the immersive header
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.forest_deep));
        getWindow().getDecorView().setSystemUiVisibility(0);

        sessionManager = new SessionManager(this);

        String fullName = sessionManager.getFullName();
        if (binding.tvCustomerName != null) {
            binding.tvCustomerName.setText((fullName != null && !fullName.isEmpty()) ? fullName : "Customer");
        }

        String cachedImage = sessionManager.getProfileImage();
        if (cachedImage != null && !cachedImage.isEmpty()) {
            loadProfileImage(cachedImage);
        }

        loadProfileData();
        loadSubscription();

        if (binding.cardSubscription != null) {
            binding.cardSubscription.setVisibility(View.GONE);
        }

        if (binding.btnManageSubscription != null) {
            binding.btnManageSubscription.setOnClickListener(v -> showManageSubscriptionDialog());
        }

        if (binding.tvViewAllSubscriptions != null) {
            binding.tvViewAllSubscriptions.setOnClickListener(v ->
                    startActivity(new Intent(this, CustomerSubscriptionsActivity.class)));
        }

        if (binding.btnEditAvatar != null) {
            binding.btnEditAvatar.setOnClickListener(v -> checkPermissionAndOpenPicker());
        }

        setupBottomNavigation();

        // Settings button (account/preferences/support/logout live in Settings now)
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, CustomerSettingsActivity.class))
            );
        }

        if (binding.btnOrderHistory != null) {
            binding.btnOrderHistory.setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class))
            );
        }

        if (binding.btnFavoriteCooks != null) {
            binding.btnFavoriteCooks.setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class))
            );
        }

        if (binding.btnReferEarn != null) {
            binding.btnReferEarn.setOnClickListener(v ->
                startActivity(new Intent(this, ReferEarnActivity.class))
            );
        }
    }


    private void setupBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);

            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, CustomerHomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_menu) {
                    startActivity(new Intent(this, CustomerMenuActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_cart) {
                    startActivity(new Intent(this, com.tiffincraft.app.activities.common.CartActivity.class));
                    return false;
                } else if (itemId == R.id.nav_orders) {
                    startActivity(new Intent(this, OrderHistoryActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }

                return false;
            });
        }
    }

    /**
     * Load customer profile data from backend
     */
    private void loadProfileData() {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                   @NonNull Response<CustomerProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {

                    currentProfile = response.body().getProfile();
                    populateProfileData(currentProfile);

                    // Update session cache with latest image URL from backend
                    if (currentProfile != null && currentProfile.getProfileImage() != null
                            && !currentProfile.getProfileImage().isEmpty()) {
                        sessionManager.saveProfileImage(currentProfile.getProfileImage());
                    }

                } else {
                    Log.e(TAG, "Failed to load profile: " + response.code());
                    Toast.makeText(CustomerProfileActivity.this,
                            "Failed to load profile data",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CustomerProfileResponse> call,
                                  @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading profile", t);
                Toast.makeText(CustomerProfileActivity.this,
                        "Network error. Please check your connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Populate UI with profile data
     */
    private void populateProfileData(CustomerProfile profile) {
        if (profile == null) return;

        // Name
        if (binding.tvCustomerName != null) {
            binding.tvCustomerName.setText(profile.getFullName() != null ?
                    profile.getFullName() : "Customer");
        }

        // Contact info (phone · email)
        if (binding.tvCustomerContact != null) {
            String contact = "";
            if (profile.getPhone() != null) {
                contact = profile.getPhone();
            }
            if (profile.getEmail() != null) {
                contact += (contact.isEmpty() ? "" : " · ") + profile.getEmail();
            }
            binding.tvCustomerContact.setText(contact.isEmpty() ? "No contact info" : contact);
        }

        // Profile image
        loadProfileImage(profile.getProfileImage());

        // Total orders
        if (binding.tvTotalOrdersCustomer != null) {
            binding.tvTotalOrdersCustomer.setText(String.valueOf(profile.getTotalOrders()));
        }

        // Foodie level (based on orders)
        if (binding.tvFoodieLevel != null) {
            String level = getFoodieLevelByOrders(profile.getTotalOrders());
            binding.tvFoodieLevel.setText(level + " · " + profile.getTotalOrders() + " orders");
        }

        // Total spent
        if (binding.tvTotalSpentCustomer != null) {
            binding.tvTotalSpentCustomer.setText(CurrencyUtils.formatRupees(profile.getTotalSpent()));
        }

        // Member since
        if (binding.tvMemberSince != null && profile.getCreatedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM yyyy", Locale.US);
                Date date = inputFormat.parse(profile.getCreatedAt().substring(0, 10));
                if (date != null) {
                    binding.tvMemberSince.setText(outputFormat.format(date));
                }
            } catch (Exception e) {
                Log.e(TAG, "Date parse error", e);
                binding.tvMemberSince.setText("2025");
            }
        }

        // Account Details card
        if (binding.tvProfilePhoneCustomer != null) {
            binding.tvProfilePhoneCustomer.setText(orDash(profile.getPhone()));
        }
        if (binding.tvProfileEmailCustomer != null) {
            binding.tvProfileEmailCustomer.setText(orDash(profile.getEmail()));
        }
        if (binding.tvProfileAddressCustomer != null) {
            binding.tvProfileAddressCustomer.setText(orDash(profile.getAddress()));
        }
    }

    private static String orDash(String value) {
        return value != null && !value.trim().isEmpty() ? value : "—";
    }

    /**
     * Load the customer's active/paused subscription (if any) and bind it to the
     * subscription card. The card stays hidden — there is no fake fallback — when
     * the customer has no subscription row in the backend.
     */
    private void loadSubscription() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getMySubscriptions(token).enqueue(new Callback<SubscriptionResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionResponse> call,
                                   @NonNull Response<SubscriptionResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<SubscriptionResponse.Subscription> subs = response.body().getSubscriptions();
                    activeSubscription = null;
                    if (subs != null) {
                        for (SubscriptionResponse.Subscription sub : subs) {
                            if (!"cancelled".equals(sub.getStatus())) {
                                activeSubscription = sub;
                                break;
                            }
                        }
                    }
                    populateSubscriptionCard(activeSubscription);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load subscription", t);
            }
        });
    }

    private void populateSubscriptionCard(SubscriptionResponse.Subscription sub) {
        if (binding.cardSubscription == null) return;

        if (sub == null || sub.getPlan() == null) {
            binding.cardSubscription.setVisibility(View.GONE);
            return;
        }

        binding.cardSubscription.setVisibility(View.VISIBLE);

        SubscriptionPlanResponse.Plan plan = sub.getPlan();
        String kitchen = plan.getKitchenName() != null && !plan.getKitchenName().isEmpty()
                ? plan.getKitchenName() : plan.getCookName();

        // Not yet activated — payment hasn't been verified, so "Active"/"Renews"
        // would be misleading (next_delivery_date is just a placeholder until then).
        if ("pending_payment".equals(sub.getStatus())) {
            String statusLabel;
            switch (sub.getPaymentStatus() != null ? sub.getPaymentStatus() : "pending") {
                case "submitted": statusLabel = "Awaiting Verification"; break;
                case "rejected": statusLabel = "Payment Rejected"; break;
                default: statusLabel = "Payment Pending"; break;
            }
            binding.tvSubscriptionStatus.setText(plan.getName() + " Subscription — " + statusLabel);
            binding.tvSubscriptionDetail.setText("From " + kitchen + " · Tap Manage to complete payment");
            return;
        }

        String statusLabel = "paused".equals(sub.getStatus()) ? "Paused" : "Active";
        binding.tvSubscriptionStatus.setText(plan.getName() + " Subscription " + statusLabel);

        String renewLabel = "paused".equals(sub.getStatus()) ? "Paused" : "Renews " + formatShortDate(sub.getNextDeliveryDate());
        binding.tvSubscriptionDetail.setText("From " + kitchen + " · " + renewLabel);
    }

    private String formatShortDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "—";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM", Locale.US);
            Date date = inputFormat.parse(isoDate.substring(0, 10));
            return date != null ? outputFormat.format(date) : isoDate;
        } catch (Exception e) {
            return isoDate;
        }
    }

    private void showManageSubscriptionDialog() {
        if (activeSubscription == null) return;

        // Not yet activated — pause/resume don't apply; route to the payment
        // screen (or just offer to cancel while it's still stuck).
        if ("pending_payment".equals(activeSubscription.getStatus())) {
            String[] options = {
                    "rejected".equals(activeSubscription.getPaymentStatus()) ? "Re-upload payment proof" : "Complete payment",
                    "Cancel subscription"
            };
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Manage Subscription")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openSubscriptionPayment(activeSubscription);
                        } else {
                            confirmCancelSubscription();
                        }
                    })
                    .show();
            return;
        }

        boolean isPaused = "paused".equals(activeSubscription.getStatus());
        String[] options = { isPaused ? "Resume subscription" : "Pause subscription", "Cancel subscription" };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Manage Subscription")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (isPaused) resumeSubscription(); else pauseSubscription();
                    } else {
                        confirmCancelSubscription();
                    }
                })
                .show();
    }

    /** Submitted subscriptions can't upload again (see uploadSubscriptionScreenshot's
     *  ["pending","rejected"] guard on the backend) — the payment screen still opens
     *  so the customer can see its live status either way. */
    private void openSubscriptionPayment(SubscriptionResponse.Subscription sub) {
        if (sub.getPlan() == null) return;
        Intent intent = new Intent(this, SubscriptionPaymentActivity.class);
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_SUBSCRIPTION_ID, sub.getId());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PLAN_NAME, sub.getPlan().getName());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PLAN_PRICE, sub.getPlan().getPricePerDelivery());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PLAN_DURATION, sub.getPlan().getDuration());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_COOK_ESEWA_QR_URL, sub.getCookEsewaQrUrl());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PAYMENT_STATUS, sub.getPaymentStatus());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_VERIFICATION_NOTES, sub.getVerificationNotes());
        startActivity(intent);
    }

    private void pauseSubscription() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.pauseSubscription(token, activeSubscription.getId()).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                Toast.makeText(CustomerProfileActivity.this, "Subscription paused", Toast.LENGTH_SHORT).show();
                loadSubscription();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CustomerProfileActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resumeSubscription() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.resumeSubscription(token, activeSubscription.getId()).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                Toast.makeText(CustomerProfileActivity.this, "Subscription resumed", Toast.LENGTH_SHORT).show();
                loadSubscription();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CustomerProfileActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmCancelSubscription() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Subscription")
                .setMessage("Are you sure you want to cancel this subscription? This cannot be undone.")
                .setPositiveButton("Cancel Subscription", (dialog, which) -> cancelSubscription())
                .setNegativeButton("Keep it", null)
                .show();
    }

    private void cancelSubscription() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.cancelSubscription(token, activeSubscription.getId()).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                Toast.makeText(CustomerProfileActivity.this, "Subscription cancelled", Toast.LENGTH_SHORT).show();
                loadSubscription();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CustomerProfileActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Get foodie level badge based on order count
     */
    private String getFoodieLevelByOrders(int orders) {
        if (orders >= 100) return "Platinum Foodie";
        if (orders >= 50) return "Gold Foodie";
        if (orders >= 20) return "Silver Foodie";
        if (orders >= 5) return "Bronze Foodie";
        return "New Foodie";
    }

    /**
     * Build full URL from relative path returned by backend
     * Backend returns: /uploads/profiles/abc.jpg
     * App needs: http://192.168.100.115:5000/uploads/profiles/abc.jpg
     */
    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        // Already a full URL (http:// or https://)
        if (imageUrl.startsWith("http")) return imageUrl;
        // Relative path — prepend server base
        return RetrofitClient.SERVER_URL + imageUrl;
    }

    /**
     * Load profile image using Glide
     */
    private void loadProfileImage(String imageUrl) {
        String fullUrl = getFullImageUrl(imageUrl);
        if (fullUrl != null) {
            Log.d(TAG, "Loading profile image: " + fullUrl);

            GlideUrl glideUrl = new GlideUrl(fullUrl, new LazyHeaders.Builder()
                .addHeader("Bypass-Tunnel-Reminder", "true")
                .build());

            Glide.with(this)
                .load(glideUrl)
                .placeholder(R.drawable.avatar_customer)
                .error(R.drawable.avatar_customer)
                .circleCrop()
                .into(binding.imgCustomerAvatar);
        } else {
            Glide.with(this)
                .load(R.drawable.avatar_customer)
                .circleCrop()
                .into(binding.imgCustomerAvatar);
        }
    }

    /**
     * Check storage permission and open image picker
     */
    private void checkPermissionAndOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission needed to select photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Open image picker
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Select Profile Photo"),
                PICK_IMAGE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                // Show preview immediately
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .into(binding.imgCustomerAvatar);

                // Start upload
                uploadProfileImage(selectedImageUri);
            }
        }
    }

    /**
     * Upload profile image to server
     */
    private void uploadProfileImage(Uri imageUri) {
        showUploadProgress(true);
        setAvatarButtonsEnabled(false);

        new Thread(() -> {
            File file = ImageUtils.compressImage(CustomerProfileActivity.this, imageUri);
            runOnUiThread(() -> {
                if (file == null) {
                    showUploadProgress(false);
                    setAvatarButtonsEnabled(true);
                    Toast.makeText(CustomerProfileActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                } else {
                    uploadFile(file);
                }
            });
        }).start();
    }

    /** Shared upload path for a freshly-picked photo. */
    private void uploadFile(File file) {
        compressedFile = file;
        MultipartBody.Part imagePart = ImageUtils.prepareFilePart("profile_image", file);
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.uploadCustomerProfileImage(token, imagePart)
                .enqueue(new Callback<UploadResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<UploadResponse> call,
                                           @NonNull Response<UploadResponse> response) {
                        showUploadProgress(false);
                        setAvatarButtonsEnabled(true);
                        ImageUtils.deleteFile(file);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {

                            String newImageUrl = response.body().getData().getUrl();
                            Log.d(TAG, "Upload successful: " + newImageUrl);

                            sessionManager.saveProfileImage(newImageUrl);
                            if (currentProfile != null) {
                                currentProfile.setProfileImage(newImageUrl);
                            }
                            loadProfileImage(newImageUrl);

                            Toast.makeText(CustomerProfileActivity.this,
                                    "Profile photo updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Upload failed: " + response.code());
                            Toast.makeText(CustomerProfileActivity.this,
                                    "Upload failed. Try again.", Toast.LENGTH_SHORT).show();
                            loadProfileImage(currentProfile != null ? currentProfile.getProfileImage() : null);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                        showUploadProgress(false);
                        setAvatarButtonsEnabled(true);
                        ImageUtils.deleteFile(file);

                        Log.e(TAG, "Upload failed", t);
                        Toast.makeText(CustomerProfileActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                        loadProfileImage(currentProfile != null ? currentProfile.getProfileImage() : null);
                    }
                });
    }

    private void setAvatarButtonsEnabled(boolean enabled) {
        if (binding.btnEditAvatar != null) binding.btnEditAvatar.setEnabled(enabled);
    }

    private void showLoading(boolean show) {
        // You can add a ProgressBar to your layout and control it here
        // For now, just disable interactions
        if (binding.btnEditAvatar != null) {
            binding.btnEditAvatar.setEnabled(!show);
        }
    }

    private void showUploadProgress(boolean show) {
        // Add progress indicator if needed in layout
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compressedFile != null) {
            ImageUtils.deleteFile(compressedFile);
        }
    }
}
