package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityOrderSummaryBinding;

public class OrderSummaryActivity extends AppCompatActivity {
    private ActivityOrderSummaryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPlaceOrder.setOnClickListener(v -> {
            startActivity(new Intent(this, OrderPlacedActivity.class));
        });
    }
}
