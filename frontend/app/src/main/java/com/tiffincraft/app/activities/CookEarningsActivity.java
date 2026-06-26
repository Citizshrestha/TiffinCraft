package com.tiffincraft.app.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityCookEarningsBinding;

public class CookEarningsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCookEarningsBinding binding = ActivityCookEarningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
    }
}