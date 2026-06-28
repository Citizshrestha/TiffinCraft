package com.tiffincraft.app.activities.customer;

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
import com.tiffincraft.app.databinding.ActivityEditCustomerProfileBinding;
import com.tiffincraft.app.models.CustomerProfile;
import com.tiffincraft.app.models.CustomerProfileRequest;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditCustomerProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditCustomerProfile";
    
    private ActivityEditCustomerProfileBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private CustomerProfile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditCustomerProfileBinding.inflate(getLayoutInflater());
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
        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                   @NonNull Response<CustomerProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentProfile = response.body().getProfile();
                    populateFields();
                } else {
                    Toast.makeText(EditCustomerProfileActivity.this,
                            "Failed to load profile", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CustomerProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error loading profile", t);
                Toast.makeText(EditCustomerProfileActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateFields() {
        if (currentProfile == null) return;

        if (binding.etFullName != null && currentProfile.getFullName() != null) {
            binding.etFullName.setText(currentProfile.getFullName());
        }

        if (binding.etPhone != null && currentProfile.getPhone() != null) {
            binding.etPhone.setText(currentProfile.getPhone());
        }

        if (binding.etEmail != null && currentProfile.getEmail() != null) {
            binding.etEmail.setText(currentProfile.getEmail());
            binding.etEmail.setEnabled(false); // Email cannot be changed
        }

        if (binding.etAddress != null && currentProfile.getAddress() != null) {
            binding.etAddress.setText(currentProfile.getAddress());
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

        // Check if anything changed
        boolean changed = false;
        if (!fullName.equals(currentProfile.getFullName())) changed = true;
        if (!phone.equals(currentProfile.getPhone())) changed = true;
        if (!address.equals(currentProfile.getAddress() != null ? currentProfile.getAddress() : "")) changed = true;

        if (!changed) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Save changes
        saveProfile(fullName, phone, address);
    }

    private void saveProfile(String fullName, String phone, String address) {
        showLoading(true);

        CustomerProfileRequest request = new CustomerProfileRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setAddress(address);

        String token = "Bearer " + sessionManager.getToken();
        apiService.updateCustomerProfile(token, request).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                   @NonNull Response<CustomerProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Update session with new name and phone
                    sessionManager.saveFullName(fullName);
                    
                    Toast.makeText(EditCustomerProfileActivity.this,
                            "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String errorMsg = "Failed to update profile";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(EditCustomerProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CustomerProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error updating profile", t);
                Toast.makeText(EditCustomerProfileActivity.this,
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
