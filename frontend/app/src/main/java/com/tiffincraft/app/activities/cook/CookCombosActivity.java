package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ComboRequest;
import com.tiffincraft.app.models.ComboResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cook-side management screen: create, edit, delete, and toggle combo deals —
 * the one-time-bundle sibling of CookSubscriptionsActivity's recurring plans.
 * Every combo a cook creates here shows up on their public profile
 * automatically (see CookDetailsActivity), same as subscription plans.
 */
public class CookCombosActivity extends AppCompatActivity {

    private static final int REQUEST_COMBO_FORM = 4101;

    private ApiService apiService;
    private SessionManager sessionManager;

    private TextView tvCombosCount, tvEmptyCombos;
    private android.widget.LinearLayout layoutCombos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_combos);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        tvCombosCount = findViewById(R.id.tvCombosCount);
        tvEmptyCombos = findViewById(R.id.tvEmptyCombos);
        layoutCombos = findViewById(R.id.layoutCombos);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddCombo).setOnClickListener(v ->
                startActivityForResult(new Intent(this, ComboFormActivity.class), REQUEST_COMBO_FORM));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCombos();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_COMBO_FORM && resultCode == RESULT_OK) {
            loadCombos();
        }
    }

    private void loadCombos() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getMyCombos(token).enqueue(new Callback<ComboResponse>() {
            @Override
            public void onResponse(@NonNull Call<ComboResponse> call, @NonNull Response<ComboResponse> response) {
                List<ComboResponse.Combo> combos = response.isSuccessful() && response.body() != null
                        ? response.body().getCombos() : null;
                renderCombos(combos);
            }

            @Override
            public void onFailure(@NonNull Call<ComboResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookCombosActivity.this, "Failed to load combos: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderCombos(List<ComboResponse.Combo> combos) {
        layoutCombos.removeAllViews();

        if (combos == null || combos.isEmpty()) {
            tvCombosCount.setText("0");
            tvEmptyCombos.setVisibility(View.VISIBLE);
            layoutCombos.setVisibility(View.GONE);
            return;
        }

        int activeCount = 0;
        for (ComboResponse.Combo combo : combos) {
            if (combo.isActive()) activeCount++;
        }
        tvCombosCount.setText(String.valueOf(activeCount));
        tvEmptyCombos.setVisibility(View.GONE);
        layoutCombos.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ComboResponse.Combo combo : combos) {
            View card = inflater.inflate(R.layout.item_combo_deal_manage, layoutCombos, false);

            TextView tvName = card.findViewById(R.id.tvComboName);
            TextView tvMeta = card.findViewById(R.id.tvComboMeta);
            TextView tvItemsSummary = card.findViewById(R.id.tvComboItemsSummary);
            SwitchCompat switchActive = card.findViewById(R.id.switchActive);
            MaterialButton btnEdit = card.findViewById(R.id.btnEditCombo);
            MaterialButton btnDelete = card.findViewById(R.id.btnDeleteCombo);

            int itemCount = combo.getItems() != null ? combo.getItems().size() : 0;
            tvName.setText(combo.getName());

            String meta = itemCount + " item" + (itemCount == 1 ? "" : "s")
                    + " · " + String.format(Locale.getDefault(), "₹%.0f", combo.getPrice());
            if (combo.getSavings() > 0) {
                meta += String.format(Locale.getDefault(), " (save ₹%.0f)", combo.getSavings());
            }
            if (combo.isActive() && !combo.isAvailable()) {
                meta += " · ⚠ item unavailable";
            }
            tvMeta.setText(meta);

            StringBuilder summary = new StringBuilder();
            if (combo.getItems() != null) {
                for (int i = 0; i < combo.getItems().size(); i++) {
                    if (i > 0) summary.append(", ");
                    summary.append(combo.getItems().get(i).getQuantity()).append("x ").append(combo.getItems().get(i).getName());
                }
            }
            tvItemsSummary.setText(summary.toString());

            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(combo.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> setComboActive(combo.getId(), isChecked));

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, ComboFormActivity.class);
                intent.putExtra(ComboFormActivity.EXTRA_COMBO_ID, combo.getId());
                startActivityForResult(intent, REQUEST_COMBO_FORM);
            });

            btnDelete.setOnClickListener(v -> confirmDeleteCombo(combo));

            layoutCombos.addView(card);
        }
    }

    private void setComboActive(int comboId, boolean active) {
        ComboRequest request = new ComboRequest(null, null, null, null);
        request.setActive(active);
        String token = "Bearer " + sessionManager.getToken();

        apiService.updateCombo(token, comboId, request).enqueue(new Callback<ComboResponse>() {
            @Override
            public void onResponse(@NonNull Call<ComboResponse> call, @NonNull Response<ComboResponse> response) {
                if (!(response.isSuccessful() && response.body() != null && response.body().isSuccess())) {
                    Toast.makeText(CookCombosActivity.this, "Failed to update combo", Toast.LENGTH_SHORT).show();
                    loadCombos();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ComboResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookCombosActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                loadCombos();
            }
        });
    }

    private void confirmDeleteCombo(ComboResponse.Combo combo) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Combo")
                .setMessage("Delete \"" + combo.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCombo(combo.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCombo(int comboId) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteCombo(token, comboId).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookCombosActivity.this, "Combo deleted", Toast.LENGTH_SHORT).show();
                    loadCombos();
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Failed to delete combo";
                    Toast.makeText(CookCombosActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookCombosActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
