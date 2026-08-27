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
            MaterialButton btnManage = card.findViewById(R.id.btnManage);

            String planName = sub.getPlanName() != null ? sub.getPlanName() : "Subscription";
            String kitchen = sub.getPlan() != null && sub.getPlan().getKitchenName() != null
                    ? sub.getPlan().getKitchenName() : null;
            String durationLabel = sub.getDurationLabel();

            tvPlanName.setText(planName);
            tvKitchenName.setText((kitchen != null ? "From " + kitchen + " · " : "") + durationLabel);

            if (sub.getPlan() != null) {
                tvPrice.setText(CurrencyUtils.formatRupees(sub.getPlan().getPricePerDelivery()) + " / delivery");
            } else {
                tvPrice.setText("—");
            }

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

    private void applyStatus(SubscriptionResponse.Subscription sub, TextView chip, TextView detail, MaterialButton btnManage) {
        String status = sub.getStatus();
        String paymentStatus = sub.getPaymentStatus();

        btnManage.setVisibility(View.VISIBLE);

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
        new MaterialAlertDialogBuilder(this)
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

    private void confirmCancel(SubscriptionResponse.Subscription sub) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Subscription")
                .setMessage("Are you sure you want to cancel this subscription? This cannot be undone.")
                .setPositiveButton("Cancel Subscription", (dialog, which) -> {
                    String token = "Bearer " + sessionManager.getToken();
                    apiService.cancelSubscription(token, sub.getId()).enqueue(new Callback<RegisterResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                            Toast.makeText(CustomerSubscriptionsActivity.this, "Subscription cancelled", Toast.LENGTH_SHORT).show();
                            loadSubscriptions();
                        }

                        @Override
                        public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                            Toast.makeText(CustomerSubscriptionsActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Keep it", null)
                .show();
    }

    private void openSubscriptionPayment(SubscriptionResponse.Subscription sub) {
        if (sub.getPlan() == null) return;
        Intent intent = new Intent(this, SubscriptionPaymentActivity.class);
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_SUBSCRIPTION_ID, sub.getId());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PLAN_NAME, sub.getPlan().getName());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PLAN_PRICE, sub.getPlan().getPricePerDelivery());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PLAN_DURATION, sub.getPlan().getDuration());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_COOK_ESEWA_QR_URL, sub.getCookEsewaQrUrl());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_PAYMENT_STATUS, sub.getPaymentStatus());
        intent.putExtra(SubscriptionPaymentActivity.EXTRA_VERIFICATION_NOTES, sub.getVerificationNotes());
        startActivity(intent);
    }
}
