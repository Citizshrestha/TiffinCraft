package com.tiffincraft.app.activities.common;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.activities.order.OrderSummaryActivity;
import com.tiffincraft.app.databinding.ActivityDeliveryAddressBinding;

public class DeliveryAddressActivity extends AppCompatActivity {
    private ActivityDeliveryAddressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeliveryAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnProceed.setOnClickListener(v -> {
            startActivity(new Intent(this, OrderSummaryActivity.class));
        });
    }
}
