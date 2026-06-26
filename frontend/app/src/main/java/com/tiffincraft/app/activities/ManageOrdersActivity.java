package com.tiffincraft.app.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityManageOrdersBinding;

public class ManageOrdersActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityManageOrdersBinding binding = ActivityManageOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
    }
}