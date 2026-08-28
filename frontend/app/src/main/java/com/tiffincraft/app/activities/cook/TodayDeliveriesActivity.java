package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.DayActionResponse;
import com.tiffincraft.app.models.TodayDeliveriesResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.DeliveryDateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The cook's prep list for today, and the one date they can still close.
 *
 * Three things this screen is careful about:
 *
 *  1. The date it SHOWS and the date it can CHANGE are different. Today's cutoff
 *     passed last night by definition, so the bulk action targets
 *     `next_changeable.date` — tomorrow — and that date is read from the
 *     response, never computed here. If the platform cutoff hour is
 *     reconfigured, the button follows without an app update.
 *
 *  2. Closing a date affects EVERY subscriber, so the confirmation dialog names
 *     the date and the number of people before anything is sent.
 *
 *  3. The count that matters to a cook is "how many meals do I make", not "how
 *     many subscribers do I have" — so `summary.cooking` is the headline and the
 *     skipped/closed counts are the small print.
 */
public class TodayDeliveriesActivity extends AppCompatActivity {

    /** Optional 'YYYY-MM-DD' — omitted means the server's today in Nepal Time. */
    public static final String EXTRA_DATE = "date";

    private ApiService apiService;
    private SessionManager sessionManager;

    private LinearLayout layoutDeliveries;
    private View progressLoading;
    private TextView tvHeaderDate, tvCookingCount, tvSummaryBreakdown, tvViewedDateClosed,
            tvNextChangeableTitle, tvCutoffCountdown, tvEmptyDeliveries;
    private MaterialButton btnToggleAvailability;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private final List<TodayDeliveriesResponse.Delivery> deliveries = new ArrayList<>();
    private String requestedDate;
    private String viewedDate;
    private TodayDeliveriesResponse.NextChangeable nextChangeable;
    private String cutoffLabel;
    /** Guards a double-tap from firing two bulk closures. */
    private boolean actionInFlight = false;

