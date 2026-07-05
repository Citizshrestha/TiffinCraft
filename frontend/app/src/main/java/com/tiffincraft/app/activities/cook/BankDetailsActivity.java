package com.tiffincraft.app.activities.cook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityBankDetailsBinding;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUploadHelper;

import org.json.JSONObject;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BankDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BankDetailsActivity";
    private static final int UPLOAD_TYPE_ESEWA = 1;
    private static final int UPLOAD_TYPE_KHALTI = 2;
    private static final int UPLOAD_TYPE_BANK = 3;

    private ActivityBankDetailsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private String esewaQrUrl;
    private String khaltiQrUrl;
    private String bankQrUrl;

    private int currentUploadType = 0;

    // Image picker launchers
    private ActivityResultLauncher<Intent> esewaPickerLauncher;
    private ActivityResultLauncher<Intent> khaltiPickerLauncher;
    private ActivityResultLauncher<Intent> bankPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBankDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        setupImagePickers();
        setupClickListeners();
        loadBankDetails();
    }

    private void setupImagePickers() {
        // eSewa picker
        esewaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            currentUploadType = UPLOAD_TYPE_ESEWA;
                            uploadQrCode(imageUri, "esewa");
                        }
                    }
                }
        );

        // Khalti picker
        khaltiPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            currentUploadType = UPLOAD_TYPE_KHALTI;
                            uploadQrCode(imageUri, "khalti");
                        }
                    }
                }
        );

        // Bank picker
        bankPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            currentUploadType = UPLOAD_TYPE_BANK;
                            uploadQrCode(imageUri, "bank");
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // eSewa upload
        binding.btnUploadEsewaQr.setOnClickListener(v -> {
            openImagePicker(esewaPickerLauncher);
        });

        binding.btnRemoveEsewaQr.setOnClickListener(v -> {
            esewaQrUrl = null;
            updateQrImageView(UPLOAD_TYPE_ESEWA, null);
        });

        // Khalti upload
        binding.btnUploadKhaltiQr.setOnClickListener(v -> {
            openImagePicker(khaltiPickerLauncher);
        });

        binding.btnRemoveKhaltiQr.setOnClickListener(v -> {
            khaltiQrUrl = null;
            updateQrImageView(UPLOAD_TYPE_KHALTI, null);
        });

        // Bank upload
        binding.btnUploadBankQr.setOnClickListener(v -> {
            openImagePicker(bankPickerLauncher);
        });

        binding.btnRemoveBankQr.setOnClickListener(v -> {
            bankQrUrl = null;
            updateQrImageView(UPLOAD_TYPE_BANK, null);
        });

        // Save button
        binding.btnSave.setOnClickListener(v -> {
            saveBankDetails();
        });
    }

    private void openImagePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        launcher.launch(intent);
    }

    private void loadBankDetails() {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMyCookProfile(token).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        // Parse bank_details JSON from profile
                        String bankDetailsJson = response.body().getProfile().getBankDetails();
                        if (bankDetailsJson != null && !bankDetailsJson.isEmpty()) {
                            JSONObject bankDetails = new JSONObject(bankDetailsJson);
                            
                            esewaQrUrl = bankDetails.optString("esewa_qr_url", null);
                            khaltiQrUrl = bankDetails.optString("khalti_qr_url", null);
                            bankQrUrl = bankDetails.optString("bank_qr_url", null);

                            // Update UI
                            updateQrImageView(UPLOAD_TYPE_ESEWA, esewaQrUrl);
                            updateQrImageView(UPLOAD_TYPE_KHALTI, khaltiQrUrl);
                            updateQrImageView(UPLOAD_TYPE_BANK, bankQrUrl);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing bank details", e);
                    }
                } else {
                    Toast.makeText(BankDetailsActivity.this, "Failed to load bank details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading bank details", t);
                Toast.makeText(BankDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadQrCode(Uri imageUri, String qrType) {
        if (!ImageUploadHelper.isImageFile(this, imageUri)) {
            Toast.makeText(this, "Please select a valid image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ImageUploadHelper.isValidFileSize(this, imageUri, 5)) {
            Toast.makeText(this, "Image size must be less than 5MB", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        MultipartBody.Part imagePart = ImageUploadHelper.createImagePart(this, imageUri);
        if (imagePart == null) {
            showLoading(false);
            Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + sessionManager.getToken();

        apiService.uploadDocumentCloudinary(token, imagePart).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call,
                                   @NonNull Response<UploadResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String uploadedUrl = response.body().getData().getUrl();

                    // Update the respective QR URL
                    switch (currentUploadType) {
                        case UPLOAD_TYPE_ESEWA:
                            esewaQrUrl = uploadedUrl;
                            updateQrImageView(UPLOAD_TYPE_ESEWA, uploadedUrl);
                            Toast.makeText(BankDetailsActivity.this, "eSewa QR uploaded", Toast.LENGTH_SHORT).show();
                            break;
                        case UPLOAD_TYPE_KHALTI:
                            khaltiQrUrl = uploadedUrl;
                            updateQrImageView(UPLOAD_TYPE_KHALTI, uploadedUrl);
                            Toast.makeText(BankDetailsActivity.this, "Khalti QR uploaded", Toast.LENGTH_SHORT).show();
                            break;
                        case UPLOAD_TYPE_BANK:
                            bankQrUrl = uploadedUrl;
                            updateQrImageView(UPLOAD_TYPE_BANK, uploadedUrl);
                            Toast.makeText(BankDetailsActivity.this, "Bank QR uploaded", Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    Toast.makeText(BankDetailsActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Upload error", t);
                Toast.makeText(BankDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQrImageView(int uploadType, String url) {
        boolean hasImage = url != null && !url.isEmpty();

        switch (uploadType) {
            case UPLOAD_TYPE_ESEWA:
                if (hasImage) {
                    Glide.with(this)
                            .load(url)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(binding.ivEsewaQr);
                    binding.btnRemoveEsewaQr.setVisibility(View.VISIBLE);
                } else {
                    binding.ivEsewaQr.setImageResource(R.drawable.ic_image_placeholder);
                    binding.btnRemoveEsewaQr.setVisibility(View.GONE);
                }
                break;

            case UPLOAD_TYPE_KHALTI:
                if (hasImage) {
                    Glide.with(this)
                            .load(url)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(binding.ivKhaltiQr);
                    binding.btnRemoveKhaltiQr.setVisibility(View.VISIBLE);
                } else {
                    binding.ivKhaltiQr.setImageResource(R.drawable.ic_image_placeholder);
                    binding.btnRemoveKhaltiQr.setVisibility(View.GONE);
                }
                break;

            case UPLOAD_TYPE_BANK:
                if (hasImage) {
                    Glide.with(this)
                            .load(url)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(binding.ivBankQr);
                    binding.btnRemoveBankQr.setVisibility(View.VISIBLE);
                } else {
                    binding.ivBankQr.setImageResource(R.drawable.ic_image_placeholder);
                    binding.btnRemoveBankQr.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void saveBankDetails() {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        // Build bank_details JSON
        JsonObject bankDetailsJson = new JsonObject();
        if (esewaQrUrl != null) bankDetailsJson.addProperty("esewa_qr_url", esewaQrUrl);
        if (khaltiQrUrl != null) bankDetailsJson.addProperty("khalti_qr_url", khaltiQrUrl);
        if (bankQrUrl != null) bankDetailsJson.addProperty("bank_qr_url", bankQrUrl);

        JsonObject requestBody = new JsonObject();
        requestBody.add("bank_details", bankDetailsJson);

        apiService.updateBankDetails(token, requestBody).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(BankDetailsActivity.this, "Bank details saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "Failed to save bank details";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(BankDetailsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error saving bank details", t);
                Toast.makeText(BankDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.btnSave.setEnabled(!show);
    }
}
