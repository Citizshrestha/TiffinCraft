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
                        if (imageUri != null) uploadScreenshot(imageUri);
                    }
                });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPayWithEsewa.setOnClickListener(v -> openEsewaApp());
        binding.ivAdminQr.setOnClickListener(v -> saveQrToGallery());
        binding.btnUploadProof.setOnClickListener(v -> openImagePicker());
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

    private void loadCurrentSettlement() {
        binding.progressLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCurrentCommissionSettlement(token).enqueue(new Callback<CommissionSettlementCurrentResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommissionSettlementCurrentResponse> call,
                                    @NonNull Response<CommissionSettlementCurrentResponse> response) {
                binding.progressLoading.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(CommissionSettlementActivity.this, "Failed to load commission status", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Prefer an unresolved past-due settlement over the current (possibly
                // not-yet-generated) month, so a cook who missed last month's due sees it first.
                CommissionSettlement toShow = response.body().getPastDue() != null
                        ? response.body().getPastDue() : response.body().getCurrent();
                current = toShow;
                accruing = response.body().getAccruing();
                render();
            }

            @Override
            public void onFailure(@NonNull Call<CommissionSettlementCurrentResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                Toast.makeText(CommissionSettlementActivity.this, "Network error", Toast.LENGTH_SHORT).show();
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
        binding.tvAccruingLabel.setText("Accruing in " + period + " · " + n + " delivered order" + (n == 1 ? "" : "s"));
    }

    private void render() {
        renderAccruing();

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
                break;
            case "submitted":
                binding.tvStatusChip.setText("Submitted");
                setSectionVisible(false, true, false);
                binding.tvScreenshotStatusLabel.setText("Awaiting admin verification");
                loadScreenshotPreview();
                break;
            case "verified":
                binding.tvStatusChip.setText("Verified ✅");
                setSectionVisible(false, true, false);
                binding.tvScreenshotStatusLabel.setText("Verified — thank you!");
                loadScreenshotPreview();
                break;
            case "rejected":
                binding.tvStatusChip.setText("Rejected");
                setSectionVisible(true, false, true);
                binding.tvAdminNotes.setText(current.getAdminNotes() != null && !current.getAdminNotes().isEmpty()
                        ? current.getAdminNotes() : "Please re-upload a clearer payment screenshot.");
                break;
            default:
                setSectionVisible(false, false, false);
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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
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

        okhttp3.MultipartBody.Part imagePart = ImageUploadHelper.createDocumentPart(this, imageUri);
        if (imagePart == null) {
            Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.btnUploadProof.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();

        // Two-step, same as the customer-pays-cook Option A flow: upload the raw
        // image to get a Cloudinary URL, then attach that URL to the settlement.
        apiService.uploadDocumentCloudinary(token, imagePart).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.btnUploadProof.setEnabled(true);
                    Toast.makeText(CommissionSettlementActivity.this, "Failed to upload screenshot", Toast.LENGTH_SHORT).show();
                    return;
                }
                String url = response.body().getData().getUrl();
                submitScreenshotUrl(url);
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.btnUploadProof.setEnabled(true);
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
                binding.progressLoading.setVisibility(View.GONE);
                binding.btnUploadProof.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CommissionSettlementActivity.this, "Payment proof submitted — an admin will verify it shortly.", Toast.LENGTH_LONG).show();
                    loadCurrentSettlement();
                } else {
                    Toast.makeText(CommissionSettlementActivity.this, "Failed to submit payment proof", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.btnUploadProof.setEnabled(true);
                Toast.makeText(CommissionSettlementActivity.this, "Network error submitting payment proof", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
