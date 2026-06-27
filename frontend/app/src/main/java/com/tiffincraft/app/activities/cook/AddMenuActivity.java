package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tiffincraft.app.databinding.ActivityAddMenuBinding;

public class AddMenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAddMenuBinding binding = ActivityAddMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPublish.setOnClickListener(v -> {
            Toast.makeText(this, "Menu Published Successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
