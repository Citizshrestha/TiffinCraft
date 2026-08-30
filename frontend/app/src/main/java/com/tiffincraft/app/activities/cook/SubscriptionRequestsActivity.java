package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.ChatActivity;
import com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.SubscriptionActionResponse;
import com.tiffincraft.app.models.SubscriptionRequestsResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.DeliveryDateUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The cook's Subscription Requests inbox — every subscription with a decision
 * blocked on them.
 *
 * Kept separate from the chat list and from CookSubscribersActivity on purpose:
 * this is a work queue, not a conversation list and not a roster of people
 * already being cooked for. A request announced in chat is still answerable from
 * the chat thread; this screen is the place a cook goes to find the ones they
 * haven't answered.
 *
 * Nothing on this screen decides a subscription's stage for itself. `stage`,
 * `headline`, `detail`, `needs_decision` and `needs_payment_check` all come from
 * the server's single stageFor(), so the buttons can never offer an action the
 * server would refuse. The chip counts likewise describe the whole inbox, not
 * the filtered slice, and are re-read from every response.
 */
public class SubscriptionRequestsActivity extends AppCompatActivity {

    /**
     * Subscription to scroll to and highlight on open. Set by a notification tap
     * (in-app row or push) so the cook lands on the request the alert was about
     * instead of a list they then have to search.
     */
    public static final String EXTRA_FOCUS_SUBSCRIPTION_ID = "focus_subscription_id";

    public static Intent intentFor(android.content.Context context, int subscriptionId) {
        Intent intent = new Intent(context, SubscriptionRequestsActivity.class);
        intent.putExtra(EXTRA_FOCUS_SUBSCRIPTION_ID, subscriptionId);
        return intent;
    }

    /** Server filter keys, in chip order. */
    private static final String FILTER_PENDING = "pending";
    private static final String FILTER_REQUESTED = "requested";
    private static final String FILTER_PROOF_CHECK = "awaiting_proof_check";
    private static final String FILTER_AWAITING_PAYMENT = "awaiting_payment";
    private static final String FILTER_ALL = "all";

    private ApiService apiService;
    private SessionManager sessionManager;

