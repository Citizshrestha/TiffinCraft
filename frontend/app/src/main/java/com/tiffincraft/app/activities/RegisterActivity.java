package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tiffincraft.app.R;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.RegisterRequest;
import com.tiffincraft.app.models.RegisterResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private View btnBack;
    private TextView tvRoleBadge, tvLogin;
    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private ImageView imgTogglePassword, imgToggleConfirm;

    private String role;
    private boolean isLoading = false;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        role = getIntent().getStringExtra("role");
        if (role == null || role.isEmpty()) {
            role = "customer";
        }

        initViews();
        setRoleBadge();
        setupListeners();
        applyEntranceAnimations();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvLogin = findViewById(R.id.tvLogin);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        imgTogglePassword = findViewById(R.id.imgTogglePassword);
        imgToggleConfirm = findViewById(R.id.imgToggleConfirm);
    }

    private void applyEntranceAnimations() {
        View tvTitle = findViewById(R.id.tvTitle);
        View tvSubtitle = findViewById(R.id.tvSubtitle);
        View layoutLoginLink = findViewById(R.id.layoutLoginLink);

        btnBack.setAlpha(0f);
        btnBack.setTranslationX(-50f);
        btnBack.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(400)
            .setStartDelay(100)
            .start();

        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(-30f);
            tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(150)
                .start();
        }

        if (tvSubtitle != null) {
            tvSubtitle.setAlpha(0f);
            tvSubtitle.setTranslationY(-20f);
            tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(250)
                .start();
        }

        tvRoleBadge.setAlpha(0f);
        tvRoleBadge.setScaleX(0.9f);
        tvRoleBadge.setScaleY(0.9f);
        tvRoleBadge.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay(350)
            .start();

        View[] inputViews = { etFullName, etPhone, etEmail,
                              etPassword.getRootView().equals(etPassword) ? etPassword : (View) etPassword.getParent(),
                              etConfirmPassword.getRootView().equals(etConfirmPassword) ? etConfirmPassword : (View) etConfirmPassword.getParent() };

        etFullName.setAlpha(0f);
        etFullName.setTranslationY(30f);
        etFullName.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(400).start();

        View passwordParent = (View) etPassword.getParent();
        View confirmParent = (View) etConfirmPassword.getParent();

        etPhone.setAlpha(0f);
        etPhone.setTranslationY(30f);
        etPhone.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(450).start();

        etEmail.setAlpha(0f);
        etEmail.setTranslationY(30f);
        etEmail.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(500).start();

        passwordParent.setAlpha(0f);
        passwordParent.setTranslationY(30f);
        passwordParent.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(550).start();

        confirmParent.setAlpha(0f);
        confirmParent.setTranslationY(30f);
        confirmParent.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(600).start();

        // Sign Up button animation
        btnSignUp.setAlpha(0f);
        btnSignUp.setTranslationY(30f);
        btnSignUp.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(650).start();

        // Login link animation
        if (layoutLoginLink != null) {
            layoutLoginLink.setAlpha(0f);
            layoutLoginLink.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(750)
                .start();
        }
    }

    private void setRoleBadge() {
        if ("cook".equals(role)) {
            tvRoleBadge.setText(R.string.registering_as_cook);
        } else {
            tvRoleBadge.setText(R.string.registering_as_customer);
        }
    }

    private void setupListeners() {
        // Back button click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Password eye toggle
        imgTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordVisible = !passwordVisible;
                if (passwordVisible) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgTogglePassword.setImageResource(R.drawable.ic_eye_off);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgTogglePassword.setImageResource(R.drawable.ic_eye);
                }
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        // Confirm password eye toggle
        imgToggleConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPasswordVisible = !confirmPasswordVisible;
                if (confirmPasswordVisible) {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgToggleConfirm.setImageResource(R.drawable.ic_eye_off);
                } else {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgToggleConfirm.setImageResource(R.drawable.ic_eye);
                }
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            }
        });

        // Sign Up button click with animation
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                        }
                    })
                    .start();
                attemptRegister();
            }
        });

        // Confirm password field - Done action triggers registration
        etConfirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        // Login link click
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void attemptRegister() {
        if (isLoading) {
            return;
        }

        // Get input values
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (fullName.isEmpty()) {
            Toast.makeText(this, R.string.error_name_required, Toast.LENGTH_SHORT).show();
            etFullName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, R.string.error_phone_required, Toast.LENGTH_SHORT).show();
            etPhone.requestFocus();
            return;
        }
        if (phone.replaceAll("[^0-9]", "").length() < 10) {
            Toast.makeText(this, R.string.error_phone_invalid, Toast.LENGTH_SHORT).show();
            etPhone.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, R.string.error_email_required, Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.error_email_invalid, Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.error_password_length, Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_passwords_dont_match, Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return;
        }

        registerUser(fullName, email, phone, password);
    }

    private void registerUser(String fullName, String email, String phone, String password) {
        isLoading = true;
        btnSignUp.setEnabled(false);
        btnSignUp.setText(R.string.registering);
        btnSignUp.setAlpha(0.7f);

        RegisterRequest registerRequest = new RegisterRequest(fullName, email, phone, password, role);

        RetrofitClient.getInstance(RegisterActivity.this).getApiService().register(registerRequest).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                isLoading = false;
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Sign Up");
                btnSignUp.setAlpha(1f);

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();

                    if (registerResponse.isSuccess()) {
                        Toast.makeText(RegisterActivity.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                        // Navigate to OTP Screen
                        Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                        intent.putExtra("role", role);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                            registerResponse.getMessage() != null ? registerResponse.getMessage() : "Registration failed",
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMessage = "Registration failed. Please try again.";
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            JSONObject errorObj = new JSONObject(errorJson);
                            errorMessage = errorObj.optString("message", errorMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorMessage = "Server error: " + response.code();
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                isLoading = false;
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Sign Up");
                btnSignUp.setAlpha(1f);

                String errorMessage;
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "Cannot reach server. Check if backend is running on http://10.0.2.2:5000";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "Connection timeout. Server is taking too long to respond.";
                } else if (t instanceof java.net.ConnectException) {
                    errorMessage = "Connection refused. Is the backend server running?";
                } else if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMessage = "Network error: Unable to resolve host. Check your internet connection.";
                } else {
                    errorMessage = "Network error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                }

                android.util.Log.e("RegisterActivity", "Registration failed", t);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
