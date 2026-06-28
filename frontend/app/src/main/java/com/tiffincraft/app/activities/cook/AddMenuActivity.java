package com.tiffincraft.app.activities.cook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookMealAddBinding;
import com.tiffincraft.app.models.MealRequest;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AddMenuActivity extends AppCompatActivity {
    private static final String TAG = "AddMenuActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private ActivityCookMealAddBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    
    private Uri selectedImageUri;
    private String selectedCategory = "Lunch Thali";
    private boolean isVeg = true;
    private boolean isSpicy = false;
    private boolean isBestseller = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookMealAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        
        setupUI();
        setupClickListeners();
    }
    
    private void setupUI() {
        // Set default veg chip as selected
        updateChipSelection();
    }
    
    private void setupClickListeners() {
        // Back button
        if (binding.btnBackMealAdd != null) {
            binding.btnBackMealAdd.setOnClickListener(v -> finish());
        }
        
        // Image upload
        binding.btnUploadMealImage.setOnClickListener(v -> showImagePickerDialog());
        
        // Category selection
        binding.btnSelectMealCategory.setOnClickListener(v -> showCategoryDialog());
        
        // Dietary tags chips
        binding.chipVeg.setOnClickListener(v -> {
            isVeg = true;
            updateChipSelection();
        });
        
        binding.chipSpicy.setOnClickListener(v -> {
            isSpicy = !isSpicy;
            updateChipSelection();
        });
        
        binding.chipBestseller.setOnClickListener(v -> {
            isBestseller = !isBestseller;
            updateChipSelection();
        });
        
        // Save meal button
        if (binding.btnSaveMeal != null) {
            binding.btnSaveMeal.setOnClickListener(v -> validateAndSaveMeal());
        }
    }
    
    private void updateChipSelection() {
        // Veg chip is always selected for now (you can make it toggle if needed)
        binding.chipVeg.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_selected_green));
        binding.chipVeg.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        
        // Spicy chip
        if (isSpicy) {
            binding.chipSpicy.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_selected_green));
            binding.chipSpicy.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        } else {
            binding.chipSpicy.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_unselected));
            binding.chipSpicy.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
        
        // Bestseller chip
        if (isBestseller) {
            binding.chipBestseller.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_selected_green));
            binding.chipBestseller.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        } else {
            binding.chipBestseller.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_unselected));
            binding.chipBestseller.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }
    
    private void showCategoryDialog() {
        String[] categories = {"Lunch Thali", "Dinner Thali", "Breakfast", "Snacks", "Dessert", "Beverages"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Category");
        builder.setItems(categories, (dialog, which) -> {
            selectedCategory = categories[which];
            binding.tvSelectedMealCategory.setText(selectedCategory);
        });
        builder.show();
    }
    
    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take Photo", "Cancel"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Meal Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkPermissionAndPickImage();
            } else if (which == 1) {
                checkPermissionAndTakePhoto();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    
    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openGallery();
        }
    }
    
    private void checkPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, but we need to know which action to perform
                // For simplicity, opening gallery
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                selectedImageUri = data.getData();
                displaySelectedImage();
            } else if (requestCode == REQUEST_CAMERA) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                selectedImageUri = saveImageToFile(photo);
                displaySelectedImage();
            }
        }
    }
    
    private Uri saveImageToFile(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "meal_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }
    
    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            binding.layoutMealImagePlaceholder.setVisibility(View.GONE);
            binding.ivMealImagePreview.setVisibility(View.VISIBLE);
            binding.ivMealImagePreview.setImageURI(selectedImageUri);
        }
    }
    
    private void validateAndSaveMeal() {
        String name = binding.etMealName.getText().toString().trim();
        String description = binding.etMealDescription.getText().toString().trim();
        String priceStr = binding.etMealPrice.getText().toString().trim();
        boolean isAvailable = binding.switchMealAvailable.isChecked();
        
        // Validation
        if (name.isEmpty()) {
            binding.etMealName.setError("Meal name is required");
            binding.etMealName.requestFocus();
            return;
        }
        
        if (priceStr.isEmpty()) {
            binding.etMealPrice.setError("Price is required");
            binding.etMealPrice.requestFocus();
            return;
        }
        
        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                binding.etMealPrice.setError("Price must be greater than 0");
                binding.etMealPrice.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            binding.etMealPrice.setError("Invalid price");
            binding.etMealPrice.requestFocus();
            return;
        }
        
        // Create meal request
        MealRequest mealRequest = new MealRequest(name, description, price, selectedCategory,
                isAvailable, isVeg, isSpicy, isBestseller);
        
        // Show progress
        binding.btnSaveMeal.setEnabled(false);
        binding.btnSaveMeal.setText("Saving...");
        
        // Create meal first
        saveMeal(mealRequest);
    }
    
    private void saveMeal(MealRequest mealRequest) {
        String token = "Bearer " + sessionManager.getToken();
        
        apiService.addMeal(token, mealRequest).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    int mealId = response.body().getMealId();
                    Log.d(TAG, "Meal created successfully with ID: " + mealId);
                    
                    // If image selected, upload it
                    if (selectedImageUri != null) {
                        uploadMealImage(mealId);
                    } else {
                        // No image, finish
                        showSuccessAndFinish();
                    }
                } else {
                    resetButton();
                    String errorMsg = "Failed to create meal";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(AddMenuActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error creating meal", t);
                resetButton();
                Toast.makeText(AddMenuActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void uploadMealImage(int mealId) {
        try {
            // Get file from URI
            File file = getFileFromUri(selectedImageUri);
            if (file == null || !file.exists()) {
                Log.e(TAG, "File not found");
                showSuccessAndFinish();
                return;
            }
            
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("meal_image", file.getName(), requestFile);
            
            String token = "Bearer " + sessionManager.getToken();
            
            apiService.uploadMealImage(token, mealId, body).enqueue(new Callback<MealResponse>() {
                @Override
                public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Log.d(TAG, "Image uploaded successfully");
                    } else {
                        Log.e(TAG, "Image upload failed but meal created");
                    }
                    showSuccessAndFinish();
                }
                
                @Override
                public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error uploading image", t);
                    // Still show success since meal was created
                    showSuccessAndFinish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image upload", e);
            showSuccessAndFinish();
        }
    }
    
    private File getFileFromUri(Uri uri) {
        try {
            // If it's a file URI, return the file directly
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return new File(uri.getPath());
            }
            
            // If it's a content URI, copy to cache
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                File tempFile = new File(getCacheDir(), "temp_meal_" + System.currentTimeMillis() + ".jpg");
                
                try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    
                    if (inputStream == null) return null;
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    return tempFile;
                } catch (IOException e) {
                    Log.e(TAG, "Error copying file", e);
                    return null;
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file from URI", e);
            return null;
        }
    }
    
    private void showSuccessAndFinish() {
        Toast.makeText(this, "Meal Saved Successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void resetButton() {
        binding.btnSaveMeal.setEnabled(true);
        binding.btnSaveMeal.setText("Save Meal");
    }
}
