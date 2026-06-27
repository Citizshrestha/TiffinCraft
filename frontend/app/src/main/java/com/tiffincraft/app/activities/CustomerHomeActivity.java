package com.tiffincraft.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CustomerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_customer_home);
            
            SessionManager sessionManager = new SessionManager(this);
            
            TextView tvUserName = findViewById(R.id.tvUserName);
            if (tvUserName != null) {
                String fullName = sessionManager.getFullName();
                if (fullName != null && !fullName.isEmpty()) {
                    tvUserName.setText(fullName);
                } else {
                    tvUserName.setText("Customer");
                }
            }
            
            Toast.makeText(this, "Customer Home Loaded", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
