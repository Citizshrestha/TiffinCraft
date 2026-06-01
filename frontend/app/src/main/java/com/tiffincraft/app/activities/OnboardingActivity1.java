package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;

public class OnboardingActivity1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding1);

        Button btnNext = findViewById(R.id.btnNext);
        TextView tvSkip = findViewById(R.id.tvSkip);

        // Next button → go to Onboarding 2
        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity1.this,
                    OnboardingActivity2.class));
        });

        // Skip → go directly to Select Role
        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity1.this,
                    SelectRoleActivity.class));
            finish();
        });
    }
}