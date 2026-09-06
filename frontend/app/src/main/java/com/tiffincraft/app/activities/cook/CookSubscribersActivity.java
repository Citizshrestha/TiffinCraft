package com.tiffincraft.app.activities.cook;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CookSubscribersResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.models.SubscriptionRequestsResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.DeliveryDateUtils;
import com.tiffincraft.app.utils.ImageUrlHelper;

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

    private LinearLayout layoutSubscribers, layoutNewSubscriptionRequests;
    private TextView tvEmptySubscribers, tvNewSubscriptionRequestCount, btnViewAllSubscriptionRequests;
    private View cardNewSubscriptionRequests;
    private TextView chipAll, chipRequests, chipSubmitted, chipActive, chipOther;

    private List<SubscriptionResponse.Subscription> allSubscribers = new ArrayList<>();
    private List<SubscriptionRequestsResponse.Item> newSubscriptionRequests = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_subscribers);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        layoutSubscribers = findViewById(R.id.layoutSubscribers);
        layoutNewSubscriptionRequests = findViewById(R.id.layoutNewSubscriptionRequests);
        cardNewSubscriptionRequests = findViewById(R.id.cardNewSubscriptionRequests);
        tvEmptySubscribers = findViewById(R.id.tvEmptySubscribers);
        tvNewSubscriptionRequestCount = findViewById(R.id.tvNewSubscriptionRequestCount);
        btnViewAllSubscriptionRequests = findViewById(R.id.btnViewAllSubscriptionRequests);
        chipAll = findViewById(R.id.chipAll);
        chipRequests = findViewById(R.id.chipSubscriptionRequests);
        chipSubmitted = findViewById(R.id.chipPaymentProofs);
        chipActive = findViewById(R.id.chipActive);
        chipOther = findViewById(R.id.chipOther);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddSubscription).setOnClickListener(v ->
                startActivityForResult(new Intent(this, SubscriptionPlanFormActivity.class), REQUEST_ADD_SUBSCRIPTION));
        btnViewAllSubscriptionRequests.setOnClickListener(v ->
                startActivity(new Intent(this, SubscriptionRequestsActivity.class)));

        chipAll.setOnClickListener(v -> setFilter("all"));
        chipRequests.setOnClickListener(v -> setFilter("requests"));
        chipSubmitted.setOnClickListener(v -> setFilter("payment_proofs"));
        chipActive.setOnClickListener(v -> setFilter("active"));
        chipOther.setOnClickListener(v -> setFilter("other"));

        String initialFilter = getIntent().getStringExtra(EXTRA_INITIAL_FILTER);
        if (initialFilter != null) {
            currentFilter = "submitted".equals(initialFilter) ? "payment_proofs" : initialFilter;
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
        TextView[] chips = { chipAll, chipRequests, chipSubmitted, chipActive, chipOther };
        String[] keys = { "all", "requests", "payment_proofs", "active", "other" };
        for (int i = 0; i < chips.length; i++) {
            boolean selected = keys[i].equals(currentFilter);
            chips[i].setBackground(ContextCompat.getDrawable(this, selected ? R.drawable.chip_selected_green : R.drawable.chip_unselected));
            chips[i].setTextColor(ContextCompat.getColor(this, selected ? R.color.dark_green : android.R.color.darker_gray));
        }
    }

    private void loadSubscribers() {
        String token = "Bearer " + sessionManager.getToken();
        loadNewSubscriptionRequests(token);
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

    /**
     * New requests are shown before the roster because they need a decision
     * before they can become subscribers. The full inbox remains available for
     * payment-proof review and past decisions.
     */
    private void loadNewSubscriptionRequests(String token) {
        apiService.getCookSubscriptionRequests(token, "requested")
                .enqueue(new Callback<SubscriptionRequestsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionRequestsResponse> call,
                                           @NonNull Response<SubscriptionRequestsResponse> response) {
                        List<SubscriptionRequestsResponse.Item> requests = response.isSuccessful()
                                && response.body() != null && response.body().isSuccess()
                                ? response.body().getRequests() : null;
                        renderNewSubscriptionRequests(requests);
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionRequestsResponse> call, @NonNull Throwable t) {
                        // The subscriber roster is still useful if this optional
                        // companion request fails to load, so keep the section hidden.
                        renderNewSubscriptionRequests(null);
                    }
                });
    }

    private void renderNewSubscriptionRequests(List<SubscriptionRequestsResponse.Item> requests) {
        newSubscriptionRequests = requests == null ? new ArrayList<>() : new ArrayList<>(requests);
        if ("requests".equals(currentFilter)) renderSubscribers();
    }

    private void renderSubscriptionRequestsTab() {
        layoutNewSubscriptionRequests.removeAllViews();
        int count = newSubscriptionRequests.size();
        cardNewSubscriptionRequests.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        if (count == 0) return;

        tvNewSubscriptionRequestCount.setText(count == 1 ? "1 new" : count + " new");
        btnViewAllSubscriptionRequests.setText(count > 3 ? "View all " + count + " requests" : "View all requests");

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < count; i++) {
            SubscriptionRequestsResponse.Item request = newSubscriptionRequests.get(i);
            View card = inflater.inflate(R.layout.item_subscription_request_compact,
                    layoutNewSubscriptionRequests, false);
            TextView customer = card.findViewById(R.id.tvRequestCustomerName);
            TextView plan = card.findViewById(R.id.tvRequestPlan);
            TextView details = card.findViewById(R.id.tvRequestDetails);
            MaterialButton review = card.findViewById(R.id.btnReviewSubscriptionRequest);

            customer.setText(request.getCustomerName() == null ? "Customer" : request.getCustomerName());
            String duration = request.getDurationDays() > 0 ? request.getDurationDays() + " days" : request.getDuration();
            plan.setText((request.getPlanName() == null ? "Subscription plan" : request.getPlanName())
                    + (duration == null || duration.isEmpty() ? "" : " · " + duration));
            String start = request.getStartDate() == null ? "Start date to be confirmed"
                    : "Starts " + DeliveryDateUtils.formatLongDate(request.getStartDate());
            String amount = request.getTotalAmount() == null ? "" : " · "
                    + CurrencyUtils.formatRupees(request.getTotalAmount());
            details.setText(start + amount);

            View.OnClickListener openRequest = v -> startActivity(
                    SubscriptionRequestsActivity.intentFor(this, request.getId()));
            card.setOnClickListener(openRequest);
            review.setOnClickListener(openRequest);
            layoutNewSubscriptionRequests.addView(card);
        }
    }

    private void renderSubscribers() {
        layoutSubscribers.removeAllViews();
        cardNewSubscriptionRequests.setVisibility(View.GONE);

        if ("requests".equals(currentFilter)) {
            boolean empty = newSubscriptionRequests.isEmpty();
            tvEmptySubscribers.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) tvEmptySubscribers.setText("No new subscription requests.");
            renderSubscriptionRequestsTab();
            return;
        }

        List<SubscriptionResponse.Subscription> filtered = new ArrayList<>();
        for (SubscriptionResponse.Subscription sub : allSubscribers) {
            switch (currentFilter) {
                case "payment_proofs":
                    // Show ALL subscriptions with payment screenshots, not just "submitted"
                    // This includes: submitted (needs review), verified, and rejected
                    if (sub.getPaymentScreenshotUrl() != null && !sub.getPaymentScreenshotUrl().isEmpty()) {
                        filtered.add(sub);
                    }
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
            tvEmptySubscribers.setText("payment_proofs".equals(currentFilter)
                    ? "No payment proofs pending review."
                    : "No subscribers in this filter yet.");
            return;
        }
        tvEmptySubscribers.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        
        // Show payment proofs in a special visual grid layout
        if ("payment_proofs".equals(currentFilter)) {
            renderPaymentProofsGrid(filtered, inflater);
            return;
        }
        
        // Regular list layout for other tabs
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

    /**
     * Renders payment proofs in a visually appealing grid layout with images
     * Shows ALL payment screenshots: pending review, verified, and rejected
     */
    private void renderPaymentProofsGrid(List<SubscriptionResponse.Subscription> subscriptions, LayoutInflater inflater) {
        for (SubscriptionResponse.Subscription sub : subscriptions) {
            View card = inflater.inflate(R.layout.item_payment_proof, layoutSubscribers, false);

            ImageView ivPaymentProof = card.findViewById(R.id.ivPaymentProof);
            TextView tvCustomerName = card.findViewById(R.id.tvCustomerName);
            TextView tvPlanName = card.findViewById(R.id.tvPlanName);
            TextView tvSubmittedDate = card.findViewById(R.id.tvSubmittedDate);
            TextView tvStatusBadge = card.findViewById(R.id.tvStatusBadge);
            LinearLayout layoutActionButtons = card.findViewById(R.id.layoutActionButtons);
            MaterialButton btnVerifyProof = card.findViewById(R.id.btnVerifyProof);
            MaterialButton btnRejectProof = card.findViewById(R.id.btnRejectProof);

            tvCustomerName.setText(sub.getCustomerName() != null ? sub.getCustomerName() : "Customer");
            tvPlanName.setText((sub.getPlanName() != null ? sub.getPlanName() : "Plan")
                    + " · " + sub.getDurationLabel());
            
            String submittedAt = sub.getPaymentSubmittedAt();
            if (submittedAt != null && !submittedAt.isEmpty()) {
                tvSubmittedDate.setText("Submitted: " + DeliveryDateUtils.formatTimestampDate(submittedAt));
            } else if (sub.getCreatedAt() != null) {
                tvSubmittedDate.setText("Submitted: " + DeliveryDateUtils.formatLongDate(sub.getCreatedAt()));
            }

            // Set status badge based on payment status
            String paymentStatus = sub.getPaymentStatus();
            if ("verified".equals(paymentStatus)) {
                tvStatusBadge.setText("Verified");
                tvStatusBadge.setBackgroundResource(R.drawable.status_chip_delivered);
                tvStatusBadge.setTextColor(getColor(R.color.status_delivered_text));
                layoutActionButtons.setVisibility(View.GONE); // Hide buttons for verified
            } else if ("rejected".equals(paymentStatus)) {
                tvStatusBadge.setText("Rejected");
                tvStatusBadge.setBackgroundColor(0xFFEF4444); // Red background
                tvStatusBadge.setTextColor(0xFFFFFFFF); // White text
                layoutActionButtons.setVisibility(View.GONE); // Hide buttons for rejected
            } else {
                // submitted or pending review
                tvStatusBadge.setText("Pending Review");
                tvStatusBadge.setBackgroundResource(R.drawable.status_chip_preparing);
                tvStatusBadge.setTextColor(getColor(R.color.status_preparing_text));
                layoutActionButtons.setVisibility(View.VISIBLE); // Show buttons for pending
            }

            // Load payment proof image
            if (sub.getPaymentScreenshotUrl() != null && !sub.getPaymentScreenshotUrl().isEmpty()) {
                ImageUrlHelper.load(ivPaymentProof, sub.getPaymentScreenshotUrl(),
                        R.drawable.ic_image_placeholder, 16);
                        
                // Click to view full image
                ivPaymentProof.setOnClickListener(v -> showFullImageDialog(sub));
            }

            btnVerifyProof.setOnClickListener(v -> submitVerification(sub.getId(), "verified", ""));
            btnRejectProof.setOnClickListener(v -> showRejectDialog(sub));

            layoutSubscribers.addView(card);
        }
    }
    
    /**
     * Shows full-size payment proof image in dialog
     */
    private void showFullImageDialog(SubscriptionResponse.Subscription sub) {
        Context ctx = this;
        ImageView ivFullImage = new ImageView(ctx);
        ivFullImage.setAdjustViewBounds(true);
        ivFullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        if (sub.getPaymentScreenshotUrl() != null) {
            ImageUrlHelper.loadNoCrop(ivFullImage, sub.getPaymentScreenshotUrl(),
                    R.drawable.ic_image_placeholder);
        }
        
        new AlertDialog.Builder(ctx)
                .setTitle(sub.getCustomerName() + " - Payment Proof")
                .setView(ivFullImage)
                .setPositiveButton("Verify", (dialog, which) ->
                        submitVerification(sub.getId(), "verified", ""))
                .setNegativeButton("Reject", (dialog, which) ->
                        showRejectDialog(sub))
                .setNeutralButton("Close", null)
                .show();
    }
    
    /**
     * Shows reject dialog with reason input
     */
    private void showRejectDialog(SubscriptionResponse.Subscription sub) {
        EditText etReason = new EditText(this);
        etReason.setHint("Tell the customer what to correct");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etReason.setPadding(pad, pad, pad, pad);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.RoundedWhiteDialog)
                .setTitle("Reject payment proof?")
                .setMessage("The customer will see your reason and can upload a corrected screenshot.")
                .setView(etReason)
                .setPositiveButton("Reject", (d, which) ->
                        submitVerification(sub.getId(), "rejected", etReason.getText().toString().trim()))
                .setNegativeButton("Back", null)
                .create();
        dialog.setOnShowListener(d -> {
            Button reject = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button back = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            reject.setTextColor(ContextCompat.getColor(this, R.color.sub_error));
            back.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        });
        dialog.show();
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
