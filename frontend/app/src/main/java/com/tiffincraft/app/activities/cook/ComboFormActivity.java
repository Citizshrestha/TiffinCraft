package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.ComboRequest;
import com.tiffincraft.app.models.ComboResponse;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full-screen form for creating/editing a cook's combo deal — a one-time
 * bundle at a fixed cook-set price. Structurally almost identical to
 * SubscriptionPlanFormActivity (pick items from the menu, set a quantity
 * each), but with a price field instead of a duration chip pair, since a
 * combo has no recurrence — see comboController.js for why.
 */
public class ComboFormActivity extends AppCompatActivity {

    public static final String EXTRA_COMBO_ID = "combo_id";

    private SessionManager sessionManager;
    private ApiService apiService;

    private TextView tvFormTitle, tvNoMeals, tvSavingsPreview;
    private TextInputEditText etComboName, etComboDescription, etComboPrice;
    private LinearLayout layoutSelectableMeals;
    private MaterialButton btnSaveCombo;

    private int comboId = -1;
    private List<Meal> myMeals = new ArrayList<>();
    // LinkedHashMap, not HashMap — selection order matters now: the first
    // selected item's meal photo becomes the combo's thumbnail everywhere
    // it's shown (see CookDetailsActivity.renderCombos), so it needs to be
    // deterministic, not whatever a HashMap happens to iterate.
    private final Map<Integer, View> rowsByMealId = new LinkedHashMap<>();
    private final Map<Integer, Integer> pendingQuantities = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combo_form);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        comboId = getIntent().getIntExtra(EXTRA_COMBO_ID, -1);

        bindViews();

        if (comboId != -1) {
            tvFormTitle.setText("Edit Combo Deal");
        }

        etComboPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updateSavingsPreview(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadMyMeals();
        if (comboId != -1) {
            loadExistingCombo();
        }

        btnSaveCombo.setOnClickListener(v -> saveCombo());
    }

    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvFormTitle = findViewById(R.id.tvFormTitle);
        etComboName = findViewById(R.id.etComboName);
        etComboDescription = findViewById(R.id.etComboDescription);
        etComboPrice = findViewById(R.id.etComboPrice);
        tvSavingsPreview = findViewById(R.id.tvComboSavingsPreview);
        layoutSelectableMeals = findViewById(R.id.layoutSelectableMeals);
        tvNoMeals = findViewById(R.id.tvNoMeals);
        btnSaveCombo = findViewById(R.id.btnSaveCombo);
    }

    private void loadMyMeals() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getMyMeals(token).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getMeals() != null && !response.body().getMeals().isEmpty()) {
                    myMeals = response.body().getMeals();
                    renderSelectableMeals();
                } else {
                    tvNoMeals.setVisibility(View.VISIBLE);
                    btnSaveCombo.setEnabled(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Toast.makeText(ComboFormActivity.this, "Failed to load your meals: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSelectableMeals() {
        layoutSelectableMeals.removeAllViews();
        rowsByMealId.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Meal meal : myMeals) {
            View row = inflater.inflate(R.layout.item_plan_meal_selectable, layoutSelectableMeals, false);

            CheckBox cbSelected = row.findViewById(R.id.cbSelected);
            TextView tvName = row.findViewById(R.id.tvMealName);
            TextView tvPrice = row.findViewById(R.id.tvMealPrice);
            TextView tvQuantity = row.findViewById(R.id.tvQuantity);
            View stepper = row.findViewById(R.id.layoutQuantityStepper);
            ImageButton btnDecrease = row.findViewById(R.id.btnDecrease);
            ImageButton btnIncrease = row.findViewById(R.id.btnIncrease);

            tvName.setText(meal.getName());
            tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f", meal.getPrice()));

            cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
                stepper.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateSavingsPreview();
            });

            btnDecrease.setOnClickListener(v -> {
                int qty = Integer.parseInt(tvQuantity.getText().toString());
                if (qty > 1) {
                    tvQuantity.setText(String.valueOf(qty - 1));
                    updateSavingsPreview();
                }
            });
            btnIncrease.setOnClickListener(v -> {
                int qty = Integer.parseInt(tvQuantity.getText().toString());
                tvQuantity.setText(String.valueOf(qty + 1));
                updateSavingsPreview();
            });

            row.setTag(meal.getId());
            rowsByMealId.put(meal.getId(), row);
            layoutSelectableMeals.addView(row);
        }

        applyPendingSelection();
    }

    private void loadExistingCombo() {
        apiService.getComboById(comboId).enqueue(new Callback<ComboResponse>() {
            @Override
            public void onResponse(@NonNull Call<ComboResponse> call, @NonNull Response<ComboResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getCombo() != null) {
                    ComboResponse.Combo combo = response.body().getCombo();
                    etComboName.setText(combo.getName());
                    etComboDescription.setText(combo.getDescription());
                    etComboPrice.setText(String.format(Locale.US, "%.0f", combo.getPrice()));

                    pendingQuantities.clear();
                    if (combo.getItems() != null) {
                        for (ComboResponse.ComboItem item : combo.getItems()) {
                            pendingQuantities.put(item.getMealId(), item.getQuantity());
                        }
                    }
                    applyPendingSelection();
                } else {
                    Toast.makeText(ComboFormActivity.this, "Failed to load combo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ComboResponse> call, @NonNull Throwable t) {
                Toast.makeText(ComboFormActivity.this, "Network error loading combo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyPendingSelection() {
        if (pendingQuantities.isEmpty() || rowsByMealId.isEmpty()) return;

        for (Map.Entry<Integer, Integer> entry : pendingQuantities.entrySet()) {
            View row = rowsByMealId.get(entry.getKey());
            if (row == null) continue;
            ((CheckBox) row.findViewById(R.id.cbSelected)).setChecked(true);
            ((TextView) row.findViewById(R.id.tvQuantity)).setText(String.valueOf(entry.getValue()));
        }
        updateSavingsPreview();
    }

    /** Live "Save ₹X vs buying separately" preview as the cook builds the combo. */
    private void updateSavingsPreview() {
        double individualTotal = 0;
        for (Map.Entry<Integer, View> entry : rowsByMealId.entrySet()) {
            View row = entry.getValue();
            CheckBox cb = row.findViewById(R.id.cbSelected);
            if (cb.isChecked()) {
                int qty = Integer.parseInt(((TextView) row.findViewById(R.id.tvQuantity)).getText().toString());
                Meal meal = findMealById(entry.getKey());
                if (meal != null) individualTotal += meal.getPrice() * qty;
            }
        }

        String priceText = etComboPrice.getText() != null ? etComboPrice.getText().toString().trim() : "";
        double comboPrice;
        try {
            comboPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            comboPrice = -1;
        }

        if (individualTotal > 0 && comboPrice > 0) {
            double savings = individualTotal - comboPrice;
            if (savings > 0) {
                tvSavingsPreview.setText(String.format(Locale.getDefault(),
                        "Customers save ₹%.0f vs. buying these separately (₹%.0f)", savings, individualTotal));
                tvSavingsPreview.setVisibility(View.VISIBLE);
            } else {
                tvSavingsPreview.setText("This price isn't cheaper than buying the items separately (₹"
                        + String.format(Locale.getDefault(), "%.0f", individualTotal) + ") — consider lowering it.");
                tvSavingsPreview.setVisibility(View.VISIBLE);
            }
        } else {
            tvSavingsPreview.setVisibility(View.GONE);
        }
    }

    private Meal findMealById(int mealId) {
        for (Meal meal : myMeals) {
            if (meal.getId() == mealId) return meal;
        }
        return null;
    }

    private void saveCombo() {
        String name = etComboName.getText() != null ? etComboName.getText().toString().trim() : "";
        String description = etComboDescription.getText() != null ? etComboDescription.getText().toString().trim() : "";
        String priceText = etComboPrice.getText() != null ? etComboPrice.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etComboName.setError("Combo name is required");
            etComboName.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etComboPrice.setError("Enter a valid price");
            etComboPrice.requestFocus();
            return;
        }

        List<ComboRequest.Item> items = new ArrayList<>();
        for (Map.Entry<Integer, View> entry : rowsByMealId.entrySet()) {
            View row = entry.getValue();
            CheckBox cb = row.findViewById(R.id.cbSelected);
            if (cb.isChecked()) {
                int qty = Integer.parseInt(((TextView) row.findViewById(R.id.tvQuantity)).getText().toString());
                items.add(new ComboRequest.Item(entry.getKey(), qty));
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(this, "Select at least one item for this combo", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveCombo.setEnabled(false);
        btnSaveCombo.setText("Saving...");

        ComboRequest request = new ComboRequest(name, description, price, items);
        String token = "Bearer " + sessionManager.getToken();

        Call<ComboResponse> call = comboId == -1
                ? apiService.createCombo(token, request)
                : apiService.updateCombo(token, comboId, request);

        call.enqueue(new Callback<ComboResponse>() {
            @Override
            public void onResponse(@NonNull Call<ComboResponse> call, @NonNull Response<ComboResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ComboFormActivity.this,
                            comboId == -1 ? "Combo created!" : "Combo updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    resetSaveButton();
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Failed to save combo";
                    Toast.makeText(ComboFormActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ComboResponse> call, @NonNull Throwable t) {
                resetSaveButton();
                Toast.makeText(ComboFormActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetSaveButton() {
        btnSaveCombo.setEnabled(true);
        btnSaveCombo.setText("Save Combo");
    }
}