    // The countdown ticks off the device's monotonic clock against the server's
    // ms_until_cutoff captured at load time, rather than re-deriving the deadline
    // from the device wall clock — a phone with a skewed clock would otherwise
    // show a deadline the server disagrees with.
    private long cutoffMsAtLoad = -1;
    private long elapsedAtLoad = 0;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTick = new Runnable() {
        @Override
        public void run() {
            renderCountdown();
            countdownHandler.postDelayed(this, 30_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_deliveries);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        requestedDate = getIntent().getStringExtra(EXTRA_DATE);

        layoutDeliveries = findViewById(R.id.layoutDeliveries);
        progressLoading = findViewById(R.id.progressLoading);
        tvHeaderDate = findViewById(R.id.tvHeaderDate);
        tvCookingCount = findViewById(R.id.tvCookingCount);
        tvSummaryBreakdown = findViewById(R.id.tvSummaryBreakdown);
        tvViewedDateClosed = findViewById(R.id.tvViewedDateClosed);
        tvNextChangeableTitle = findViewById(R.id.tvNextChangeableTitle);
        tvCutoffCountdown = findViewById(R.id.tvCutoffCountdown);
        tvEmptyDeliveries = findViewById(R.id.tvEmptyDeliveries);
        btnToggleAvailability = findViewById(R.id.btnToggleAvailability);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadDeliveries);

        loadDeliveries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        countdownHandler.postDelayed(countdownTick, 30_000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        countdownHandler.removeCallbacks(countdownTick);
    }

    private void loadDeliveries() {
        progressLoading.setVisibility(deliveries.isEmpty() ? View.VISIBLE : View.GONE);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getTodayDeliveries(token, requestedDate).enqueue(new Callback<TodayDeliveriesResponse>() {
            @Override
            public void onResponse(@NonNull Call<TodayDeliveriesResponse> call,
                                   @NonNull Response<TodayDeliveriesResponse> response) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(TodayDeliveriesActivity.this,
                            response.code() == 403
                                    ? "Only a cook account can open this screen."
                                    : "Couldn't load today's deliveries.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                TodayDeliveriesResponse body = response.body();
                viewedDate = body.getDate();
                nextChangeable = body.getNextChangeable();
                cutoffLabel = body.getCutoff() != null && body.getCutoff().getLabel() != null
                        ? body.getCutoff().getLabel()
                        : "the daily cutoff";

                deliveries.clear();
                if (body.getDeliveries() != null) deliveries.addAll(body.getDeliveries());

                renderHeader(body);
                renderSummary(body);
                renderNextChangeable();
                renderDeliveries();
            }

            @Override
            public void onFailure(@NonNull Call<TodayDeliveriesResponse> call, @NonNull Throwable t) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(TodayDeliveriesActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderHeader(TodayDeliveriesResponse body) {
        String pretty = DeliveryDateUtils.formatDayHeading(viewedDate);
        tvHeaderDate.setText(body.isToday() ? pretty + " · today" : pretty);
    }

    private void renderSummary(TodayDeliveriesResponse body) {
        TodayDeliveriesResponse.Summary summary = body.getSummary();
        if (summary == null) {
            tvCookingCount.setText("—");
            tvSummaryBreakdown.setText("");
            tvViewedDateClosed.setVisibility(View.GONE);
            return;
        }

        int cooking = summary.getCooking();
        tvCookingCount.setText(cooking + (cooking == 1 ? " meal to make" : " meals to make"));

        // Only mention the exceptions that actually exist — a line reading
        // "0 skipped · 0 closed" is noise on a normal day.
        StringBuilder detail = new StringBuilder();
        detail.append(summary.getTotal()).append(summary.getTotal() == 1 ? " subscriber" : " subscribers");
        if (summary.getDelivered() > 0) detail.append(" · ").append(summary.getDelivered()).append(" delivered");
        if (summary.getCustomerSkipped() > 0) detail.append(" · ").append(summary.getCustomerSkipped()).append(" skipped by customer");
        if (summary.getCookUnavailable() > 0) detail.append(" · ").append(summary.getCookUnavailable()).append(" kitchen closed");
        if (summary.getMissed() > 0) detail.append(" · ").append(summary.getMissed()).append(" missed");
        if (summary.getCustomMealsConfirmed() > 0) detail.append(" · ").append(summary.getCustomMealsConfirmed()).append(" swapped");
        if (summary.getCustomMealsPending() > 0) detail.append(" · ").append(summary.getCustomMealsPending()).append(" swap to answer");
        tvSummaryBreakdown.setText(detail.toString());

        if (body.isViewedDateUnavailable()) {
            // No extension: the window was fixed at verification and a closed
            // date is simply a day with no meal and no charge.
            tvViewedDateClosed.setText("You marked this date closed. Nothing is being delivered — "
                    + "no one is charged, and no subscription's end date moves.");
            tvViewedDateClosed.setVisibility(View.VISIBLE);
        } else {
            tvViewedDateClosed.setVisibility(View.GONE);
        }
    }

    /**
     * The open/close control for the one date that's still changeable.
     *
     * The button label carries the date so it can never read as "cancel today" —
     * today is already past its cutoff and cannot be closed at all.
     */
    private void renderNextChangeable() {
        if (nextChangeable == null || nextChangeable.getDate() == null) {
            tvNextChangeableTitle.setText("No date can be changed right now");
            tvCutoffCountdown.setText("");
            btnToggleAvailability.setEnabled(false);
            btnToggleAvailability.setText("Unavailable");
            btnToggleAvailability.setOnClickListener(null);
            cutoffMsAtLoad = -1;
            return;
        }

        String pretty = DeliveryDateUtils.formatLongDate(nextChangeable.getDate());
        boolean closed = nextChangeable.isMarkedUnavailable();

        tvNextChangeableTitle.setText(closed
                ? "Kitchen closed for " + pretty
                : "Cooking as normal on " + pretty);

        cutoffMsAtLoad = nextChangeable.getMsUntilCutoff();
        elapsedAtLoad = SystemClock.elapsedRealtime();
        renderCountdown();

        if (nextChangeable.isLocked()) {
            // Past the cutoff for tomorrow too — the next changeable date only
            // reopens when the server rolls the window forward.
            btnToggleAvailability.setEnabled(false);
            btnToggleAvailability.setText("Cutoff passed for " + pretty);
            btnToggleAvailability.setOnClickListener(null);
            return;
        }

        btnToggleAvailability.setEnabled(true);
        if (closed) {
            btnToggleAvailability.setText("Reopen " + pretty);
            btnToggleAvailability.setOnClickListener(v -> confirmReopen(nextChangeable.getDate(), pretty));
        } else {
            btnToggleAvailability.setText("Mark " + pretty + " unavailable");
            btnToggleAvailability.setOnClickListener(v -> confirmClose(nextChangeable.getDate(), pretty));
        }
    }

    private void renderCountdown() {
        if (nextChangeable == null || cutoffMsAtLoad < 0) {
            tvCutoffCountdown.setText("");
            return;
        }
        long remaining = cutoffMsAtLoad - (SystemClock.elapsedRealtime() - elapsedAtLoad);
        if (remaining <= 0) {
            tvCutoffCountdown.setText("The " + cutoffLabel + " cutoff has passed — pull down to refresh.");
            return;
        }
        String date = DeliveryDateUtils.formatShortDate(nextChangeable.getDate());
        tvCutoffCountdown.setText("You can change " + date + " until " + cutoffLabel
                + " tonight — " + DeliveryDateUtils.formatDuration(remaining) + " left."
                + (nextChangeable.getReason() != null && !nextChangeable.getReason().trim().isEmpty()
                        ? "\nYour note: " + nextChangeable.getReason()
                        : ""));
    }

    private void renderDeliveries() {
        layoutDeliveries.removeAllViews();
        tvEmptyDeliveries.setVisibility(deliveries.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (TodayDeliveriesResponse.Delivery delivery : deliveries) {
            View row = inflater.inflate(R.layout.item_today_delivery, layoutDeliveries, false);

            TextView tvCustomer = row.findViewById(R.id.tvDeliveryCustomer);
            TextView tvChip = row.findViewById(R.id.tvDeliveryStatusChip);
            TextView tvPlan = row.findViewById(R.id.tvDeliveryPlan);
            TextView tvAddress = row.findViewById(R.id.tvDeliveryAddress);
            TextView tvReason = row.findViewById(R.id.tvDeliveryReason);
            MaterialButton btnCall = row.findViewById(R.id.btnDeliveryCall);
            MaterialButton btnSchedule = row.findViewById(R.id.btnDeliverySchedule);
            View accent = row.findViewById(R.id.viewDeliveryAccent);

            tvCustomer.setText(delivery.getCustomerName() != null ? delivery.getCustomerName() : "Customer");
            applyStatusChip(delivery, tvChip, accent);

            String plan = delivery.getPlanName() != null ? delivery.getPlanName() : "Subscription";
            tvPlan.setText(delivery.getMealsRemaining() != null
                    ? plan + " · " + delivery.getMealsRemaining() + " meals left"
                    : plan);

            tvAddress.setText(delivery.getDeliveryAddress() != null && !delivery.getDeliveryAddress().trim().isEmpty()
                    ? delivery.getDeliveryAddress()
                    : "No delivery address on file — call to confirm.");

            // The swap belongs on the customer's own row: it changes what to cook
            // for them, so putting it on a separate screen means cooking the wrong
            // thing while a correct answer sits one tap away. It takes priority
            // over the skip/close note because it is the actionable one.
            com.tiffincraft.app.models.CustomMeal swap = delivery.getCustomMeal();
            if (swap != null && (swap.isPending() || swap.isConfirmed())) {
                tvReason.setText(swap.isConfirmed()
                        ? "Cook instead: " + swap.describe()
                        : "Asked for instead: " + swap.describe() + " — needs your answer");
                tvReason.setVisibility(View.VISIBLE);
            } else if (delivery.getReason() != null && !delivery.getReason().trim().isEmpty()) {
                tvReason.setText("cook".equals(delivery.getToggledBy())
                        ? "Your note: " + delivery.getReason()
                        : "Customer's note: " + delivery.getReason());
                tvReason.setVisibility(View.VISIBLE);
            } else {
                tvReason.setVisibility(View.GONE);
            }

            String phone = delivery.getCustomerPhone();
            if (phone != null && !phone.trim().isEmpty()) {
                btnCall.setEnabled(true);
                // ACTION_DIAL, not ACTION_CALL — it opens the dialer with the
                // number filled in and needs no CALL_PHONE permission.
                btnCall.setOnClickListener(v -> startActivity(
                        new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()))));
            } else {
                btnCall.setEnabled(false);
                btnCall.setOnClickListener(null);
            }

            // Same button, repurposed when there is a swap waiting: an unanswered
            // request is the only thing on this row the cook has to decide, and a
            // second button would make the common row (no swap) busier for nothing.
            if (swap != null && swap.isPending()) {
                btnSchedule.setText("Answer swap");
                btnSchedule.setOnClickListener(v -> promptSwapDecision(delivery, swap));
            } else {
                btnSchedule.setText("Schedule");
                btnSchedule.setOnClickListener(v -> startActivity(SubscriptionCalendarActivity.intentFor(
                        TodayDeliveriesActivity.this, delivery.getSubscriptionId(), delivery.getPlanName())));
            }

            layoutDeliveries.addView(row);
        }
    }

    /**
     * Accept or decline one swap.
     *
     * Declining asks for no reason and sends none: the day still gets the plan's
     * default meal, so there is nothing for the customer to fix. Accepting is the
     * one that changes what gets cooked, which is why it names the meal back.
     */
    private void promptSwapDecision(TodayDeliveriesResponse.Delivery delivery,
                                    com.tiffincraft.app.models.CustomMeal swap) {
        String who = delivery.getCustomerName() != null ? delivery.getCustomerName() : "This customer";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Cook something else?")
                .setMessage(who + " asked for:\n\n" + swap.describe()
                        + "\n\nAccept and this replaces their plan meal for this day only. "
                        + "Decline and they get the usual plan meal — either way they're charged the same.")
                .setPositiveButton("Accept", (d, w) -> sendSwapDecision(swap.getRequestId(), "accept"))
                .setNegativeButton("Decline", (d, w) -> sendSwapDecision(swap.getRequestId(), "decline"))
                .setNeutralButton("Later", null)
                .show();
    }

    private void sendSwapDecision(int requestId, String action) {
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("action", action);

        apiService.respondToCustomMealRequest("Bearer " + sessionManager.getToken(), requestId, body)
                .enqueue(new Callback<com.tiffincraft.app.models.SubscriptionActionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.tiffincraft.app.models.SubscriptionActionResponse> call,
                                           @NonNull Response<com.tiffincraft.app.models.SubscriptionActionResponse> response) {
                        com.tiffincraft.app.models.SubscriptionActionResponse b = response.body();
                        boolean ok = response.isSuccessful() && b != null && b.isSuccess();
                        Toast.makeText(TodayDeliveriesActivity.this,
                                b != null && b.getMessage() != null ? b.getMessage()
                                        : (ok ? "Answered." : "Couldn't send that."),
                                Toast.LENGTH_LONG).show();
                        // Re-read rather than patch the row: the summary counters
                        // move too, and the swap may have expired past its cutoff.
                        if (ok) loadDeliveries();
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.tiffincraft.app.models.SubscriptionActionResponse> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(TodayDeliveriesActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyStatusChip(TodayDeliveriesResponse.Delivery delivery, TextView chip, View accent) {
        // The wording is the server's — one vocabulary for both sides.
        chip.setText(delivery.getLabel() != null ? delivery.getLabel() : delivery.getStatus());

        int chipBg;
        int chipText;
        if (delivery.isDelivered()) {
            chipBg = R.drawable.status_chip_delivered;
            chipText = getColor(R.color.status_delivered_text);
        } else if (delivery.isCustomerSkipped()) {
            chipBg = R.drawable.status_chip_pending;
            chipText = getColor(R.color.status_pending_text);
        } else if (delivery.isCookUnavailable() || delivery.isMissed()) {
            chipBg = R.drawable.status_chip_sold_out;
            chipText = getColor(R.color.sub_error);
        } else {
            chipBg = R.drawable.status_chip_preparing;
            chipText = getColor(R.color.status_preparing_text);
        }
        chip.setBackgroundResource(chipBg);
        chip.setTextColor(chipText);
        accent.setBackgroundColor(delivery.needsCooking()
                ? getColor(R.color.green_primary_dark)
                : chipText);
    }

    /**
     * Bulk closure needs an explicit confirmation because it touches every
     * subscriber at once. A cook who thinks this cancels one delivery would
     * otherwise stand up their whole list.
     *
     * Deliberately no subscriber COUNT here: the only count this screen holds is
     * for the date being viewed, and the closure targets a different date — a
     * subscription ending today or starting tomorrow makes the two sets differ.
     * The server reports the true `affected_subscriptions` in its response, and
     * that number is shown verbatim once the work has actually happened.
     */
    private void confirmClose(String date, String pretty) {
        View input = LayoutInflater.from(this).inflate(R.layout.dialog_availability_reason, null);
        android.widget.EditText etReason = input.findViewById(R.id.etReason);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Close the kitchen on " + pretty + "?")
                .setMessage("This affects EVERY subscriber expecting a meal that day, not just one — "
                        + "no meals will be delivered.\n\n"
                        + "Nobody is charged for it, and every subscription runs one day longer instead. "
                        + "You can reopen the day until " + cutoffLabel + " tonight.")
                .setView(input)
                .setPositiveButton("Close the kitchen", (d, w) ->
                        setUnavailable(date, etReason.getText().toString().trim()))
                .setNegativeButton("Keep cooking", null)
                .show();
    }

    private void confirmReopen(String date, String pretty) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reopen " + pretty + "?")
                .setMessage("Deliveries go back to normal for every subscriber who hadn't already "
                        + "skipped that day themselves.")
                .setPositiveButton("Reopen the day", (d, w) -> clearUnavailable(date))
                .setNegativeButton("Leave it closed", null)
                .show();
    }

    private void setUnavailable(String date, String reason) {
        if (actionInFlight) return;
        actionInFlight = true;

        JsonObject body = new JsonObject();
        body.addProperty("date", date);
        if (reason != null && !reason.isEmpty()) body.addProperty("reason", reason);

        apiService.setCookDailyUnavailability("Bearer " + sessionManager.getToken(), body)
                .enqueue(dayActionCallback("Couldn't close that day. Please try again."));
    }

    private void clearUnavailable(String date) {
        if (actionInFlight) return;
        actionInFlight = true;

        apiService.clearCookDailyUnavailability("Bearer " + sessionManager.getToken(), date)
                .enqueue(dayActionCallback("Couldn't reopen that day. Please try again."));
    }

    private Callback<DayActionResponse> dayActionCallback(String fallbackMessage) {
        return new Callback<DayActionResponse>() {
            @Override
            public void onResponse(@NonNull Call<DayActionResponse> call, @NonNull Response<DayActionResponse> response) {
                actionInFlight = false;
                DayActionResponse body = response.body();

                // Shown verbatim: the server's message is the only thing that
                // reports how many days it could NOT change (already delivered,
                // already settled), and that's exactly what the cook needs.
                Toast.makeText(TodayDeliveriesActivity.this,
                        body != null && body.getMessage() != null ? body.getMessage() : fallbackMessage,
                        Toast.LENGTH_LONG).show();

                // Refresh on rejection too — a refusal means our view was stale.
                loadDeliveries();
            }

            @Override
            public void onFailure(@NonNull Call<DayActionResponse> call, @NonNull Throwable t) {
                actionInFlight = false;
                Toast.makeText(TodayDeliveriesActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public static Intent intentFor(android.content.Context context, String isoDate) {
        Intent intent = new Intent(context, TodayDeliveriesActivity.class);
        if (isoDate != null) intent.putExtra(EXTRA_DATE, isoDate);
        return intent;
    }
}
