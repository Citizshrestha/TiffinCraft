package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.databinding.ActivityOtpBinding;

public class OtpActivity extends AppCompatActivity {

    private ActivityOtpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String role = getIntent().getStringExtra("role");
        String phone = getIntent().getStringExtra("phone");
        if (phone != null) {
            binding.tvPhoneNumber.setText(phone);
        }

        setupOtpInputs();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnVerify.setOnClickListener(v -> {
            String otp = binding.otp1.getText().toString() +
                         binding.otp2.getText().toString() +
                         binding.otp3.getText().toString() +
                         binding.otp4.getText().toString();

            if (otp.length() == 4) {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();
                
                Intent intent;
                if ("cook".equals(role)) {
                    intent = new Intent(OtpActivity.this, CookHomeActivity.class);
                } else {
                    intent = new Intent(OtpActivity.this, CustomerHomeActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupOtpInputs() {
        binding.otp1.addTextChangedListener(new OtpTextWatcher(binding.otp1, binding.otp2));
        binding.otp2.addTextChangedListener(new OtpTextWatcher(binding.otp2, binding.otp3));
        binding.otp3.addTextChangedListener(new OtpTextWatcher(binding.otp3, binding.otp4));
        binding.otp4.addTextChangedListener(new OtpTextWatcher(binding.otp4, null));
    }

    private class OtpTextWatcher implements TextWatcher {
        private final EditText currentView;
        private final EditText nextView;

        public OtpTextWatcher(EditText currentView, EditText nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}