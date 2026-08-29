package com.tiffincraft.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ForgotPasswordRequest;
import com.tiffincraft.app.models.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView imgBack;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnResetPassword;
    private CircularProgressIndicator progressSending;
    private TextView tvBackToLogin;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setupListeners();
        applyEntranceAnimations();
    }

    private void initViews() {
        imgBack = findViewById(R.id.imgBack);
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressSending = findViewById(R.id.progressSending);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupListeners() {
        imgBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        btnResetPassword.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
            attemptPasswordReset();
        });

        // The field already advertises actionDone; without this the key did nothing.
        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            attemptPasswordReset();
            return true;
        });

        // Clear a stale "invalid email" the moment the user starts correcting it,
        // rather than leaving red text under a field they are actively fixing.
        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (tilEmail.getError() != null) tilEmail.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
    }

    /**
     * Loading state lives in one place so no path can leave the button stuck
     * disabled — the previous version repeated the reset in three callbacks.
     */
    private void setLoading(boolean loading) {
        isLoading = loading;
        btnResetPassword.setEnabled(!loading);
        // Text is cleared, not swapped for "Sending…": the spinner is drawn on top of
        // the button and would otherwise sit over the label.
        btnResetPassword.setText(loading ? "" : getString(R.string.fp_send_code));
        btnResetPassword.setIconResource(loading ? 0 : R.drawable.ic_arrow_forward);
        btnResetPassword.setAlpha(loading ? 0.75f : 1f);
        progressSending.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void attemptPasswordReset() {
        if (isLoading) return;

        tilEmail.setError(null);
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            etEmail.requestFocus();
            return;
        }

        setLoading(true);

        RetrofitClient.getInstance(this).getApiService()
            .forgotPassword(new ForgotPasswordRequest(email))
            .enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    setLoading(false);

                    if (response.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                            R.string.fp_code_sent, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ForgotPasswordActivity.this,
                            ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    } else {
                        // The server says why (unverified account, mail send failed);
                        // a fixed "Failed to send reset code" hid all of it.
                        String message = serverMessage(response);
                        Toast.makeText(ForgotPasswordActivity.this,
                            message != null ? message : getString(R.string.fp_send_failed),
                            Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    setLoading(false);
                    // A timeout here is almost always the free-tier backend cold-starting,
                    // not a dead connection — "Network error. Please try again." sent users
                    // hunting for a Wi-Fi problem they didn't have.
                    boolean timedOut = t instanceof java.net.SocketTimeoutException;
                    Toast.makeText(ForgotPasswordActivity.this,
                        timedOut ? R.string.fp_slow_network : R.string.network_error,
                        Toast.LENGTH_LONG).show();
                }
            });
    }

    /** The server's own "message" for a non-2xx response, or null. */
    private String serverMessage(Response<RegisterResponse> response) {
        try {
            if (response.errorBody() == null) return null;
            String message = new org.json.JSONObject(response.errorBody().string())
                    .optString("message", "");
            return message.isEmpty() ? null : message;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyEntranceAnimations() {
        View cardBack = findViewById(R.id.cardBack);
        View imgIllustration = findViewById(R.id.imgIllustration);
        View tvTitle = findViewById(R.id.tvTitle);
        View tvSubtitle = findViewById(R.id.tvSubtitle);
        View cardMainContent = findViewById(R.id.cardMainContent);
        View layoutLoginLink = findViewById(R.id.layoutLoginLink);

        if (cardBack != null) {
            cardBack.setAlpha(0f);
            cardBack.setTranslationX(-50f);
            cardBack.animate().alpha(1f).translationX(0f).setDuration(400).setStartDelay(100).start();
        }
        if (imgIllustration != null) {
            imgIllustration.setAlpha(0f);
            imgIllustration.setScaleX(0.8f);
            imgIllustration.setScaleY(0.8f);
            imgIllustration.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setStartDelay(200).start();
        }
        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(-30f);
            tvTitle.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(300).start();
        }
        if (tvSubtitle != null) {
            tvSubtitle.setAlpha(0f);
            tvSubtitle.setTranslationY(-20f);
            tvSubtitle.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(400).start();
        }
        if (cardMainContent != null) {
            cardMainContent.setAlpha(0f);
            cardMainContent.setTranslationY(50f);
            cardMainContent.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(500).start();
        }
        if (layoutLoginLink != null) {
            layoutLoginLink.setAlpha(0f);
            layoutLoginLink.animate().alpha(1f).setDuration(500).setStartDelay(650).start();
        }
    }
}
