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
import com.tiffincraft.app.models.CookProfileRequest;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.session.SessionManager;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditCookProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditCookProfile";

    private ActivityEditCookProfileBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private JSONObject currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditCookProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        setupToolbar();
        loadCurrentProfile();
        setupListeners();
    }

    private void setupToolbar() {
        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Edit Profile");
            }
            binding.toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadCurrentProfile() {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getMyCookProfile(token).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        currentProfile = new JSONObject(response.body().getProfile().toString());
                        populateFields();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing profile", e);
                        Toast.makeText(EditCookProfileActivity.this,
                                "Failed to load profile", Toast.LENGTH_SHORT).show();
                        finish();
                    }
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

        try {
            if (binding.etFullName != null && currentProfile.has("full_name")) {
                binding.etFullName.setText(currentProfile.getString("full_name"));
            }

            if (binding.etPhone != null && currentProfile.has("phone")) {
                binding.etPhone.setText(currentProfile.getString("phone"));
            }

            if (binding.etEmail != null && currentProfile.has("email")) {
                binding.etEmail.setText(currentProfile.getString("email"));
                binding.etEmail.setEnabled(false); // Email cannot be changed
            }

            if (binding.etKitchenName != null && currentProfile.has("kitchen_name")) {
                binding.etKitchenName.setText(currentProfile.getString("kitchen_name"));
            }

            if (binding.etFoodType != null && currentProfile.has("food_type")) {
                binding.etFoodType.setText(currentProfile.getString("food_type"));
            }

            if (binding.etDescription != null && currentProfile.has("description")) {
                binding.etDescription.setText(currentProfile.getString("description"));
            }

            if (binding.etAddress != null && currentProfile.has("address")) {
                binding.etAddress.setText(currentProfile.getString("address"));
            }

            if (binding.etCapacity != null && currentProfile.has("capacity_per_day")) {
                binding.etCapacity.setText(String.valueOf(currentProfile.getInt("capacity_per_day")));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error populating fields", e);
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
        String kitchenName = binding.etKitchenName.getText().toString().trim();
        String foodType = binding.etFoodType.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String capacityStr = binding.etCapacity.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            binding.etFullName.setError("Name is required");
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

        if (TextUtils.isEmpty(foodType)) {
            binding.etFoodType.setError("Food type is required");
            binding.etFoodType.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            binding.etDescription.setError("Description is required");
            binding.etDescription.requestFocus();
            return;
        }

        int capacity = 0;
        if (!TextUtils.isEmpty(capacityStr)) {
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                binding.etCapacity.setError("Invalid number");
                binding.etCapacity.requestFocus();
                return;
            }
        }

        // Save changes
        saveProfile(fullName, phone, address, kitchenName, foodType, description, capacity);
    }

    private void saveProfile(String fullName, String phone, String address,
                             String kitchenName, String foodType, String description, int capacity) {
        showLoading(true);

        CookProfileRequest request = new CookProfileRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setAddress(address);
        request.setKitchenName(kitchenName);
        request.setFoodType(foodType);
        request.setDescription(description);
        request.setCapacityPerDay(capacity);

        String token = "Bearer " + sessionManager.getToken();
        apiService.updateCookProfile(token, request).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Update session with new name
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
