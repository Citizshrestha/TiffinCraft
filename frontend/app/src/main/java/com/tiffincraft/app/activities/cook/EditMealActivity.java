package com.tiffincraft.app.activities.cook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookMealAddBinding;
import com.tiffincraft.app.models.Meal;
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

public class EditMealActivity extends AppCompatActivity {

    private static final String TAG = "EditMealActivity";
    public static final String EXTRA_MEAL_ID          = "meal_id";
    public static final String EXTRA_MEAL_NAME        = "meal_name";
    public static final String EXTRA_MEAL_DESCRIPTION = "meal_description";
    public static final String EXTRA_MEAL_PRICE       = "meal_price";
    public static final String EXTRA_MEAL_CATEGORY    = "meal_category";
    public static final String EXTRA_MEAL_IS_VEG      = "meal_is_veg";
    public static final String EXTRA_MEAL_IS_SPICY    = "meal_is_spicy";
    public static final String EXTRA_MEAL_IS_AVAILABLE= "meal_is_available";
    public static final String EXTRA_MEAL_IMAGE_URL   = "meal_image_url";

    private static final int PICK_IMAGE_REQUEST    = 1;
    private static final int REQUEST_CAMERA        = 2;
    private static final int PERMISSION_REQUEST    = 100;

    private ActivityCookMealAddBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    // Meal being edited
    private int     mealId;
    private String  existingImageUrl;

    // UI state
    private Uri    selectedImageUri;
    private String selectedCategory;
    private boolean isVeg;
    private boolean isSpicy;

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookMealAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService     = RetrofitClient.getInstance(this).getApiService();

