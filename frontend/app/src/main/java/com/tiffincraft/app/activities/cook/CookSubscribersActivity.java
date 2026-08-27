package com.tiffincraft.app.activities.cook;

import android.content.Context;
import android.content.Intent;
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
import com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookSubscribersResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.DeliveryDateUtils;

import java.util.ArrayList;
import java.util.List;

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

    private static final int REQUEST_ADD_SUBSCRIPTION = 5001;

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
        findViewById(R.id.fabAddSubscription).setOnClickListener(v ->
                startActivityForResult(new Intent(this, SubscriptionPlanFormActivity.class), REQUEST_ADD_SUBSCRIPTION));

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_SUBSCRIPTION && resultCode == RESULT_OK) {
            Toast.makeText(this, "Subscription plan created successfully", Toast.LENGTH_SHORT).show();
        }
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
                    // `scheduled` belongs here, not in "other": the customer has
                    // paid and been verified, they're simply waiting for their
                    // chosen start date. Filing them beside cancelled rows would
                    // hide the people the cook is about to start cooking for.
                    if ("active".equals(sub.getStatus()) || "scheduled".equals(sub.getStatus())) filtered.add(sub);
                    break;
                case "other":
                    if (!"active".equals(sub.getStatus()) && !"scheduled".equals(sub.getStatus())
                            && !"submitted".equals(sub.getPaymentStatus())) filtered.add(sub);
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
            // getDurationLabel() rather than a weekly/monthly ternary — the old
            // version silently printed "1 Month" for a 2-week plan.
            tvPlanName.setText((sub.getPlanName() != null ? sub.getPlanName() : "Plan")
                    + " · " + sub.getDurationLabel());

            applyPaymentStatusChip(tvPaymentStatusChip, sub);

            if ("scheduled".equals(sub.getStatus())) {
                // Nothing to do yet, and saying so is the point — a cook who
                // sees only "Verified" has no idea whether to cook tomorrow.
                String starts = DeliveryDateUtils.formatLongDate(sub.getStartDate());
                tvNextDelivery.setText("Starts " + starts + " — no action needed until then");
                tvNextDelivery.setVisibility(View.VISIBLE);
            } else if ("active".equals(sub.getStatus())) {
                // Nullable once every remaining day is skipped or closed.
                tvNextDelivery.setText(sub.getNextDeliveryDate() != null
                        ? "Next delivery: " + DeliveryDateUtils.formatLongDate(sub.getNextDeliveryDate())
                        : "No upcoming delivery — every remaining day is skipped or closed");
                tvNextDelivery.setVisibility(View.VISIBLE);
            } else if ("paused".equals(sub.getStatus())) {
                tvNextDelivery.setText("Paused by customer");
                tvNextDelivery.setVisibility(View.VISIBLE);
            } else if ("completed".equals(sub.getStatus())) {
                tvNextDelivery.setText("Completed — every meal used");
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

            // Read-only for the cook: the calendar shows which days this one
            // customer skipped. Closing a date is a bulk action and lives on
            // Today's Deliveries instead, because it hits every subscriber.
            if (sub.isLiveAndPaid()) {
                card.setOnClickListener(v -> startActivity(
                        SubscriptionCalendarActivity.intentFor(this, sub.getId(), sub.getPlanName())));
            } else {
                card.setOnClickListener(null);
                card.setClickable(false);
            }

            layoutSubscribers.addView(card);
        }
    }

    private void applyPaymentStatusChip(TextView chip, SubscriptionResponse.Subscription sub) {
        String paymentStatus = sub.getPaymentStatus();
        if (paymentStatus == null) paymentStatus = "pending";

        switch (paymentStatus) {
            case "verified":
                if ("scheduled".equals(sub.getStatus())) {
                    // Paid and verified, but NOT delivering yet. "Active" here
                    // would tell the cook to start cooking days early.
                    chip.setText("Starts soon");
                    chip.setBackgroundResource(R.drawable.status_chip_preparing);
                    chip.setTextColor(getColor(R.color.status_preparing_text));
                    break;
                }
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

        // The customer picked a start date at checkout, and verifying no longer
        // means "start today" — so the cook has to see that date before tapping
        // Verify, or they'll expect a delivery that isn't coming for a week.
        TextView tvStartDate = new TextView(ctx);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        startParams.topMargin = pad;
        tvStartDate.setLayoutParams(startParams);
        tvStartDate.setTextColor(getColor(R.color.text_ink));
        tvStartDate.setTextSize(14f);

        String requestedStart = sub.getStartDate();
        if (requestedStart != null) {
            // Only the date is stated, not whether it's in the future: "today" is
            // the server's in Nepal Time, and a phone with a skewed clock would
            // otherwise promise an immediate start the server is going to defer.
            // The response message says which one actually happened.
            tvStartDate.setText("First delivery: " + DeliveryDateUtils.formatLongDate(requestedStart)
                    + "\nVerifying confirms the payment — deliveries begin on that date, not today.");
        } else {
            tvStartDate.setText("No start date on file — verifying will start deliveries today.");
        }
        dialogLayout.addView(tvStartDate);

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
                    // The server's message is shown verbatim on success because it
                    // is the only thing that distinguishes "active, cook today"
                    // from "deliveries start Sep 3 — nothing to do until then".
                    String okMsg = response.body().getMessage();
                    if (okMsg == null || okMsg.isEmpty()) {
                        okMsg = "verified".equals(status)
                                ? "Payment verified."
                                : "Payment rejected — customer notified.";
                    }
                    Toast.makeText(CookSubscribersActivity.this, okMsg, Toast.LENGTH_LONG).show();
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
