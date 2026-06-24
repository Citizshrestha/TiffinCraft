package com.tiffincraft.app.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityUpdateStatusBinding;

public class UpdateStatusActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUpdateStatusBinding binding = ActivityUpdateStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnSaveStatus.setOnClickListener(v -> {
            Toast.makeText(this, "Order Status Updated!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}