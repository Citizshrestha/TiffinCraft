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
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCustomerProfileBinding;
import com.tiffincraft.app.models.CustomerProfile;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.activities.onboarding.SelectRoleActivity;
import com.tiffincraft.app.activities.order.OrderHistoryActivity;
import com.tiffincraft.app.utils.ImageUtils;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Set initial name from session while loading full profile
        String fullName = sessionManager.getFullName();
        if (binding.tvCustomerName != null) {
            binding.tvCustomerName.setText((fullName != null && !fullName.isEmpty()) ? fullName : "Customer");
        }

        // Load profile data from backend
        loadProfileData();

        // Profile image upload click listener
        if (binding.btnEditAvatar != null) {
            binding.btnEditAvatar.setOnClickListener(v -> checkPermissionAndOpenPicker());
        }

        // Setup bottom navigation
        setupBottomNavigation();

        // Settings button
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Order History
        if (binding.btnOrderHistory != null) {
            binding.btnOrderHistory.setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class))
            );
        }

        // Favorites
        if (binding.btnFavoriteCooks != null) {
            binding.btnFavoriteCooks.setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class))
            );
        }

        // Refer & Earn
        if (binding.btnReferEarn != null) {
            binding.btnReferEarn.setOnClickListener(v ->
                Toast.makeText(this, "Refer & Earn — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Edit Profile
        if (binding.menuEditProfile != null) {
            binding.menuEditProfile.setOnClickListener(v ->
                Toast.makeText(this, "Edit Profile — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Saved Addresses
        if (binding.menuSavedAddresses != null) {
            binding.menuSavedAddresses.setOnClickListener(v ->
                Toast.makeText(this, "Saved Addresses — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Payment Methods
        if (binding.menuPaymentMethods != null) {
            binding.menuPaymentMethods.setOnClickListener(v ->
                Toast.makeText(this, "Payment Methods — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Dietary Preferences
        if (binding.menuDietaryPreferences != null) {
            binding.menuDietaryPreferences.setOnClickListener(v ->
                Toast.makeText(this, "Dietary Preferences — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Logout
        if (binding.btnLogoutCustomer != null) {
            binding.btnLogoutCustomer.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        sessionManager.logout();
                        Intent intent = new Intent(this, SelectRoleActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    /**
     * Setup bottom navigation
     */
    private void setupBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);

            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, CustomerHomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    startActivity(new Intent(this, OrderHistoryActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_favorites) {
                    startActivity(new Intent(this, FavoritesActivity.class));
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
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            String formattedAmount = currencyFormat.format(profile.getTotalSpent());
            binding.tvTotalSpentCustomer.setText(formattedAmount);
        }

        // Member since
        if (binding.tvMemberSince != null && profile.getCreatedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM yyyy", Locale.US);
                Date date = inputFormat.parse(profile.getCreatedAt().substring(0, 10));
                if (date != null) {
                    binding.tvMemberSince.setText(outputFormat.format(date));
                }
            } catch (Exception e) {
                Log.e(TAG, "Date parse error", e);
                binding.tvMemberSince.setText("2025");
            }
        }

        // Address count (placeholder)
        if (binding.tvAddressCount != null) {
            binding.tvAddressCount.setText(profile.getAddress() != null && !profile.getAddress().isEmpty() ? "1" : "0");
        }
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
     * Load profile image using Glide
     */
    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image: " + imageUrl);

            Glide.with(this)
                .load(imageUrl)
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
        binding.btnEditAvatar.setEnabled(false);

        new Thread(() -> {
            compressedFile = ImageUtils.compressImage(CustomerProfileActivity.this, imageUri);

            if (compressedFile == null) {
                runOnUiThread(() -> {
                    showUploadProgress(false);
                    binding.btnEditAvatar.setEnabled(true);
                    Toast.makeText(CustomerProfileActivity.this,
                            "Failed to process image",
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            MultipartBody.Part imagePart = ImageUtils.prepareFilePart("profile_image", compressedFile);
            String token = "Bearer " + sessionManager.getToken();

            runOnUiThread(() -> {
                ApiService apiService = RetrofitClient.getInstance(CustomerProfileActivity.this)
                        .getApiService();

                apiService.uploadCustomerProfileImage(token, imagePart)
                        .enqueue(new Callback<UploadResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<UploadResponse> call,
                                                   @NonNull Response<UploadResponse> response) {
                                showUploadProgress(false);
                                binding.btnEditAvatar.setEnabled(true);
                                ImageUtils.deleteFile(compressedFile);

                                if (response.isSuccessful() && response.body() != null
                                        && response.body().isSuccess()) {

                                    String newImageUrl = response.body().getImageUrl();
                                    Log.d(TAG, "Upload successful: " + newImageUrl);

                                    sessionManager.saveProfileImage(newImageUrl);

                                    Toast.makeText(CustomerProfileActivity.this,
                                            "Profile photo updated!",
                                            Toast.LENGTH_SHORT).show();

                                } else {
                                    Log.e(TAG, "Upload failed: " + response.code());
                                    Toast.makeText(CustomerProfileActivity.this,
                                            "Upload failed. Try again.",
                                            Toast.LENGTH_SHORT).show();
                                    loadProfileImage(currentProfile != null ?
                                            currentProfile.getProfileImage() : null);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<UploadResponse> call,
                                                  @NonNull Throwable t) {
                                showUploadProgress(false);
                                binding.btnEditAvatar.setEnabled(true);
                                ImageUtils.deleteFile(compressedFile);

                                Log.e(TAG, "Upload failed", t);
                                Toast.makeText(CustomerProfileActivity.this,
                                        "Network error. Try again.",
                                        Toast.LENGTH_SHORT).show();
                                loadProfileImage(currentProfile != null ?
                                        currentProfile.getProfileImage() : null);
                            }
                        });
            });
        }).start();
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
