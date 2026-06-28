package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.activities.cook.UpdateStatusActivity;
import com.tiffincraft.app.databinding.ActivityCookOrderDetailsBinding;

public class OrderDetailsCookActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCookOrderDetailsBinding binding = ActivityCookOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup back button if it exists
        if (binding.btnBackOrderDetails != null) {
            binding.btnBackOrderDetails.setOnClickListener(v -> finish());
        }
        
        // Setup update status button if it exists
        if (binding.btnUpdateOrderStatus != null) {
            binding.btnUpdateOrderStatus.setOnClickListener(v -> {
                startActivity(new Intent(this, UpdateStatusActivity.class));
            });
        }
    }
}
