package com.tiffincraft.app.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityOrderDetailsCustomerBinding;

public class OrderDetailsCustomerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOrderDetailsCustomerBinding binding = ActivityOrderDetailsCustomerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
    }
}