package com.tiffincraft.app.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.session.SessionManager;

public class CookHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_cook_home);
            
            SessionManager sessionManager = new SessionManager(this);
            
            TextView tvUserName = findViewById(R.id.tvUserName);
            if (tvUserName != null) {
                String fullName = sessionManager.getFullName();
                if (fullName != null && !fullName.isEmpty()) {
                    tvUserName.setText(fullName);
                } else {
                    tvUserName.setText("Home Cook");
                }
            }
            
            Toast.makeText(this, "Cook Home Loaded", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
