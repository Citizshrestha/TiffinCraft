package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;

public class CookDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack, btnFavorite;
    private TextView tvKitchenName, tvRating, tvFoodType, tvTotalPrice;
    private LinearLayout layoutMenuItems;
    private MaterialButton btnOrderNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_details);

        // Match status bar style from existing app
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setupMenu();
        applyEntranceAnimations();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        tvKitchenName = findViewById(R.id.tvKitchenName);
        tvRating = findViewById(R.id.tvRating);
        tvFoodType = findViewById(R.id.tvFoodType);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        layoutMenuItems = findViewById(R.id.layoutMenuItems);
        btnOrderNow = findViewById(R.id.btnOrderNow);

        btnBack.setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> {
            btnFavorite.setSelected(!btnFavorite.isSelected());
            btnFavorite.setColorFilter(btnFavorite.isSelected() ? 
                ContextCompat.getColor(this, R.color.orange_warm) : 
                ContextCompat.getColor(this, R.color.text_subtitle));
        });

        btnOrderNow.setOnClickListener(v -> {
            Intent intent = new Intent(CookDetailsActivity.this, MealSelectionActivity.class);
            startActivity(intent);
        });
    }

    private void setupMenu() {
        // According to Design Screen 11: Anita's Kitchen Menu
        String[] menuItems = {
            "Dal Tadka",
            "Jeera Rice",
            "Roti (2)",
            "Fresh Salad",
            "Home-style Curd"
        };

        for (String item : menuItems) {
            addMenuItemView(item);
        }
    }

    private void addMenuItemView(String name) {
        View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, layoutMenuItems, false);
        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText("• " + name);
        textView.setTextColor(ContextCompat.getColor(this, R.color.text_on_light));
        textView.setTextSize(15);
        layoutMenuItems.addView(view);
    }

    private void applyEntranceAnimations() {
        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            appBarLayout.setAlpha(0f);
            appBarLayout.setTranslationY(-50f);
            appBarLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start();
        }
    }
}