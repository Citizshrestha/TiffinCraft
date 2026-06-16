package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;

public class OnboardingActivity2 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding2);

        Button btnNext = findViewById(R.id.btnNext);
        TextView tvSkip = findViewById(R.id.tvSkip);

        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity2.this,
                    SelectRoleActivity.class));
            finish();
        });

        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity2.this,
                    SelectRoleActivity.class));
            finish();
        });
    }
}