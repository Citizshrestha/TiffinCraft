package com.tiffincraft.app.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
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
import com.tiffincraft.app.models.CustomMeal;
import com.tiffincraft.app.models.DayActionResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionActionResponse;
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
                // Cutoff banner removed - confusing for users
                // renderCutoff(body.getCutoff());
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
            // If started (first delivery is today), show "Started" instead of "Starts soon"
            Integer until = info.getDaysUntilStart();
            boolean hasStarted = until != null && until == 0;
            
            tvSummaryStatusChip.setText(hasStarted ? "Started" : "Starts soon");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_preparing);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_preparing_text));

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
            tvSummaryDetail.setText("Its calendar window has ended.");
        } else {
            tvSummaryStatusChip.setText(info.getStatus() != null ? info.getStatus() : "—");
            tvSummaryStatusChip.setBackgroundResource(R.drawable.status_chip_pending);
            tvSummaryStatusChip.setTextColor(getColor(R.color.status_pending_text));
            tvSummaryDetail.setText("This subscription isn't delivering right now.");
        }

        // The window, not a meal count, is what the customer needs here:
        // meals_remaining is bookkeeping only, and the window itself moves — an
        // advance skip pushes end_date out by a day — so the live dates are the
        // honest thing to lead with.
        if (info.getEndDate() != null) {
            String range = DeliveryDateUtils.formatShortDate(info.getStartDate())
                    + " – " + DeliveryDateUtils.formatLongDate(info.getEndDate());
            
            // Calculate skipped meals count from total days
            if (info.getMealsTotal() != null) {
                Integer planDuration = info.getPlanDuration();  // Original plan duration
                int totalDays = info.getMealsTotal();
                
                if (planDuration != null && totalDays > planDuration) {
                    int skippedDays = totalDays - planDuration;
                    tvSummaryCredits.setText(range + " · " + planDuration + " days + " 
                            + skippedDays + (skippedDays == 1 ? " day added (Skipped Meals)" : " days added (Skipped Meals)"));
                } else {
                    tvSummaryCredits.setText(range + " · " + totalDays + " days");
                }
            } else {
                tvSummaryCredits.setText(range);
            }
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
            MaterialButton btnHandshake = row.findViewById(R.id.btnDayHandshake);
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

            // Handshake first: whether it took the row's action slot decides
            // whether applyDayAction has a note to add underneath.
            boolean handshakeShown = applyDayHandshake(day, btnHandshake);
            applyDayAction(day, btnAction, layoutLocked, tvLocked, handshakeShown);
            applyCustomMeal(day, row);

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
        } else if (day.isSent()) {
            // Blue, not green: the meal is in motion but the day isn't closed yet.
            // Sharing the delivered colour would make a handed-over meal look
            // confirmed on the cook's screen before the customer ever said so.
            chipBg = R.drawable.status_chip_out_for_delivery;
            chipText = getColor(R.color.blue);
            accentColor = getColor(R.color.blue);
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
     * The sent → received handshake button for one day. Returns true if it is
     * showing, so the caller knows this row already has its primary action.
     *
     * One button serves both sides because it is one exchange seen from two ends:
     * the cook marks the meal sent, the customer confirms it arrived. Which half
     * appears is the server's decision, not ours — `canMarkSent` is only ever true
     * for a cook and `canMarkReceived` only ever for a customer, and both mirror
     * exactly what their endpoint will accept. Reading them instead of combining
     * `isCustomerView` with a status guess is what keeps the two in step when the
     * server's rules change.
     */
    private boolean applyDayHandshake(SubscriptionCalendarResponse.Day day, MaterialButton btn) {
        btn.setOnClickListener(null);

        if (day.canMarkSent()) {
            btn.setText("Mark as sent");
            btn.setOnClickListener(v -> confirmMarkSent(day));
        } else if (day.canMarkReceived()) {
            btn.setText("Mark as received");
            btn.setOnClickListener(v -> confirmMarkReceived(day));
        } else {
            btn.setVisibility(View.GONE);
            return false;
        }

        btn.setVisibility(View.VISIBLE);
        btn.setEnabled(true);
        return true;
    }

    /**
     * Decides the row's button purely from the server's `canSkip`/`isLocked`.
     *
     * A day with nothing to do gets NO button — just a one-line note. A disabled
     * full-width button saying "You already skipped this day" occupied a real
     * action's worth of space to tell the customer they had nothing to press, and
     * the chip on the same row already says the day's state.
     *
     * Skipping stays customer-only: closing a date is a bulk action on the cook's
     * own Today's Deliveries screen, because it hits every subscriber rather than
     * just this one. The cook's per-day action is the handshake button above,
     * which is why this no longer means the cook sees nothing at all.
     *
     * `handshakeShown` suppresses the note. A day whose meal is on its way already
     * has a button on it, and adding "🔒 this day can't be changed" under a live
     * "Mark as received" reads as a contradiction.
     */
    private void applyDayAction(SubscriptionCalendarResponse.Day day, MaterialButton btn,
                                LinearLayout layoutLocked, TextView lockedNote,
                                boolean handshakeShown) {
        layoutLocked.setVisibility(View.GONE);
        btn.setOnClickListener(null);

        if (!isCustomerView || handshakeShown) {
            btn.setVisibility(View.GONE);
            return;
        }

        if (day.canSkip()) {
            btn.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
            btn.setText("Skip this day");
            btn.setOnClickListener(v -> confirmSkip(day));
            return;
        }

        btn.setVisibility(View.GONE);

        String icon;
        String note;
        if (day.isSent()) {
            // Ahead of the isLocked branch on purpose: today is always past its
            // own cutoff, so the generic "too late to change this" would otherwise
            // win and say nothing about the meal that is actually on its way.
            icon = "🛵";
            note = "Your cook has sent this meal. Confirm it once it reaches you.";
        } else if (day.isLocked()) {
            icon = "🔒";
            // The server's own sentence when it sent one — it knows the cutoff hour.
            note = day.getLockedMessage() != null
                    ? day.getLockedMessage()
                    : "The cutoff for this day has passed, so it can't be changed.";
        } else if (day.isCustomerSkipped()) {
            icon = "⏭";
            note = "You skipped this day, so your subscription runs one day longer.";
        } else if (day.isCookUnavailable()) {
            icon = "🚫";
            note = "The kitchen is closed on this day — nothing to skip.";
        } else if (day.isDelivered()) {
            icon = "✅";
            note = "Delivered.";
        } else if (day.isMissed()) {
            icon = "⚠️";
            note = "Marked missed.";
        } else {
            icon = "🔒";
            note = "This day can't be changed.";
        }

        TextView noteIcon = layoutLocked.findViewById(R.id.tvDayNoteIcon);
        if (noteIcon != null) noteIcon.setText(icon);
        lockedNote.setText(note);
        layoutLocked.setVisibility(View.VISIBLE);
    }

    /**
     * The swap row and the "request a different meal" button for one day.
     *
     * Both are driven entirely by the server: `customMeal` is the request that
     * already exists, and `canRequestCustom` is the server's own answer to
     * "would I accept a request for this date right now" — same conditions the
     * create endpoint enforces, so the button never appears for a day the server
     * would refuse.
     */
    private void applyCustomMeal(SubscriptionCalendarResponse.Day day, View row) {
        LinearLayout layout = row.findViewById(R.id.layoutDayCustomMeal);
        TextView tvMeal = row.findViewById(R.id.tvDayCustomMeal);
        TextView tvStatus = row.findViewById(R.id.tvDayCustomMealStatus);
        TextView tvCancel = row.findViewById(R.id.tvDayCustomMealCancel);
        MaterialButton btnRequest = row.findViewById(R.id.btnDayCustomMeal);

        CustomMeal swap = day.getCustomMeal();
        if (swap == null) {
            layout.setVisibility(View.GONE);
        } else {
            layout.setVisibility(View.VISIBLE);
            tvMeal.setText(swap.describe());
            tvStatus.setText(swap.isAccepted()
                    ? "The cook agreed to this instead of the usual plan meal."
                    : swap.isPending()
                        ? "Waiting for the cook to accept or decline."
                        : swap.isDeclined()
                            ? "Declined — you'll get the usual plan meal."
                            : "This request is closed.");

            boolean canWithdraw = isCustomerView && swap.canCancel() && !day.isLocked();
            tvCancel.setVisibility(canWithdraw ? View.VISIBLE : View.GONE);
            tvCancel.setOnClickListener(canWithdraw ? v -> confirmWithdrawSwap(swap) : null);
        }

        boolean canRequest = isCustomerView && day.canRequestCustom();
        btnRequest.setVisibility(canRequest ? View.VISIBLE : View.GONE);
        btnRequest.setOnClickListener(canRequest ? v -> promptCustomMeal(day) : null);
    }

    /**
     * Asks for the swap as free text.
     *
     * A meal picker would be better, but it needs the plan's own meal list, and
     * the note field is what the server already accepts on its own — meal_id is
     * optional there. Sending a note keeps this one dialog honest instead of
     * shipping a picker that can't be populated from this screen's payload.
     */
    private void promptCustomMeal(SubscriptionCalendarResponse.Day day) {
        String pretty = DeliveryDateUtils.formatLongDate(day.getDate());
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("e.g. dal bhat instead of momo");
        input.setMinLines(2);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(pad, pad / 2, pad, 0);
        wrapper.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Different meal on " + pretty + "?")
                .setMessage("Tell the cook what you'd like instead. They'll accept or decline it, "
                        + "and you'll get the usual plan meal if they can't do it.")
                .setView(wrapper)
                .setPositiveButton("Send request", (d, w) -> {
                    String note = input.getText().toString().trim();
                    if (note.isEmpty()) {
                        Toast.makeText(this, "Write what you'd like instead.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    requestCustomMeal(day, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestCustomMeal(SubscriptionCalendarResponse.Day day, String note) {
        if (actionInFlight) return;
        actionInFlight = true;

        JsonObject body = new JsonObject();
        body.addProperty("delivery_date", day.getDate());
        body.addProperty("note", note);

        apiService.createCustomMealRequest("Bearer " + sessionManager.getToken(), subscriptionId, body)
                .enqueue(new Callback<SubscriptionActionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionActionResponse> call,
                                           @NonNull Response<SubscriptionActionResponse> response) {
                        actionInFlight = false;
                        // Server wording verbatim: "that day is already skipped"
                        // and "the cutoff has passed" call for different reactions.
                        SubscriptionActionResponse b = response.body();
                        Toast.makeText(SubscriptionCalendarActivity.this,
                                b != null && b.getMessage() != null
                                        ? b.getMessage()
                                        : "Couldn't send that request.",
                                Toast.LENGTH_LONG).show();
                        loadCalendar();
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionActionResponse> call, @NonNull Throwable t) {
                        actionInFlight = false;
                        Toast.makeText(SubscriptionCalendarActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmWithdrawSwap(CustomMeal swap) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Withdraw this request?")
                .setMessage("The cook will make the usual plan meal for that day instead.")
                .setPositiveButton("Withdraw", (d, w) -> withdrawSwap(swap))
                .setNegativeButton("Keep it", null)
                .show();
    }

    private void withdrawSwap(CustomMeal swap) {
        if (actionInFlight) return;
        actionInFlight = true;

        apiService.cancelCustomMealRequest("Bearer " + sessionManager.getToken(), swap.getRequestId())
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                        actionInFlight = false;
                        RegisterResponse b = response.body();
                        Toast.makeText(SubscriptionCalendarActivity.this,
                                b != null && b.getMessage() != null ? b.getMessage() : "Request withdrawn.",
                                Toast.LENGTH_SHORT).show();
                        loadCalendar();
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                        actionInFlight = false;
                        Toast.makeText(SubscriptionCalendarActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmSkip(SubscriptionCalendarResponse.Day day) {
        String pretty = DeliveryDateUtils.formatLongDate(day.getDate());
        new MaterialAlertDialogBuilder(this)
                .setTitle("Skip " + pretty + "?")
                .setMessage("No meal will be delivered that day and you won't be charged for it.\n\n"
                        + "Because you're skipping it in advance, your subscription runs one day "
                        + "longer — that meal moves to the end of the window.\n\n"
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

    /**
     * Cook's half of the handshake. Confirmed rather than fired on tap because it
     * tells the customer their food is on the way and can't be taken back — and
     * the button sits in a scrolling list where a mis-tap is easy.
     */
    private void confirmMarkSent(SubscriptionCalendarResponse.Day day) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mark " + DeliveryDateUtils.formatLongDate(day.getDate()) + " as sent?")
                .setMessage("The customer will be notified straight away that their meal is on its way, "
                        + "and they'll be asked to confirm when it arrives.\n\nOnly do this once the meal has actually left.")
                .setPositiveButton("Yes, it's sent", (d, w) -> postDayHandshake(day, true))
                .setNegativeButton("Not yet", null)
                .show();
    }

    /**
     * Customer's half. This closes the day permanently — 'delivered' is terminal,
     * which is exactly why the confirmation is the customer's to give and not the
     * cook's — so it asks first and says so.
     */
    private void confirmMarkReceived(SubscriptionCalendarResponse.Day day) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Got " + DeliveryDateUtils.formatLongDate(day.getDate()) + "'s meal?")
                .setMessage("Confirming closes this day as delivered and lets your cook know it arrived. "
                        + "It can't be undone here, so only confirm if you actually received the meal.")
                .setPositiveButton("Yes, I got it", (d, w) -> postDayHandshake(day, false))
                .setNegativeButton("Not yet", null)
                .show();
    }

    /**
     * Posts one half of the handshake. `sent` picks which endpoint — the two are
     * separate routes with separate role guards on the server, so a customer
     * cannot declare their own meal sent.
     *
     * Same shape as skipDay: single in-flight guard, the server's message shown
     * verbatim, and a reload whether it succeeded or not. A rejection means this
     * screen's idea of the day was already stale, which is precisely when a
     * refresh matters most.
     */
    private void postDayHandshake(SubscriptionCalendarResponse.Day day, boolean sent) {
        if (actionInFlight) return;
        actionInFlight = true;

        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("date", day.getDate());

        Call<DayActionResponse> call = sent
                ? apiService.markSubscriptionDaySent(token, subscriptionId, body)
                : apiService.markSubscriptionDayReceived(token, subscriptionId, body);

        call.enqueue(new Callback<DayActionResponse>() {
            @Override
            public void onResponse(@NonNull Call<DayActionResponse> call, @NonNull Response<DayActionResponse> response) {
                actionInFlight = false;
                DayActionResponse b = response.body();
                String message = b != null && b.getMessage() != null
                        ? b.getMessage()
                        : (sent ? "Couldn't mark that day as sent. Please try again."
                                : "Couldn't confirm that day. Please try again.");
                Toast.makeText(SubscriptionCalendarActivity.this, message, Toast.LENGTH_LONG).show();
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
