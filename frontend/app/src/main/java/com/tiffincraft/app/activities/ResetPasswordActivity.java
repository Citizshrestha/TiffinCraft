package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private EditText etNewPassword, etConfirmPassword;
    private ImageView imgToggleNew, imgToggleConfirm;
    private MaterialButton btnReset;
    private TextView tvEmail;
    private String email;
    private boolean isLoading = false;
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        email = getIntent().getStringExtra("email");

        initViews();
        setupOtpBoxes();
        setupListeners();
    }

    private void initViews() {
        tvEmail = findViewById(R.id.tvEmail);
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        imgToggleNew = findViewById(R.id.imgToggleNew);
        imgToggleConfirm = findViewById(R.id.imgToggleConfirm);
        btnReset = findViewById(R.id.btnReset);

        if (tvEmail != null && email != null) {
            tvEmail.setText(getString(R.string.rp_code_sent_to, email));
        }
    }

    private void setupOtpBoxes() {
        EditText[] boxes = {etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    if (s.length() == 1 && index < boxes.length - 1) boxes[index + 1].requestFocus();
                    if (s.length() == 0 && index > 0) boxes[index - 1].requestFocus();
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private String getOtpValue() {
        return etOtp1.getText().toString() + etOtp2.getText().toString()
             + etOtp3.getText().toString() + etOtp4.getText().toString()
             + etOtp5.getText().toString() + etOtp6.getText().toString();
    }

    private void setupListeners() {
        imgToggleNew.setOnClickListener(v -> {
            newPasswordVisible = !newPasswordVisible;
            etNewPassword.setInputType(newPasswordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgToggleNew.setImageResource(newPasswordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

        imgToggleConfirm.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            etConfirmPassword.setInputType(confirmPasswordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgToggleConfirm.setImageResource(confirmPasswordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnReset.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
            attemptReset();
        });
    }

    private void attemptReset() {
        if (isLoading) return;

        String otp = getOtpValue();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (otp.length() < 6) {
            Toast.makeText(this, R.string.otp_incomplete, Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(this, R.string.error_password_length, Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_passwords_dont_match, Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return;
        }

        isLoading = true;
        btnReset.setEnabled(false);
        btnReset.setText(R.string.rp_resetting);
        btnReset.setAlpha(0.7f);

        RetrofitClient.getInstance(this).getApiService()
            .resetPassword(new ResetPasswordRequest(email, otp, newPassword))
            .enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    isLoading = false;
                    btnReset.setEnabled(true);
                    btnReset.setText(R.string.rp_reset_btn);
                    btnReset.setAlpha(1f);

                    if (response.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this,
                            R.string.rp_success, Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        String msg = getString(R.string.rp_invalid_code);
                        if (response.code() == 400) {
                            try {
                                String body = response.errorBody().string();
                                if (body.contains("expired")) msg = getString(R.string.rp_code_expired);
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(ResetPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    isLoading = false;
                    btnReset.setEnabled(true);
                    btnReset.setText(R.string.rp_reset_btn);
                    btnReset.setAlpha(1f);
                    Toast.makeText(ResetPasswordActivity.this,
                        R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
    }
}
