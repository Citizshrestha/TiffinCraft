package com.tiffincraft.app.activities.cook;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookSubscribersResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cook reviews everyone subscribed to their plans — filters by payment
 * status, and for anyone who has submitted proof of payment, opens a dialog
 * to view the screenshot and verify or reject it. This is the gate that
 * makes a subscription's "Subscribed!" mean something real instead of
 * firing the instant a customer taps the button (see subscriptionController.js).
 */
public class CookSubscribersActivity extends AppCompatActivity {

    /** Opens straight into a specific filter (e.g. "submitted" from a
     *  "payment submitted" notification) instead of the default "all". */
    public static final String EXTRA_INITIAL_FILTER = "initial_filter";

    private ApiService apiService;
    private SessionManager sessionManager;

    private LinearLayout layoutSubscribers;
    private TextView tvEmptySubscribers;
    private TextView chipAll, chipSubmitted, chipActive, chipOther;

    private List<SubscriptionResponse.Subscription> allSubscribers = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_subscribers);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        layoutSubscribers = findViewById(R.id.layoutSubscribers);
        tvEmptySubscribers = findViewById(R.id.tvEmptySubscribers);
        chipAll = findViewById(R.id.chipAll);
        chipSubmitted = findViewById(R.id.chipSubmitted);
        chipActive = findViewById(R.id.chipActive);
        chipOther = findViewById(R.id.chipOther);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        chipAll.setOnClickListener(v -> setFilter("all"));
        chipSubmitted.setOnClickListener(v -> setFilter("submitted"));
        chipActive.setOnClickListener(v -> setFilter("active"));
        chipOther.setOnClickListener(v -> setFilter("other"));

        String initialFilter = getIntent().getStringExtra(EXTRA_INITIAL_FILTER);
        if (initialFilter != null) {
            currentFilter = initialFilter;
            updateChipStyles();
        }

        loadSubscribers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubscribers();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateChipStyles();
        renderSubscribers();
    }

    private void updateChipStyles() {
        TextView[] chips = { chipAll, chipSubmitted, chipActive, chipOther };
        String[] keys = { "all", "submitted", "active", "other" };
        for (int i = 0; i < chips.length; i++) {
            boolean selected = keys[i].equals(currentFilter);
            chips[i].setBackground(ContextCompat.getDrawable(this, selected ? R.drawable.chip_selected_green : R.drawable.chip_unselected));
            chips[i].setTextColor(ContextCompat.getColor(this, selected ? R.color.dark_green : android.R.color.darker_gray));
        }
    }

    private void loadSubscribers() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCookSubscribers(token).enqueue(new Callback<CookSubscribersResponse>() {
            @Override
            public void onResponse(@NonNull Call<CookSubscribersResponse> call, @NonNull Response<CookSubscribersResponse> response) {
                allSubscribers = response.isSuccessful() && response.body() != null && response.body().getSubscriptions() != null
                        ? response.body().getSubscriptions() : new ArrayList<>();
                renderSubscribers();
            }

            @Override
            public void onFailure(@NonNull Call<CookSubscribersResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookSubscribersActivity.this, "Failed to load subscribers: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSubscribers() {
        layoutSubscribers.removeAllViews();

        List<SubscriptionResponse.Subscription> filtered = new ArrayList<>();
        for (SubscriptionResponse.Subscription sub : allSubscribers) {
            switch (currentFilter) {
                case "submitted":
                    if ("submitted".equals(sub.getPaymentStatus())) filtered.add(sub);
                    break;
                case "active":
                    if ("active".equals(sub.getStatus())) filtered.add(sub);
                    break;
                case "other":
                    if (!"active".equals(sub.getStatus()) && !"submitted".equals(sub.getPaymentStatus())) filtered.add(sub);
                    break;
                default:
                    filtered.add(sub);
            }
        }

        if (filtered.isEmpty()) {
            tvEmptySubscribers.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptySubscribers.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SubscriptionResponse.Subscription sub : filtered) {
            View card = inflater.inflate(R.layout.item_cook_subscriber, layoutSubscribers, false);

            TextView tvCustomerName = card.findViewById(R.id.tvCustomerName);
            TextView tvPlanName = card.findViewById(R.id.tvPlanName);
            TextView tvPaymentStatusChip = card.findViewById(R.id.tvPaymentStatusChip);
            TextView tvNextDelivery = card.findViewById(R.id.tvNextDelivery);
            MaterialButton btnVerify = card.findViewById(R.id.btnVerifySubscriber);

            tvCustomerName.setText(sub.getCustomerName() != null ? sub.getCustomerName() : "Customer");
            String durationLabel = "weekly".equals(sub.getDuration()) ? "1 Week" : "1 Month";
            tvPlanName.setText((sub.getPlanName() != null ? sub.getPlanName() : "Plan") + " · " + durationLabel);

            applyPaymentStatusChip(tvPaymentStatusChip, sub);

            if ("active".equals(sub.getStatus()) && sub.getNextDeliveryDate() != null) {
                tvNextDelivery.setText("Next delivery: " + sub.getNextDeliveryDate());
                tvNextDelivery.setVisibility(View.VISIBLE);
            } else if ("paused".equals(sub.getStatus())) {
                tvNextDelivery.setText("Paused by customer");
                tvNextDelivery.setVisibility(View.VISIBLE);
            } else if ("cancelled".equals(sub.getStatus())) {
                tvNextDelivery.setText("Cancelled");
                tvNextDelivery.setVisibility(View.VISIBLE);
            } else {
                tvNextDelivery.setVisibility(View.GONE);
            }

            if ("submitted".equals(sub.getPaymentStatus())) {
                btnVerify.setVisibility(View.VISIBLE);
                btnVerify.setOnClickListener(v -> showVerifyDialog(sub));
            } else {
                btnVerify.setVisibility(View.GONE);
            }

            layoutSubscribers.addView(card);
        }
    }

    private void applyPaymentStatusChip(TextView chip, SubscriptionResponse.Subscription sub) {
        String paymentStatus = sub.getPaymentStatus();
        if (paymentStatus == null) paymentStatus = "pending";

        switch (paymentStatus) {
            case "verified":
                chip.setText("active".equals(sub.getStatus()) ? "Active" : "Verified");
                chip.setBackgroundResource(R.drawable.status_chip_delivered);
                chip.setTextColor(getColor(R.color.status_delivered_text));
                break;
            case "submitted":
                chip.setText("Needs Review");
                chip.setBackgroundResource(R.drawable.status_chip_preparing);
                chip.setTextColor(getColor(R.color.status_preparing_text));
                break;
            case "rejected":
                chip.setText("Rejected");
                chip.setBackgroundResource(R.drawable.status_chip_sold_out);
                chip.setTextColor(0xFFA32D2D);
                break;
            default:
                chip.setText("pending_payment".equals(sub.getStatus()) ? "Awaiting Payment" : "Pending");
                chip.setBackgroundResource(R.drawable.status_chip_pending);
                chip.setTextColor(getColor(R.color.status_pending_text));
                break;
        }
    }

    private void showVerifyDialog(SubscriptionResponse.Subscription sub) {
        Context ctx = this;
        LinearLayout dialogLayout = new LinearLayout(ctx);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        dialogLayout.setPadding(pad, pad, pad, pad);

        ImageView ivScreenshot = new ImageView(ctx);
        ivScreenshot.setAdjustViewBounds(true);
        ivScreenshot.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (260 * getResources().getDisplayMetrics().density)));
        if (sub.getPaymentScreenshotUrl() != null) {
            Glide.with(this).load(sub.getPaymentScreenshotUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivScreenshot);
        }
        dialogLayout.addView(ivScreenshot);

        EditText etNotes = new EditText(ctx);
        etNotes.setHint("Notes (optional)");
        LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        notesParams.topMargin = pad;
        etNotes.setLayoutParams(notesParams);
        dialogLayout.addView(etNotes);

        new AlertDialog.Builder(ctx)
                .setTitle("Verify Payment — " + (sub.getCustomerName() != null ? sub.getCustomerName() : "Customer"))
                .setView(dialogLayout)
                .setPositiveButton("Verify", (dialog, which) ->
                        submitVerification(sub.getId(), "verified", etNotes.getText().toString().trim()))
                .setNegativeButton("Reject", (dialog, which) ->
                        submitVerification(sub.getId(), "rejected", etNotes.getText().toString().trim()))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void submitVerification(int subscriptionId, String status, String notes) {
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("status", status);
        if (!notes.isEmpty()) body.addProperty("notes", notes);

        apiService.verifySubscriptionPayment(token, subscriptionId, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CookSubscribersActivity.this,
                            "verified".equals(status) ? "Subscription activated!" : "Payment rejected — customer notified.",
                            Toast.LENGTH_SHORT).show();
                    loadSubscribers();
                } else {
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage() : "Failed to update subscription";
                    Toast.makeText(CookSubscribersActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CookSubscribersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
