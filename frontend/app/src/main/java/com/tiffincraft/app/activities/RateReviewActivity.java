package com.tiffincraft.app.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityRateReviewBinding;

public class RateReviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRateReviewBinding binding = ActivityRateReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSubmitReview.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}