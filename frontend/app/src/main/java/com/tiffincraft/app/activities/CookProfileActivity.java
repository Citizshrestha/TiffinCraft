package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityCookProfileBinding;
import com.tiffincraft.app.session.SessionManager;

public class CookProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCookProfileBinding binding = ActivityCookProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnLogout.setOnClickListener(v -> {
            new SessionManager(this).logout();
            Intent intent = new Intent(this, SelectRoleActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}