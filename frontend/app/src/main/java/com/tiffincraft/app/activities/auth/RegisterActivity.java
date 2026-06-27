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

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.FacebookLoginRequest;
import com.tiffincraft.app.models.GoogleLoginRequest;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.models.RegisterRequest;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.session.SessionManager;

import org.json.JSONObject;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private View btnBack, btnGoogleSignIn, btnFacebookSignIn;
    private TextView tvRoleBadge, tvLogin;
    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private ImageView imgTogglePassword, imgToggleConfirm;

    private String role;
    private SessionManager sessionManager;
    private boolean isLoading = false;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Facebook Login
    private CallbackManager facebookCallbackManager;

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

        sessionManager = new SessionManager(this);

        initGoogleSignIn();
        initFacebookLogin();
        initViews();
        setRoleBadge();
        setupListeners();
        applyEntranceAnimations();
    }

    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

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

    private void initFacebookLogin() {
        facebookCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(facebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "Facebook login success");
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Facebook login cancelled");
                        Toast.makeText(RegisterActivity.this, R.string.oauth_cancelled, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull FacebookException error) {
                        Log.e(TAG, "Facebook login error", error);
                        Toast.makeText(RegisterActivity.this, R.string.oauth_login_failed, Toast.LENGTH_SHORT).show();
                    }
                });
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
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
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

        btnSignUp.setAlpha(0f);
        btnSignUp.setTranslationY(30f);
        btnSignUp.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(650).start();

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
        btnBack.setOnClickListener(v -> finish());

        imgTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgTogglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgTogglePassword.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        imgToggleConfirm.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            if (confirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgToggleConfirm.setImageResource(R.drawable.ic_eye_off);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgToggleConfirm.setImageResource(R.drawable.ic_eye);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnSignUp.setOnClickListener(v -> {
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
            attemptRegister();
        });

        etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        btnFacebookSignIn.setOnClickListener(v -> signInWithFacebook());

        tvLogin.setOnClickListener(v -> finish());
    }

    // ═══════════════════════════════════════════════════════════
    // GOOGLE SIGN-IN
    // ═══════════════════════════════════════════════════════════

    private void signInWithGoogle() {
        if (isLoading) return;

        Toast.makeText(this, R.string.google_signin_loading, Toast.LENGTH_SHORT).show();

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

                                Toast.makeText(RegisterActivity.this, "Welcome to TiffinCraft!", Toast.LENGTH_SHORT).show();
                                navigateToHome(loginResponse.getUser().getRole());
                            } else {
                                Toast.makeText(RegisterActivity.this,
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
                        Toast.makeText(RegisterActivity.this, R.string.oauth_network_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // FACEBOOK SIGN-IN
    // ═══════════════════════════════════════════════════════════

    private void signInWithFacebook() {
        if (isLoading) return;

        Toast.makeText(this, R.string.facebook_signin_loading, Toast.LENGTH_SHORT).show();
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        isLoading = true;

        FacebookLoginRequest request = new FacebookLoginRequest(accessToken.getToken(), role);

        RetrofitClient.getInstance(this).getApiService()
                .facebookLogin(request)
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

                                Toast.makeText(RegisterActivity.this, "Welcome to TiffinCraft!", Toast.LENGTH_SHORT).show();
                                navigateToHome(loginResponse.getUser().getRole());
                            } else {
                                Toast.makeText(RegisterActivity.this,
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
                        Log.e(TAG, "Facebook verification failed", t);
                        Toast.makeText(RegisterActivity.this, R.string.oauth_network_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // REGULAR EMAIL/PASSWORD REGISTRATION
    // ═══════════════════════════════════════════════════════════

    private void attemptRegister() {
        if (isLoading) return;

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!validateInput(fullName, email, phone, password, confirmPassword)) {
            return;
        }

        registerUser(fullName, email, phone, password);
    }

    private boolean validateInput(String fullName, String email, String phone, String password, String confirmPassword) {
        if (fullName.isEmpty()) {
            Toast.makeText(this, R.string.error_name_required, Toast.LENGTH_SHORT).show();
            etFullName.requestFocus();
            return false;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, R.string.error_phone_required, Toast.LENGTH_SHORT).show();
            etPhone.requestFocus();
            return false;
        }

        if (phone.replaceAll("[^0-9]", "").length() < 10) {
            Toast.makeText(this, R.string.error_phone_invalid, Toast.LENGTH_SHORT).show();
            etPhone.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, R.string.error_email_required, Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.error_email_invalid, Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.error_password_length, Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_passwords_dont_match, Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void registerUser(String fullName, String email, String phone, String password) {
        isLoading = true;
        btnSignUp.setEnabled(false);
        btnSignUp.setText(R.string.registering);
        btnSignUp.setAlpha(0.7f);

        RegisterRequest registerRequest = new RegisterRequest(fullName, email, phone, password, role);

        RetrofitClient.getInstance(this).getApiService()
                .register(registerRequest)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                        isLoading = false;
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("Sign Up");
                        btnSignUp.setAlpha(1f);

                        if (response.isSuccessful() && response.body() != null) {
                            RegisterResponse registerResponse = response.body();

                            if (registerResponse.isSuccess()) {
                                Toast.makeText(RegisterActivity.this,
                                    R.string.otp_sent_to_email, Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                                intent.putExtra("email", email);
                                intent.putExtra("role", role);
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
                                    Log.e(TAG, "Error parsing error response", e);
                                    errorMessage = "Server error: " + response.code();
                                }
                            }
                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                        isLoading = false;
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("Sign Up");
                        btnSignUp.setAlpha(1f);

                        String errorMessage = getNetworkErrorMessage(t);
                        Log.e(TAG, "Registration failed", t);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void navigateToHome(String userRole) {
        Intent intent;
        if ("cook".equals(userRole)) {
            intent = new Intent(RegisterActivity.this, CookHomeActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, CustomerHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleErrorResponse(Response<LoginResponse> response) {
        String errorMessage = "Authentication failed. Please try again.";
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
            return "Cannot reach server. Check if backend is running on http://10.0.2.2:5000";
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
