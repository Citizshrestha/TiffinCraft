package com.tiffincraft.app.views;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.cook.CommissionSettlementActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CommissionSettlement;
import com.tiffincraft.app.models.CommissionSettlementCurrentResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CommissionFormat;
import com.tiffincraft.app.utils.CurrencyUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Binds {@code view_commission_banner.xml} to GET /commission/settlements/current.
 *
 * The point of this class is that the cook's commission obligation is visible
 * without being hunted for. Before it, the only in-app route to
 * {@link CommissionSettlementActivity} was one flat white row buried mid-scroll
 * in Earnings that showed no amount, no due date and no overdue state — so a cook
 * who owed money had nothing telling them so, and the settlement screen was
 * effectively reachable only from a push notification.
 *
 * Attach it wherever a cook is likely to be looking:
 * <pre>
 *   commissionBanner = CommissionBanner.attach(this, R.id.commissionBanner, true);
 *   // ... then in onResume() — required, attach() does not fetch:
 *   commissionBanner.refresh();
 * </pre>
 *
 * @see #attach(Activity, int, boolean) for the hideWhenNothingDue trade-off
 */
public final class CommissionBanner {

    private final Activity activity;
    private final MaterialCardView card;
    private final View iconFrame;
    private final ImageView icon;
    private final TextView title;
    private final TextView chip;
    private final TextView subtitle;
    private final TextView amount;
    private final MaterialButton action;
    private final boolean hideWhenNothingDue;

    private final ApiService apiService;
    private final SessionManager sessionManager;

    private CommissionBanner(Activity activity, MaterialCardView card, boolean hideWhenNothingDue) {
        this.activity = activity;
        this.card = card;
        this.hideWhenNothingDue = hideWhenNothingDue;
        this.iconFrame = card.findViewById(R.id.frameCommissionIcon);
        this.icon = card.findViewById(R.id.ivCommissionIcon);
        this.title = card.findViewById(R.id.tvCommissionTitle);
        this.chip = card.findViewById(R.id.tvCommissionChip);
        this.subtitle = card.findViewById(R.id.tvCommissionSubtitle);
        this.amount = card.findViewById(R.id.tvCommissionAmount);
        this.action = card.findViewById(R.id.btnCommissionAction);
        this.apiService = RetrofitClient.getInstance(activity).getApiService();
        this.sessionManager = new SessionManager(activity);

        View.OnClickListener open = v -> activity.startActivity(
                new Intent(activity, CommissionSettlementActivity.class));
        card.setOnClickListener(open);
        action.setOnClickListener(open);
    }

    /**
     * @param bannerId           id of the {@code <include>} of view_commission_banner
     * @param hideWhenNothingDue true on screens where the banner is a nudge (home):
     *                           a cook with nothing owed and nothing accruing sees
     *                           no clutter. false on Earnings, where the banner is
     *                           the permanent entry point to settlement history and
     *                           must never disappear — otherwise a cook who has paid
     *                           everything loses the only way back in.
     *                           <p>
     *                           This does not fetch. Call {@link #refresh()} from the
     *                           host's {@code onResume} — required, not optional: with
     *                           hideWhenNothingDue the card starts hidden and would
     *                           otherwise never appear.
     */
    public static CommissionBanner attach(@NonNull Activity activity, int bannerId, boolean hideWhenNothingDue) {
        MaterialCardView card = activity.findViewById(bannerId);
        if (card == null) return null;
        CommissionBanner banner = new CommissionBanner(activity, card, hideWhenNothingDue);
        // Start hidden on nudge screens so the card never flashes generic copy
        // before the real state arrives.
        if (hideWhenNothingDue) card.setVisibility(View.GONE);
        return banner;
    }

    /** Re-reads the cook's commission state. Safe to call from onResume. */
    public void refresh() {
        String token = sessionManager.getToken();
        if (token == null) {
            renderNothingDue();
            return;
        }
        apiService.getCurrentCommissionSettlement("Bearer " + token)
                .enqueue(new Callback<CommissionSettlementCurrentResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CommissionSettlementCurrentResponse> call,
                                           @NonNull Response<CommissionSettlementCurrentResponse> response) {
                        if (isGone()) return;
                        CommissionSettlementCurrentResponse body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccess()) {
                            // Nothing trustworthy to show. Don't invent a state — fall
                            // back to the neutral entry point (or hide, on home).
                            renderNothingDue();
                            return;
                        }
                        // Prefer an unresolved past-due settlement over the current
                        // month, matching CommissionSettlementActivity — a cook who
                        // missed last month's due date must see that first.
                        CommissionSettlement bill = body.getPastDue() != null
                                ? body.getPastDue() : body.getCurrent();
                        render(bill, body.getAccruing());
                    }

