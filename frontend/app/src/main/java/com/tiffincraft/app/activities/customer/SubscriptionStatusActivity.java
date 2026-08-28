package com.tiffincraft.app.activities.customer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.ChatActivity;
import com.tiffincraft.app.activities.common.MediaViewerActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.SubscriptionActionResponse;
import com.tiffincraft.app.models.SubscriptionDetailResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.DeliveryDateUtils;
import com.tiffincraft.app.utils.ImageUploadHelper;

import java.util.Locale;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The customer's view of one subscription, at every stage: waiting for the cook,
 * awaiting payment, waiting on verification, and running.
 *
 * One screen instead of four because the customer's question never changes —
 * "what's happening with this?" — and because `headline`/`detail` come from the
 * server's single stageFor(). Nothing here re-words a status locally, so the
 * customer and the cook can never be told two different stories about the same
 * subscription.
 *
 * The payment block is the only stage-specific machinery and it appears only when
 * the server says canUploadProof(). That is deliberate: paying before the cook
 * has accepted is the exact failure the request-first ordering exists to prevent,
 * and the upload endpoint refuses it anyway.
 */
public class SubscriptionStatusActivity extends AppCompatActivity {

    private static final String EXTRA_SUBSCRIPTION_ID = "subscription_id";
    private static final String ESEWA_PACKAGE = "com.f1soft.esewa";

