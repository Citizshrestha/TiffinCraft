package com.tiffincraft.app.activities;

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

        btnResetPassword.setOnClickListener(v -> {
            // Add button press animation
            v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
                })
                .start();

            attemptPasswordReset();
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptPasswordReset() {
        if (isLoading) {
            return;
        }

        tilEmail.setError(null);

        String email = etEmail.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            tilEmail.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.shake));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            tilEmail.startAnimation(android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.shake));
            return;
        }

        isLoading = true;
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Sending...");
        btnResetPassword.setAlpha(0.7f);

        btnResetPassword.postDelayed(() -> {
            isLoading = false;
            btnResetPassword.setEnabled(true);
            btnResetPassword.setText("Send Reset Link");
            btnResetPassword.setAlpha(1f);

            // Show success message
            Toast.makeText(this,
                "Password reset link sent to " + email + "\n(Feature coming soon)",
                Toast.LENGTH_LONG).show();

            // Close activity after a delay
            btnResetPassword.postDelayed(this::finish, 1500);
        }, 2000);
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
            cardBack.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(400)
                .setStartDelay(100)
                .start();
        }

        if (imgIllustration != null) {
            imgIllustration.setAlpha(0f);
            imgIllustration.setScaleX(0.8f);
            imgIllustration.setScaleY(0.8f);
            imgIllustration.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
        }

        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(-30f);
            tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300)
                .start();
        }

        if (tvSubtitle != null) {
            tvSubtitle.setAlpha(0f);
            tvSubtitle.setTranslationY(-20f);
            tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(400)
                .start();
        }

        if (cardMainContent != null) {
            cardMainContent.setAlpha(0f);
            cardMainContent.setTranslationY(50f);
            cardMainContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(500)
                .start();
        }

        if (layoutLoginLink != null) {
            layoutLoginLink.setAlpha(0f);
            layoutLoginLink.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(650)
                .start();
        }
    }
}