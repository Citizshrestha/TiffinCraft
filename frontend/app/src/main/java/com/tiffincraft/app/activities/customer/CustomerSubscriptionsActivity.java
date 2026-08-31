package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionActionResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.DeliveryDateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Every subscription this customer has — active, paused, awaiting payment or
 * verification, rejected, and cancelled — not just the single most-recent one
 * CustomerProfileActivity's summary card shows. A customer can have more than
 * one simultaneous subscription (different cooks/plans), and this is the only
 * screen that actually surfaces all of them.
 */
public class CustomerSubscriptionsActivity extends AppCompatActivity {

    private ApiService apiService;
    private SessionManager sessionManager;

    private LinearLayout layoutSubscriptions;
    private View layoutEmptyState;
    private View progressLoading;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private final List<SubscriptionResponse.Subscription> subscriptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_subscriptions);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        layoutSubscriptions = findViewById(R.id.layoutSubscriptions);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressLoading = findViewById(R.id.progressLoading);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadSubscriptions);

        loadSubscriptions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cheap enough to just always refresh — this screen is only opened
        // occasionally, not a hot path that needs caching.
        if (!subscriptions.isEmpty()) loadSubscriptions();
    }

    private void loadSubscriptions() {
        progressLoading.setVisibility(subscriptions.isEmpty() ? View.VISIBLE : View.GONE);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMySubscriptions(token).enqueue(new Callback<SubscriptionResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionResponse> call, @NonNull Response<SubscriptionResponse> response) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(CustomerSubscriptionsActivity.this, "Failed to load subscriptions", Toast.LENGTH_SHORT).show();
                    return;
                }

                subscriptions.clear();
                if (response.body().getSubscriptions() != null) {
                    subscriptions.addAll(response.body().getSubscriptions());
                }
                render();
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionResponse> call, @NonNull Throwable t) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CustomerSubscriptionsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render() {
        layoutSubscriptions.removeAllViews();

        boolean isEmpty = subscriptions.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SubscriptionResponse.Subscription sub : subscriptions) {
            View card = inflater.inflate(R.layout.item_customer_subscription, layoutSubscriptions, false);

            TextView tvPlanName = card.findViewById(R.id.tvPlanName);
            TextView tvKitchenName = card.findViewById(R.id.tvKitchenName);
            TextView tvStatusChip = card.findViewById(R.id.tvStatusChip);
            TextView tvDetail = card.findViewById(R.id.tvSubscriptionDetail);
            TextView tvPrice = card.findViewById(R.id.tvPlanPrice);
            TextView tvPriceCaption = card.findViewById(R.id.tvPriceCaption);
            View layoutProgress = card.findViewById(R.id.layoutProgress);
            com.google.android.material.progressindicator.LinearProgressIndicator progressDays =
                    card.findViewById(R.id.progressDays);
            TextView tvProgressLabel = card.findViewById(R.id.tvProgressLabel);
            MaterialButton btnManage = card.findViewById(R.id.btnManage);

            String planName = sub.getPlanName() != null ? sub.getPlanName() : "Subscription";
            String kitchen = sub.getPlan() != null && sub.getPlan().getKitchenName() != null
                    ? sub.getPlan().getKitchenName() : null;
            String durationLabel = sub.getDurationLabel();

            tvPlanName.setText(planName);
            tvKitchenName.setText((kitchen != null ? "From " + kitchen + " · " : "") + durationLabel);

            // One payment for the whole plan, not a daily rate. The column behind
            // it is still called price_per_delivery, which is what made this card
            // say "/ delivery" and imply seven times the real cost.
            if (sub.getPlan() != null) {
                tvPrice.setText(CurrencyUtils.formatRupees(sub.getPlan().getPricePerDelivery()));
                tvPriceCaption.setText("one-time · " + durationLabel.toLowerCase() + ", 1 meal a day");
                tvPriceCaption.setVisibility(View.VISIBLE);
            } else {
                tvPrice.setText("—");
                tvPriceCaption.setVisibility(View.GONE);
            }

            applyProgress(sub, layoutProgress, progressDays, tvProgressLabel);

            applyStatus(sub, tvStatusChip, tvDetail, btnManage);

            // Tapping the card itself opens the schedule for any subscription
            // that has actually been paid for — one tap to the thing customers
            // come here for, with pause/cancel still behind the Manage button.
            if (sub.isLiveAndPaid() || "verified".equals(sub.getStatus())) {
                card.setOnClickListener(v -> openCalendar(sub));
            } else {
                card.setClickable(false);
            }

            layoutSubscriptions.addView(card);
        }
    }

    /**
     * "Day 3 of 9 · 2 Sep – Sep 10, 2026", with a bar.
     *
     * The length is measured from the actual start/end dates rather than the plan's
     * nominal 7/14/30, because the window GROWS: every day skipped in advance pushes
     * the end date out by one, and a bar capped at the plan length would sit full
     * while deliveries were still running.
     *
     * Hidden entirely until the cook has fixed the window — before that there is no
     * end date and any bar would be inventing one.
     */
    private void applyProgress(SubscriptionResponse.Subscription sub, View container,
                               com.google.android.material.progressindicator.LinearProgressIndicator bar,
                               TextView label) {
        String start = sub.getStartDate();
        String end = sub.getEndDate();
        Long span = start != null && end != null ? DeliveryDateUtils.daysBetween(start, end) : null;
        if (span == null || span < 0) {
            container.setVisibility(View.GONE);
            return;
        }

        int totalDays = (int) (span + 1);
        Long elapsed = DeliveryDateUtils.daysBetween(start, DeliveryDateUtils.deviceToday());
        // Clamped both ways: a scheduled subscription hasn't started (day 0) and a
        // completed one can't be on day 12 of 9.
        int dayNumber = elapsed == null ? 0 : (int) Math.max(0, Math.min(totalDays, elapsed + 1));

        container.setVisibility(View.VISIBLE);
        bar.setMax(totalDays);
        bar.setProgress(dayNumber);
        label.setText((dayNumber == 0 ? "Starts " + DeliveryDateUtils.formatShortDate(start)
                : "Day " + dayNumber + " of " + totalDays)
                + "  ·  ends " + DeliveryDateUtils.formatLongDate(end));
    }

    private void applyStatus(SubscriptionResponse.Subscription sub, TextView chip, TextView detail, MaterialButton btnManage) {
        String status = sub.getStatus();
        String paymentStatus = sub.getPaymentStatus();

        btnManage.setVisibility(View.VISIBLE);

        // The request-flow statuses. They all resolve to the same answer — "open
        // the status screen" — because that screen renders the server's own
        // headline/detail, and re-wording them here is how the two sides start
        // telling the customer different things. Without this branch these three
        // fell through to the "active" tail and claimed deliveries were running.
        if ("requested".equals(status) || "accepted".equals(status) || "rejected".equals(status)) {
            boolean declined = "rejected".equals(status);
            boolean accepted = "accepted".equals(status);
            chip.setText(declined ? "Declined" : accepted ? "Pay now" : "Waiting for the cook");
            chip.setBackgroundResource(declined ? R.drawable.status_chip_sold_out
                    : accepted ? R.drawable.status_chip_pending : R.drawable.status_chip_new);
            chip.setTextColor(declined ? getColor(R.color.sub_error) : getColor(R.color.status_pending_text));
            detail.setText(declined
                    ? "The cook declined this request. Nothing was charged."
                    : accepted
                        ? "The cook accepted. Pay and upload a screenshot to start."
                        : "Sent to the cook. Don't pay until they've accepted.");
            btnManage.setText(declined ? "Details" : accepted ? "Pay Now" : "View Status");
            btnManage.setOnClickListener(v -> openSubscriptionPayment(sub));
            return;
        }

        if ("pending_payment".equals(status)) {
            // A gateway payment that failed or was abandoned retries through
            // eSewa, not the upload-proof screen — the two paths are told apart
            // by payment_method, which the server sets, never guessed here.
            if (sub.isEsewaPayment()) {
                boolean failed = "failed".equals(paymentStatus);
                chip.setText(failed ? "Payment Failed" : "Payment Pending");
                chip.setBackgroundResource(failed ? R.drawable.status_chip_sold_out : R.drawable.status_chip_pending);
                chip.setTextColor(failed ? getColor(R.color.sub_error) : getColor(R.color.status_pending_text));
                detail.setText(failed
                        ? "The payment didn't go through, so this subscription was never activated. Nothing was charged — you can retry."
                        : "This subscription activates as soon as your eSewa payment is confirmed.");
                btnManage.setText(failed ? "Retry Payment" : "Pay Now");
                btnManage.setOnClickListener(v -> retryEsewaSubscriptionPayment(sub));
                return;
            }

            switch (paymentStatus != null ? paymentStatus : "pending") {
                case "submitted":
                    chip.setText("Awaiting Verification");
                    chip.setBackgroundResource(R.drawable.status_chip_preparing);
                    chip.setTextColor(getColor(R.color.status_preparing_text));
                    detail.setText("The cook is reviewing your payment proof.");
                    btnManage.setText("View Status");
                    break;
                case "rejected":
                    chip.setText("Rejected");
                    chip.setBackgroundResource(R.drawable.status_chip_sold_out);
                    chip.setTextColor(0xFFA32D2D);
                    detail.setText(sub.getVerificationNotes() != null && !sub.getVerificationNotes().isEmpty()
                            ? sub.getVerificationNotes() : "Payment proof couldn't be verified — please re-upload.");
                    btnManage.setText("Re-upload Proof");
                    break;
                case "failed":
                    chip.setText("Payment Failed");
                    chip.setBackgroundResource(R.drawable.status_chip_sold_out);
                    chip.setTextColor(getColor(R.color.sub_error));
                    detail.setText("This payment didn't complete, so the subscription was never activated. You can try again.");
                    btnManage.setText("Try Again");
                    break;
                default:
                    chip.setText("Payment Pending");
                    chip.setBackgroundResource(R.drawable.status_chip_pending);
                    chip.setTextColor(getColor(R.color.status_pending_text));
                    detail.setText("Pay the cook and upload proof to activate this subscription.");
                    btnManage.setText("Complete Payment");
                    break;
            }
            btnManage.setOnClickListener(v -> openSubscriptionPayment(sub));
            return;
        }

        if ("paused".equals(status)) {
            chip.setText("Paused");
            chip.setBackgroundResource(R.drawable.status_chip_pending);
            chip.setTextColor(getColor(R.color.status_pending_text));
            detail.setText("Deliveries are on hold — resume anytime.");
            btnManage.setText("Resume");
            btnManage.setOnClickListener(v -> resumeSubscription(sub));
            return;
        }

        if ("cancelled".equals(status)) {
            chip.setText("Cancelled");
            chip.setBackgroundResource(R.drawable.status_chip_sold_out);
            chip.setTextColor(0xFFA32D2D);
            detail.setText("This subscription has ended.");
            btnManage.setVisibility(View.GONE);
            return;
        }

        if ("completed".equals(status)) {
            // The paid cycle ran to the end of end_date. Distinct from cancelled:
            // it finished normally, and resubscribing is the natural next step.
            chip.setText("Completed");
            chip.setBackgroundResource(R.drawable.status_chip_delivered);
            chip.setTextColor(getColor(R.color.status_delivered_text));
            detail.setText("All deliveries in this plan were completed. Subscribe again to continue.");
            btnManage.setText("Subscribe Again");
            btnManage.setOnClickListener(v -> openCookProfile(sub));
            return;
        }

        if ("pending_verification".equals(status)) {
            // Paid, but the cook hasn't confirmed it yet. A real state the
            // customer waits in — not the same as "hasn't paid".
            chip.setText("Awaiting Verification");
            chip.setBackgroundResource(R.drawable.status_chip_preparing);
            chip.setTextColor(getColor(R.color.status_preparing_text));
            detail.setText("The cook is confirming your payment. Deliveries start "
                    + DeliveryDateUtils.formatLongDate(sub.getStartDate()) + ".");
            btnManage.setText("View Status");
            btnManage.setOnClickListener(v -> openSubscriptionPayment(sub));
            return;
        }

        if ("verified".equals(status) || "scheduled".equals(status)) {
            // Paid AND confirmed — the customer's chosen start date simply hasn't
            // arrived. Showing "Active" here would promise deliveries that aren't
            // coming yet; showing a payment prompt would ask for money twice.
            chip.setText("Starts soon");
            chip.setBackgroundResource(R.drawable.status_chip_preparing);
            chip.setTextColor(getColor(R.color.status_preparing_text));
            detail.setText("All set — your first delivery is "
                    + DeliveryDateUtils.formatLongDate(sub.getStartDate())
                    + ". Nothing to do until then.");
            btnManage.setText("View Schedule");
            btnManage.setOnClickListener(v -> openCalendar(sub));
            return;
        }

        // active
        chip.setText("Active");
        chip.setBackgroundResource(R.drawable.status_chip_delivered);
        chip.setTextColor(getColor(R.color.status_delivered_text));
        // Nullable now: a subscription whose every remaining day is skipped has
        // no next delivery, and printing "null" (or the raw ISO timestamp this
        // used to show) is worse than saying so.
        detail.setText(sub.getNextDeliveryDate() != null
                ? "Next delivery " + DeliveryDateUtils.formatLongDate(sub.getNextDeliveryDate())
                : "No upcoming delivery — every remaining day is skipped or closed.");
        btnManage.setText("Manage");
        btnManage.setOnClickListener(v -> showManageDialog(sub));
    }

    /** The per-day schedule: skip a day, see what the cook has closed. */
    private void openCalendar(SubscriptionResponse.Subscription sub) {
        startActivity(SubscriptionCalendarActivity.intentFor(this, sub.getId(), sub.getPlanName()));
    }

    /**
     * Sends an unpaid/failed eSewa subscription back through the gateway.
     * /api/subscriptions/initiate reuses the existing pending_payment row rather
     * than creating a second one, so retrying can't leave duplicates behind.
     */
    private void retryEsewaSubscriptionPayment(SubscriptionResponse.Subscription sub) {
        Intent intent = new Intent(this, com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.class);
        intent.putExtra(com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.EXTRA_MODE,
                com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.MODE_SUBSCRIPTION);
        intent.putExtra(com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.EXTRA_PLAN_ID, sub.getPlanId());
        intent.putExtra(com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.EXTRA_COOK_ID, sub.getCookId());
        intent.putExtra(com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.EXTRA_DELIVERY_ADDRESS,
                sub.getDeliveryAddress());
        intent.putExtra(com.tiffincraft.app.activities.order.EsewaEpayCheckoutActivity.EXTRA_PLAN_NAME,
                sub.getPlanName());
        startActivity(intent);
    }

    private void openCookProfile(SubscriptionResponse.Subscription sub) {
        Intent intent = new Intent(this, com.tiffincraft.app.activities.meal.CookDetailsActivity.class);
        intent.putExtra(com.tiffincraft.app.activities.meal.CookDetailsActivity.EXTRA_COOK_ID, sub.getCookId());
        startActivity(intent);
    }

    /**
     * Pause/cancel, plus the way into the per-day schedule.
     *
     * The calendar option is listed FIRST and worded as the everyday action:
     * skipping one day is what a customer actually wants most of the time, and
     * before this screen had it the only thing they could do to a single
     * inconvenient day was pause or cancel the whole subscription.
     */
    private void showManageDialog(SubscriptionResponse.Subscription sub) {
        String[] options = { "View & skip delivery days", "Pause subscription", "Cancel subscription" };
        new MaterialAlertDialogBuilder(this, R.style.RoundedWhiteDialog)
                .setTitle("Manage Subscription")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCalendar(sub);
                    else if (which == 1) pauseSubscription(sub);
                    else confirmCancel(sub);
                })
                .show();
    }

    private void pauseSubscription(SubscriptionResponse.Subscription sub) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.pauseSubscription(token, sub.getId()).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                Toast.makeText(CustomerSubscriptionsActivity.this, "Subscription paused", Toast.LENGTH_SHORT).show();
                loadSubscriptions();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CustomerSubscriptionsActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resumeSubscription(SubscriptionResponse.Subscription sub) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.resumeSubscription(token, sub.getId()).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                Toast.makeText(CustomerSubscriptionsActivity.this, "Subscription resumed", Toast.LENGTH_SHORT).show();
                loadSubscriptions();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(CustomerSubscriptionsActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cancel — and say what it costs BEFORE the customer commits.
     *
     * Once the cook has confirmed the payment the full plan amount is owed, no
     * matter how early this happens or how many days were skipped, because the
     * cook blocked out the whole window. That is not something a customer should
     * discover after the fact, so the charge is in the dialog and the server's own
     * sentence is what gets shown afterwards.
     */
    private void confirmCancel(SubscriptionResponse.Subscription sub) {
        // Same set the server treats as chargeable, so the warning and the charge
        // can't disagree.
        String status = sub.getStatus() == null ? "" : sub.getStatus();
        boolean chargeable = "verified".equals(status) || "scheduled".equals(status)
                || "active".equals(status) || "paused".equals(status);

        String message = chargeable
                ? "Please note that cancelling this subscription will not result in a refund. "
                        + "The cook has already committed to preparing meals for the scheduled days. "
                        + "Your payment will remain with the cook.\n\nThis action cannot be undone."
                : "The cook hasn't confirmed this yet, so nothing is owed. If you already "
                        + "transferred money, cancelling flags it to be returned to you."
                        + "\n\nThis cannot be undone.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Subscription")
                .setMessage(message)
                .setPositiveButton("Cancel Subscription", (dialog, which) -> {
                    String token = "Bearer " + sessionManager.getToken();
                    apiService.cancelSubscription(token, sub.getId()).enqueue(new Callback<SubscriptionActionResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<SubscriptionActionResponse> call, @NonNull Response<SubscriptionActionResponse> response) {
                            SubscriptionActionResponse b = response.body();
                            // The server's sentence states the amount owed or the
                            // refund flagged; a 409 ("already cancelled") comes back
                            // the same way and is equally worth reading.
                            String note = b != null && b.getMessage() != null && !b.getMessage().trim().isEmpty()
                                    ? b.getMessage()
                                    : (response.isSuccessful() ? "Subscription cancelled" : "Could not cancel. Try again.");
                            new MaterialAlertDialogBuilder(CustomerSubscriptionsActivity.this)
                                    .setTitle(response.isSuccessful() ? "Cancelled" : "Not cancelled")
                                    .setMessage(note)
                                    .setPositiveButton("OK", null)
                                    .show();
                            loadSubscriptions();
                        }

                        @Override
                        public void onFailure(@NonNull Call<SubscriptionActionResponse> call, @NonNull Throwable t) {
                            Toast.makeText(CustomerSubscriptionsActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Keep it", null)
                .show();
    }

    /**
     * Every "what's happening / pay / re-upload" route lands on the one status
     * screen, which re-fetches the subscription itself.
     *
     * It takes only the id on purpose. The old screen was handed a plan name,
     * price, QR and payment status through Intent extras, which went stale the
     * moment the cook acted — and left the screen unable to show anything at all
     * for a row whose plan had been deleted.
     */
    private void openSubscriptionPayment(SubscriptionResponse.Subscription sub) {
        startActivity(SubscriptionStatusActivity.intentFor(this, sub.getId()));
    }
}