    private LinearLayout layoutRequests;
    private TextView tvHeaderSubtitle, tvEmpty;
    private TextView chipPending, chipRequested, chipProofCheck, chipAwaitingPayment, chipAll;
    private View progressLoading, cardTrustNote;
    private androidx.core.widget.NestedScrollView scrollRequests;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private final List<SubscriptionRequestsResponse.Item> items = new ArrayList<>();
    private String activeFilter = FILTER_PENDING;
    /** Guards a double-tap from sending two decisions for the same subscription. */
    private boolean actionInFlight = false;
    /** Cleared once used, so a rotate or resume doesn't re-flash the card. */
    private int focusSubscriptionId = 0;
    /** "Needs you" hides already-answered subscriptions — widen to All, but once. */
    private boolean focusFallbackTried = false;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_requests);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        layoutRequests = findViewById(R.id.layoutRequests);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressLoading = findViewById(R.id.progressLoading);
        cardTrustNote = findViewById(R.id.cardTrustNote);
        scrollRequests = findViewById(R.id.scrollRequests);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        chipPending = findViewById(R.id.chipPending);
        chipRequested = findViewById(R.id.chipRequested);
        chipProofCheck = findViewById(R.id.chipProofCheck);
        chipAwaitingPayment = findViewById(R.id.chipAwaitingPayment);
        chipAll = findViewById(R.id.chipAll);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadRequests);

        chipPending.setOnClickListener(v -> selectFilter(FILTER_PENDING));
        chipRequested.setOnClickListener(v -> selectFilter(FILTER_REQUESTED));
        chipProofCheck.setOnClickListener(v -> selectFilter(FILTER_PROOF_CHECK));
        chipAwaitingPayment.setOnClickListener(v -> selectFilter(FILTER_AWAITING_PAYMENT));
        chipAll.setOnClickListener(v -> selectFilter(FILTER_ALL));

        focusSubscriptionId = getIntent().getIntExtra(EXTRA_FOCUS_SUBSCRIPTION_ID, 0);

        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // onCreate already loaded — skip the first resume so a cold start doesn't
        // fire two identical requests. After that, refresh unconditionally:
        // coming back from the verify screen this list is stale by definition,
        // and an empty list is exactly the case that needs refreshing (the old
        // `if (!items.isEmpty())` guard left a deep link into an empty filter
        // permanently empty).
        if (firstResume) {
            firstResume = false;
            return;
        }
        loadRequests();
    }

    private void selectFilter(String filter) {
        if (filter.equals(activeFilter)) return;
        switchFilter(filter);
    }

    /** The filter change itself, without selectFilter's same-filter early return. */
    private void switchFilter(String filter) {
        activeFilter = filter;
        applyChipStyles();
        items.clear();
        loadRequests();
    }

    private void applyChipStyles() {
        styleChip(chipPending, FILTER_PENDING.equals(activeFilter));
        styleChip(chipRequested, FILTER_REQUESTED.equals(activeFilter));
        styleChip(chipProofCheck, FILTER_PROOF_CHECK.equals(activeFilter));
        styleChip(chipAwaitingPayment, FILTER_AWAITING_PAYMENT.equals(activeFilter));
        styleChip(chipAll, FILTER_ALL.equals(activeFilter));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.chip_selected_green : R.drawable.chip_unselected);
        chip.setTextColor(selected ? getColor(R.color.white) : getColor(android.R.color.darker_gray));
    }

    private void loadRequests() {
        progressLoading.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        apiService.getCookSubscriptionRequests("Bearer " + sessionManager.getToken(), activeFilter)
                .enqueue(new Callback<SubscriptionRequestsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionRequestsResponse> call,
                                           @NonNull Response<SubscriptionRequestsResponse> response) {
                        progressLoading.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                            Toast.makeText(SubscriptionRequestsActivity.this,
                                    "Couldn't load your subscription requests.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SubscriptionRequestsResponse body = response.body();
                        items.clear();
                        if (body.getRequests() != null) items.addAll(body.getRequests());

                        renderCounts(body.getCounts());
                        renderItems();
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionRequestsResponse> call, @NonNull Throwable t) {
                        progressLoading.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(SubscriptionRequestsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Counts go into the chip labels themselves. They describe the whole inbox,
     * so a cook on the "New requests" chip can still see that three payment
     * proofs are waiting.
     */
    private void renderCounts(SubscriptionRequestsResponse.Counts counts) {
        if (counts == null) {
            tvHeaderSubtitle.setVisibility(View.GONE);
            return;
        }
        int actionable = counts.getActionable();
        tvHeaderSubtitle.setText(actionable == 0
                ? "Nothing waiting on you"
                : actionable + (actionable == 1 ? " thing needs you" : " things need you"));
        tvHeaderSubtitle.setVisibility(View.VISIBLE);

        chipPending.setText(actionable > 0 ? "Needs you (" + actionable + ")" : "Needs you");
        chipRequested.setText(counts.getRequested() > 0
                ? "New requests (" + counts.getRequested() + ")" : "New requests");
        chipProofCheck.setText(counts.getAwaitingProofCheck() > 0
                ? "Payment proofs (" + counts.getAwaitingProofCheck() + ")" : "Payment proofs");
        chipAwaitingPayment.setText(counts.getAwaitingPayment() > 0
                ? "Awaiting payment (" + counts.getAwaitingPayment() + ")" : "Awaiting payment");

        // The by-eye caveat only matters when there's actually a proof to judge.
        cardTrustNote.setVisibility(counts.getAwaitingProofCheck() > 0 ? View.VISIBLE : View.GONE);
    }

    private void renderItems() {
        layoutRequests.removeAllViews();
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(FILTER_ALL.equals(activeFilter)
                ? "No subscription requests yet."
                : "Nothing waiting on you in this filter.");

        LayoutInflater inflater = LayoutInflater.from(this);
        View focusCard = null;
        for (SubscriptionRequestsResponse.Item item : items) {
            View card = buildCard(inflater, item);
            layoutRequests.addView(card);
            if (focusSubscriptionId != 0 && item.getId() == focusSubscriptionId) focusCard = card;
        }

        if (focusSubscriptionId == 0) return;

        if (focusCard == null) {
            // The cook already answered it, so it isn't in "Needs you". Widen the
            // filter once rather than showing an unrelated list — that silent
            // mismatch is what makes the notification tap feel broken.
            if (!focusFallbackTried && !FILTER_ALL.equals(activeFilter)) {
                focusFallbackTried = true;
                switchFilter(FILTER_ALL);
            } else {
                focusSubscriptionId = 0;
            }
            return;
        }

        highlightCard(focusCard);
        focusSubscriptionId = 0;
    }

    /** Scroll the deep-linked card into view and flash it, so the tap clearly landed. */
    private void highlightCard(View card) {
        card.post(() -> {
            if (scrollRequests != null) {
                // card.getTop() is relative to layoutRequests, which itself sits
                // below the trust note inside the scroll view's child column.
                int y = layoutRequests.getTop() + card.getTop() - 24;
                scrollRequests.smoothScrollTo(0, Math.max(0, y));
            }
            card.setAlpha(0.35f);
            card.animate().alpha(1f).setDuration(650).start();
        });
    }

    private View buildCard(LayoutInflater inflater, SubscriptionRequestsResponse.Item item) {
        View card = inflater.inflate(R.layout.item_subscription_request, layoutRequests, false);

        ImageView imgCustomer = card.findViewById(R.id.imgCustomer);
        TextView tvCustomerName = card.findViewById(R.id.tvCustomerName);
        TextView tvPlanLine = card.findViewById(R.id.tvPlanLine);
        TextView tvStageChip = card.findViewById(R.id.tvStageChip);
        TextView tvHeadline = card.findViewById(R.id.tvHeadline);
        TextView tvDetail = card.findViewById(R.id.tvDetail);
        TextView tvWindow = card.findViewById(R.id.tvWindow);
        TextView tvAmount = card.findViewById(R.id.tvAmount);
        TextView tvAddress = card.findViewById(R.id.tvAddress);
        LinearLayout layoutNote = card.findViewById(R.id.layoutNote);
        TextView tvNote = card.findViewById(R.id.tvNote);
        TextView tvRetryWarning = card.findViewById(R.id.tvRetryWarning);
        LinearLayout layoutDecisionActions = card.findViewById(R.id.layoutDecisionActions);
        MaterialButton btnAccept = card.findViewById(R.id.btnAccept);
        MaterialButton btnReject = card.findViewById(R.id.btnReject);
        MaterialButton btnReviewProof = card.findViewById(R.id.btnReviewProof);
        MaterialButton btnOpenChat = card.findViewById(R.id.btnOpenChat);
        MaterialButton btnOpenSchedule = card.findViewById(R.id.btnOpenSchedule);

        tvCustomerName.setText(item.getCustomerName() != null ? item.getCustomerName() : "Customer");
        tvPlanLine.setText(join(" · ", item.getPlanName(), item.getDuration()));

        if (item.getCustomerImage() != null && !item.getCustomerImage().trim().isEmpty()) {
            Glide.with(this).load(item.getCustomerImage())
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imgCustomer);
        } else {
            imgCustomer.setImageResource(R.drawable.ic_default_avatar);
        }

        applyStageChip(item, tvStageChip);
        tvHeadline.setText(item.getHeadline() != null ? item.getHeadline() : "—");
        tvDetail.setText(item.getDetail() != null ? item.getDetail() : "");
        tvDetail.setVisibility(item.getDetail() == null || item.getDetail().isEmpty() ? View.GONE : View.VISIBLE);

        // Both dates, plus the day count, so the window is unambiguous — the cook
        // is agreeing to cook every one of those calendar days.
        String window = DeliveryDateUtils.formatShortDate(item.getStartDate());
        if (item.getEndDate() != null) window += " – " + DeliveryDateUtils.formatLongDate(item.getEndDate());
        if (item.getDurationDays() > 0) window += "  ·  " + item.getDurationDays() + " days";
        tvWindow.setText(window);

        Double total = item.getTotalAmount();
        if (total != null) {
            tvAmount.setText("Rs. " + fmt(total) + " total · one-time");
            tvAmount.setVisibility(View.VISIBLE);
        } else {
            tvAmount.setVisibility(View.GONE);
        }

        if (item.getDeliveryAddress() != null && !item.getDeliveryAddress().trim().isEmpty()) {
            tvAddress.setText("Deliver to: " + item.getDeliveryAddress());
            tvAddress.setVisibility(View.VISIBLE);
        } else {
            tvAddress.setVisibility(View.GONE);
        }

        String note = item.getRequestNote();
        if (note != null && !note.trim().isEmpty()) {
            tvNote.setText(note);
            layoutNote.setVisibility(View.VISIBLE);
        } else {
            layoutNote.setVisibility(View.GONE);
        }

        // Worth flagging: a second screenshot means the first one was rejected,
        // and the reason for that rejection is the context for this decision.
        if (item.isRetriedProof()) {
            String reason = item.getPaymentRejectionReason();
            tvRetryWarning.setText("Attempt " + item.getPaymentProofAttempts()
                    + (reason != null && !reason.trim().isEmpty()
                        ? " — you rejected the last one: \"" + reason + "\""
                        : " — an earlier proof was rejected."));
            tvRetryWarning.setVisibility(View.VISIBLE);
        } else {
            tvRetryWarning.setVisibility(View.GONE);
        }

        // The two action flags are mutually exclusive server-side.
        layoutDecisionActions.setVisibility(item.needsDecision() ? View.VISIBLE : View.GONE);
        btnReviewProof.setVisibility(item.needsPaymentCheck() ? View.VISIBLE : View.GONE);

        btnAccept.setOnClickListener(v -> confirmDecision(item, true));
        btnReject.setOnClickListener(v -> confirmDecision(item, false));
        btnReviewProof.setOnClickListener(v ->
                startActivity(VerifyPaymentProofActivity.intentFor(this, item.getId())));

        // No thread yet is a real state (they've never talked), so say so rather
        // than opening an empty screen that looks broken.
        btnOpenChat.setOnClickListener(v -> {
            if (item.getConversationId() == null) {
                Toast.makeText(this, "No chat with this customer yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent chat = new Intent(this, ChatActivity.class);
            chat.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, item.getConversationId().intValue());
            chat.putExtra(ChatActivity.EXTRA_CONTACT_ID, item.getCustomerId());
            chat.putExtra(ChatActivity.EXTRA_CONTACT_NAME, item.getCustomerName());
            chat.putExtra(ChatActivity.EXTRA_CONTACT_PHONE, item.getCustomerPhone());
            chat.putExtra(ChatActivity.EXTRA_CONTACT_AVATAR, item.getCustomerImage());
            chat.putExtra(ChatActivity.EXTRA_CONTACT_ROLE, "customer");
            startActivity(chat);
        });

        btnOpenSchedule.setOnClickListener(v -> startActivity(
                SubscriptionCalendarActivity.intentFor(this, item.getId(), item.getPlanName())));

        return card;
    }

    /** Chip colour by stage. The text is the server's `stage`, humanised. */
    private void applyStageChip(SubscriptionRequestsResponse.Item item, TextView chip) {
        String stage = item.getStage() != null ? item.getStage() : "";
        int bg;
        int text;
        String label;
        switch (stage) {
            case "waiting_accept":
                bg = R.drawable.status_chip_new;
                text = getColor(R.color.status_pending_text);
                label = "New";
                break;
            case "awaiting_payment":
                bg = R.drawable.status_chip_pending;
                text = getColor(R.color.status_pending_text);
                label = "Awaiting payment";
                break;
            case "verifying":
                bg = R.drawable.status_chip_preparing;
                text = getColor(R.color.status_preparing_text);
                label = "Check proof";
                break;
            case "scheduled":
                bg = R.drawable.status_chip_preparing;
                text = getColor(R.color.status_preparing_text);
                label = "Starts soon";
                break;
            case "active":
                bg = R.drawable.status_chip_delivered;
                text = getColor(R.color.status_delivered_text);
                label = "Active";
                break;
            case "rejected":
            case "cancelled":
                bg = R.drawable.status_chip_sold_out;
                text = getColor(R.color.sub_error);
                label = "rejected".equals(stage) ? "Declined" : "Cancelled";
                break;
            default:
                bg = R.drawable.status_chip_pending;
                text = getColor(R.color.status_pending_text);
                label = stage.isEmpty() ? "—" : stage.replace('_', ' ');
                break;
        }
        chip.setBackgroundResource(bg);
        chip.setTextColor(text);
        chip.setText(label);
    }

    /**
     * Accept or decline, with an optional note.
     *
     * The dialog spells out what acceptance commits the cook to — every calendar
     * day in the window — because that is the part a cook can't take back later
     * without cancelling on the customer.
     */
    private void confirmDecision(SubscriptionRequestsResponse.Item item, boolean accept) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(accept ? "Optional note to the customer" : "Why can't you take this on?");
        input.setMinLines(2);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(pad, pad / 2, pad, 0);
        wrapper.addView(input);

        String who = item.getCustomerName() != null ? item.getCustomerName() : "this customer";
        String message = accept
                ? "You'll be cooking for " + who + " every day from "
                    + DeliveryDateUtils.formatShortDate(item.getStartDate())
                    + " to " + DeliveryDateUtils.formatLongDate(item.getEndDate())
                    + " (" + item.getDurationDays() + " days).\n\n"
                    + "They'll be asked to pay and upload a screenshot next — nothing starts until you've checked it."
                : "The request is closed and " + who + " is told. Nothing is left pending, "
                    + "and they can request again with a different start date.";

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(accept ? "Accept this subscription?" : "Decline this request?")
                .setMessage(message)
                .setView(wrapper)
                .setPositiveButton(accept ? "Accept" : "Decline", (d, w) ->
                        sendDecision(item, accept, input.getText().toString().trim()))
                .setNegativeButton("Back", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Set white background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }
    }

    private void sendDecision(SubscriptionRequestsResponse.Item item, boolean accept, String note) {
        if (actionInFlight) return;
        actionInFlight = true;

        JsonObject body = new JsonObject();
        body.addProperty("action", accept ? "accept" : "reject");
        if (!note.isEmpty()) body.addProperty("note", note);

        apiService.respondToSubscriptionRequest("Bearer " + sessionManager.getToken(), item.getId(), body)
                .enqueue(new Callback<SubscriptionActionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionActionResponse> call,
                                           @NonNull Response<SubscriptionActionResponse> response) {
                        actionInFlight = false;
                        SubscriptionActionResponse b = response.body();

                        // A 409 here means somebody already answered it — usually
                        // the same cook from the chat thread. The server's own
                        // wording says that; a generic failure would not.
                        Toast.makeText(SubscriptionRequestsActivity.this,
                                b != null && b.getMessage() != null
                                        ? b.getMessage()
                                        : "Couldn't record that. Please try again.",
                                Toast.LENGTH_LONG).show();
                        loadRequests();
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionActionResponse> call, @NonNull Throwable t) {
                        actionInFlight = false;
                        Toast.makeText(SubscriptionRequestsActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    /** Whole rupees unless there really are paisa — "Rs. 2100" beats "Rs. 2100.0". */
    static String fmt(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format(java.util.Locale.US, "%.2f", value);
    }
}
