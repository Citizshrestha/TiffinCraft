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
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.DayActionResponse;
import com.tiffincraft.app.models.SubscriptionCalendarResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.DeliveryDateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The per-day delivery schedule for one subscription — this is what replaced the
 * flat "Active" chip with a raw ISO next-delivery date.
 *
 * Two rules this screen follows strictly:
 *
 *  1. It never decides for itself whether a day can be changed. `canSkip`,
 *     `isLocked` and `lockedMessage` come from the server, which computes the
 *     8pm-Nepal-Time cutoff against the configurable platform setting. Doing
 *     that arithmetic here as well would give us two implementations that
 *     disagree at the boundary — and the boundary is the only place it matters.
 *
 *  2. A locked day still shows its button, disabled and relabelled. Hiding the
 *     control looked like a bug; a greyed control plus the server's own
 *     explanation ("the cutoff was 8:00 PM on ...") tells the customer why.
 *
 * Opened by both sides. A cook arrives here read-only: the response's `viewer`
 * field decides, not the local session role, so the UI can't offer an action
 * the server would refuse.
 */
public class SubscriptionCalendarActivity extends AppCompatActivity {

    public static final String EXTRA_SUBSCRIPTION_ID = "subscription_id";
    /** Optional — shown in the header while the first load is in flight. */
    public static final String EXTRA_PLAN_NAME = "plan_name";

    private ApiService apiService;
    private SessionManager sessionManager;

    private int subscriptionId;

    private LinearLayout layoutDays;
    private View cardSummary;
    private View progressLoading;
    private TextView tvHeaderPlan, tvSummaryTitle, tvSummaryStatusChip, tvSummaryDetail,
            tvSummaryCredits, tvCutoffBanner, tvEmptyDays;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private View cardCutoffBanner;

