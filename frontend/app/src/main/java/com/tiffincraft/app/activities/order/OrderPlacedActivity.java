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
        int[] orderIds = getIntent().getIntArrayExtra("order_ids");

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

        // Both COD and online checkout create the same order records, so the
        // success screen should expose the same order actions for both methods.
        if (binding.btnOrderDetails != null) {
            binding.btnOrderDetails.setVisibility(android.view.View.VISIBLE);
            binding.btnOrderDetails.setOnClickListener(v -> {
                if (orderIds != null && orderIds.length == 1) {
                    Intent intent = new Intent(this, OrderDetailsCustomerActivity.class);
                    intent.putExtra("order_id", orderIds[0]);
                    startActivity(intent);
                } else {
                    // Multiple cooks in one cart → multiple orders, each paid
                    // separately — send them to the list rather than guessing which one.
                    startActivity(new Intent(this, OrderHistoryActivity.class));
                }
                finish();
            });
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
