package com.tiffincraft.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.databinding.ActivitySearchFilterBinding;

public class SearchFilterActivity extends AppCompatActivity {

    private ActivitySearchFilterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.tvReset.setOnClickListener(v -> {
            binding.etSearch.setText("");
            Toast.makeText(this, "Filters reset", Toast.LENGTH_SHORT).show();
        });

        binding.btnClearAll.setOnClickListener(v -> {
            binding.etSearch.setText("");
            // Logic to clear chip selections
        });

        binding.btnApply.setOnClickListener(v -> {
            String query = binding.etSearch.getText().toString();
            Toast.makeText(this, "Applying filters for: " + query, Toast.LENGTH_SHORT).show();
            // Pass filters back to CookListActivity
            finish();
        });
    }
}