        readIntent();
        prefillFields();
        setupClickListeners();
    }

    // ── Read meal data passed from CookMealActivity ───────────────────────────
    private void readIntent() {
        Intent i = getIntent();
        mealId           = i.getIntExtra(EXTRA_MEAL_ID,   -1);
        selectedCategory = i.getStringExtra(EXTRA_MEAL_CATEGORY);
        isVeg            = i.getBooleanExtra(EXTRA_MEAL_IS_VEG,  true);
        isSpicy          = i.getBooleanExtra(EXTRA_MEAL_IS_SPICY, false);
        existingImageUrl = i.getStringExtra(EXTRA_MEAL_IMAGE_URL);

        if (selectedCategory == null) selectedCategory = "Lunch Thali";

        if (mealId == -1) {
            Toast.makeText(this, "Invalid meal data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ── Pre-fill all form fields with existing meal data ──────────────────────
    private void prefillFields() {
        // Change title and button text
        // The layout has a plain TextView for the title (no binding id) so we
        // update the Save button label instead:
        binding.btnSaveMeal.setText("Update Meal");

        Intent i = getIntent();
        binding.etMealName.setText(i.getStringExtra(EXTRA_MEAL_NAME));
        binding.etMealDescription.setText(i.getStringExtra(EXTRA_MEAL_DESCRIPTION));
        binding.etMealPrice.setText(String.valueOf(i.getDoubleExtra(EXTRA_MEAL_PRICE, 0)));
        binding.tvSelectedMealCategory.setText(selectedCategory);
        binding.switchMealAvailable.setChecked(i.getBooleanExtra(EXTRA_MEAL_IS_AVAILABLE, true));

        updateChipSelection();

        // Load existing image
        if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
            String fullUrl = existingImageUrl.startsWith("http")
                    ? existingImageUrl
                    : RetrofitClient.SERVER_URL + existingImageUrl;

            binding.layoutMealImagePlaceholder.setVisibility(View.GONE);
            binding.ivMealImagePreview.setVisibility(View.VISIBLE);

            GlideUrl glideUrl = new GlideUrl(fullUrl, new LazyHeaders.Builder()
                    .addHeader("Bypass-Tunnel-Reminder", "true")
                    .build());
            Glide.with(this)
                    .load(glideUrl)
                    .centerCrop()
                    .into(binding.ivMealImagePreview);
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        binding.btnBackMealAdd.setOnClickListener(v -> finish());

        binding.btnUploadMealImage.setOnClickListener(v -> showImagePickerDialog());
        binding.btnSelectMealCategory.setOnClickListener(v -> showCategoryDialog());

        binding.chipVeg.setOnClickListener(v -> {
            isVeg = true;
            updateChipSelection();
        });
        binding.chipSpicy.setOnClickListener(v -> {
            isSpicy = !isSpicy;
            updateChipSelection();
        });
        // Bestseller chip is read-only in edit (managed via reviews)
        binding.chipBestseller.setOnClickListener(v ->
                Toast.makeText(this, "Bestseller is set automatically via ratings", Toast.LENGTH_SHORT).show());

        binding.btnSaveMeal.setOnClickListener(v -> validateAndUpdate());
    }

    // ── Chip visuals ──────────────────────────────────────────────────────────
    private void updateChipSelection() {
        binding.chipVeg.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_selected_green));
        binding.chipVeg.setTextColor(ContextCompat.getColor(this, R.color.dark_green));

        if (isSpicy) {
            binding.chipSpicy.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_selected_green));
            binding.chipSpicy.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        } else {
            binding.chipSpicy.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_unselected));
            binding.chipSpicy.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        binding.chipBestseller.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_unselected));
        binding.chipBestseller.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    // ── Category picker ───────────────────────────────────────────────────────
    private void showCategoryDialog() {
        String[] categories = {"Lunch Thali", "Dinner Thali", "Breakfast", "Snacks", "Dessert", "Beverages"};
        new AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setItems(categories, (dialog, which) -> {
                    selectedCategory = categories[which];
                    binding.tvSelectedMealCategory.setText(selectedCategory);
                })
                .show();
    }

    // ── Image picker ──────────────────────────────────────────────────────────
    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take Photo", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Change Meal Photo")
                .setItems(options, (dialog, which) -> {
                    if      (which == 0) checkPermissionAndPickImage();
                    else if (which == 1) checkPermissionAndTakePhoto();
                })
                .show();
    }

    private void checkPermissionAndPickImage() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQUEST);
        } else {
            openGallery();
        }
    }

    private void checkPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == PICK_IMAGE_REQUEST) {
            selectedImageUri = data.getData();
        } else if (requestCode == REQUEST_CAMERA) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            selectedImageUri = saveBitmapToCache(photo);
        }

        if (selectedImageUri != null) {
            binding.layoutMealImagePlaceholder.setVisibility(View.GONE);
            binding.ivMealImagePreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedImageUri).centerCrop().into(binding.ivMealImagePreview);
        }
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "edit_meal_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap", e);
            return null;
        }
    }

    // ── Validation + update ───────────────────────────────────────────────────
    private void validateAndUpdate() {
        String name        = binding.etMealName.getText().toString().trim();
        String description = binding.etMealDescription.getText().toString().trim();
        String priceStr    = binding.etMealPrice.getText().toString().trim();
        boolean isAvailable = binding.switchMealAvailable.isChecked();

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

        MealRequest request = new MealRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setCategory(selectedCategory);
        request.setCuisineType("Indian");
        request.setAvailable(isAvailable);
        request.setVegetarian(isVeg);
        request.setVegan(false);
        request.setSpiceLevel(isSpicy ? "hot" : "mild");
        request.setPreparationTime(30);

        binding.btnSaveMeal.setEnabled(false);
        binding.btnSaveMeal.setText("Updating...");

        updateMeal(request);
    }

    private void updateMeal(MealRequest request) {
        String token = "Bearer " + sessionManager.getToken();

        apiService.updateMeal(token, mealId, request).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call,
                                   @NonNull Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Log.d(TAG, "Meal updated successfully");

                    // Upload new image if the user picked one
                    if (selectedImageUri != null) {
                        uploadMealImage();
                    } else {
                        showSuccessAndFinish();
                    }
                } else {
                    resetButton();
                    String msg = "Failed to update meal";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }
                    Toast.makeText(EditMealActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error updating meal", t);
                resetButton();
                Toast.makeText(EditMealActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Image upload ──────────────────────────────────────────────────────────
    private void uploadMealImage() {
        try {
            File file = getFileFromUri(selectedImageUri);
            if (file == null || !file.exists()) {
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
                showSuccessAndFinish();
                return;
            }

            String mimeType;
            String fn = file.getName().toLowerCase();
            if      (fn.endsWith(".png"))  mimeType = "image/png";
            else if (fn.endsWith(".webp")) mimeType = "image/webp";
            else                           mimeType = "image/jpeg";

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("meal_image", file.getName(), requestFile);

            String token = "Bearer " + sessionManager.getToken();

            apiService.uploadMealImage(token, mealId, body).enqueue(new Callback<MealResponse>() {
                @Override
                public void onResponse(@NonNull Call<MealResponse> call,
                                       @NonNull Response<MealResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSuccess()) {
                        Log.d(TAG, "Image updated: " + response.body().getImageUrl());
                    } else {
                        Log.w(TAG, "Image upload failed but meal details updated");
                        Toast.makeText(EditMealActivity.this,
                                "Meal updated but image upload failed", Toast.LENGTH_SHORT).show();
                    }
                    showSuccessAndFinish();
                }

                @Override
                public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Image upload network error", t);
                    Toast.makeText(EditMealActivity.this,
                            "Meal updated but image upload failed", Toast.LENGTH_SHORT).show();
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
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return new File(uri.getPath());
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                File temp = new File(getCacheDir(), "edit_meal_upload_" + System.currentTimeMillis() + ".jpg");
                try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(temp)) {
                    if (in == null) return null;
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    return temp;
                } catch (IOException e) {
                    Log.e(TAG, "Error copying file", e);
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getFileFromUri error", e);
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showSuccessAndFinish() {
        Toast.makeText(this, "Meal Updated Successfully!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void resetButton() {
        binding.btnSaveMeal.setEnabled(true);
        binding.btnSaveMeal.setText("Update Meal");
    }
}
