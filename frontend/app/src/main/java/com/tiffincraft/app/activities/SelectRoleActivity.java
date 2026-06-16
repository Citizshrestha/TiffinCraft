package com.tiffincraft.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.tiffincraft.app.R;

public class SelectRoleActivity extends AppCompatActivity {

    private View cardCustomer;
    private View cardCook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        cardCustomer = findViewById(R.id.cardCustomer);
        cardCook = findViewById(R.id.cardCook);

        applyEntranceAnimations();

    // Set click listeners with animations
        cardCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardPress(v, () -> navigateToLogin("customer"));
            }
        });

        cardCook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardPress(v, () -> navigateToLogin("cook"));
            }
        });
    }

    private void applyEntranceAnimations() {
        View tvSelectTitle = findViewById(R.id.tvSelectTitle);
        View tvSelectDesc = findViewById(R.id.tvSelectDesc);

        if (tvSelectTitle != null) {
            tvSelectTitle.setAlpha(0f);
            tvSelectTitle.setTranslationY(-30f);
            tvSelectTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(100)
                .start();
        }

        if (tvSelectDesc != null) {
            tvSelectDesc.setAlpha(0f);
            tvSelectDesc.setTranslationY(-20f);
            tvSelectDesc.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
        }

        if (cardCustomer != null) {
            cardCustomer.setAlpha(0f);
            cardCustomer.setTranslationX(-100f);
            cardCustomer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(600)
                .setStartDelay(300)
                .start();
        }

        if (cardCook != null) {
            cardCook.setAlpha(0f);
            cardCook.setTranslationX(-100f);
            cardCook.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(600)
                .setStartDelay(400)
                .start();
        }
    }

    private void animateCardPress(View view, Runnable action) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction(action)
                    .start();
            })
            .start();
    }

    private void navigateToLogin(String role) {
        Intent intent = new Intent(SelectRoleActivity.this, LoginActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}