    public static Intent intentFor(Context context, int subscriptionId) {
        Intent intent = new Intent(context, SubscriptionStatusActivity.class);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId);
        return intent;
    }

    private ApiService apiService;
    private SessionManager sessionManager;
    private int subscriptionId;

    private SwipeRefreshLayout swipeRefresh;
    private View progressLoading, cardPay, layoutSubmittedProof;
    private TextView tvHeaderPlan, tvHeaderCook, tvStageChip, tvHeadline, tvDetail,
            tvStep1, tvStep2, tvStep3, tvStep4, tvRejectionNote, tvTotalAmount,
            tvAmountBreakdown, tvWindow, tvWindowNote, tvAddress, tvMealsTitle, tvMeals,
            tvScanToPay, tvEventsTitle;
    private ImageView imgCookQr, imgProof;
    private LinearLayout layoutEvents;
    private MaterialButton btnPayWithEsewa, btnUploadProof, btnOpenSchedule, btnMessageCook;

    private SubscriptionDetailResponse.Subscription current;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private boolean uploadInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_status);

        subscriptionId = getIntent().getIntExtra(EXTRA_SUBSCRIPTION_ID, 0);
        if (subscriptionId <= 0) {
            Toast.makeText(this, "Missing subscription.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        bindViews();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri picked = result.getData().getData();
                        if (picked != null) uploadProof(picked);
                    }
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadDetail);
        btnPayWithEsewa.setOnClickListener(v -> openEsewa());
        btnUploadProof.setOnClickListener(v -> confirmUpload());
        btnOpenSchedule.setOnClickListener(v -> {
            if (current == null) return;
            startActivity(SubscriptionCalendarActivity.intentFor(this, subscriptionId, current.getPlanName()));
        });
        btnMessageCook.setOnClickListener(v -> openChat());

        loadDetail();
    }

    private void bindViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressLoading = findViewById(R.id.progressLoading);
        cardPay = findViewById(R.id.cardPay);
        layoutSubmittedProof = findViewById(R.id.layoutSubmittedProof);

        tvHeaderPlan = findViewById(R.id.tvHeaderPlan);
        tvHeaderCook = findViewById(R.id.tvHeaderCook);
        tvStageChip = findViewById(R.id.tvStageChip);
        tvHeadline = findViewById(R.id.tvHeadline);
        tvDetail = findViewById(R.id.tvDetail);
        tvStep1 = findViewById(R.id.tvStep1);
        tvStep2 = findViewById(R.id.tvStep2);
        tvStep3 = findViewById(R.id.tvStep3);
        tvStep4 = findViewById(R.id.tvStep4);
        tvRejectionNote = findViewById(R.id.tvRejectionNote);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvAmountBreakdown = findViewById(R.id.tvAmountBreakdown);
        tvWindow = findViewById(R.id.tvWindow);
        tvWindowNote = findViewById(R.id.tvWindowNote);
        tvAddress = findViewById(R.id.tvAddress);
        tvMealsTitle = findViewById(R.id.tvMealsTitle);
        tvMeals = findViewById(R.id.tvMeals);
        tvScanToPay = findViewById(R.id.tvScanToPay);
        tvEventsTitle = findViewById(R.id.tvEventsTitle);
        imgCookQr = findViewById(R.id.imgCookQr);
        imgProof = findViewById(R.id.imgProof);
        layoutEvents = findViewById(R.id.layoutEvents);
        btnPayWithEsewa = findViewById(R.id.btnPayWithEsewa);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        btnOpenSchedule = findViewById(R.id.btnOpenSchedule);
        btnMessageCook = findViewById(R.id.btnMessageCook);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The cook may have accepted or verified while this screen was backgrounded.
        if (current != null) loadDetail();
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
                            Toast.makeText(SubscriptionStatusActivity.this,
                                    "Couldn't load this subscription.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        render(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionDetailResponse> call, @NonNull Throwable t) {
                        progressLoading.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(SubscriptionStatusActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void render(SubscriptionDetailResponse body) {
        SubscriptionDetailResponse.Subscription sub = body.getSubscription();
        current = sub;

        tvHeaderPlan.setText(sub.getPlanName() != null ? sub.getPlanName() : "Subscription");
        tvHeaderCook.setText(sub.getCookName() != null ? "from " + sub.getCookName() : "");

        applyStage(sub);
        applyRejectionNote(sub);
        applyPlanCard(sub, body);
        applyPayBlock(sub);
        applyProof(sub);

        // A schedule only exists once there are days to show.
        btnOpenSchedule.setVisibility(sub.isRunning() || "completed".equals(sub.getStage())
                ? View.VISIBLE : View.GONE);

        renderEvents(body);
    }

    /** Chip + the four-step trail. The labels are the server's stage, humanised. */
    private void applyStage(SubscriptionDetailResponse.Subscription sub) {
        tvHeadline.setText(sub.getHeadline() != null ? sub.getHeadline() : "—");
        boolean hasDetail = sub.getDetail() != null && !sub.getDetail().trim().isEmpty();
        tvDetail.setText(hasDetail ? sub.getDetail() : "");
        tvDetail.setVisibility(hasDetail ? View.VISIBLE : View.GONE);

        String stage = sub.getStage() != null ? sub.getStage() : "";
        int bg = R.drawable.status_chip_pending;
        int color = getColor(R.color.status_pending_text);
        String label;
        int reached; // how many of the four steps are done

        switch (stage) {
            case "waiting_accept":
                bg = R.drawable.status_chip_new;
                label = "Waiting for the cook";
                reached = 1;
                break;
            case "awaiting_payment":
                label = "Pay now";
                reached = 2;
                break;
            case "verifying":
                bg = R.drawable.status_chip_preparing;
                color = getColor(R.color.status_preparing_text);
                label = "Checking your payment";
                reached = 3;
                break;
            case "scheduled":
                bg = R.drawable.status_chip_preparing;
                color = getColor(R.color.status_preparing_text);
                label = "Starts soon";
                reached = 4;
                break;
            case "active":
                bg = R.drawable.status_chip_delivered;
                color = getColor(R.color.status_delivered_text);
                label = "Active";
                reached = 4;
                break;
            case "paused":
                label = "Paused";
                reached = 4;
                break;
            case "completed":
                bg = R.drawable.status_chip_delivered;
                color = getColor(R.color.status_delivered_text);
                label = "Finished";
                reached = 4;
                break;
            case "rejected":
                bg = R.drawable.status_chip_sold_out;
                color = getColor(R.color.sub_error);
                label = "Declined";
                reached = 0;
                break;
            case "cancelled":
                bg = R.drawable.status_chip_sold_out;
                color = getColor(R.color.sub_error);
                label = "Cancelled";
                reached = 0;
                break;
            default:
                label = stage.isEmpty() ? "—" : stage.replace('_', ' ');
                reached = 0;
                break;
        }

        tvStageChip.setBackgroundResource(bg);
        tvStageChip.setTextColor(color);
        tvStageChip.setText(label);

        // A closed subscription has no progress left to show, so the trail would
        // only assert something that stopped being true.
        boolean showTrail = !sub.isClosed();
        findViewById(R.id.layoutSteps).setVisibility(showTrail ? View.VISIBLE : View.GONE);
        if (showTrail) {
            styleStep(tvStep1, reached >= 1);
            styleStep(tvStep2, reached >= 2);
            styleStep(tvStep3, reached >= 3);
            styleStep(tvStep4, reached >= 4);
        }
    }

    private void styleStep(TextView step, boolean done) {
        step.setTextColor(done ? getColor(R.color.green_primary_dark) : getColor(R.color.text_subtitle));
        step.setAlpha(done ? 1f : 0.6f);
        step.setTypeface(null, done ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    /**
     * One banner for two different refusals — the cook declining the request, and
     * the cook rejecting a screenshot — because from here they are the same thing:
     * a reason the customer has to read before they can do anything else.
     */
    private void applyRejectionNote(SubscriptionDetailResponse.Subscription sub) {
        String note = null;
        if ("rejected".equals(sub.getStage())) {
            note = sub.getResponseNote() != null && !sub.getResponseNote().trim().isEmpty()
                    ? "The cook declined: " + sub.getResponseNote()
                    : "The cook declined this request. Nothing is owed, and you can request again with a different start date.";
        } else if (sub.getPaymentRejectionReason() != null
                && !sub.getPaymentRejectionReason().trim().isEmpty()
                && sub.canUploadProof()) {
            note = "Your last screenshot was rejected: " + sub.getPaymentRejectionReason()
                    + "\n\nUpload a new one below.";
        }

        tvRejectionNote.setText(note != null ? note : "");
        tvRejectionNote.setVisibility(note != null ? View.VISIBLE : View.GONE);
    }

    private void applyPlanCard(SubscriptionDetailResponse.Subscription sub, SubscriptionDetailResponse body) {
        Double total = sub.getTotalAmount();
        if (total != null) {
            tvTotalAmount.setText("Rs. " + fmt(total));
            // One payment for the whole plan, and one meal a day. The old line
            // multiplied the price by the day count, which showed a 7-day plan as
            // seven times its real cost.
            tvAmountBreakdown.setText("One-time payment  ·  " + sub.getDurationDays()
                    + " days, 1 meal a day");
            tvAmountBreakdown.setVisibility(View.VISIBLE);
        } else {
            tvTotalAmount.setText("Price not set");
            tvAmountBreakdown.setVisibility(View.GONE);
        }

        // Before verification the end date is not fixed yet, so promising one
        // would be a guess. Say what is actually known.
        if (sub.getEndDate() != null) {
            tvWindow.setText(DeliveryDateUtils.formatShortDate(sub.getStartDate()) + " – "
                    + DeliveryDateUtils.formatLongDate(sub.getEndDate())
                    + "  ·  " + sub.getDurationDays() + " days");
            tvWindowNote.setVisibility(View.VISIBLE);
        } else {
            tvWindow.setText("Starts " + DeliveryDateUtils.formatLongDate(sub.getStartDate())
                    + "  ·  " + sub.getDurationDays() + " days");
            tvWindowNote.setText("The end date is fixed once the cook confirms your payment.");
            tvWindowNote.setVisibility(View.VISIBLE);
        }

        boolean hasAddress = sub.getDeliveryAddress() != null && !sub.getDeliveryAddress().trim().isEmpty();
        tvAddress.setText(hasAddress ? "Deliver to: " + sub.getDeliveryAddress() : "");
        tvAddress.setVisibility(hasAddress ? View.VISIBLE : View.GONE);

        boolean hasMeals = body.getMeals() != null && !body.getMeals().isEmpty();
        tvMealsTitle.setVisibility(hasMeals ? View.VISIBLE : View.GONE);
        tvMeals.setVisibility(hasMeals ? View.VISIBLE : View.GONE);
        if (hasMeals) {
            StringBuilder sb = new StringBuilder();
            for (SubscriptionDetailResponse.Meal meal : body.getMeals()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(Math.max(1, meal.getQuantity())).append("× ").append(meal.getName());
            }
            tvMeals.setText(sb.toString());
        }
    }

    private void applyPayBlock(SubscriptionDetailResponse.Subscription sub) {
        boolean canPay = sub.canUploadProof();
        cardPay.setVisibility(canPay ? View.VISIBLE : View.GONE);
        if (!canPay) return;

        final String qr = sub.getCookEsewaQrUrl();
        boolean hasQr = qr != null && !qr.trim().isEmpty();
        // A cook who never set up a QR is a real case, and an empty 220dp box
        // just reads as a broken screen. "Open eSewa" is still the route.
        tvScanToPay.setVisibility(hasQr ? View.VISIBLE : View.GONE);
        imgCookQr.setVisibility(hasQr ? View.VISIBLE : View.GONE);
        if (hasQr) {
            Glide.with(this).load(qr).into(imgCookQr);
            imgCookQr.setOnClickListener(v -> {
                Intent viewer = new Intent(this, MediaViewerActivity.class);
                viewer.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, qr);
                viewer.putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, false);
                startActivity(viewer);
            });
        }

        btnUploadProof.setText(sub.getPaymentProofAttempts() > 0
                ? "Upload a new screenshot" : "Upload payment screenshot");
    }

    private void applyProof(SubscriptionDetailResponse.Subscription sub) {
        final String url = sub.getPaymentScreenshotUrl();
        boolean show = url != null && !url.trim().isEmpty();
        layoutSubmittedProof.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;

        Glide.with(this).load(url).into(imgProof);
        imgProof.setOnClickListener(v -> {
            Intent viewer = new Intent(this, MediaViewerActivity.class);
            viewer.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, url);
            viewer.putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, false);
            startActivity(viewer);
        });
    }

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
            if (event.getAmount() != null) label += "  ·  Rs. " + fmt(event.getAmount());
            title.setText(label);

            boolean hasDetail = event.getDetail() != null && !event.getDetail().trim().isEmpty();
            detail.setText(hasDetail ? event.getDetail() : "");
            detail.setVisibility(hasDetail ? View.VISIBLE : View.GONE);

            time.setText(DeliveryDateUtils.formatShortDate(event.getCreatedAt()));
            layoutEvents.addView(row);
        }
    }

    private void openEsewa() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(ESEWA_PACKAGE);
        if (launch != null) {
            startActivity(launch);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("eSewa app required")
                .setMessage("Paying this way needs the eSewa app installed on this device. "
                        + "You can also scan the cook's QR from any wallet app.")
                .setPositiveButton("Install eSewa", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + ESEWA_PACKAGE)));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + ESEWA_PACKAGE)));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openChat() {
        if (current == null) return;
        if (current.getConversationId() == null) {
            Toast.makeText(this, "No chat with this cook yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent chat = new Intent(this, ChatActivity.class);
        chat.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, current.getConversationId().intValue());
        chat.putExtra(ChatActivity.EXTRA_CONTACT_ID, current.getCookId());
        chat.putExtra(ChatActivity.EXTRA_CONTACT_NAME, current.getCookName());
        chat.putExtra(ChatActivity.EXTRA_CONTACT_PHONE, current.getCookPhone());
        chat.putExtra(ChatActivity.EXTRA_CONTACT_ROLE, "cook");
        startActivity(chat);
    }

    /**
     * Warns before the picker, not after.
     *
     * The duplicate-image block is server-side and absolute — one screenshot can
     * only ever pay for one subscription — so telling the customer here saves them
     * picking an old receipt and getting a refusal they don't understand.
     */
    private void confirmUpload() {
        if (current == null) return;
        Double total = current.getTotalAmount();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Have you already paid?")
                .setMessage("Pick the screenshot of the transfer you just made"
                        + (total != null ? " for Rs. " + fmt(total) : "")
                        + ".\n\nIt has to be the receipt for this payment — a screenshot already "
                        + "used for another subscription is refused, and the cook checks the amount, "
                        + "date and name by hand before confirming.")
                .setPositiveButton("Choose screenshot", (d, w) -> openImagePicker())
                .setNegativeButton("Not yet", null)
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * One multipart POST, straight to the subscription.
     *
     * Deliberately not the older upload-to-Cloudinary-then-send-the-URL pair: the
     * server needs the raw bytes to hash them, and that hash is what makes the
     * same image unusable on a second subscription.
     */
    private void uploadProof(Uri imageUri) {
        if (uploadInFlight) return;

        if (!ImageUploadHelper.isImageFile(this, imageUri)) {
            Toast.makeText(this, "Pick an image file.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ImageUploadHelper.isValidFileSize(this, imageUri, 5)) {
            Toast.makeText(this, "That image is over 5MB. Try a screenshot instead of a photo.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        MultipartBody.Part part = ImageUploadHelper.createImagePart(this, imageUri, "proof");
        if (part == null) {
            Toast.makeText(this, "Couldn't read that image.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadInFlight = true;
        btnUploadProof.setEnabled(false);
        progressLoading.setVisibility(View.VISIBLE);

        apiService.submitPaymentProof("Bearer " + sessionManager.getToken(), subscriptionId, part)
                .enqueue(new Callback<SubscriptionActionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SubscriptionActionResponse> call,
                                           @NonNull Response<SubscriptionActionResponse> response) {
                        uploadInFlight = false;
                        btnUploadProof.setEnabled(true);
                        progressLoading.setVisibility(View.GONE);

                        SubscriptionActionResponse b = response.body();
                        boolean ok = response.isSuccessful() && b != null && b.isSuccess();

                        // The refusals here are specific and actionable — a reused
                        // screenshot, or a cook who hasn't accepted yet — so the
                        // server's own sentence is what the customer should read.
                        Toast.makeText(SubscriptionStatusActivity.this,
                                b != null && b.getMessage() != null
                                        ? b.getMessage()
                                        : (ok ? "Sent to the cook." : "Couldn't send that. Please try again."),
                                Toast.LENGTH_LONG).show();

                        loadDetail();
                    }

                    @Override
                    public void onFailure(@NonNull Call<SubscriptionActionResponse> call, @NonNull Throwable t) {
                        uploadInFlight = false;
                        btnUploadProof.setEnabled(true);
                        progressLoading.setVisibility(View.GONE);
                        Toast.makeText(SubscriptionStatusActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Whole rupees unless there really are paisa. */
    private static String fmt(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format(Locale.US, "%.2f", value);
    }
}
