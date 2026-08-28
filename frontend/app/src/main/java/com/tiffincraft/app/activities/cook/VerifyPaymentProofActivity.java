package com.tiffincraft.app.activities.cook;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.MediaViewerActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.SubscriptionActionResponse;
import com.tiffincraft.app.models.SubscriptionDetailResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.DeliveryDateUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Verify or reject one payment screenshot.
 *
 * A whole screen rather than a dialog because the cook has to actually read an
 * amount, a date and a sender name off the image, and a thumbnail in a list is
 * how a wrong screenshot gets approved.
 *
 * MANUAL, TRUST-BASED STEP. Nothing here proves money arrived. The only
 * mechanical guarantee is server-side: the same image file can never be
 * submitted against two different subscriptions. That caveat is printed on this
 * screen, next to the button that acts on it, not buried in a help page.
 *
 * Rejecting does not delete anything — the image stays on the record for a later
 * dispute and the subscription goes back to a state the customer can re-upload
 * from. The reason is mandatory for exactly that reason: it is what the customer
 * is shown, and what an admin reads if this is disputed.
 */
public class VerifyPaymentProofActivity extends AppCompatActivity {

    private static final String EXTRA_SUBSCRIPTION_ID = "subscription_id";

    public static Intent intentFor(Context context, int subscriptionId) {
        Intent intent = new Intent(context, VerifyPaymentProofActivity.class);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId);
        return intent;
    }

    private ApiService apiService;
    private SessionManager sessionManager;
    private int subscriptionId;

    private SwipeRefreshLayout swipeRefresh;
    private View progressLoading;
    private TextView tvHeaderCustomer, tvExpectedAmount, tvAmountBreakdown, tvPlanLine,
            tvWindow, tvSubmittedAt, tvRetryWarning, tvNoProof, tvEventsTitle;
    private ImageView imgProof;
    private LinearLayout layoutEvents, layoutActions;
    private MaterialButton btnVerifyProof, btnRejectProof;

    private SubscriptionDetailResponse.Subscription current;
    private boolean actionInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_payment_proof);

        subscriptionId = getIntent().getIntExtra(EXTRA_SUBSCRIPTION_ID, 0);
        if (subscriptionId <= 0) {
            Toast.makeText(this, "Missing subscription.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressLoading = findViewById(R.id.progressLoading);
        tvHeaderCustomer = findViewById(R.id.tvHeaderCustomer);
        tvExpectedAmount = findViewById(R.id.tvExpectedAmount);
        tvAmountBreakdown = findViewById(R.id.tvAmountBreakdown);
        tvPlanLine = findViewById(R.id.tvPlanLine);
        tvWindow = findViewById(R.id.tvWindow);
        tvSubmittedAt = findViewById(R.id.tvSubmittedAt);
        tvRetryWarning = findViewById(R.id.tvRetryWarning);
        tvNoProof = findViewById(R.id.tvNoProof);
        tvEventsTitle = findViewById(R.id.tvEventsTitle);
        imgProof = findViewById(R.id.imgProof);
        layoutEvents = findViewById(R.id.layoutEvents);
        layoutActions = findViewById(R.id.layoutActions);
        btnVerifyProof = findViewById(R.id.btnVerifyProof);
        btnRejectProof = findViewById(R.id.btnRejectProof);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadDetail);

        btnVerifyProof.setOnClickListener(v -> confirmVerify());
        btnRejectProof.setOnClickListener(v -> promptReject());

        loadDetail();
    }

    private void loadDetail() {
        if (current == null) progressLoading.setVisibility(View.VISIBLE);

        apiService.getSubscriptionDetail("Bearer " + sessionManager.getToken(), subscriptionId)
                .enqueue(new Callback<SubscriptionDetailResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionDetailResponse> call,
                                           @NonNull Response<SubscriptionDetailResponse> response) {
                        progressLoading.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (!response.isSuccessful() || response.body() == null
                                || !response.body().isSuccess() || response.body().getSubscription() == null) {
                            Toast.makeText(VerifyPaymentProofActivity.this,
                                    "Couldn't load this subscription.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        render(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionDetailResponse> call, @NonNull Throwable t) {
                        progressLoading.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(VerifyPaymentProofActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void render(SubscriptionDetailResponse body) {
        SubscriptionDetailResponse.Subscription sub = body.getSubscription();
        current = sub;

        tvHeaderCustomer.setText(sub.getCustomerName() != null ? sub.getCustomerName() : "Customer");

        // The number the cook is checking the screenshot against. Showing the
        // per-day rate underneath it is what makes the total checkable rather
        // than just asserted.
        Double total = sub.getTotalAmount();
        if (total != null) {
            tvExpectedAmount.setText("Rs. " + SubscriptionRequestsActivity.fmt(total));
            tvAmountBreakdown.setText("Rs. " + SubscriptionRequestsActivity.fmt(sub.getAmount())
                    + " per day × " + sub.getDurationDays() + " days");
            tvAmountBreakdown.setVisibility(View.VISIBLE);
        } else {
            tvExpectedAmount.setText("Amount not set on this plan");
            tvAmountBreakdown.setVisibility(View.GONE);
        }

        tvPlanLine.setText(sub.getPlanName() != null ? sub.getPlanName() : "Subscription");

        String window = DeliveryDateUtils.formatShortDate(sub.getStartDate());
        if (sub.getEndDate() != null) window += " – " + DeliveryDateUtils.formatLongDate(sub.getEndDate());
        tvWindow.setText(window + "  ·  " + sub.getDurationDays() + " days");

        if (sub.getPaymentSubmittedAt() != null) {
            tvSubmittedAt.setText("Screenshot sent " + DeliveryDateUtils.formatShortDate(sub.getPaymentSubmittedAt()));
            tvSubmittedAt.setVisibility(View.VISIBLE);
        } else {
            tvSubmittedAt.setVisibility(View.GONE);
        }

        renderRetryWarning(sub);
        renderProof(sub);
        renderEvents(body);

        // Only offered when the server says this subscription is actually at the
        // verifying stage. On any other stage the buttons would be a lie.
        boolean canDecide = sub.isVerifying();
        layoutActions.setVisibility(canDecide ? View.VISIBLE : View.GONE);
    }

    private void renderRetryWarning(SubscriptionDetailResponse.Subscription sub) {
        if (sub.getPaymentProofAttempts() > 1) {
            String reason = sub.getPaymentRejectionReason();
            tvRetryWarning.setText("This is attempt " + sub.getPaymentProofAttempts() + "."
                    + (reason != null && !reason.trim().isEmpty()
                        ? " You rejected the previous one: \"" + reason + "\""
                        : " An earlier screenshot was rejected."));
            tvRetryWarning.setVisibility(View.VISIBLE);
        } else {
            tvRetryWarning.setVisibility(View.GONE);
        }
    }

    private void renderProof(SubscriptionDetailResponse.Subscription sub) {
        final String url = sub.getPaymentScreenshotUrl();
        boolean hasProof = url != null && !url.trim().isEmpty();

        imgProof.setVisibility(hasProof ? View.VISIBLE : View.GONE);
        ((View) imgProof.getParent()).setVisibility(hasProof ? View.VISIBLE : View.GONE);
        tvNoProof.setVisibility(hasProof ? View.GONE : View.VISIBLE);

        if (!hasProof) return;

        Glide.with(this).load(url).into(imgProof);
        // Pinch-zoom lives in the existing full-screen viewer; reading a small
        // transaction ID off a 420dp preview is not realistic.
        imgProof.setOnClickListener(v -> {
            Intent viewer = new Intent(this, MediaViewerActivity.class);
            viewer.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, url);
            viewer.putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, false);
            startActivity(viewer);
        });
    }

    /** The audit trail — the other half of a dispute. */
    private void renderEvents(SubscriptionDetailResponse body) {
        layoutEvents.removeAllViews();
        boolean any = body.getEvents() != null && !body.getEvents().isEmpty();
        tvEventsTitle.setVisibility(any ? View.VISIBLE : View.GONE);
        layoutEvents.setVisibility(any ? View.VISIBLE : View.GONE);
        if (!any) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SubscriptionDetailResponse.Event event : body.getEvents()) {
            View row = inflater.inflate(R.layout.item_subscription_event, layoutEvents, false);
            TextView title = row.findViewById(R.id.tvEventTitle);
            TextView detail = row.findViewById(R.id.tvEventDetail);
            TextView time = row.findViewById(R.id.tvEventTime);

            String label = event.getEvent() == null ? "—" : event.getEvent().replace('_', ' ');
            if (event.getAmount() != null) {
                label += "  ·  Rs. " + SubscriptionRequestsActivity.fmt(event.getAmount());
            }
            title.setText(label);

            // Shown verbatim: the server writes the actor into `detail` for
            // dispute resolution, so reformatting it here would lose that.
            boolean hasDetail = event.getDetail() != null && !event.getDetail().trim().isEmpty();
            detail.setText(hasDetail ? event.getDetail() : "");
            detail.setVisibility(hasDetail ? View.VISIBLE : View.GONE);

            time.setText(DeliveryDateUtils.formatShortDate(event.getCreatedAt()));
            layoutEvents.addView(row);
        }
    }

    /**
     * Verify. The dialog restates the amount and what verification starts,
     * because this is the irreversible-feeling step for the customer.
     */
    private void confirmVerify() {
        if (current == null) return;
        Double total = current.getTotalAmount();
        String amount = total != null ? "Rs. " + SubscriptionRequestsActivity.fmt(total) : "the plan amount";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm you received the payment?")
                .setMessage("Only do this if " + amount + " has actually landed in your account — "
                        + "check the amount, the date and the sender name on the screenshot against "
                        + "your own records.\n\nVerifying starts the subscription: deliveries run from "
                        + DeliveryDateUtils.formatShortDate(current.getStartDate())
                        + " for " + current.getDurationDays() + " calendar days.")
                .setPositiveButton("Yes, payment received", (d, w) -> sendDecision("verify", null))
                .setNegativeButton("Not yet", null)
                .show();
    }

    /**
     * Reject, with a mandatory reason.
     *
     * The reason is what the customer sees and what they have to act on to send
     * a usable screenshot, so an empty one is refused rather than defaulted.
     */
    private void promptReject() {
        final EditText input = new EditText(this);
        input.setHint("e.g. amount is short, or this is an old transfer");
        input.setMinLines(2);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(pad, pad / 2, pad, 0);
        wrapper.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reject this screenshot?")
                .setMessage("The customer is told why and can upload a new one. Nothing is deleted — "
                        + "this image stays on the record in case the payment is disputed later.")
                .setView(wrapper)
                .setPositiveButton("Reject proof", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this,
                                "Give a reason — it's what the customer has to fix.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    sendDecision("reject", reason);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void sendDecision(String action, String reason) {
        if (actionInFlight) return;
        actionInFlight = true;
        setButtonsEnabled(false);

        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        if (reason != null) body.addProperty("reason", reason);

        apiService.verifySubscriptionProof("Bearer " + sessionManager.getToken(), subscriptionId, body)
                .enqueue(new Callback<SubscriptionActionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionActionResponse> call,
                                           @NonNull Response<SubscriptionActionResponse> response) {
                        actionInFlight = false;
                        setButtonsEnabled(true);

                        SubscriptionActionResponse b = response.body();
                        boolean ok = response.isSuccessful() && b != null && b.isSuccess();

                        Toast.makeText(VerifyPaymentProofActivity.this,
                                b != null && b.getMessage() != null
                                        ? b.getMessage()
                                        : (ok ? "Done." : "Couldn't record that. Please try again."),
                                Toast.LENGTH_LONG).show();

                        if (ok) {
                            // The inbox behind this screen re-reads on resume, so
                            // closing is enough to keep both views consistent.
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            // Most likely somebody already acted on it; the
                            // re-fetch shows whichever state actually won.
                            loadDetail();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionActionResponse> call, @NonNull Throwable t) {
                        actionInFlight = false;
                        setButtonsEnabled(true);
                        Toast.makeText(VerifyPaymentProofActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnVerifyProof.setEnabled(enabled);
        btnRejectProof.setEnabled(enabled);
    }
}
