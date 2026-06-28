package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.R;
import com.tiffincraft.app.databinding.ActivityCookEarningsBinding;

public class CookEarningsActivity extends AppCompatActivity {
    private ActivityCookEarningsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookEarningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
        setupBottomNavigation();
    }

    private void setupClickListeners() {
        // Setup withdraw earnings button if it exists
        if (binding.btnWithdrawEarnings != null) {
            binding.btnWithdrawEarnings.setOnClickListener(v -> {
                // TODO: Implement withdrawal flow
                Toast.makeText(this, "Withdrawal feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup view all transactions if it exists
        if (binding.tvViewAllTransactions != null) {
            binding.tvViewAllTransactions.setOnClickListener(v -> {
                // TODO: Show all transactions
                Toast.makeText(this, "Transaction history coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_earnings);

            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, CookHomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_meals) {
                    startActivity(new Intent(this, CookMealActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    Toast.makeText(this, "Orders screen coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_earnings) {
                    return true; // Already on this screen
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, CookProfileActivity.class));
                    finish();
                    return true;
                }

                return false;
            });
        }
    }
}
