package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.EsewaStatusResponse;
import com.tiffincraft.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lands here after eSewa's app redirects back via the tiffincraft://payment-result
 * deeplink (registered in AndroidManifest.xml). Deliberately ignores anything
 * eSewa put in the returned URI — a redirect can be replayed or spoofed — and
 * instead re-derives which payment to check from durable local state
 * (SessionManager's pending-transaction fields, set right before the eSewa
 * app was launched), then asks our OWN backend for the server-confirmed
 * status via the eSewa status-check API.
 */
public class PaymentResultActivity extends AppCompatActivity {

    private static final int POLL_INTERVAL_MS = 7000;
    private static final int POLL_TIMEOUT_MS = 120000; // ~2 minutes, per eSewa's own "check after 5 min if no callback" guidance halved for a snappier UX loop before falling back to manual refresh.

    private ProgressBar progressChecking;
    private ImageView imgStatusIcon;
    private TextView tvStatusTitle, tvStatusMessage;
    private MaterialButton btnPrimaryAction, btnBackToOrders;

    private ApiService apiService;
    private SessionManager sessionManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private long pollStartedAt;

    private String transactionUuid;
    private int orderId = -1;
    private int subscriptionId = -1;
    /** True when the pending payment was for a subscription, not an order. */
    private boolean subscriptionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_result);

        progressChecking = findViewById(R.id.progressChecking);
        imgStatusIcon = findViewById(R.id.imgStatusIcon);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnBackToOrders = findViewById(R.id.btnBackToOrders);

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        transactionUuid = sessionManager.getPendingEsewaTransactionUuid();
        orderId = sessionManager.getPendingEsewaOrderId();
        subscriptionId = sessionManager.getPendingEsewaSubscriptionId();
        // Which flow this was is read from our own durable state, never from the
        // returned deeplink — a redirect can be replayed or hand-crafted.
        subscriptionMode = subscriptionId > 0;

        btnBackToOrders.setText(subscriptionMode ? "My Subscriptions" : "Back to Orders");
        btnBackToOrders.setOnClickListener(v -> goToLanding());

        if (transactionUuid == null || transactionUuid.isEmpty()) {
            showTerminalState(false, "No Payment In Progress",
                    "We couldn't find a payment to check. If you completed a payment, check its status from your order details.",
                    subscriptionMode ? "My Subscriptions" : "Back to Orders", this::goToLanding);
            return;
        }

        pollStartedAt = System.currentTimeMillis();
        checkStatus();
    }

    private void checkStatus() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getEsewaPaymentStatus(token, transactionUuid).enqueue(new Callback<EsewaStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<EsewaStatusResponse> call, @NonNull Response<EsewaStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    handleStatus(response.body().getStatus());
                } else {
                    scheduleNextPollOrTimeout("Couldn't reach the server to confirm your payment.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<EsewaStatusResponse> call, @NonNull Throwable t) {
                scheduleNextPollOrTimeout("Network error while confirming your payment.");
            }
        });
    }

    private void handleStatus(String status) {
        if (status == null) status = "";
        switch (status) {
            case "SUCCESS":
                sessionManager.clearPendingEsewaTransaction();
                if (subscriptionMode) {
                    // The backend only reports SUCCESS after it re-checked with
                    // eSewa's status API and flipped the subscription to active,
                    // so this is the first point at which claiming "Subscribed"
                    // is actually true.
                    showTerminalState(true, "Subscribed successfully!",
                            "Your payment is confirmed and your subscription is now active. The cook has been notified.",
                            "View Subscription", this::goToSubscriptions);
                } else {
                    showTerminalState(true, "Payment Successful!",
                            "Your order has been confirmed and the cook has been notified.",
                            "Track Order", () -> {
                                Intent intent = new Intent(this, TrackOrderActivity.class);
                                intent.putExtra("order_id", orderId);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                }
                break;

            case "FAILED":
                sessionManager.clearPendingEsewaTransaction();
                showTerminalState(false, "Payment Failed",
                        subscriptionMode
                                ? "Your payment could not be completed, so the subscription was not activated. No amount was charged and no meals were added — you can retry from My Subscriptions."
                                : "Your payment could not be completed. No amount was charged — you can try again.",
                        "Retry Payment", subscriptionMode ? this::goToSubscriptions : this::finish);
                break;

            case "CANCELED":
                sessionManager.clearPendingEsewaTransaction();
                showTerminalState(false, "Payment Cancelled",
                        subscriptionMode
                                ? "You cancelled the payment, so the subscription stays inactive. Nothing was charged — you can retry it from My Subscriptions."
                                : "You cancelled the payment in the eSewa app. You can try again anytime.",
                        "Retry Payment", subscriptionMode ? this::goToSubscriptions : this::finish);
                break;

            case "REVERTED":
                sessionManager.clearPendingEsewaTransaction();
                showTerminalState(false, "Payment Reverted",
                        "This payment was reverted by eSewa. Contact support if you weren't expecting this.",
                        subscriptionMode ? "My Subscriptions" : "Back to Orders", this::goToLanding);
                break;

            case "BOOKED":
            case "PENDING":
            default:
                scheduleNextPollOrTimeout(null);
                break;
        }
    }

    /** Keeps polling until POLL_TIMEOUT_MS, then swaps to a manual "Refresh Status" button instead of polling forever. */
    private void scheduleNextPollOrTimeout(String transientErrorMessage) {
        long elapsed = System.currentTimeMillis() - pollStartedAt;
        if (elapsed >= POLL_TIMEOUT_MS) {
            progressChecking.setVisibility(View.GONE);
            imgStatusIcon.setVisibility(View.VISIBLE);
            imgStatusIcon.setImageResource(R.drawable.ic_clock);
            imgStatusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.orange_warm)));
            tvStatusTitle.setText("Still Confirming…");
            tvStatusMessage.setText(subscriptionMode
                    ? "This is taking longer than usual. You can check again, or come back later — your subscription activates automatically once eSewa confirms the payment. Nothing is active until then."
                    : "This is taking longer than usual. You can check again, or come back later — your order will update automatically once eSewa confirms.");
            btnPrimaryAction.setVisibility(View.VISIBLE);
            btnPrimaryAction.setText("Refresh Status");
            btnPrimaryAction.setOnClickListener(v -> {
                pollStartedAt = System.currentTimeMillis();
                progressChecking.setVisibility(View.VISIBLE);
                imgStatusIcon.setVisibility(View.GONE);
                btnPrimaryAction.setVisibility(View.GONE);
                tvStatusTitle.setText("Confirming your payment…");
                tvStatusMessage.setText(transientErrorMessage != null ? transientErrorMessage : "Please wait while we verify your payment with eSewa.");
                checkStatus();
            });
            btnBackToOrders.setVisibility(View.VISIBLE);
            return;
        }

        if (transientErrorMessage != null) {
            tvStatusMessage.setText(transientErrorMessage + " Retrying…");
        }
        pollRunnable = this::checkStatus;
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void showTerminalState(boolean success, String title, String message, String primaryLabel, Runnable primaryAction) {
        progressChecking.setVisibility(View.GONE);
        imgStatusIcon.setVisibility(View.VISIBLE);
        imgStatusIcon.setImageResource(success ? R.drawable.ic_check_circle : R.drawable.ic_error);
        int tint = success ? R.color.green_primary : R.color.error_red;
        imgStatusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(tint)));

        tvStatusTitle.setText(title);
        tvStatusMessage.setText(message);

        btnPrimaryAction.setVisibility(View.VISIBLE);
        btnPrimaryAction.setText(primaryLabel);
        btnPrimaryAction.setOnClickListener(v -> primaryAction.run());

        btnBackToOrders.setVisibility(View.VISIBLE);
    }

    private void goToLanding() {
        if (subscriptionMode) {
            goToSubscriptions();
        } else {
            goToOrderHistory();
        }
    }

    private void goToSubscriptions() {
        Intent intent = new Intent(this,
                com.tiffincraft.app.activities.customer.CustomerSubscriptionsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void goToOrderHistory() {
        Intent intent = new Intent(this, OrderHistoryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
        }
    }
}
