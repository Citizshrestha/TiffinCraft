package com.tiffincraft.app.activities.customer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.cook.ChangePasswordActivity;
import com.tiffincraft.app.activities.onboarding.SelectRoleActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCustomerSettingsBinding;
import com.tiffincraft.app.models.CustomerProfileResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Customer-side Settings screen: Account (profile/address/payment/password),
 * Preferences (notifications, dietary), and Support — mirrors the cook
 * Settings screen's structure and wiring pattern.
 */
public class CustomerSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CustomerSettingsActivity";
    private static final String PREFS_NAME = "CustomerPreferences";
    private static final String KEY_PUSH = "push_notifications";
    private static final String KEY_SMS = "sms_order_updates";
    private static final String KEY_DIET = "dietary_preference";
    private static final String[] DIET_OPTIONS = {"Everything", "Vegetarian", "Vegan", "Non-Vegetarian"};

    private ActivityCustomerSettingsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupClickListeners();
        loadPreferences();
        loadSavedAddress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedAddress();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.layoutEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditCustomerProfileActivity.class)));

        binding.layoutSavedAddress.setOnClickListener(v ->
                startActivity(new Intent(this, EditCustomerProfileActivity.class)));

        binding.layoutPaymentMethods.setOnClickListener(v -> showPaymentMethodsDialog());

        binding.layoutChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        binding.switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                prefs.edit().putBoolean(KEY_PUSH, isChecked).apply();
            }
        });

        binding.switchSmsUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                prefs.edit().putBoolean(KEY_SMS, isChecked).apply();
            }
        });

        binding.layoutDietaryPreference.setOnClickListener(v -> showDietaryPreferenceDialog());

        binding.layoutHelpSupport.setOnClickListener(v -> showHelpSupportDialog());
        binding.layoutRateApp.setOnClickListener(v -> openPlayStoreListing());
        binding.layoutTerms.setOnClickListener(v -> showTermsDialog());
        binding.layoutPrivacy.setOnClickListener(v -> showPrivacyDialog());
        binding.layoutAbout.setOnClickListener(v -> showAboutDialog());

        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void loadPreferences() {
        binding.switchPushNotifications.setChecked(prefs.getBoolean(KEY_PUSH, true));
        binding.switchSmsUpdates.setChecked(prefs.getBoolean(KEY_SMS, false));
        binding.tvDietaryPreferenceValue.setText(prefs.getString(KEY_DIET, DIET_OPTIONS[0]));
    }

    private void loadSavedAddress() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerProfile(token).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                   @NonNull Response<CustomerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getProfile() != null) {
                    String address = response.body().getProfile().getAddress();
                    binding.tvSavedAddressPreview.setText(
                            address != null && !address.trim().isEmpty() ? address : "No address saved yet");
                } else {
                    binding.tvSavedAddressPreview.setText("No address saved yet");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CustomerProfileResponse> call, @NonNull Throwable t) {
                binding.tvSavedAddressPreview.setText("Could not load address");
            }
        });
    }

    private void showDietaryPreferenceDialog() {
        String current = prefs.getString(KEY_DIET, DIET_OPTIONS[0]);
        int checkedIndex = 0;
        for (int i = 0; i < DIET_OPTIONS.length; i++) {
            if (DIET_OPTIONS[i].equals(current)) checkedIndex = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Dietary Preference")
                .setSingleChoiceItems(DIET_OPTIONS, checkedIndex, (dialog, which) -> {
                    prefs.edit().putString(KEY_DIET, DIET_OPTIONS[which]).apply();
                    binding.tvDietaryPreferenceValue.setText(DIET_OPTIONS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPaymentMethodsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Methods")
                .setMessage(
                        "TiffinCraft supports two ways to pay for your order:\n\n" +
                        "• Cash on Delivery (COD)\n" +
                        "Pay the delivery person in cash when your order arrives.\n\n" +
                        "• Online Payment (eSewa / Mobile Banking QR)\n" +
                        "Scan the cook's QR code shown at checkout and upload your payment " +
                        "screenshot — the cook verifies it before preparing your order.\n\n" +
                        "You choose the payment method for each order at checkout.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHelpSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage(
                        "Need help with an order, payment, or your account?\n\n" +
                        "• Chat with your cook directly from the order details screen for " +
                        "delivery or order questions.\n\n" +
                        "• Email us at support@tiffincraft.com and we'll respond within 24 hours.\n\n" +
                        "• For payment issues, include your order ID so we can look it up quickly.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void openPlayStoreListing() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=" + getPackageName()));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            startActivity(intent);
        }
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Terms & Conditions")
                .setMessage(
                        "1. Acceptance of Terms\n" +
                        "By placing an order on TiffinCraft, you agree to pay for the meals you order and to " +
                        "provide accurate delivery information.\n\n" +
                        "2. Orders & Payment\n" +
                        "Orders are confirmed once the cook accepts them. Payment is due via Cash on Delivery " +
                        "or the online method you selected at checkout.\n\n" +
                        "3. Cancellations\n" +
                        "You may cancel an order before the cook marks it as preparing. Repeated cancellations " +
                        "may affect your account standing.\n\n" +
                        "4. Food Safety\n" +
                        "TiffinCraft connects you with independent home cooks; while we vet cooks on the " +
                        "platform, we are not the direct preparer of your meal.\n\n" +
                        "5. Account Suspension\n" +
                        "TiffinCraft may suspend accounts that abuse the platform, cooks, or delivery partners.\n\n" +
                        "6. Changes to Terms\n" +
                        "These terms may be updated from time to time; continued use of the app means you " +
                        "accept the updated terms.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage(
                        "1. Information We Collect\n" +
                        "We collect your name, contact details, delivery address, and order history to operate " +
                        "your customer account.\n\n" +
                        "2. How We Use Your Data\n" +
                        "Your data is used to process orders, coordinate delivery with cooks, and send " +
                        "order-related notifications.\n\n" +
                        "3. Data Sharing\n" +
                        "Your name, phone number and delivery address are shared with the cook only for orders " +
                        "you place with them. We do not sell your personal data to third parties.\n\n" +
                        "4. Data Security\n" +
                        "Passwords are stored using industry-standard hashing, and payment screenshots are " +
                        "handled securely.\n\n" +
                        "5. Your Rights\n" +
                        "You can update your profile at any time from Settings, or contact support to request " +
                        "account deletion.\n\n" +
                        "6. Contact\n" +
                        "For privacy questions, reach out to the TiffinCraft support team from within the app.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String appVersion = getAppVersionName();
        new AlertDialog.Builder(this)
                .setTitle("About TiffinCraft")
                .setMessage("TiffinCraft Customer App\n\nVersion " + appVersion
                        + "\n\nDiscover fresh homemade meals from home cooks near you.\n\n© " + currentYear
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
        apiService.deleteAccount(token, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call,
                                   @NonNull Response<RegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.tiffincraft.app.utils.SocketManager.getInstance(CustomerSettingsActivity.this).disconnect();
                    sessionManager.logout();
                    Intent intent = new Intent(CustomerSettingsActivity.this, com.tiffincraft.app.activities.onboarding.SelectRoleActivity.class);
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
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
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
        String token = "Bearer " + sessionManager.getToken();

        apiService.logout(token).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call,
                                   @NonNull Response<RegisterResponse> response) {
                finishLogout();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        showLoading(false);
        com.tiffincraft.app.utils.SocketManager.getInstance(this).disconnect();
        sessionManager.logout();
        Intent intent = new Intent(this, SelectRoleActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
