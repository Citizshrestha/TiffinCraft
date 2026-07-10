package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ChangePasswordResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Lets the logged-in cook change their account password (requires current password). */
public class ChangePasswordActivity extends AppCompatActivity {

    private ApiService apiService;
    private SessionManager sessionManager;

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String current = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
        String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (current.isEmpty()) {
            etCurrentPassword.setError("Enter your current password");
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError("New password must be at least 6 characters");
            return;
        }
        if (!newPass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("currentPassword", current);
        body.addProperty("newPassword", newPass);

        setLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.changePassword(token, body).enqueue(new Callback<ChangePasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChangePasswordResponse> call,
                                   @NonNull Response<ChangePasswordResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String message = "Failed to change password";
                    if (response.code() == 401) {
                        message = "Current password is incorrect";
                    } else if (response.body() != null && response.body().getMessage() != null) {
                        message = response.body().getMessage();
                    }
                    Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChangePasswordResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ChangePasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnChangePassword.setEnabled(!loading);
    }
}
