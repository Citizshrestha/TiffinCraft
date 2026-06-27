package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.databinding.ActivityCookProfileBinding;
import com.tiffincraft.app.session.SessionManager;

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
        if (binding.tvKitchenName != null) {
            binding.tvKitchenName.setText(
                (fullName != null && !fullName.isEmpty()) ? fullName : "Home Cook"
            );
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnEditProfile.setOnClickListener(v ->
            Toast.makeText(this, "Edit Profile — coming soon", Toast.LENGTH_SHORT).show()
        );

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
}
