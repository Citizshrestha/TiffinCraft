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
import com.tiffincraft.app.models.LoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.session.SessionManager;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;
    private View btnGoogleSignIn, btnFacebookSignIn;
    private ImageView imgTogglePassword;

    private String role;
    private SessionManager sessionManager;
    private boolean isLoading = false;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        role = getIntent().getStringExtra("role");
        if (role == null || role.isEmpty()) {
            role = "customer";
        }

        sessionManager = new SessionManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
        imgTogglePassword = findViewById(R.id.imgTogglePassword);

        // Apply animations for smooth entrance
        applyEntranceAnimations();
    }

    private void applyEntranceAnimations() {
        View cardLogo = findViewById(R.id.cardLogo);
        View tvTitle = findViewById(R.id.tvTitle);
        View tvSubtitle = findViewById(R.id.tvSubtitle);
        View layoutRegisterLink = findViewById(R.id.layoutRegisterLink);

        if (cardLogo != null) {
            cardLogo.setAlpha(0f);
            cardLogo.setScaleX(0.8f);
            cardLogo.setScaleY(0.8f);
            cardLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(100)
                .start();
        }

        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(-30f);
            tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
        }

        if (tvSubtitle != null) {
            tvSubtitle.setAlpha(0f);
            tvSubtitle.setTranslationY(-20f);
            tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300)
                .start();
        }

        if (etEmail != null) {
            etEmail.setAlpha(0f);
            etEmail.setTranslationY(30f);
            etEmail.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(400)
                .start();
        }

        View passwordContainer = etPassword != null ? (View) etPassword.getParent() : null;
        if (passwordContainer != null) {
            passwordContainer.setAlpha(0f);
            passwordContainer.setTranslationY(30f);
            passwordContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(450)
                .start();
        }

        if (btnLogin != null) {
            btnLogin.setAlpha(0f);
            btnLogin.setTranslationY(30f);
            btnLogin.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(500)
                .start();
        }

        if (layoutRegisterLink != null) {
            layoutRegisterLink.setAlpha(0f);
            layoutRegisterLink.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(600)
                .start();
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
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
                attemptLogin();
            }
        });

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        imgTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgTogglePassword.setImageResource(R.drawable.ic_eye_off);
                    isPasswordVisible = false;
                } else {
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgTogglePassword.setImageResource(R.drawable.ic_eye);
                    isPasswordVisible = true;
                }
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, R.string.google_signin_coming_soon, Toast.LENGTH_SHORT).show();
            }
        });

        btnFacebookSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, R.string.facebook_signin_coming_soon, Toast.LENGTH_SHORT).show();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                intent.putExtra("role", role);
                startActivity(intent);
            }
        });
    }

    private void attemptLogin() {
        if (isLoading) {
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;

        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_email_required), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_email_invalid), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            isValid = false;
        }

        if (isValid && password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_password_required), Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            isValid = false;
        } else if (isValid && password.length() < 6) {
            Toast.makeText(this, getString(R.string.error_password_length), Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        loginUser(email, password);
    }

    private void loginUser(String email, String password) {
        isLoading = true;
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        btnLogin.setAlpha(0.7f);

        LoginRequest loginRequest = new LoginRequest(email, password);

        RetrofitClient.getInstance(LoginActivity.this).getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                isLoading = false;
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login_btn);
                btnLogin.setAlpha(1f);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getUser() != null) {
                        sessionManager.createSession(
                            loginResponse.getUser().getRole(),
                            loginResponse.getToken(),
                            String.valueOf(loginResponse.getUser().getId()),
                            loginResponse.getUser().getFullName()
                        );

                        Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();

                        Intent intent;
                        if ("cook".equals(loginResponse.getUser().getRole())) {
                            intent = new Intent(LoginActivity.this, CookHomeActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                            loginResponse.getMessage() != null ? loginResponse.getMessage() : "Login failed",
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "Login failed. Please check your credentials.";
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            JSONObject errorObj = new JSONObject(errorJson);
                            errorMessage = errorObj.optString("message", errorMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                isLoading = false;
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login_btn);
                btnLogin.setAlpha(1f);

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

                android.util.Log.e("LoginActivity", "Login failed", t);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}