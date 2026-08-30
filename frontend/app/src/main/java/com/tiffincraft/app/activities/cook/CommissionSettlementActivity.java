package com.tiffincraft.app.activities.cook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCommissionSettlementBinding;
import com.tiffincraft.app.models.AdminQrResponse;
import com.tiffincraft.app.models.CommissionSettlement;
import com.tiffincraft.app.models.CommissionSettlementCurrentResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.ImageUploadHelper;
import com.tiffincraft.app.utils.ImageUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cook-side screen for the "Commission Settlement" feature — how the admin
 * actually collects commission money from cooks without an eSewa merchant
 * account. Mirrors the customer-pays-cook Option A flow: the platform's own
 * QR is shown here, the cook pays it externally, uploads a screenshot, and
 * an admin verifies it in the Admin panel. See commissionController.js.
 */
public class CommissionSettlementActivity extends AppCompatActivity {

    private static final String ESEWA_PACKAGE = "com.f1soft.esewa";
    private static final int REQUEST_STORAGE_PERMISSION = 2101;

    private ActivityCommissionSettlementBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private CommissionSettlement current;
    /** This month's own bill, if one exists — `current` may be an older past-due one. */
    private CommissionSettlement currentMonthBill;
    private CommissionSettlementCurrentResponse.Accruing accruing;
    private String adminEsewaQrUrl;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommissionSettlementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) confirmThenUpload(imageUri);
                    }
                });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPayWithEsewa.setOnClickListener(v -> openEsewaApp());
        // Tap zooms, a separate button saves. Tap-to-save was an invisible
        // gesture on the one thing a cook must be able to read: a 220dp QR is
        // genuinely hard to scan off another phone's screen.
        binding.ivAdminQr.setOnClickListener(v -> openZoom(adminEsewaQrUrl));
        binding.btnSaveQr.setOnClickListener(v -> saveQrToGallery());
        binding.ivPaymentScreenshot.setOnClickListener(v ->
                openZoom(current == null ? null : current.getPaymentScreenshotUrl()));
        binding.btnRetryLoad.setOnClickListener(v -> loadCurrentSettlement());
        binding.btnUploadProof.setOnClickListener(v -> openImagePicker());
        binding.btnPayAccruingNow.setOnClickListener(v -> confirmSettleNow());
        binding.tvViewHistory.setOnClickListener(v ->
                startActivity(new Intent(this, CommissionHistoryActivity.class)));

        loadAdminQr();
        loadCurrentSettlement();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentSettlement();
    }

    private void loadAdminQr() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getAdminQr(token).enqueue(new Callback<AdminQrResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdminQrResponse> call, @NonNull Response<AdminQrResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()
                        || response.body().getBankDetails() == null) {
                    showQrUnavailable("Couldn't load the platform's payment QR.");
                    return;
                }

                // The admin may have uploaded ANY of the three QR types — the old
                // code read esewa_qr_url only, so an admin who uploaded just a
                // bank QR left every cook staring at a blank box with no error.
                // Fall back through them in the order a cook can most easily pay.
                com.tiffincraft.app.models.BankDetails bd = response.body().getBankDetails();
                String url = firstNonEmpty(bd.getEsewaQrUrl(), bd.getKhaltiQrUrl(), bd.getBankQrUrl());
                String label = url == null ? null
                        : url.equals(bd.getEsewaQrUrl()) ? "eSewa"
                        : url.equals(bd.getKhaltiQrUrl()) ? "Khalti" : "bank";

                if (url == null) {
                    showQrUnavailable("The admin hasn't uploaded a payment QR yet — contact them to get one added.");
                    return;
                }

                adminEsewaQrUrl = url;
                binding.ivAdminQr.setVisibility(View.VISIBLE);
                binding.tvScanToPay.setText("Or scan the platform's " + label + " QR (tap to save)");

                // No .error(placeholder) — a broken asset used to render as an
                // indistinguishable grey icon, so a dead URL looked identical to
                // a QR that simply hadn't decoded yet. Say so instead.
                Glide.with(CommissionSettlementActivity.this)
                        .load(url)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                        Object model,
                                                        @NonNull com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                        boolean isFirstResource) {
                                showQrUnavailable("The platform's QR image failed to load. Use \"Pay with eSewa\" or ask the admin to re-upload it.");
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                           @NonNull Object model,
                                                           com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                           @NonNull com.bumptech.glide.load.DataSource dataSource,
                                                           boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(binding.ivAdminQr);
            }

            @Override
            public void onFailure(@NonNull Call<AdminQrResponse> call, @NonNull Throwable t) {
                // Non-fatal — the "Pay with eSewa" button still works without the QR.
                showQrUnavailable("Couldn't reach the server to load the payment QR.");
            }
        });
    }

    /** Returns the first non-null, non-blank value, or null when there is none. */
    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }

    /**
     * A cook who can't see the QR can't pay, so this must never fail silently:
     * hide the dead image box and put the actual reason where the caption was.
     */
    private void showQrUnavailable(String reason) {
        adminEsewaQrUrl = null;
        if (binding == null) return;
        binding.ivAdminQr.setVisibility(View.GONE);
        binding.tvScanToPay.setText(reason);
    }

    /**
     * Initial-load skeleton. Only used for the first paint and for an explicit
     * retry — onResume refreshes silently, because flashing a skeleton over a
     * screen that already shows the right amount every time the cook comes back
     * from the eSewa app reads as a glitch.
     */
    private void setLoading(boolean loading) {
        boolean firstPaint = current == null && accruing == null;
        binding.shimmerLoading.setVisibility(loading && firstPaint ? View.VISIBLE : View.GONE);
        binding.scrollContent.setVisibility(loading && firstPaint ? View.GONE : View.VISIBLE);
        if (loading && firstPaint) binding.shimmerLoading.startShimmer();
        else binding.shimmerLoading.stopShimmer();
    }

    /** Inline retry instead of a Toast: a failed load otherwise looks like "you owe nothing". */
    private void showLoadError(String message) {
        binding.layoutLoadError.setVisibility(View.VISIBLE);
        binding.tvLoadErrorText.setText(message);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void loadCurrentSettlement() {
        setLoading(true);
        binding.layoutLoadError.setVisibility(View.GONE);
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCurrentCommissionSettlement(token).enqueue(new Callback<CommissionSettlementCurrentResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommissionSettlementCurrentResponse> call,
                                    @NonNull Response<CommissionSettlementCurrentResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    showLoadError("Couldn't load your commission status. Your dues are safe — this is only a display problem.");
                    return;
                }
                // Prefer an unresolved past-due settlement over the current (possibly
                // not-yet-generated) month, so a cook who missed last month's due sees it first.
                CommissionSettlement toShow = response.body().getPastDue() != null
                        ? response.body().getPastDue() : response.body().getCurrent();
                current = toShow;
                currentMonthBill = response.body().getCurrent();
                accruing = response.body().getAccruing();
                render();
            }

            @Override
            public void onFailure(@NonNull Call<CommissionSettlementCurrentResponse> call, @NonNull Throwable t) {
                setLoading(false);
                showLoadError("No connection. Check your internet and try again.");
            }
        });
    }

    // Shared with the CommissionBanner on home/earnings so a due date can never
    // render one way here and another way there. See CommissionFormat.
    private static final String[] MONTH_NAMES = com.tiffincraft.app.utils.CommissionFormat.MONTH_NAMES;

    private String todayNptIso() {
        return com.tiffincraft.app.utils.CommissionFormat.todayNptIso();
    }

    private String formatDueDate(String iso) {
        return com.tiffincraft.app.utils.CommissionFormat.formatDueDate(iso);
    }

    /**
     * Live accrual for the month in progress. Shown whether or not a bill
     * exists, because the two answer different questions ("what do I owe?"
     * vs "what am I building up?"). Hidden at zero so a cook with no
     * deliveries this month isn't shown a meaningless ₹0.
     */
    private void renderAccruing() {
        boolean show = accruing != null && accruing.getAmount() > 0;
        binding.layoutAccruing.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;

        binding.tvAccruingAmount.setText(CurrencyUtils.formatRupees(accruing.getAmount()));
        int n = accruing.getOrderCount();
        String period = accruing.getMonth() >= 1 && accruing.getMonth() <= 12
                ? MONTH_NAMES[accruing.getMonth() - 1] : "this month";
        binding.tvAccruingLabel.setText("Remaining in " + period + " · " + n + " delivered order" + (n == 1 ? "" : "s"));

        // The accrual is only payable while no OUTSTANDING bill exists for it.
        // While one does, the amount-due card below owns the payment flow and a
        // second "pay" CTA here would be two buttons for one debt. A bill that is
        // already verified is not one of those: orders delivered after it was paid
        // are new debt with no CTA of their own until the month closes, which is
        // exactly the state this button exists for.
        boolean payable = accruing.isPayableNow() && !hasOutstandingBillFor(accruing.getMonth(), accruing.getYear());
        binding.btnPayAccruingNow.setVisibility(payable ? View.VISIBLE : View.GONE);
        binding.btnPayAccruingNow.setText(settleButtonLabel());
        binding.tvAccruingNote.setText(payable
                ? "Pay it now, or leave it — it is billed automatically once this month closes."
                : "Billed automatically once this month closes.");
    }

    /** True when a bill for that period exists and still owes money. */
    private boolean hasOutstandingBillFor(int month, int year) {
        for (CommissionSettlement s : new CommissionSettlement[]{ currentMonthBill, current }) {
            if (s != null && s.getMonth() == month && s.getYear() == year && !s.isVerified()) return true;
        }
        return false;
    }

    /** Amount in the label: "Pay Now" alone hides which figure is being paid. */
    private String settleButtonLabel() {
        return accruing == null ? "Pay Now"
                : "Pay " + CurrencyUtils.formatRupees(accruing.getAmount()) + " Now";
    }

    /**
     * Early payment is not reversible from the cook's side — the bill exists from
     * then on and later deliveries roll into the next cycle — so the amount and
     * that consequence are both stated before anything is created.
     */
    private void confirmSettleNow() {
        if (accruing == null || accruing.getAmount() <= 0) return;
        String period = accruing.getMonth() >= 1 && accruing.getMonth() <= 12
                ? MONTH_NAMES[accruing.getMonth() - 1] + " " + accruing.getYear() : "this month";

        // A verified bill already exists for this period when the cook paid early and
        // then delivered more — the unique (cook, month, year) key means this money
        // is added to that bill, not billed as a second one. Say which it is.
        boolean topUp = currentMonthBill != null && currentMonthBill.isVerified()
                && currentMonthBill.getMonth() == accruing.getMonth()
                && currentMonthBill.getYear() == accruing.getYear();
        String amount = CurrencyUtils.formatRupees(accruing.getAmount());

        new AlertDialog.Builder(this)
                .setTitle("Pay " + amount + " now?")
                .setMessage((topUp
                        ? "This adds " + amount + " to your " + period + " commission bill, so you can pay it "
                        : "This creates your " + period + " commission bill for " + amount + " right away, so you can pay it ")
                        + "and upload your payment screenshot today.\n\nOrders you deliver after this are "
                        + "billed in the next cycle.")
                .setPositiveButton(topUp ? "Add to bill" : "Create bill", (d, w) -> settleNow())
                .setNegativeButton("Not now", null)
                .show();
    }

    private void settleNow() {
        binding.btnPayAccruingNow.setEnabled(false);
        binding.btnPayAccruingNow.setText("Creating bill…");

        apiService.settleCommissionNow("Bearer " + sessionManager.getToken())
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call,
                                           @NonNull Response<RegisterResponse> response) {
                        resetSettleButton();
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(CommissionSettlementActivity.this,
                                    "Bill created — pay the QR below and upload your screenshot.",
                                    Toast.LENGTH_LONG).show();
                            loadCurrentSettlement();
                            return;
                        }
                        // 409 means a bill for this month already exists (submitted or
                        // settled). Reloading shows the cook that real state instead of
                        // leaving them looking at a stale accruing card. Retrofit puts a
                        // non-2xx body in errorBody(), not body(), so read it from there.
                        String message = serverMessage(response);
                        Toast.makeText(CommissionSettlementActivity.this, message, Toast.LENGTH_LONG).show();
                        loadCurrentSettlement();
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                        resetSettleButton();
                        Toast.makeText(CommissionSettlementActivity.this,
                                "No connection — couldn't create the bill.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Server's own message for a non-2xx response, or a readable fallback. */
    private String serverMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                org.json.JSONObject json = new org.json.JSONObject(raw);
                if (json.has("message")) return json.getString("message");
            }
        } catch (Exception ignored) {}
        return "Couldn't create the bill. Please try again.";
    }

    private void resetSettleButton() {
        binding.btnPayAccruingNow.setEnabled(true);
        binding.btnPayAccruingNow.setText(settleButtonLabel());
    }

    /**
     * The 3-step strip: Pay -> Upload proof -> Verified.
     *
     * `step` is how many steps are COMPLETE, so 0 = nothing paid yet (the ball is
     * in the cook's court), 2 = proof submitted and we are waiting on an admin,
     * 3 = done. A rejected settlement deliberately falls back to 0, not 2: the
     * proof was not accepted, so the cook is back at the start of the flow, and
     * showing 2/3 there would tell them to keep waiting for nothing.
     */
    private void renderProgress(int step) {
        android.widget.ImageView[] dots = { binding.dotStep1, binding.dotStep2, binding.dotStep3 };
        android.widget.TextView[] labels = { binding.tvStep1, binding.tvStep2, binding.tvStep3 };
        View[] lines = { binding.lineStep1, binding.lineStep2 };

        for (int i = 0; i < dots.length; i++) {
            boolean done = i < step;
            dots[i].setBackgroundResource(done
                    ? R.drawable.circle_step_hero_done : R.drawable.circle_step_hero_pending);
            // The tick is the same drawable in both states; hiding it by alpha
            // rather than visibility keeps every dot exactly 24dp, so the strip
            // does not reflow as the settlement progresses.
            dots[i].setAlpha(done ? 1f : 0.55f);
            dots[i].setImageAlpha(done ? 255 : 0);
            labels[i].setAlpha(done || i == step ? 1f : 0.6f);
        }
        for (int i = 0; i < lines.length; i++) {
            lines[i].setAlpha(i < step ? 1f : 0.4f);
        }
    }

    private void render() {
        renderAccruing();
        binding.layoutLoadError.setVisibility(View.GONE);

        if (current == null) {
            setSectionVisible(false, false, false);
            binding.layoutOverdueBanner.setVisibility(View.GONE);
            binding.tvDueDateLabel.setVisibility(View.GONE);
            // No bill exists, so the amount-due card would just show a stale
            // "₹0 / This Month" above the accruing card and contradict it.
            binding.layoutAmountDueCard.setVisibility(View.GONE);
            // Only truly "nothing going on" if there's also no live accrual —
            // otherwise the accruing card is the content and a competing
            // "nothing due" illustration just contradicts it.
            binding.layoutEmptyState.setVisibility(
                    binding.layoutAccruing.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            binding.layoutVerifiedCard.setVisibility(View.GONE);
            return;
        }
        binding.layoutAmountDueCard.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);

        binding.tvPeriodLabel.setText(MONTH_NAMES[current.getMonth() - 1] + " " + current.getYear());

        // EC3: once a part payment has been recorded, the headline figure must be
        // what's still owed — showing the original amount_due would tell a cook who
        // has already paid ₹100 of ₹145 to pay ₹145 again. amount_due itself is
        // never rewritten, so it's still shown, as context on the line below.
        String orderLine = current.getOrderCount() + " order" + (current.getOrderCount() == 1 ? "" : "s") + " this period";
        if (current.isPartiallyPaid()) {
            binding.tvAmountDue.setText(CurrencyUtils.formatRupees(current.getAmountRemaining()));
            orderLine += " · " + CurrencyUtils.formatRupees(current.getAmountPaid())
                    + " of " + CurrencyUtils.formatRupees(current.getAmountDue()) + " already received";
        } else {
            binding.tvAmountDue.setText(CurrencyUtils.formatRupees(current.getAmountDue()));
        }
        binding.tvOrderCountLabel.setText(orderLine);

        // Due date + overdue warning. Policy: warn only — nothing is blocked.
        String today = todayNptIso();
        String pretty = formatDueDate(current.getDueDate());
        boolean overdue = current.isOverdue(today);

        if (pretty != null && !current.isVerified()) {
            binding.tvDueDateLabel.setText(overdue ? "Was due " + pretty : "Due by " + pretty);
            binding.tvDueDateLabel.setVisibility(View.VISIBLE);
        } else {
            binding.tvDueDateLabel.setVisibility(View.GONE);
        }

        if (overdue) {
            binding.tvOverdueBanner.setText(pretty != null
                    ? "This commission was due on " + pretty + ". Please pay it as soon as possible to keep your kitchen in good standing."
                    : "This commission is past its due date. Please pay it as soon as possible to keep your kitchen in good standing.");
            binding.layoutOverdueBanner.setVisibility(View.VISIBLE);
        } else {
            binding.layoutOverdueBanner.setVisibility(View.GONE);
        }

        switch (current.getStatus()) {
            case "pending":
                binding.tvStatusChip.setText(current.isPartiallyPaid() ? "Part paid" : "Pending");
                setSectionVisible(true, false, false);
                // A recorded part payment IS a payment, so step 1 is genuinely
                // done even though proof for the remainder has not been sent.
                renderProgress(current.isPartiallyPaid() ? 1 : 0);
                binding.layoutVerifiedCard.setVisibility(View.GONE);
                break;
            case "submitted":
                binding.tvStatusChip.setText("Submitted");
                setSectionVisible(false, true, false);
                binding.tvScreenshotStatusLabel.setText("Awaiting admin verification");
                renderProgress(2);
                binding.layoutVerifiedCard.setVisibility(View.GONE);
                loadScreenshotPreview();
                break;
            case "verified":
                binding.tvStatusChip.setText("Verified");
                setSectionVisible(false, true, false);
                binding.tvScreenshotStatusLabel.setText("Verified — thank you!");
                renderProgress(3);
                binding.layoutVerifiedCard.setVisibility(View.VISIBLE);
                binding.tvVerifiedDetail.setText(
                        CurrencyUtils.formatRupees(current.getAmountPaid() > 0
                                ? current.getAmountPaid() : current.getAmountDue())
                        + " received for " + MONTH_NAMES[current.getMonth() - 1] + " " + current.getYear()
                        + ". Nothing further is owed for this period.");
                loadScreenshotPreview();
                break;
            case "rejected":
                binding.tvStatusChip.setText("Rejected");
                setSectionVisible(true, false, true);
                renderProgress(0);
                binding.layoutVerifiedCard.setVisibility(View.GONE);
                binding.tvAdminNotes.setText(current.getAdminNotes() != null && !current.getAdminNotes().isEmpty()
                        ? current.getAdminNotes() : "Please re-upload a clearer payment screenshot.");
                break;
            default:
                setSectionVisible(false, false, false);
                renderProgress(0);
                binding.layoutVerifiedCard.setVisibility(View.GONE);
        }
    }

    private void loadScreenshotPreview() {
        String url = current.getPaymentScreenshotUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(this).load(url)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(binding.ivPaymentScreenshot);
        }
    }

    private void setSectionVisible(boolean paySection, boolean screenshotPreview, boolean adminNotes) {
        binding.layoutPaySection.setVisibility(paySection ? View.VISIBLE : View.GONE);
        binding.layoutScreenshotPreview.setVisibility(screenshotPreview ? View.VISIBLE : View.GONE);
        binding.layoutAdminNotes.setVisibility(adminNotes ? View.VISIBLE : View.GONE);
        // Rejected settlements let the cook re-upload — show the pay/upload
        // section alongside the rejection notice.
        if (adminNotes) binding.layoutPaySection.setVisibility(View.VISIBLE);
    }

    /** Opens the real eSewa app so the cook can pay from there. */
    private void openEsewaApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(ESEWA_PACKAGE);
        if (launch != null) {
            startActivity(launch);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("eSewa App Required")
                .setMessage("Paying with eSewa requires the eSewa app to be installed on this device.")
                .setPositiveButton("Install eSewa", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ESEWA_PACKAGE)));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + ESEWA_PACKAGE)));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Downloads the platform's QR (already on-screen via Glide) and saves it into the gallery. */
    private void saveQrToGallery() {
        if (adminEsewaQrUrl == null || adminEsewaQrUrl.isEmpty()) return;

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            Toast.makeText(this, "Storage permission needed — tap the QR again after allowing.", Toast.LENGTH_LONG).show();
            return;
        }

        Glide.with(this).asBitmap().load(adminEsewaQrUrl).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                boolean saved = ImageUtils.saveBitmapToGallery(
                        CommissionSettlementActivity.this, bitmap, "esewa_qr_commission_" + System.currentTimeMillis());
                Toast.makeText(CommissionSettlementActivity.this,
                        saved ? "QR saved to gallery" : "Could not save QR", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

            @Override
            public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                Toast.makeText(CommissionSettlementActivity.this, "Could not load QR", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Full-screen zoomable view of a QR or a payment proof, via the existing
     * MediaViewerActivity rather than a new dialog — it already handles zoom and
     * is what the rest of the app uses for exactly this.
     */
    private void openZoom(String url) {
        if (url == null || url.isEmpty()) return;
        Intent i = new Intent(this, com.tiffincraft.app.activities.common.MediaViewerActivity.class);
        i.putExtra(com.tiffincraft.app.activities.common.MediaViewerActivity.EXTRA_MEDIA_URL, url);
        startActivity(i);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * One tap of friction between picking an image and submitting it. Undoing a
     * wrong submission means asking an admin to reject it, so showing the actual
     * image and the actual amount first is much cheaper than that round trip.
     */
    private void confirmThenUpload(Uri imageUri) {
        if (current == null) return;

        double claiming = current.isPartiallyPaid() ? current.getAmountRemaining() : current.getAmountDue();
        View sheet = getLayoutInflater().inflate(R.layout.sheet_confirm_commission_proof, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dialog.setContentView(sheet);

        ((android.widget.TextView) sheet.findViewById(R.id.tvSheetAmount)).setText(
                "Claiming payment of " + CurrencyUtils.formatRupees(claiming)
                        + " for " + MONTH_NAMES[current.getMonth() - 1] + " " + current.getYear());
        Glide.with(this).load(imageUri).into((android.widget.ImageView) sheet.findViewById(R.id.ivSheetPreview));

        sheet.findViewById(R.id.btnSheetSubmit).setOnClickListener(v -> {
            dialog.dismiss();
            uploadScreenshot(imageUri);
        });
        sheet.findViewById(R.id.btnSheetChooseAnother).setOnClickListener(v -> {
            dialog.dismiss();
            openImagePicker();
        });
        dialog.show();
    }

    /** Progress for the upload itself — the button IS the indicator, so there is
     *  no full-screen spinner covering the amount the cook just confirmed. */
    private void setSubmitting(boolean submitting) {
        binding.btnUploadProof.setEnabled(!submitting);
        binding.btnUploadProof.setText(submitting ? "Submitting…" : "Upload Payment Proof");
    }

    private void uploadScreenshot(Uri imageUri) {
        if (current == null) return;

        if (!ImageUploadHelper.isImageFile(this, imageUri)) {
            Toast.makeText(this, "Please select a valid image file", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ImageUploadHelper.isValidFileSize(this, imageUri, 5)) {
            Toast.makeText(this, "Image size must be less than 5MB", Toast.LENGTH_SHORT).show();
            return;
        }

        setSubmitting(true);

        // Compress only when the file is actually big. compressImage() caps the
        // long edge at 800px, which is fine for a 4MB camera photo but would
        // needlessly soften a 300KB phone screenshot — and the one thing an admin
        // must be able to read on this image is the amount and the timestamp.
        // ponytail: single 2MB threshold, no quality ladder. Revisit only if
        // admins start reporting unreadable proofs.
        boolean needsCompression = !ImageUploadHelper.isValidFileSize(this, imageUri, 2);
        // compressImage() blocks on Glide's synchronous FutureTarget, so it must
        // not run on the main thread.
        new Thread(() -> {
            okhttp3.MultipartBody.Part part = null;
            if (needsCompression) {
                java.io.File compressed = ImageUtils.compressImage(this, imageUri);
                if (compressed != null) part = ImageUtils.prepareFilePart("document", compressed);
            }
            if (part == null) part = ImageUploadHelper.createDocumentPart(this, imageUri);
            final okhttp3.MultipartBody.Part imagePart = part;
            runOnUiThread(() -> {
                if (imagePart == null) {
                    setSubmitting(false);
                    Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendScreenshot(imagePart);
            });
        }).start();
    }

    private void sendScreenshot(okhttp3.MultipartBody.Part imagePart) {
        String token = "Bearer " + sessionManager.getToken();

        // Two-step, same as the customer-pays-cook Option A flow: upload the raw
        // image to get a Cloudinary URL, then attach that URL to the settlement.
        apiService.uploadDocumentCloudinary(token, imagePart).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    setSubmitting(false);
                    Toast.makeText(CommissionSettlementActivity.this, "Failed to upload screenshot", Toast.LENGTH_SHORT).show();
                    return;
                }
                String url = response.body().getData().getUrl();
                submitScreenshotUrl(url);
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                setSubmitting(false);
                Toast.makeText(CommissionSettlementActivity.this, "Network error uploading screenshot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitScreenshotUrl(String url) {
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("payment_screenshot_url", url);

        apiService.uploadCommissionScreenshot(token, current.getId(), body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                setSubmitting(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CommissionSettlementActivity.this, "Payment proof submitted — an admin will verify it shortly.", Toast.LENGTH_LONG).show();
                    loadCurrentSettlement();
                    return;
                }
                // 409 is the server's duplicate-screenshot guard. It needs its own
                // message: "Failed to submit" would leave a cook re-picking the
                // same old screenshot forever, which is exactly what was rejected.
                if (response.code() == 409) {
                    new AlertDialog.Builder(CommissionSettlementActivity.this)
                            .setTitle("Screenshot already used")
                            .setMessage("This screenshot has already been submitted for another commission payment. Please upload the screenshot of this month's payment.")
                            .setPositiveButton("Choose another", (d, w) -> openImagePicker())
                            .setNegativeButton("Later", null)
                            .show();
                    return;
                }
                Toast.makeText(CommissionSettlementActivity.this, "Failed to submit payment proof", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                setSubmitting(false);
                Toast.makeText(CommissionSettlementActivity.this, "Network error submitting payment proof", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
