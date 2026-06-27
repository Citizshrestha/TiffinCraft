package com.tiffincraft.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.cook.CookHomeActivity;
import com.tiffincraft.app.activities.customer.CustomerHomeActivity;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.LoginResponse;
import com.tiffincraft.app.models.OtpRequest;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.ResendOtpRequest;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpActivity extends AppCompatActivity {

    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private Button btnVerify;
    private TextView tvResendOtp, tvOtpSubtitle;
    private String email, role;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        email = getIntent().getStringExtra("email");
        role = getIntent().getStringExtra("role");

        initViews();
        setupOtpBoxes();
        startResendTimer();

        tvOtpSubtitle.setText(getString(R.string.otp_subtitle_email, email));

        btnVerify.setOnClickListener(v -> verifyOTP());

        tvResendOtp.setOnClickListener(v -> {
            if (canResend) resendOTP();
        });
    }

    private void initViews() {
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        btnVerify = findViewById(R.id.btnVerify);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle);
    }

    private void setupOtpBoxes() {
        EditText[] boxes = {etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6};

        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                    if (s.length() == 0 && index > 0) {
                        boxes[index - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private String getOtpValue() {
        return etOtp1.getText().toString()
                + etOtp2.getText().toString()
                + etOtp3.getText().toString()
                + etOtp4.getText().toString()
                + etOtp5.getText().toString()
                + etOtp6.getText().toString();
    }

    private void verifyOTP() {
        String otp = getOtpValue();

        if (otp.length() < 6) {
            Toast.makeText(this, R.string.otp_incomplete, Toast.LENGTH_SHORT).show();
            return;
        }

        btnVerify.setEnabled(false);
        btnVerify.setText(R.string.otp_verifying);

        OtpRequest request = new OtpRequest(email, otp);

        RetrofitClient.getInstance(this).getApiService().verifyOtp(request)
                .enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnVerify.setEnabled(true);
                btnVerify.setText(R.string.otp_verify_btn);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    SessionManager sessionManager = new SessionManager(OtpActivity.this);
                    sessionManager.saveSession(
                            loginResponse.getToken(),
                            loginResponse.getUser().getRole(),
                            loginResponse.getUser().getId(),
                            loginResponse.getUser().getFullName()
                    );

                    Toast.makeText(OtpActivity.this,
                            R.string.otp_verified_success, Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("cook".equals(loginResponse.getUser().getRole())) {
                        intent = new Intent(OtpActivity.this, CookHomeActivity.class);
                    } else {
                        intent = new Intent(OtpActivity.this, CustomerHomeActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(OtpActivity.this,
                            R.string.otp_invalid, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                btnVerify.setText(R.string.otp_verify_btn);
                Toast.makeText(OtpActivity.this,
                        R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendOTP() {
        canResend = false;
        tvResendOtp.setTextColor(ContextCompat.getColor(this, R.color.text_hint));

        ResendOtpRequest request = new ResendOtpRequest(email);

        RetrofitClient.getInstance(this).getApiService().resendOtp(request)
                .enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpActivity.this,
                            getString(R.string.otp_resent, email), Toast.LENGTH_SHORT).show();
                    startResendTimer();
                } else {
                    Toast.makeText(OtpActivity.this,
                            R.string.otp_resend_failed, Toast.LENGTH_SHORT).show();
                    canResend = true;
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(OtpActivity.this,
                        R.string.otp_resend_failed, Toast.LENGTH_SHORT).show();
                canResend = true;
            }
        });
    }

    private void startResendTimer() {
        canResend = false;
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvResendOtp.setText(getString(R.string.otp_resend_timer_format, seconds));
                tvResendOtp.setTextColor(ContextCompat.getColor(OtpActivity.this, R.color.text_hint));
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResendOtp.setText(R.string.otp_resend_now);
                tvResendOtp.setTextColor(ContextCompat.getColor(OtpActivity.this, R.color.green_primary));
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
