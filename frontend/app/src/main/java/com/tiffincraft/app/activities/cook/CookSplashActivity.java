package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.auth.LoginActivity;

public class CookSplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Using shared splash layout
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class).putExtra("role", "cook"));
            finish();
        }, 2000);
    }
}
