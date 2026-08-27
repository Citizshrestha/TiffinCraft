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
import com.tiffincraft.app.models.CookSubscribersResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionPlanRequest;
import com.tiffincraft.app.models.SubscriptionPlanResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cook-side management screen: create, edit, delete, and toggle subscription
 * plans. Every plan a cook creates here shows up on their public profile
 * automatically (see CookDetailsActivity) — no separate "publish" step.
 */
public class CookSubscriptionsActivity extends AppCompatActivity {

    private static final int REQUEST_PLAN_FORM = 4001;

    private ApiService apiService;
    private SessionManager sessionManager;

    private TextView tvPlansCount, tvSubscribersCount, tvEmptyPlans;
    private android.widget.LinearLayout layoutPlans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_subscriptions);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        tvPlansCount = findViewById(R.id.tvPlansCount);
        tvSubscribersCount = findViewById(R.id.tvSubscribersCount);
        tvEmptyPlans = findViewById(R.id.tvEmptyPlans);
        layoutPlans = findViewById(R.id.layoutPlans);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddPlan).setOnClickListener(v ->
                startActivityForResult(new Intent(this, SubscriptionPlanFormActivity.class), REQUEST_PLAN_FORM));
        findViewById(R.id.rowViewSubscribers).setOnClickListener(v ->
                startActivity(new Intent(this, CookSubscribersActivity.class)));
        findViewById(R.id.rowViewCombos).setOnClickListener(v ->
                startActivity(new Intent(this, CookCombosActivity.class)));
        // No date extra — the server decides what "today" is in Nepal Time.
        findViewById(R.id.rowTodayDeliveries).setOnClickListener(v ->
                startActivity(TodayDeliveriesActivity.intentFor(this, null)));

        loadSubscriberCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlans();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PLAN_FORM && resultCode == RESULT_OK) {
            loadPlans();
        }
    }

    private void loadSubscriberCount() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCookSubscribers(token).enqueue(new Callback<CookSubscribersResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookSubscribersResponse> call, @NonNull Response<CookSubscribersResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    tvSubscribersCount.setText(String.valueOf(response.body().getActiveSubscriberCount()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CookSubscribersResponse> call, @NonNull Throwable t) { /* leave default */ }
        });
    }

    private void loadPlans() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getMySubscriptionPlans(token).enqueue(new Callback<SubscriptionPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Response<SubscriptionPlanResponse> response) {
                List<SubscriptionPlanResponse.Plan> plans = response.isSuccessful() && response.body() != null
                        ? response.body().getPlans() : null;
                renderPlans(plans);
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookSubscriptionsActivity.this, "Failed to load plans: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderPlans(List<SubscriptionPlanResponse.Plan> plans) {
        layoutPlans.removeAllViews();

        if (plans == null || plans.isEmpty()) {
            tvPlansCount.setText("0");
            tvEmptyPlans.setVisibility(View.VISIBLE);
            layoutPlans.setVisibility(View.GONE);
            return;
        }

        int activeCount = 0;
        for (SubscriptionPlanResponse.Plan plan : plans) {
            if (plan.isActive()) activeCount++;
        }
        tvPlansCount.setText(String.valueOf(activeCount));
        tvEmptyPlans.setVisibility(View.GONE);
        layoutPlans.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SubscriptionPlanResponse.Plan plan : plans) {
            View card = inflater.inflate(R.layout.item_subscription_plan_manage, layoutPlans, false);

            TextView tvName = card.findViewById(R.id.tvPlanName);
            TextView tvDurationBadge = card.findViewById(R.id.tvPlanDurationBadge);
            TextView tvMeta = card.findViewById(R.id.tvPlanMeta);
            TextView tvItemsSummary = card.findViewById(R.id.tvPlanItemsSummary);
            SwitchCompat switchActive = card.findViewById(R.id.switchActive);
            MaterialButton btnEdit = card.findViewById(R.id.btnEditPlan);
            MaterialButton btnDelete = card.findViewById(R.id.btnDeletePlan);

            int itemCount = plan.getItems() != null ? plan.getItems().size() : 0;
            tvName.setText(plan.getName());
            if (tvDurationBadge != null) {
                tvDurationBadge.setText(plan.getDurationLabel());
            }

            String meta = itemCount + " item" + (itemCount == 1 ? "" : "s")
                    + " · " + String.format(Locale.getDefault(), "₹%.0f one-time", plan.getPricePerDelivery());
            if (plan.getSavings() > 0) {
                meta += String.format(Locale.getDefault(), " · Save ₹%.0f", plan.getSavings());
            }
            if (plan.isActive() && !plan.isAvailable()) {
                meta += " · ⚠ item unavailable";
            }
            tvMeta.setText(meta);

            StringBuilder summary = new StringBuilder();
            if (plan.getItems() != null) {
                for (int i = 0; i < plan.getItems().size(); i++) {
                    if (i > 0) summary.append(", ");
                    summary.append(plan.getItems().get(i).getQuantity()).append("x ").append(plan.getItems().get(i).getName());
                }
            }
            tvItemsSummary.setText(summary.toString());

            // Avoid firing the listener while we're just setting the initial state.
            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(plan.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> setPlanActive(plan.getId(), isChecked));

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, SubscriptionPlanFormActivity.class);
                intent.putExtra(SubscriptionPlanFormActivity.EXTRA_PLAN_ID, plan.getId());
                startActivityForResult(intent, REQUEST_PLAN_FORM);
            });

            btnDelete.setOnClickListener(v -> confirmDeletePlan(plan));

            layoutPlans.addView(card);
        }
    }

    private void setPlanActive(int planId, boolean active) {
        SubscriptionPlanRequest request = new SubscriptionPlanRequest(null, null, null, null);
        request.setActive(active);
        String token = "Bearer " + sessionManager.getToken();

        apiService.updateSubscriptionPlan(token, planId, request).enqueue(new Callback<SubscriptionPlanResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Response<SubscriptionPlanResponse> response) {
                if (!(response.isSuccessful() && response.body() != null && response.body().isSuccess())) {
                    Toast.makeText(CookSubscriptionsActivity.this, "Failed to update plan", Toast.LENGTH_SHORT).show();
                    loadPlans(); // revert the switch to actual server state
                }
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionPlanResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookSubscriptionsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                loadPlans();
            }
        });
    }

    private void confirmDeletePlan(SubscriptionPlanResponse.Plan plan) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Plan")
                .setMessage("Delete \"" + plan.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePlan(plan.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePlan(int planId) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteSubscriptionPlan(token, planId).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookSubscriptionsActivity.this, "Plan deleted", Toast.LENGTH_SHORT).show();
                    loadPlans();
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Failed to delete plan";
                    Toast.makeText(CookSubscriptionsActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookSubscriptionsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
