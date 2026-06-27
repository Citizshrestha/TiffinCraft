package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.databinding.ActivityCookProfileBinding;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.activities.onboarding.SelectRoleActivity;

public class CookProfileActivity extends AppCompatActivity {

    private ActivityCookProfileBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Display cook's name from session
        String fullName = sessionManager.getFullName();
        if (binding.tvCookName != null) {
            binding.tvCookName.setText(
                (fullName != null && !fullName.isEmpty()) ? fullName : "Home Cook"
            );
        }
        
        if (binding.tvKitchenNameProfile != null) {
            binding.tvKitchenNameProfile.setText(
                (fullName != null && !fullName.isEmpty()) ? fullName + "'s Kitchen" : "Home Cook Kitchen"
            );
        }

        // Settings button
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Edit profile menu item
        if (binding.menuEditKitchenProfile != null) {
            binding.menuEditKitchenProfile.setOnClickListener(v ->
                Toast.makeText(this, "Edit Profile — coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Logout button
        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        sessionManager.logout();
                        Intent intent = new Intent(this, SelectRoleActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        
        // Setup other menu items
        setupMenuItems();
        
        // Setup bottom navigation
        setupBottomNavigation();
    }
    
    private void setupBottomNavigation() {
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
            
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_home) {
                    // Navigate to Cook Home
                    Intent intent = new Intent(CookProfileActivity.this, CookHomeActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_meals) {
                    startActivity(new Intent(CookProfileActivity.this, AddMenuActivity.class));
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    startActivity(new Intent(CookProfileActivity.this, ManageOrdersActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                
                return false;
            });
        }
    }
    
    private void setupMenuItems() {
        if (binding.menuPayoutDetails != null) {
            binding.menuPayoutDetails.setOnClickListener(v ->
                Toast.makeText(this, "Payout Details — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
        
        if (binding.menuDocuments != null) {
            binding.menuDocuments.setOnClickListener(v ->
                Toast.makeText(this, "Documents — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
        
        if (binding.menuHelpSupport != null) {
            binding.menuHelpSupport.setOnClickListener(v ->
                Toast.makeText(this, "Help & Support — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
        
        if (binding.menuAbout != null) {
            binding.menuAbout.setOnClickListener(v ->
                Toast.makeText(this, "About TiffinCraft — coming soon", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
