package com.tiffincraft.app.activities.onboarding;

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

        btnNext.setOnClickListener(v -> {
            Intent i = new Intent(OnboardingActivity1.this,
                    OnboardingActivity2.class);
            startActivity(i);
            finish();
        });

        tvSkip.setOnClickListener(v -> {
            Intent i = new Intent(OnboardingActivity1.this,
                    SelectRoleActivity.class);
            startActivity(i);
            finish();
        });
    }
}
