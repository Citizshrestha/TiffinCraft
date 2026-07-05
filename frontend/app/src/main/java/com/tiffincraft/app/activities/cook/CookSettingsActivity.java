package com.tiffincraft.app.activities.cook;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.auth.LoginActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookSettingsBinding;
import com.tiffincraft.app.models.CookProfileResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CookSettingsActivity";

    private ActivityCookSettingsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private boolean isHolidayMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        setupClickListeners();
        loadCurrentSettings();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Holiday Mode Toggle
        binding.switchHolidayMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only respond to user actions, not programmatic changes
                updateHolidayMode(isChecked);
            }
        });

        // Operating Hours
        binding.layoutOperatingHours.setOnClickListener(v -> {
            Toast.makeText(this, "Operating hours configuration - Coming in next update", Toast.LENGTH_SHORT).show();
            // TODO: Open OperatingHoursActivity
        });

        // Bank Details
        binding.layoutBankDetails.setOnClickListener(v -> {
            Toast.makeText(this, "Bank details management - Coming in next update", Toast.LENGTH_SHORT).show();
            // TODO: Open BankDetailsActivity
        });

        // Change Password
        binding.layoutChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Change password - Coming in next update", Toast.LENGTH_SHORT).show();
            // TODO: Open ChangePasswordActivity
        });

        // Notification Preferences
        binding.layoutNotificationPreferences.setOnClickListener(v -> {
            Toast.makeText(this, "Notification preferences - Coming in next update", Toast.LENGTH_SHORT).show();
            // TODO: Open NotificationPreferencesActivity
        });

        // Terms & Conditions
        binding.layoutTerms.setOnClickListener(v -> {
            Toast.makeText(this, "Terms & Conditions - Coming in next update", Toast.LENGTH_SHORT).show();
            // TODO: Open WebView with Terms
        });

        // Privacy Policy
        binding.layoutPrivacy.setOnClickListener(v -> {
            Toast.makeText(this, "Privacy Policy - Coming in next update", Toast.LENGTH_SHORT).show();
            // TODO: Open WebView with Privacy
        });

        // About App
        binding.layoutAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    private void loadCurrentSettings() {
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMyCookProfile(token).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Get holiday mode status from profile
                    // Note: CookProfileResponse needs to have isHolidayMode field
                    // For now, set to false by default
                    isHolidayMode = false; // TODO: Get from response.body().getProfile().isHolidayMode()
                    binding.switchHolidayMode.setChecked(isHolidayMode);

                    // Update status text
                    updateHolidayModeStatus(isHolidayMode);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load settings", t);
            }
        });
    }

    private void updateHolidayMode(boolean enabled) {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("is_holiday_mode", enabled);

        apiService.updateHolidayMode(token, jsonBody).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    isHolidayMode = enabled;
                    updateHolidayModeStatus(enabled);
                    Toast.makeText(CookSettingsActivity.this,
                            enabled ? "Holiday mode enabled" : "Holiday mode disabled",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Revert switch
                    binding.switchHolidayMode.setChecked(!enabled);
                    String errorMsg = "Failed to update holiday mode";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(CookSettingsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookProfileResponse> call, @NonNull Throwable t) {
                showLoading(false);
                // Revert switch
                binding.switchHolidayMode.setChecked(!enabled);
                Log.e(TAG, "Network error updating holiday mode", t);
                Toast.makeText(CookSettingsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHolidayModeStatus(boolean enabled) {
        if (enabled) {
            binding.tvHolidayModeStatus.setText("You are not accepting new orders");
            binding.tvHolidayModeStatus.setTextColor(getColor(R.color.error));
        } else {
            binding.tvHolidayModeStatus.setText("You are accepting orders");
            binding.tvHolidayModeStatus.setTextColor(getColor(R.color.success));
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About TiffinCraft")
                .setMessage("TiffinCraft Cook App\n\nVersion 1.0.0\n\nConnect home cooks with hungry customers.\n\n© 2024 TiffinCraft. All rights reserved.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();

        apiService.logout(token).enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call,
                                   @NonNull Response<com.tiffincraft.app.models.RegisterResponse> response) {
                showLoading(false);
                // Clear session regardless of response
                sessionManager.logout();

                // Navigate to login
                Intent intent = new Intent(CookSettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Logout API failed, clearing session anyway", t);

                // Clear session anyway
                sessionManager.logout();

                // Navigate to login
                Intent intent = new Intent(CookSettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
