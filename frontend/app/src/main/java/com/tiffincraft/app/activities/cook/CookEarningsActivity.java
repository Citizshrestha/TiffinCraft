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
        // Setup view all transactions if it exists
        if (binding.tvViewAllTransactions != null) {
            binding.tvViewAllTransactions.setOnClickListener(v -> {
                // TODO: Show all transactions
                Toast.makeText(this, "Full transaction history coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup download statement button if it exists
        if (binding.btnDownloadStatement != null) {
            binding.btnDownloadStatement.setOnClickListener(v -> {
                Toast.makeText(this, "Download statement coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup month selector if it exists
        if (binding.btnSelectMonth != null) {
            binding.btnSelectMonth.setOnClickListener(v -> {
                Toast.makeText(this, "Month selector coming soon", Toast.LENGTH_SHORT).show();
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
                    startActivity(new Intent(this, ManageOrdersActivity.class));
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
