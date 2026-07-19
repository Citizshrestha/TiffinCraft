package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.activities.customer.CustomerHomeActivity;
import com.tiffincraft.app.databinding.ActivityOrderPlacedBinding;

public class OrderPlacedActivity extends AppCompatActivity {
    private ActivityOrderPlacedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderPlacedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int orderCount = getIntent().getIntExtra("order_count", 1);
        String message = getIntent().getStringExtra("message");

        TextView tvMessage = findViewById(com.tiffincraft.app.R.id.tvOrderMessage);
        if (tvMessage != null) {
            if (message != null && !message.isEmpty()) {
                tvMessage.setText(message);
            } else if (orderCount > 1) {
                tvMessage.setText(orderCount + " orders placed — each cook was notified separately.");
            } else {
                tvMessage.setText("Your cook has been notified of your order.");
            }
        }

        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        binding.btnTrackOrder.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderHistoryActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
