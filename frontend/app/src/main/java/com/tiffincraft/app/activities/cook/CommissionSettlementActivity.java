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
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getBankDetails() != null) {
                    adminEsewaQrUrl = response.body().getBankDetails().getEsewaQrUrl();
                    if (adminEsewaQrUrl != null && !adminEsewaQrUrl.isEmpty()) {
                        Glide.with(CommissionSettlementActivity.this)
                                .load(adminEsewaQrUrl)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(binding.ivAdminQr);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminQrResponse> call, @NonNull Throwable t) {
                // Non-fatal — the "Pay with eSewa" button still works without the QR.
            }
        });
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
                render();
            }

            @Override
            public void onFailure(@NonNull Call<CommissionSettlementCurrentResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                Toast.makeText(CommissionSettlementActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private void render() {
        if (current == null) {
            setSectionVisible(false, false, false);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        binding.layoutEmptyState.setVisibility(View.GONE);

        binding.tvPeriodLabel.setText(MONTH_NAMES[current.getMonth() - 1] + " " + current.getYear());
        binding.tvAmountDue.setText(CurrencyUtils.formatRupees(current.getAmountDue()));
        binding.tvOrderCountLabel.setText(current.getOrderCount() + " order" + (current.getOrderCount() == 1 ? "" : "s") + " this period");

        switch (current.getStatus()) {
            case "pending":
                binding.tvStatusChip.setText("Pending");
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
