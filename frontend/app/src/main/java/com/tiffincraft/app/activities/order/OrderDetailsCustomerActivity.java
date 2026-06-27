package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.activities.common.RateReviewActivity;
import com.tiffincraft.app.activities.order.TrackOrderActivity;
import com.tiffincraft.app.databinding.ActivityOrderDetailsCustomerBinding;

public class OrderDetailsCustomerActivity extends AppCompatActivity {

    private ActivityOrderDetailsCustomerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsCustomerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // "Rate & Review" button — wire to RateReviewActivity if it exists in the layout
        try {
            binding.btnRateReview.setOnClickListener(v ->
                startActivity(new Intent(this, RateReviewActivity.class))
            );
        } catch (NullPointerException ignored) {
            // button may not be present in all layout variants
        }

        // "Track Order" button — wire to TrackOrderActivity if it exists
        try {
            binding.btnTrackOrder.setOnClickListener(v ->
                startActivity(new Intent(this, TrackOrderActivity.class))
            );
        } catch (NullPointerException ignored) {}
    }
}
