package com.tiffincraft.app.activities.meal;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.activities.common.CartActivity;
import com.tiffincraft.app.databinding.ActivityMealSelectionBinding;

public class MealSelectionActivity extends AppCompatActivity {
    private ActivityMealSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMealSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddToCart.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });
    }
}
