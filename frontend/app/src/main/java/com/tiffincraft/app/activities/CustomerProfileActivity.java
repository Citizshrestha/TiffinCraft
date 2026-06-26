package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CustomerProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        sessionManager = new SessionManager(this);

        // Display user name
        TextView tvName = findViewById(R.id.tvCustomerName);
        String fullName = sessionManager.getFullName();
        if (tvName != null) {
            tvName.setText((fullName != null && !fullName.isEmpty()) ? fullName : "Customer");
        }

        // Display user email (stored as userId for now; show role)
        TextView tvRole = findViewById(R.id.tvCustomerRole);
        if (tvRole != null) {
            tvRole.setText("Customer Account");
        }

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Order history shortcut
        View layoutOrders = findViewById(R.id.layoutOrders);
        if (layoutOrders != null) {
            layoutOrders.setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class))
            );
        }

        // Favorites shortcut
        View layoutFavorites = findViewById(R.id.layoutFavorites);
        if (layoutFavorites != null) {
            layoutFavorites.setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class))
            );
        }

        // Logout
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
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
}
