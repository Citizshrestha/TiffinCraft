package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityEditCookProfileBinding;
import com.tiffincraft.app.models.CookProfile;
import com.tiffincraft.app.models.CookProfileRequest;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditCookProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditCookProfile";

    private ActivityEditCookProfileBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private CookProfile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditCookProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        setupToolbar();
        loadCurrentData();
        setupListeners();
    }

    private void setupToolbar() {
        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Edit Kitchen Profile");
            }
            binding.toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadCurrentData() {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getCookProfile(token).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentProfile = response.body().getProfile();
                    populateFields();
                } else {
                    Toast.makeText(EditCookProfileActivity.this,
                            "Failed to load profile", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error loading profile", t);
                Toast.makeText(EditCookProfileActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateFields() {
        if (currentProfile == null) return;

        if (currentProfile.getFullName() != null) {
            binding.etFullName.setText(currentProfile.getFullName());
        }
        if (currentProfile.getPhone() != null) {
            binding.etPhone.setText(currentProfile.getPhone());
        }
        if (currentProfile.getAddress() != null) {
            binding.etAddress.setText(currentProfile.getAddress());
        }
        if (currentProfile.getKitchenName() != null) {
            binding.etKitchenName.setText(currentProfile.getKitchenName());
        }
        if (currentProfile.getFoodType() != null) {
            binding.etFoodType.setText(currentProfile.getFoodType());
        }
        if (currentProfile.getDescription() != null) {
            binding.etDescription.setText(currentProfile.getDescription());
        }
    }

    private void setupListeners() {
        if (binding.btnSaveProfile != null) {
            binding.btnSaveProfile.setOnClickListener(v -> validateAndSave());
        }
        if (binding.btnCancel != null) {
            binding.btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void validateAndSave() {
        String fullName = binding.etFullName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String kitchenName = binding.etKitchenName.getText().toString().trim();
        String foodType = binding.etFoodType.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            binding.etFullName.setError("Full name is required");
            binding.etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            binding.etPhone.setError("Phone is required");
            binding.etPhone.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            binding.etPhone.setError("Phone must be at least 10 digits");
            binding.etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(kitchenName)) {
            binding.etKitchenName.setError("Kitchen name is required");
            binding.etKitchenName.requestFocus();
            return;
        }

        // Check if anything changed
        boolean changed = false;
        if (!fullName.equals(currentProfile.getFullName())) changed = true;
        if (!phone.equals(currentProfile.getPhone())) changed = true;
        if (!address.equals(currentProfile.getAddress() != null ? currentProfile.getAddress() : "")) changed = true;
        if (!kitchenName.equals(currentProfile.getKitchenName())) changed = true;
        if (!foodType.equals(currentProfile.getFoodType() != null ? currentProfile.getFoodType() : "")) changed = true;
        if (!description.equals(currentProfile.getDescription() != null ? currentProfile.getDescription() : "")) changed = true;

        if (!changed) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        saveProfile(fullName, phone, address, kitchenName, foodType, description);
    }

    private void saveProfile(String fullName, String phone, String address,
                             String kitchenName, String foodType, String description) {
        showLoading(true);

        CookProfileRequest request = new CookProfileRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setAddress(address);
        request.setKitchenName(kitchenName);
        request.setFoodType(foodType);
        request.setDescription(description);

        String token = "Bearer " + sessionManager.getToken();
        apiService.updateCookCompleteProfile(token, request).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    sessionManager.saveFullName(fullName);

                    Toast.makeText(EditCookProfileActivity.this,
                            "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                    setResult(RESULT_OK);
                    finish();
                } else {
                    String errorMsg = "Failed to update profile";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(EditCookProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error updating profile", t);
                Toast.makeText(EditCookProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (binding.btnSaveProfile != null) {
            binding.btnSaveProfile.setEnabled(!show);
        }
        if (binding.btnCancel != null) {
            binding.btnCancel.setEnabled(!show);
        }
    }
}
