package com.tiffincraft.app.activities.cook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.tiffincraft.app.databinding.ActivityCookProfileBinding;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUtils;

import java.io.File;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookProfileActivity extends AppCompatActivity {

    private static final String TAG = "CookProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST_CODE = 1002;

    private ActivityCookProfileBinding binding;
    private SessionManager sessionManager;
    private Uri selectedImageUri;
    private File compressedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Display cook's name from session
        String fullName = sessionManager.getFullName();
        if (binding.tvCookName != null) {
            binding.tvCookName.setText(
                (fullName != null && !fullName.isEmpty()) ? fullName : "Home Cook"
            );
        }
        
        if (binding.tvKitchenNameProfile != null) {
            binding.tvKitchenNameProfile.setText(
                (fullName != null && !fullName.isEmpty()) ? fullName + "'s Kitchen" : "Home Cook Kitchen"
            );
        }

        // Load existing profile image
        loadProfileImage();

        // Profile image upload click listener
        if (binding.btnEditAvatar != null) {
            binding.btnEditAvatar.setOnClickListener(v -> checkPermissionAndOpenPicker());
        }

        if (binding.btnRotateAvatar != null) {
            binding.btnRotateAvatar.setOnClickListener(v -> rotateSavedPhoto());
        }

        // Settings button (account/support/logout live in Settings now)
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, CookSettingsActivity.class);
                startActivity(intent);
            });
        }

        // Add click listener for reviews
        if (binding.tvProfileRating != null) {
            binding.tvProfileRating.setOnClickListener(v -> {
                Intent intent = new Intent(this, CookReviewsActivity.class);
                startActivity(intent);
            });
        }

        // Kitchen open/closed toggle ↔ holiday mode (checked = open = holiday off)
        binding.switchKitchenStatusProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                updateKitchenStatus(isChecked);
            }
        });

        // Real data for stats, rating chip and kitchen details
        loadCookProfileData();
        loadEarningsTotals();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    /** Populate the profile screen from GET /cook/profile/me — no hardcoded values. */
    private void loadCookProfileData() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getMyCookProfile(token).enqueue(new Callback<com.tiffincraft.app.models.CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.CookProfileResponse> call,
                                   @NonNull Response<com.tiffincraft.app.models.CookProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !response.body().isSuccess() || response.body().getProfile() == null) {
                    Log.e(TAG, "Failed to load cook profile: " + response.code());
                    return;
                }
                com.tiffincraft.app.models.CookProfile profile = response.body().getProfile();

                if (profile.getKitchenName() != null && !profile.getKitchenName().isEmpty()) {
                    binding.tvKitchenNameProfile.setText(profile.getKitchenName());
                }

                // Rating chip: live review average + count
                java.text.DecimalFormat ratingFmt = new java.text.DecimalFormat("0.0");
                String ratingLabel = profile.getTotalReviews() > 0
                        ? ratingFmt.format(profile.getRating()) + " · " + profile.getTotalReviews() + " reviews"
                        : "No reviews yet";
                binding.tvProfileRating.setText(ratingLabel);

                // Joined date from users.created_at
                java.util.Date joined =
                        com.tiffincraft.app.models.ChatMessage.parseServerDate(profile.getUserCreatedAt());
                binding.tvJoinedDate.setText(joined != null
                        ? new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(joined)
                        : "—");

                // Kitchen details card
                binding.tvProfileFoodType.setText(orDash(profile.getFoodType()));
                binding.tvProfileCapacity.setText(profile.getCapacityPerDay() > 0
                        ? profile.getCapacityPerDay() + " meals/day" : "—");
                binding.tvProfilePhone.setText(orDash(profile.getPhone()));
                binding.tvProfileEmail.setText(orDash(profile.getEmail()));
                binding.tvProfileAddress.setText(orDash(profile.getAddress()));

                // Kitchen status toggle: open = not in holiday mode
                boolean open = !profile.isHolidayMode();
                binding.switchKitchenStatusProfile.setChecked(open);
                updateKitchenStatusText(open);
            }

            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.CookProfileResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "Cook profile load failed", t);
            }
        });
    }

    /** Populate the stats row from GET /orders/cook/earnings (all-time totals). */
    private void loadEarningsTotals() {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getCookEarningsTotals(token).enqueue(new Callback<com.tiffincraft.app.models.CookEarningsTotalsResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.CookEarningsTotalsResponse> call,
                                   @NonNull Response<com.tiffincraft.app.models.CookEarningsTotalsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getEarnings() != null) {
                    com.tiffincraft.app.models.CookEarningsTotalsResponse.Totals totals =
                            response.body().getEarnings();
                    binding.tvTotalOrdersProfile.setText(String.valueOf(totals.getTotalOrders()));
                    binding.tvTotalEarningsProfile.setText(
                            com.tiffincraft.app.utils.CurrencyUtils.formatRupees(totals.getTotalEarned()));
                } else {
                    Log.e(TAG, "Failed to load earnings totals: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.CookEarningsTotalsResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "Earnings totals load failed", t);
            }
        });
    }

    private static String orDash(String value) {
        return value != null && !value.trim().isEmpty() ? value : "—";
    }

    private void updateKitchenStatusText(boolean open) {
        binding.tvKitchenStatusProfile.setText(open ? "Kitchen is Open" : "Kitchen is Closed");
    }

    /** Toggle kitchen availability via the holiday-mode endpoint (open = holiday off). */
    private void updateKitchenStatus(boolean open) {
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("is_holiday_mode", !open);

        apiService.updateHolidayMode(token, body).enqueue(new Callback<com.tiffincraft.app.models.CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.CookProfileResponse> call,
                                   @NonNull Response<com.tiffincraft.app.models.CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    updateKitchenStatusText(open);
                    Toast.makeText(CookProfileActivity.this,
                            open ? "Kitchen is now open" : "Kitchen is now closed",
                            Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchKitchenStatusProfile.setChecked(!open);
                    Toast.makeText(CookProfileActivity.this,
                            "Could not update kitchen status.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.CookProfileResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "updateKitchenStatus failed", t);
                binding.switchKitchenStatusProfile.setChecked(!open);
                Toast.makeText(CookProfileActivity.this,
                        "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    
    private void loadProfileImage() {
        String imageUrl = sessionManager.getProfileImage();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image: " + imageUrl);
            
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.imgCookAvatar);
        } else {
            // Show default avatar
            Glide.with(this)
                .load(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.imgCookAvatar);
        }
    }

    
    private void checkPermissionAndOpenPicker() {
        // Android 13+ uses READ_MEDIA_IMAGES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } 
        // Android 6-12 uses READ_EXTERNAL_STORAGE
        else {
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
                        .into(binding.imgCookAvatar);

                // Start upload
                uploadProfileImage(selectedImageUri);
            }
        } else if (requestCode == 1003 && resultCode == RESULT_OK) {
            // Profile was updated, reload profile display
            String fullName = sessionManager.getFullName();
            if (binding.tvCookName != null) {
                binding.tvCookName.setText(
                    (fullName != null && !fullName.isEmpty()) ? fullName : "Home Cook"
                );
            }
            if (binding.tvKitchenNameProfile != null) {
                binding.tvKitchenNameProfile.setText(
                    (fullName != null && !fullName.isEmpty()) ? fullName + "'s Kitchen" : "Home Cook Kitchen"
                );
            }
            Toast.makeText(this, "Profile refreshed", Toast.LENGTH_SHORT).show();
        }
    }

  
    private void uploadProfileImage(Uri imageUri) {
        showUploadProgress(true);
        setAvatarButtonsEnabled(false);

        new Thread(() -> {
            File file = ImageUtils.compressImage(CookProfileActivity.this, imageUri);
            runOnUiThread(() -> {
                if (file == null) {
                    showUploadProgress(false);
                    setAvatarButtonsEnabled(true);
                    Toast.makeText(CookProfileActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                } else {
                    uploadFile(file);
                }
            });
        }).start();
    }

    /**
     * Manual rotate control: some source photos (downloaded/forwarded images,
     * screenshots re-saved by another app) carry no usable orientation
     * metadata at all, so no decoder — Glide included — can auto-detect that
     * they're sideways. This lets the cook fix an already-saved profile photo
     * directly, without re-picking it from their gallery.
     */
    private void rotateSavedPhoto() {
        String imageUrl = sessionManager.getProfileImage();
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Add a profile photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        showUploadProgress(true);
        setAvatarButtonsEnabled(false);

        new Thread(() -> {
            File file = ImageUtils.rotateRemoteImage(CookProfileActivity.this, imageUrl, 90);
            runOnUiThread(() -> {
                if (file == null) {
                    showUploadProgress(false);
                    setAvatarButtonsEnabled(true);
                    Toast.makeText(CookProfileActivity.this, "Couldn't rotate photo. Try again.", Toast.LENGTH_SHORT).show();
                } else {
                    uploadFile(file);
                }
            });
        }).start();
    }

    /** Shared upload path for both a freshly-picked photo and a rotated existing one. */
    private void uploadFile(File file) {
        compressedFile = file;
        MultipartBody.Part imagePart = ImageUtils.prepareFilePart("profile_image", file);
        String token = "Bearer " + sessionManager.getToken();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.uploadCookProfileImage(token, imagePart)
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
                            loadProfileImage();

                            Toast.makeText(CookProfileActivity.this,
                                    "Profile photo updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Upload failed: " + response.code());
                            Toast.makeText(CookProfileActivity.this,
                                    "Upload failed. Try again.", Toast.LENGTH_SHORT).show();
                            loadProfileImage();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                        showUploadProgress(false);
                        setAvatarButtonsEnabled(true);
                        ImageUtils.deleteFile(file);

                        Log.e(TAG, "Upload failed", t);
                        Toast.makeText(CookProfileActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                        loadProfileImage();
                    }
                });
    }

    private void setAvatarButtonsEnabled(boolean enabled) {
        if (binding.btnEditAvatar != null) binding.btnEditAvatar.setEnabled(enabled);
        if (binding.btnRotateAvatar != null) binding.btnRotateAvatar.setEnabled(enabled);
    }

    /**
     * Show/hide upload progress
     * You can add ProgressBar views to your layout and control them here
     */
    private void showUploadProgress(boolean show) {
        // If you have progress views in layout, show/hide them here
        // Example:
        // binding.uploadProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void setupBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
            
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_home) {
                    // Navigate to Cook Home
                    Intent intent = new Intent(CookProfileActivity.this, CookHomeActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_meals) {
                    startActivity(new Intent(CookProfileActivity.this, CookMealActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    startActivity(new Intent(CookProfileActivity.this, ManageOrdersActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                
                return false;
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compressedFile != null) {
            ImageUtils.deleteFile(compressedFile);
        }
    }
}
