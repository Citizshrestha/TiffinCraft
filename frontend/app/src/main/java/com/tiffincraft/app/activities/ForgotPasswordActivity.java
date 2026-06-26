package com.tiffincraft.app.activities;

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

        isLoading = true;
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText(R.string.fp_sending);
        btnResetPassword.setAlpha(0.7f);

        RetrofitClient.getInstance(this).getApiService()
            .forgotPassword(new ForgotPasswordRequest(email))
            .enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    isLoading = false;
                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText(R.string.fp_send_code);
                    btnResetPassword.setAlpha(1f);

                    if (response.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                            R.string.fp_code_sent, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ForgotPasswordActivity.this,
                            ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                            R.string.fp_send_failed, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    isLoading = false;
                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText(R.string.fp_send_code);
                    btnResetPassword.setAlpha(1f);
                    Toast.makeText(ForgotPasswordActivity.this,
                        R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
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
