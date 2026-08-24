package com.tiffincraft.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.cook.CookHomeActivity;
import com.tiffincraft.app.activities.customer.CustomerHomeActivity;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.GoogleLoginRequest;
import com.tiffincraft.app.models.LoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.session.SessionManager;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;
    private View btnGoogleSignIn, btnFacebookSignIn;
    private ImageView imgTogglePassword;

    private String role;
    private SessionManager sessionManager;
    private boolean isLoading = false;
    private boolean isPasswordVisible = false;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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

        initGoogleSignIn();
        initViews();
        setupListeners();
    }

    private void initGoogleSignIn() {
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Register activity result launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(this, R.string.oauth_cancelled, Toast.LENGTH_SHORT).show();
                    }
                }
        );
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
        btnLogin.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start())
                .start();
            attemptLogin();
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        imgTogglePassword.setOnClickListener(v -> {
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
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        btnFacebookSignIn.setOnClickListener(v -> {
            Toast.makeText(this, "Facebook login will be available soon!", Toast.LENGTH_SHORT).show();
        });

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.putExtra("role", role);
            startActivity(intent);
        });
    }

    // ═══════════════════════════════════════════════════════════
    // GOOGLE SIGN-IN
    // ═══════════════════════════════════════════════════════════

    private void signInWithGoogle() {
        if (isLoading) return;

        Toast.makeText(this, R.string.google_signin_loading, Toast.LENGTH_SHORT).show();

        // Sign out first to force account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();

            if (idToken != null) {
                Log.d(TAG, "Google ID Token obtained");
                verifyGoogleToken(idToken);
            } else {
                Log.e(TAG, "Google ID Token is null");
                Toast.makeText(this, R.string.oauth_login_failed, Toast.LENGTH_SHORT).show();
            }

        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            Log.e(TAG, "Google sign-in failed: " + statusCode, e);
            Toast.makeText(this, getString(R.string.google_signin_failed_code, statusCode), Toast.LENGTH_LONG).show();
        }
    }

    private void verifyGoogleToken(String idToken) {
        isLoading = true;

        GoogleLoginRequest request = new GoogleLoginRequest(idToken, role);

        RetrofitClient.getInstance(this).getApiService()
                .googleLogin(request)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                        isLoading = false;

                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse loginResponse = response.body();

                            if (loginResponse.isSuccess() && loginResponse.getUser() != null) {
                                sessionManager.createSession(
                                        loginResponse.getUser().getRole(),
                                        loginResponse.getToken(),
                                        String.valueOf(loginResponse.getUser().getId()),
                                        loginResponse.getUser().getFullName()
                                );
                                
                                // Save profile image if present
                                if (loginResponse.getUser().getProfileImage() != null) {
                                    sessionManager.saveProfileImage(loginResponse.getUser().getProfileImage());
                                }

                                Toast.makeText(LoginActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                                navigateToHome(loginResponse.getUser().getRole());
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        loginResponse.getMessage() != null ? loginResponse.getMessage() : getString(R.string.oauth_login_failed),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                        isLoading = false;
                        Log.e(TAG, "Google verification failed", t);
                        Toast.makeText(LoginActivity.this, R.string.oauth_network_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // REGULAR EMAIL/PASSWORD LOGIN
    // ═══════════════════════════════════════════════════════════

    private void attemptLogin() {
        if (isLoading) return;

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        loginUser(email, password);
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_email_required), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_email_invalid), Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_password_required), Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.error_password_length), Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        isLoading = true;
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        btnLogin.setAlpha(0.7f);

        LoginRequest loginRequest = new LoginRequest(email, password);

        RetrofitClient.getInstance(this).getApiService()
                .login(loginRequest)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
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
                                
                                // Save profile image if present
                                if (loginResponse.getUser().getProfileImage() != null) {
                                    sessionManager.saveProfileImage(loginResponse.getUser().getProfileImage());
                                }

                                Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                navigateToHome(loginResponse.getUser().getRole());
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        loginResponse.getMessage() != null ? loginResponse.getMessage() : "Login failed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                        isLoading = false;
                        btnLogin.setEnabled(true);
                        btnLogin.setText(R.string.login_btn);
                        btnLogin.setAlpha(1f);

                        String errorMessage = getNetworkErrorMessage(t);
                        Log.e(TAG, "Login failed", t);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void navigateToHome(String userRole) {
        Intent intent;
        if ("cook".equals(userRole)) {
            intent = new Intent(LoginActivity.this, CookHomeActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleErrorResponse(Response<LoginResponse> response) {
        String errorMessage = "Login failed. Please try again.";
        if (response.errorBody() != null) {
            try {
                String errorJson = response.errorBody().string();
                JSONObject errorObj = new JSONObject(errorJson);
                errorMessage = errorObj.optString("message", errorMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response", e);
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private String getNetworkErrorMessage(Throwable t) {
        if (t instanceof java.net.UnknownHostException) {
            return "Cannot reach the server. Check your internet connection and try again.";
        } else if (t instanceof java.net.SocketTimeoutException) {
            return "Connection timeout. Server is taking too long to respond.";
        } else if (t instanceof java.net.ConnectException) {
            return "Connection refused. Is the backend server running?";
        } else if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
            return "Network error: Unable to resolve host. Check your internet connection.";
        } else {
            return "Network error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
        }
    }
}
