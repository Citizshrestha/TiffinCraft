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
        binding.layoutOperatingHours.setOnClickListener(v ->
                startActivity(new Intent(this, OperatingHoursActivity.class)));

        // Bank Details
        binding.layoutBankDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, BankDetailsActivity.class);
            startActivity(intent);
        });

        // Edit Kitchen Profile
        binding.layoutEditKitchenProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditCookProfileActivity.class);
            startActivity(intent);
        });

        // Change Password
        binding.layoutChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        // Notification Preferences
        binding.layoutNotificationPreferences.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationPreferencesActivity.class)));

        // Terms & Conditions
        binding.layoutTerms.setOnClickListener(v -> showTermsDialog());

        // Privacy Policy
        binding.layoutPrivacy.setOnClickListener(v -> showPrivacyDialog());

        // About App
        binding.layoutAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });

        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void loadCurrentSettings() {
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMyCookProfile(token).enqueue(new Callback<CookProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookProfileResponse> call,
                                   @NonNull Response<CookProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getProfile() != null) {
                    isHolidayMode = response.body().getProfile().isHolidayMode();
                    binding.switchHolidayMode.setChecked(isHolidayMode);
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
            binding.tvHolidayModeStatus.setTextColor(getColor(R.color.error_red));
        } else {
            binding.tvHolidayModeStatus.setText("You are accepting orders");
            binding.tvHolidayModeStatus.setTextColor(getColor(R.color.green_primary));
        }
    }

    private void showAboutDialog() {
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String appVersion = getAppVersionName();
        new AlertDialog.Builder(this)
                .setTitle("About TiffinCraft")
                .setMessage("TiffinCraft Cook App\n\nVersion " + appVersion
                        + "\n\nConnect home cooks with hungry customers.\n\n© " + currentYear
                        + " TiffinCraft. All rights reserved.")
                .setPositiveButton("OK", null)
                .show();
    }

    private String getAppVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Terms & Conditions")
                .setMessage(
                        "1. Acceptance of Terms\n" +
                        "By listing meals on TiffinCraft, you agree to prepare and deliver food that meets local " +
                        "health and safety standards.\n\n" +
                        "2. Cook Responsibilities\n" +
                        "You are responsible for the accuracy of meal descriptions, pricing, allergen information, " +
                        "and for fulfilling orders within the time you commit to.\n\n" +
                        "3. Payments\n" +
                        "TiffinCraft calculates your earnings from delivered orders. Payouts follow the bank " +
                        "details you provide in Settings.\n\n" +
                        "4. Order Cancellations\n" +
                        "Repeated cancellations or unfulfilled orders may affect your kitchen's visibility and " +
                        "rating on the platform.\n\n" +
                        "5. Account Suspension\n" +
                        "TiffinCraft may suspend accounts that violate food safety regulations or platform policies.\n\n" +
                        "6. Changes to Terms\n" +
                        "These terms may be updated from time to time; continued use of the app means you accept " +
                        "the updated terms.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage(
                        "1. Information We Collect\n" +
                        "We collect your name, contact details, kitchen profile, bank/payment details, and order " +
                        "history to operate your cook account.\n\n" +
                        "2. How We Use Your Data\n" +
                        "Your data is used to process orders, calculate earnings, show your kitchen to nearby " +
                        "customers, and send order-related notifications.\n\n" +
                        "3. Data Sharing\n" +
                        "Customer contact and delivery details are shared with you only for orders you accept. " +
                        "We do not sell your personal data to third parties.\n\n" +
                        "4. Data Security\n" +
                        "Passwords are stored using industry-standard hashing, and payment details are handled " +
                        "securely.\n\n" +
                        "5. Your Rights\n" +
                        "You can update your profile at any time from Settings, or contact support to request " +
                        "account deletion.\n\n" +
                        "6. Contact\n" +
                        "For privacy questions, reach out to the TiffinCraft support team from within the app.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account and all associated data. " +
                        "This action cannot be undone.\n\n" +
                        "Enter your password to confirm:")
                .setView(getLayoutInflater().inflate(R.layout.dialog_password_confirm, null))
                .setPositiveButton("Delete", (dialog, which) -> {
                    android.widget.EditText etPassword =
                            ((androidx.appcompat.app.AlertDialog) dialog).findViewById(R.id.etPassword);
                    String password = etPassword != null ? etPassword.getText().toString() : "";
                    if (password.isEmpty()) {
                        com.google.android.material.snackbar.Snackbar.make(
                                binding.getRoot(), "Password is required", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    performAccountDeletion(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performAccountDeletion(String password) {
        showLoading(true);

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("password", password);

        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteAccount(token, body).enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call,
                                   @NonNull Response<com.tiffincraft.app.models.RegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.tiffincraft.app.utils.SocketManager.getInstance(CookSettingsActivity.this).disconnect();
                    sessionManager.logout();
                    Intent intent = new Intent(CookSettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String msg = "Failed to delete account";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    } else if (response.code() == 401) {
                        msg = "Password is incorrect";
                    }
                    com.google.android.material.snackbar.Snackbar.make(
                            binding.getRoot(), msg, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call, @NonNull Throwable t) {
                showLoading(false);
                com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(), "Network error: " + t.getMessage(),
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showLogoutConfirmation() {
        com.tiffincraft.app.utils.DialogUtils.showLogoutConfirmation(this, this::performLogout);
    }

    private void performLogout() {
        showLoading(true);

        com.tiffincraft.app.utils.SocketManager.getInstance(this).disconnect();

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
