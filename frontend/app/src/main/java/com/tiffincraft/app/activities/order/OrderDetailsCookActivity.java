package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityOrderDetailsCookBinding;

public class OrderDetailsCookActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOrderDetailsCookBinding binding = ActivityOrderDetailsCookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnUpdateStatus.setOnClickListener(v -> {
            startActivity(new Intent(this, UpdateStatusActivity.class));
        });
    }
}
