package com.tiffincraft.app.activities.example;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUploadHelper;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Example Activity showing complete Cloudinary image upload flow
 * 
 * Features:
 * - Select image from gallery
 * - Validate image (type and size)
 * - Upload to Cloudinary
 * - Display uploaded image
 * - Show loading progress
 * - Handle errors
 * 
 * Copy this code into your actual activities (CookMealActivity, EditCookProfileActivity, etc.)
 */
public class ExampleImageUploadActivity extends AppCompatActivity {

    // Request code for image picker
    private static final int PICK_IMAGE_REQUEST = 100;
    
    // UI Components
    private ImageView imagePreview;
    private Button selectImageButton;
    private Button uploadImageButton;
    private Button saveButton;
    
    // Data
    private Uri selectedImageUri;
    private String uploadedImageUrl; // Cloudinary URL after upload
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_image_upload);
        
        initializeViews();
        setupClickListeners();
        setupProgressDialog();
    }

    private void initializeViews() {
        imagePreview = findViewById(R.id.imagePreview);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveButton = findViewById(R.id.saveButton);
        
        // Initially disable upload and save buttons
        uploadImageButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void setupClickListeners() {
        // Select image from gallery
        selectImageButton.setOnClickListener(v -> selectImageFromGallery());
        
        // Upload selected image to Cloudinary
        uploadImageButton.setOnClickListener(v -> uploadImageToCloudinary());
        
        // Save (use the uploaded URL)
        saveButton.setOnClickListener(v -> saveWithUploadedImage());
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    /**
     * Step 1: Select image from gallery
     */
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Handle image selection result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            
            if (selectedImageUri != null) {
                // Validate image
                if (!validateImage()) {
                    return;
                }
                
                // Show preview
                showImagePreview();
                
                // Enable upload button
                uploadImageButton.setEnabled(true);
            }
        }
    }

    /**
     * Step 2: Validate selected image
     */
    private boolean validateImage() {
        // Check if it's an image file
        if (!ImageUploadHelper.isImageFile(this, selectedImageUri)) {
            Toast.makeText(this, 
                "Please select a valid image file (JPEG, PNG, GIF, WebP)", 
                Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Check file size (max 5MB)
        if (!ImageUploadHelper.isValidFileSize(this, selectedImageUri, 5)) {
            Toast.makeText(this, 
                "Image size must be less than 5MB. Please select a smaller image.", 
                Toast.LENGTH_LONG).show();
            return false;
        }
        
        return true;
    }

    /**
     * Show image preview
     */
    private void showImagePreview() {
        Glide.with(this)
            .load(selectedImageUri)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(imagePreview);
    }

    /**
     * Step 3: Upload image to Cloudinary
     */
    private void uploadImageToCloudinary() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        progressDialog.setMessage("Uploading image to cloud...");
        progressDialog.show();
        
        // Create multipart body from URI
        MultipartBody.Part imagePart = ImageUploadHelper.createImagePart(this, selectedImageUri);
        
        if (imagePart == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get API service and token
        ApiService apiService = RetrofitClient.getApiService();
        String token = "Bearer " + SessionManager.getToken(this);
        
        // Choose appropriate upload endpoint based on your use case:
        // - uploadMealImageCloudinary() for meal images
        // - uploadProfileImageCloudinary() for profile images
        // - uploadDocumentCloudinary() for documents
        
        Call<UploadResponse> call = apiService.uploadMealImageCloudinary(token, imagePart);
        
        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    handleUploadSuccess(response.body());
                } else {
                    handleUploadError(response.code(), response.message());
                }
            }
            
            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                progressDialog.dismiss();
                handleUploadFailure(t);
            }
        });
    }

    /**
     * Handle successful upload
     */
    private void handleUploadSuccess(UploadResponse response) {
        if (response.isSuccess()) {
            // Get the Cloudinary URL
            uploadedImageUrl = response.getData().getUrl();
            
            Toast.makeText(this, 
                "Image uploaded successfully!", 
                Toast.LENGTH_SHORT).show();
            
            // Enable save button
            saveButton.setEnabled(true);
            
            // Optional: Show the uploaded image from Cloudinary
            loadUploadedImage();
            
        } else {
            Toast.makeText(this, 
                "Upload failed: " + response.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle upload errors
     */
    private void handleUploadError(int statusCode, String message) {
        String errorMessage;
        
        switch (statusCode) {
            case 401:
                errorMessage = "Session expired. Please login again.";
                // Optionally redirect to login
                // redirectToLogin();
                break;
            case 400:
                errorMessage = "Invalid file. Please select a valid image.";
                break;
            case 413:
                errorMessage = "File too large. Maximum size is 5MB.";
                break;
            case 500:
                errorMessage = "Server error. Please try again later.";
                break;
            default:
                errorMessage = "Upload failed: " + message;
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Handle network failures
     */
    private void handleUploadFailure(Throwable t) {
        String errorMessage;
        
        if (t instanceof java.net.UnknownHostException) {
            errorMessage = "No internet connection. Please check your network.";
        } else if (t instanceof java.net.SocketTimeoutException) {
            errorMessage = "Upload timeout. Please try again.";
        } else {
            errorMessage = "Network error: " + t.getMessage();
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Load uploaded image from Cloudinary (optional - to confirm upload)
     */
    private void loadUploadedImage() {
        Glide.with(this)
            .load(uploadedImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(imagePreview);
    }

    /**
     * Step 4: Save/Use the uploaded image URL
     */
    private void saveWithUploadedImage() {
        if (uploadedImageUrl == null) {
            Toast.makeText(this, "Please upload an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Now use uploadedImageUrl to create/update your entity
        // For example:
        
        // Creating a meal:
        // MealRequest mealRequest = new MealRequest();
        // mealRequest.setMealImage(uploadedImageUrl);
        // mealRequest.setName(...);
        // mealRequest.setPrice(...);
        // apiService.addMeal(token, mealRequest).enqueue(...);
        
        // Updating profile:
        // CookProfileRequest profileRequest = new CookProfileRequest();
        // profileRequest.setProfileImage(uploadedImageUrl);
        // profileRequest.setBio(...);
        // apiService.updateCookProfile(token, profileRequest).enqueue(...);
        
        Toast.makeText(this, 
            "Image URL ready to use: " + uploadedImageUrl, 
            Toast.LENGTH_SHORT).show();
    }

    /**
     * Clean up temporary files when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageUploadHelper.cleanupTempFiles(this);
        
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // ========== ALTERNATIVE: COMBINED FLOW ==========
    // If you want to select and upload in one step:
    
    /**
     * Alternative: Auto-upload after selection
     */
    private void selectAndUploadImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
        
        // Then in onActivityResult, call uploadImageToCloudinary() automatically
    }
    
    // ========== IMAGE TRANSFORMATIONS ==========
    // You can get different versions of the image by modifying the URL:
    
    /**
     * Get thumbnail URL (200x200)
     */
    private String getThumbnailUrl(String originalUrl) {
        return originalUrl.replace("/upload/", "/upload/w_200,h_200,c_fill/");
    }
    
    /**
     * Get resized URL (custom size)
     */
    private String getResizedUrl(String originalUrl, int width, int height) {
        String transformation = String.format("w_%d,h_%d/", width, height);
        return originalUrl.replace("/upload/", "/upload/" + transformation);
    }
    
    /**
     * Get circular profile image URL
     */
    private String getCircularUrl(String originalUrl) {
        return originalUrl.replace("/upload/", "/upload/w_400,h_400,c_fill,r_max/");
    }
}

/* ============================================================
 * LAYOUT FILE: res/layout/activity_example_image_upload.xml
 * ============================================================
 
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <ImageView
        android:id="@+id/imagePreview"
        android:layout_width="match_parent"
        android:layout_height="300dp"
        android:scaleType="centerCrop"
        android:src="@drawable/placeholder_image"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/selectImageButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Select Image from Gallery"
        android:layout_marginBottom="8dp"/>

    <Button
        android:id="@+id/uploadImageButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Upload to Cloudinary"
        android:enabled="false"
        android:layout_marginBottom="8dp"/>

    <Button
        android:id="@+id/saveButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Save"
        android:enabled="false"/>

</LinearLayout>

 */
