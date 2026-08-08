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
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Meal;
import com.tiffincraft.app.models.MealResponse;
import com.tiffincraft.app.models.SubscriptionPlanRequest;
import com.tiffincraft.app.models.SubscriptionPlanResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Full-screen form for both creating and editing a cook's subscription plan.
 * Items are always picked from the cook's own menu — a plan item borrows its
 * name/description/price live from `meals`, only quantity is plan-specific.
 * Price per delivery is an optional cook-set override — see
 * subscriptionPlanController.js for why that matters: without it, a plan
 * just charges the summed menu price on repeat, which is worse value than
 * ordering daily and defeats the point of subscribing.
 */
public class SubscriptionPlanFormActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN_ID = "plan_id";

    private SessionManager sessionManager;
    private ApiService apiService;

    private TextView tvFormTitle, tvNoMeals, chipWeekly, chipMonthly, tvSavingsPreview;
    private TextInputEditText etPlanName, etPlanDescription, etPlanPrice;
    private LinearLayout layoutSelectableMeals;
    private MaterialButton btnSavePlan;

    private int planId = -1;
    private String duration = "weekly";
    private List<Meal> myMeals = new ArrayList<>();
    private final Map<Integer, View> rowsByMealId = new HashMap<>();
    private final Map<Integer, Integer> pendingQuantities = new HashMap<>(); // used only to prefill on edit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_plan_form);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        planId = getIntent().getIntExtra(EXTRA_PLAN_ID, -1);

        bindViews();
        setupDurationChips();

        if (planId != -1) {
            tvFormTitle.setText("Edit Subscription Plan");
        }

        etPlanPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updateSavingsPreview(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadMyMeals();
        if (planId != -1) {
            loadExistingPlan();
        }

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvFormTitle = findViewById(R.id.tvFormTitle);
        etPlanName = findViewById(R.id.etPlanName);
        etPlanDescription = findViewById(R.id.etPlanDescription);
        etPlanPrice = findViewById(R.id.etPlanPrice);
        tvSavingsPreview = findViewById(R.id.tvPlanSavingsPreview);
        chipWeekly = findViewById(R.id.chipWeekly);
        chipMonthly = findViewById(R.id.chipMonthly);
        layoutSelectableMeals = findViewById(R.id.layoutSelectableMeals);
        tvNoMeals = findViewById(R.id.tvNoMeals);
        btnSavePlan = findViewById(R.id.btnSavePlan);
    }

    private void setupDurationChips() {
        chipWeekly.setOnClickListener(v -> {
            duration = "weekly";
            updateDurationChips();
        });
        chipMonthly.setOnClickListener(v -> {
            duration = "monthly";
            updateDurationChips();
        });
        updateDurationChips();
    }

    private void updateDurationChips() {
        boolean weekly = "weekly".equals(duration);
        chipWeekly.setBackground(ContextCompat.getDrawable(this, weekly ? R.drawable.chip_selected_green : R.drawable.chip_unselected));
        chipWeekly.setTextColor(ContextCompat.getColor(this, weekly ? R.color.dark_green : android.R.color.darker_gray));
        chipMonthly.setBackground(ContextCompat.getDrawable(this, !weekly ? R.drawable.chip_selected_green : R.drawable.chip_unselected));
        chipMonthly.setTextColor(ContextCompat.getColor(this, !weekly ? R.color.dark_green : android.R.color.darker_gray));
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
                    btnSavePlan.setEnabled(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                Toast.makeText(SubscriptionPlanFormActivity.this, "Failed to load your meals: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void loadExistingPlan() {
        apiService.getSubscriptionPlanById(planId).enqueue(new Callback<SubscriptionPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Response<SubscriptionPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getPlan() != null) {
                    SubscriptionPlanResponse.Plan plan = response.body().getPlan();
                    etPlanName.setText(plan.getName());
                    etPlanDescription.setText(plan.getDescription());
                    duration = plan.getDuration();
                    updateDurationChips();

                    // Only prefill the price field when the cook actually set an
                    // override — leave it blank (not the auto-summed figure) so
                    // editing an old auto-summed plan doesn't silently "lock in"
                    // that price as if it were deliberate.
                    if (plan.hasCustomPrice()) {
                        etPlanPrice.setText(String.format(Locale.US, "%.0f", plan.getPricePerDelivery()));
                    }

                    pendingQuantities.clear();
                    if (plan.getItems() != null) {
                        for (SubscriptionPlanResponse.PlanItem item : plan.getItems()) {
                            pendingQuantities.put(item.getMealId(), item.getQuantity());
                        }
                    }
                    applyPendingSelection();
                } else {
                    Toast.makeText(SubscriptionPlanFormActivity.this, "Failed to load plan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Throwable t) {
                Toast.makeText(SubscriptionPlanFormActivity.this, "Network error loading plan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Meal rows and the edited plan's items load independently (two network calls) —
     *  re-apply whichever arrived, each time either one lands. */
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

    /** Live "Save ₹X vs ordering these separately" preview as the cook builds the plan. */
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

        String priceText = etPlanPrice.getText() != null ? etPlanPrice.getText().toString().trim() : "";
        double setPrice;
        try {
            setPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            setPrice = -1;
        }

        if (individualTotal <= 0) {
            tvSavingsPreview.setVisibility(View.GONE);
            return;
        }

        if (setPrice <= 0) {
            // No price set yet — show what auto-summed pricing would charge, so
            // the cook sees the number they're implicitly choosing by leaving
            // this blank.
            tvSavingsPreview.setText(String.format(Locale.getDefault(),
                    "Without a price, this charges ₹%.0f/delivery — the full menu price, no discount.", individualTotal));
            tvSavingsPreview.setVisibility(View.VISIBLE);
            return;
        }

        double savings = individualTotal - setPrice;
        if (savings > 0) {
            tvSavingsPreview.setText(String.format(Locale.getDefault(),
                    "Subscribers save ₹%.0f per delivery vs. ordering these separately (₹%.0f)", savings, individualTotal));
        } else {
            tvSavingsPreview.setText("This isn't cheaper than ordering the items separately (₹"
                    + String.format(Locale.getDefault(), "%.0f", individualTotal) + ") — subscribers won't see any benefit.");
        }
        tvSavingsPreview.setVisibility(View.VISIBLE);
    }

    private Meal findMealById(int mealId) {
        for (Meal meal : myMeals) {
            if (meal.getId() == mealId) return meal;
        }
        return null;
    }

    private void savePlan() {
        String name = etPlanName.getText() != null ? etPlanName.getText().toString().trim() : "";
        String description = etPlanDescription.getText() != null ? etPlanDescription.getText().toString().trim() : "";
        String priceText = etPlanPrice.getText() != null ? etPlanPrice.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etPlanName.setError("Plan name is required");
            etPlanName.requestFocus();
            return;
        }

        Double price = null;
        if (!priceText.isEmpty()) {
            try {
                price = Double.parseDouble(priceText);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etPlanPrice.setError("Enter a valid price, or leave blank");
                etPlanPrice.requestFocus();
                return;
            }
        }

        List<SubscriptionPlanRequest.Item> items = new ArrayList<>();
        for (Map.Entry<Integer, View> entry : rowsByMealId.entrySet()) {
            View row = entry.getValue();
            CheckBox cb = row.findViewById(R.id.cbSelected);
            if (cb.isChecked()) {
                int qty = Integer.parseInt(((TextView) row.findViewById(R.id.tvQuantity)).getText().toString());
                items.add(new SubscriptionPlanRequest.Item(entry.getKey(), qty));
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(this, "Select at least one item for this plan", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSavePlan.setEnabled(false);
        btnSavePlan.setText("Saving...");

        SubscriptionPlanRequest request = new SubscriptionPlanRequest(name, duration, description, items);
        request.setPricePerDelivery(price);
        String token = "Bearer " + sessionManager.getToken();

        Call<SubscriptionPlanResponse> call = planId == -1
                ? apiService.createSubscriptionPlan(token, request)
                : apiService.updateSubscriptionPlan(token, planId, request);

        call.enqueue(new Callback<SubscriptionPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Response<SubscriptionPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(SubscriptionPlanFormActivity.this,
                            planId == -1 ? "Plan created!" : "Plan updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    resetSaveButton();
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Failed to save plan";
                    Toast.makeText(SubscriptionPlanFormActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Throwable t) {
                resetSaveButton();
                Toast.makeText(SubscriptionPlanFormActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetSaveButton() {
        btnSavePlan.setEnabled(true);
        btnSavePlan.setText("Save Plan");
    }
}