    private final List<SubscriptionCalendarResponse.Day> days = new ArrayList<>();
    private String todayNpt;
    private boolean isCustomerView = true;
    /** Guards against a double-tap firing two skips for the same date. */
    private boolean actionInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_calendar);

        subscriptionId = getIntent().getIntExtra(EXTRA_SUBSCRIPTION_ID, -1);
        if (subscriptionId <= 0) {
            Toast.makeText(this, "Couldn't open that subscription.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        layoutDays = findViewById(R.id.layoutDays);
        cardSummary = findViewById(R.id.cardSummary);
        cardCutoffBanner = findViewById(R.id.cardCutoffBanner);
        progressLoading = findViewById(R.id.progressLoading);
        tvHeaderPlan = findViewById(R.id.tvHeaderPlan);
        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvSummaryStatusChip = findViewById(R.id.tvSummaryStatusChip);
        tvSummaryDetail = findViewById(R.id.tvSummaryDetail);
        tvSummaryCredits = findViewById(R.id.tvSummaryCredits);
        tvCutoffBanner = findViewById(R.id.tvCutoffBanner);
        tvEmptyDays = findViewById(R.id.tvEmptyDays);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        String planName = getIntent().getStringExtra(EXTRA_PLAN_NAME);
        if (planName != null && !planName.isEmpty()) {
            tvHeaderPlan.setText(planName);
            tvHeaderPlan.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadCalendar);

        loadCalendar();
    }

    private void loadCalendar() {
        progressLoading.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getSubscriptionCalendar(token, subscriptionId).enqueue(new Callback<SubscriptionCalendarResponse>() {
            @Override
            public void onResponse(@NonNull Call<SubscriptionCalendarResponse> call,
                                   @NonNull Response<SubscriptionCalendarResponse> response) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    // 403 here means the logged-in user doesn't own this
                    // subscription — worth saying plainly rather than "failed".
                    Toast.makeText(SubscriptionCalendarActivity.this,
                            response.code() == 403
                                    ? "This subscription isn't yours to view."
                                    : "Couldn't load the delivery schedule.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                SubscriptionCalendarResponse body = response.body();
                todayNpt = body.getToday();
                isCustomerView = body.isCustomerView();

                days.clear();
                if (body.getDays() != null) days.addAll(body.getDays());

                renderSummary(body);
                renderCutoff(body.getCutoff());
                renderDays();
            }

            @Override
            public void onFailure(@NonNull Call<SubscriptionCalendarResponse> call, @NonNull Throwable t) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(SubscriptionCalendarActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSummary(SubscriptionCalendarResponse body) {
        SubscriptionCalendarResponse.Info info = body.getSubscription();
        if (info == null) return;

        cardSummary.setVisibility(View.VISIBLE);
        String planName = info.getPlanName() != null ? info.getPlanName() : "Subscription";
        tvSummaryTitle.setText(planName);
        tvHeaderPlan.setText(planName);
        tvHeaderPlan.setVisibility(View.VISIBLE);

        if (info.isScheduled()) {
            // Deliberately NOT "Active" — nothing is being delivered yet, and the
            // customer has already paid. Both facts have to survive to the label.
            tvSummaryStatusChip.setText("Starts soon");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_preparing);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_preparing_text));

            Integer until = info.getDaysUntilStart();
            String when = DeliveryDateUtils.formatLongDate(info.getStartDate());
            String relative = until == null ? ""
                    : until == 0 ? " — today"
                    : until == 1 ? " — tomorrow"
                    : " — in " + until + " days";
            tvSummaryDetail.setText("Your first delivery is " + when + relative
                    + ". Nothing to do until then.");
        } else if ("active".equals(info.getStatus())) {
            tvSummaryStatusChip.setText("Active");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_delivered);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_delivered_text));
            tvSummaryDetail.setText(info.getNextDeliveryDate() != null
                    ? "Next delivery " + DeliveryDateUtils.formatLongDate(info.getNextDeliveryDate())
                    : "No further deliveries are scheduled.");
        } else if ("paused".equals(info.getStatus())) {
            tvSummaryStatusChip.setText("Paused");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_pending);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_pending_text));
            tvSummaryDetail.setText("Deliveries are on hold, so days can't be changed until you resume.");
        } else if ("completed".equals(info.getStatus())) {
            tvSummaryStatusChip.setText("Completed");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_delivered);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_delivered_text));
            tvSummaryDetail.setText("Every meal in this plan has been used.");
        } else {
            tvSummaryStatusChip.setText(info.getStatus() != null ? info.getStatus() : "—");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_pending);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_pending_text));
            tvSummaryDetail.setText("This subscription isn't delivering right now.");
        }

        // Legacy rows predate meal credits and have neither number — showing
        // "null of null meals left" would be worse than showing nothing.
        if (info.getMealsRemaining() != null && info.getMealsTotal() != null) {
            tvSummaryCredits.setText(info.getMealsRemaining() + " of " + info.getMealsTotal()
                    + " meals left · runs to " + DeliveryDateUtils.formatLongDate(info.getEndDate()));
            tvSummaryCredits.setVisibility(View.VISIBLE);
        } else {
            tvSummaryCredits.setVisibility(View.GONE);
        }
    }

    private void renderCutoff(SubscriptionCalendarResponse.Cutoff cutoff) {
        if (cutoff == null || !isCustomerView) {
            cardCutoffBanner.setVisibility(View.GONE);
            return;
        }
        String label = cutoff.getLabel() != null ? cutoff.getLabel() : "the daily cutoff";
        String date = DeliveryDateUtils.formatShortDate(cutoff.getNextEditableDate());
        String remaining = DeliveryDateUtils.formatDuration(cutoff.getMsUntilCutoff());

        tvCutoffBanner.setText(cutoff.getMsUntilCutoff() > 0
                ? "Changes to " + date + " close at " + label + " — " + remaining + " left."
                : "Today's " + label + " cutoff has passed. The earliest day you can still change is the one after "
                        + date + ".");
        cardCutoffBanner.setVisibility(View.VISIBLE);
    }

    private void renderDays() {
        layoutDays.removeAllViews();
        tvEmptyDays.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SubscriptionCalendarResponse.Day day : days) {
            View row = inflater.inflate(R.layout.item_subscription_day, layoutDays, false);

            TextView tvDate = row.findViewById(R.id.tvDayDate);
            TextView tvRelative = row.findViewById(R.id.tvDayRelative);
            TextView tvChip = row.findViewById(R.id.tvDayStatusChip);
            LinearLayout layoutReason = row.findViewById(R.id.layoutDayReason);
            TextView tvReason = row.findViewById(R.id.tvDayReason);
            LinearLayout layoutLocked = row.findViewById(R.id.layoutDayLocked);
            TextView tvLocked = row.findViewById(R.id.tvDayLockedNote);
            MaterialButton btnAction = row.findViewById(R.id.btnDayAction);
            View accent = row.findViewById(R.id.viewDayAccent);

            tvDate.setText(DeliveryDateUtils.formatDayHeading(day.getDate()));

            String relative = DeliveryDateUtils.describeRelative(day.getDate(), todayNpt);
            tvRelative.setText(relative);
            tvRelative.setVisibility(relative == null ? View.GONE : View.VISIBLE);

            applyDayChip(day, tvChip, accent);

            if (day.getReason() != null && !day.getReason().trim().isEmpty()) {
                tvReason.setText("cook".equals(day.getToggledBy())
                        ? "Cook's note: " + day.getReason()
                        : "Your note: " + day.getReason());
                layoutReason.setVisibility(View.VISIBLE);
            } else {
                layoutReason.setVisibility(View.GONE);
            }

            applyDayAction(day, btnAction, layoutLocked, tvLocked);

            layoutDays.addView(row);
        }
    }

    /** Colour and wording for one day. The label text is the server's, not ours. */
    private void applyDayChip(SubscriptionCalendarResponse.Day day, TextView chip, View accent) {
        String label = day.getLabel() != null ? day.getLabel() : day.getStatus();
        chip.setText(label);

        int chipBg;
        int chipText;
        int accentColor;
        if (day.isDelivered()) {
            chipBg = R.drawable.status_chip_delivered;
            chipText = getColor(R.color.status_delivered_text);
            accentColor = getColor(R.color.status_delivered_text);
        } else if (day.isCustomerSkipped()) {
            chipBg = R.drawable.status_chip_pending;
            chipText = getColor(R.color.status_pending_text);
            accentColor = getColor(R.color.status_pending_text);
        } else if (day.isCookUnavailable()) {
            chipBg = R.drawable.status_chip_sold_out;
            chipText = getColor(R.color.sub_error);
            accentColor = getColor(R.color.sub_error);
        } else if (day.isMissed()) {
            chipBg = R.drawable.status_chip_sold_out;
            chipText = getColor(R.color.sub_error);
            accentColor = getColor(R.color.sub_error);
        } else {
            // scheduled
            chipBg = R.drawable.status_chip_preparing;
            chipText = getColor(R.color.status_preparing_text);
            accentColor = getColor(R.color.green_primary_dark);
        }
        chip.setBackgroundResource(chipBg);
        chip.setTextColor(chipText);
        accent.setBackgroundColor(accentColor);
    }

    /**
     * Decides the row's button purely from the server's `canSkip`/`isLocked`.
     *
     * A cook viewing this screen gets no button at all — closing a date is a
     * bulk action on the cook's own Today's Deliveries screen, because it hits
     * every subscriber, not just this one.
     */
    private void applyDayAction(SubscriptionCalendarResponse.Day day, MaterialButton btn, LinearLayout layoutLocked, TextView lockedNote) {
        layoutLocked.setVisibility(View.GONE);

        if (!isCustomerView) {
            btn.setVisibility(View.GONE);
            return;
        }
        btn.setVisibility(View.VISIBLE);

        if (day.canSkip()) {
            btn.setEnabled(true);
            btn.setText("Skip this day");
            btn.setOnClickListener(v -> confirmSkip(day));
            return;
        }

        btn.setEnabled(false);
        btn.setOnClickListener(null);

        if (day.isLocked()) {
            // Greyed out but present, with the server's own explanation beneath.
            btn.setText("Cutoff passed");
            if (day.getLockedMessage() != null) {
                lockedNote.setText(day.getLockedMessage());
                layoutLocked.setVisibility(View.VISIBLE);
            }
        } else if (day.isCustomerSkipped()) {
            btn.setText("You already skipped this day");
        } else if (day.isCookUnavailable()) {
            btn.setText("Kitchen closed — nothing to skip");
        } else if (day.isDelivered()) {
            btn.setText("Already delivered");
        } else if (day.isMissed()) {
            btn.setText("Marked missed");
        } else {
            btn.setText("Can't be changed");
        }
    }

    private void confirmSkip(SubscriptionCalendarResponse.Day day) {
        String pretty = DeliveryDateUtils.formatLongDate(day.getDate());
        new MaterialAlertDialogBuilder(this)
                .setTitle("Skip " + pretty + "?")
                .setMessage("No meal will be delivered that day and you won't be charged for it — "
                        + "your subscription just runs a day longer instead.\n\n"
                        + "This can't be undone once the cutoff passes.")
                .setPositiveButton("Skip this day", (d, w) -> skipDay(day))
                .setNegativeButton("Keep it", null)
                .show();
    }

    private void skipDay(SubscriptionCalendarResponse.Day day) {
        if (actionInFlight) return;
        actionInFlight = true;

        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("date", day.getDate());

        apiService.skipSubscriptionDay(token, subscriptionId, body).enqueue(new Callback<DayActionResponse>() {
            @Override
            public void onResponse(@NonNull Call<DayActionResponse> call, @NonNull Response<DayActionResponse> response) {
                actionInFlight = false;
                DayActionResponse body = response.body();

                // The server's message is shown verbatim: "too late" and "the
                // kitchen is closed anyway" need completely different reactions
                // from the customer, and a generic error teaches them to retry.
                String message = body != null && body.getMessage() != null
                        ? body.getMessage()
                        : "Couldn't skip that day. Please try again.";
                Toast.makeText(SubscriptionCalendarActivity.this, message, Toast.LENGTH_LONG).show();

                // Reload either way — a rejection means our view of that day was
                // already stale, which is exactly when a refresh matters most.
                loadCalendar();
            }

            @Override
            public void onFailure(@NonNull Call<DayActionResponse> call, @NonNull Throwable t) {
                actionInFlight = false;
                Toast.makeText(SubscriptionCalendarActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Convenience for callers that only have the id and a name to show. */
    public static Intent intentFor(android.content.Context context, int subscriptionId, String planName) {
        Intent intent = new Intent(context, SubscriptionCalendarActivity.class);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId);
        if (planName != null) intent.putExtra(EXTRA_PLAN_NAME, planName);
        return intent;
    }
}