                    @Override
                    public void onFailure(@NonNull Call<CommissionSettlementCurrentResponse> call,
                                          @NonNull Throwable t) {
                        if (isGone()) return;
                        renderNothingDue();
                    }
                });
    }

    private boolean isGone() {
        return activity.isFinishing() || activity.isDestroyed();
    }

    private void render(CommissionSettlement bill, CommissionSettlementCurrentResponse.Accruing accruing) {
        // A verified row is settled — it is not something to chase, so it falls
        // through to the accruing/idle state below.
        if (bill != null && !bill.isVerified()) {
            renderBill(bill);
            return;
        }
        if (accruing != null && accruing.getAmount() > 0) {
            renderAccruing(accruing);
            return;
        }
        renderNothingDue();
    }

    private void renderBill(CommissionSettlement bill) {
        card.setVisibility(View.VISIBLE);
        String period = CommissionFormat.monthName(bill.getMonth(), "last month") + " " + bill.getYear();
        String due = CommissionFormat.formatDueDate(bill.getDueDate());
        boolean overdue = bill.isOverdue(CommissionFormat.todayNptIso());

        if (bill.isSubmitted()) {
            applyPalette(R.color.commission_review_bg, R.color.commission_review_stroke,
                    R.color.blue_light, R.color.commission_review_accent, R.color.commission_review_accent);
            title.setText("Payment under review");
            setChip("IN REVIEW");
            subtitle.setText("We're verifying your " + period + " payment of "
                    + CurrencyUtils.formatRupees(bill.getAmountDue()) + ".");
            setAmount(null);
            action.setText("Track Status");
            return;
        }

        if (bill.isRejected()) {
            applyPalette(R.color.commission_overdue_bg, R.color.commission_overdue_stroke,
                    R.color.red_light, R.color.error_red, R.color.error_red);
            title.setText("Payment not accepted");
            setChip("ACTION NEEDED");
            subtitle.setText("Your " + period + " proof was rejected — please pay and upload it again.");
            setAmount(bill.getAmountRemaining());
            action.setText("Re-submit Proof");
            return;
        }

        // Pending: the money is actually owed. This is the only state that gets
        // the red treatment, and only once the 15-day grace period has passed.
        String orders = bill.getOrderCount() + (bill.getOrderCount() == 1 ? " order" : " orders");
        if (overdue) {
            applyPalette(R.color.commission_overdue_bg, R.color.commission_overdue_stroke,
                    R.color.red_light, R.color.error_red, R.color.error_red);
            title.setText("Commission overdue");
            setChip("OVERDUE");
            subtitle.setText(due != null
                    ? "Was due " + due + " · " + orders + " in " + period
                    : orders + " in " + period + " · payment is past due");
        } else {
            applyPalette(R.color.commission_due_bg, R.color.commission_due_stroke,
                    R.color.card_light_orange, R.color.commission_due_accent, R.color.green_primary_dark);
            title.setText("Commission due");
            setChip(null);
            subtitle.setText(due != null
                    ? "Pay by " + due + " · " + orders + " in " + period
                    : orders + " in " + period);
        }

        // EC3: after a part payment the headline must be what's still owed. Showing
        // amount_due would tell a cook who already paid ₹100 of ₹145 to pay ₹145.
        if (bill.isPartiallyPaid()) {
            setChip(overdue ? "OVERDUE" : "PART PAID");
            subtitle.setText(CurrencyUtils.formatRupees(bill.getAmountPaid()) + " of "
                    + CurrencyUtils.formatRupees(bill.getAmountDue()) + " received"
                    + (due != null ? (overdue ? " · was due " + due : " · pay by " + due) : ""));
            action.setText("Pay Balance");
        } else {
            action.setText("Pay Now");
        }
        setAmount(bill.getAmountRemaining());
    }

    /**
     * The month in progress. Nothing is overdue, so this stays calm — green, no
     * urgency chip. The verb is still "Pay Now" when the accrual is settleable
     * early (payable_now), because it genuinely is payable today; it is only
     * "View Details" when the cook must wait for month close.
     */
    private void renderAccruing(CommissionSettlementCurrentResponse.Accruing accruing) {
        card.setVisibility(View.VISIBLE);
        applyPalette(R.color.white, R.color.stroke_soft,
                R.color.green_light, R.color.green_primary_dark, R.color.green_primary_dark);
        title.setText("Commission this month");
        setChip(null);
        int n = accruing.getOrderCount();
        subtitle.setText(n + (n == 1 ? " delivered order" : " delivered orders") + " in "
                + CommissionFormat.monthName(accruing.getMonth(), "this month")
                + (accruing.isPayableNow() ? " · pay now or after the month ends" : " · billed after the month ends"));
        setAmount(accruing.getAmount());
        action.setText(accruing.isPayableNow() ? "Pay Now" : "View Details");
    }

    private void renderNothingDue() {
        if (hideWhenNothingDue) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        applyPalette(R.color.white, R.color.stroke_soft,
                R.color.green_light, R.color.green_primary_dark, R.color.green_primary_dark);
        title.setText("Platform Commission");
        setChip(null);
        subtitle.setText("Nothing due right now · view your past settlements");
        setAmount(null);
        action.setText("View Details");
    }

    private void applyPalette(@ColorRes int cardBg, @ColorRes int stroke, @ColorRes int iconBg,
                              @ColorRes int accent, @ColorRes int button) {
        int accentColor = ContextCompat.getColor(activity, accent);
        card.setCardBackgroundColor(ContextCompat.getColor(activity, cardBg));
        card.setStrokeColor(ContextCompat.getColor(activity, stroke));
        iconFrame.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(activity, iconBg)));
        icon.setImageTintList(ColorStateList.valueOf(accentColor));
        chip.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        action.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(activity, button)));
    }

    private void setChip(String text) {
        if (text == null) {
            chip.setVisibility(View.GONE);
            return;
        }
        chip.setText(text);
        chip.setVisibility(View.VISIBLE);
    }

    private void setAmount(Double value) {
        if (value == null) {
            amount.setVisibility(View.GONE);
            return;
        }
        amount.setText(CurrencyUtils.formatRupees(value));
        amount.setVisibility(View.VISIBLE);
    }
}
