package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityOrderPlacedBinding;

public class OrderPlacedActivity extends AppCompatActivity {
    private ActivityOrderPlacedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderPlacedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        
        binding.btnTrackOrder.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrackOrderActivity.class);
            startActivity(intent);
        });
    }
}
