package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);

            if (sessionManager.isLoggedIn()) {
                String role = sessionManager.getRole();
                Intent intent;
                if ("cook".equals(role)) {
                    intent = new Intent(SplashActivity.this, CookHomeActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, CustomerHomeActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Intent i = new Intent(SplashActivity.this, OnboardingActivity1.class);
                startActivity(i);
            }
            finish();
        }, SPLASH_DELAY);
    }
}
