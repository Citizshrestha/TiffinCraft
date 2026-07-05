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
import com.tiffincraft.app.activities.onboarding.SelectRoleActivity;
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

        // Settings button
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, CookSettingsActivity.class);
                startActivity(intent);
            });
        }

        // Edit profile menu item
        if (binding.menuEditKitchenProfile != null) {
            binding.menuEditKitchenProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditCookProfileActivity.class);
                startActivityForResult(intent, 1003);
            });
        }

        // Logout button
        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v -> {
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
        
        // Setup other menu items
        setupMenuItems();

        // Add click listener for reviews
        if (binding.tvProfileRating != null) {
            binding.tvProfileRating.setOnClickListener(v -> {
                Intent intent = new Intent(this, CookReviewsActivity.class);
                startActivity(intent);
            });
        }

        // Setup bottom navigation
        setupBottomNavigation();
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
        binding.btnEditAvatar.setEnabled(false);

        new Thread(() -> {
            compressedFile = ImageUtils.compressImage(CookProfileActivity.this, imageUri);

            if (compressedFile == null) {
                runOnUiThread(() -> {
                    showUploadProgress(false);
                    binding.btnEditAvatar.setEnabled(true);
                    Toast.makeText(CookProfileActivity.this,
                            "Failed to process image",
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Prepare multipart
            MultipartBody.Part imagePart = ImageUtils.prepareFilePart("profile_image", compressedFile);

            // Get auth token
            String token = "Bearer " + sessionManager.getToken();

            // Make API call on main thread
            runOnUiThread(() -> {
                ApiService apiService = RetrofitClient.getInstance(CookProfileActivity.this)
                        .getApiService();

                apiService.uploadCookProfileImage(token, imagePart)
                        .enqueue(new Callback<UploadResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<UploadResponse> call,
                                                   @NonNull Response<UploadResponse> response) {
                                showUploadProgress(false);
                                binding.btnEditAvatar.setEnabled(true);

                                // Clean up temp file
                                ImageUtils.deleteFile(compressedFile);

                                if (response.isSuccessful() && response.body() != null
                                        && response.body().isSuccess()) {

                                    String newImageUrl = response.body().getData().getUrl();
                                    Log.d(TAG, "Upload successful: " + newImageUrl);

                                    // Save to session
                                    sessionManager.saveProfileImage(newImageUrl);

                                    // Show success message
                                    Toast.makeText(CookProfileActivity.this,
                                            "Profile photo updated!",
                                            Toast.LENGTH_SHORT).show();

                                } else {
                                    Log.e(TAG, "Upload failed: " + response.code());
                                    Toast.makeText(CookProfileActivity.this,
                                            "Upload failed. Try again.",
                                            Toast.LENGTH_SHORT).show();

                                    loadProfileImage();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<UploadResponse> call,
                                                  @NonNull Throwable t) {
                                showUploadProgress(false);
                                binding.btnEditAvatar.setEnabled(true);

                                ImageUtils.deleteFile(compressedFile);

                                Log.e(TAG, "Upload failed", t);
                                Toast.makeText(CookProfileActivity.this,
                                        "Network error. Try again.",
                                        Toast.LENGTH_SHORT).show();

                                // Reload previous image
                                loadProfileImage();
                            }
                        });
            });
        }).start();
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
                    startActivity(new Intent(CookProfileActivity.this, AddMenuActivity.class));
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    startActivity(new Intent(CookProfileActivity.this, ManageOrdersActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                
                return false;
            });
        }
    }
    
    private void setupMenuItems() {
        if (binding.menuPayoutDetails != null) {
            binding.menuPayoutDetails.setOnClickListener(v ->
                Toast.makeText(this, "Payout Details — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
        
        if (binding.menuDocuments != null) {
            binding.menuDocuments.setOnClickListener(v ->
                Toast.makeText(this, "Documents — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
        
        if (binding.menuHelpSupport != null) {
            binding.menuHelpSupport.setOnClickListener(v ->
                Toast.makeText(this, "Help & Support — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
        
        if (binding.menuAbout != null) {
            binding.menuAbout.setOnClickListener(v ->
                Toast.makeText(this, "About TiffinCraft — coming soon", Toast.LENGTH_SHORT).show()
            );
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
