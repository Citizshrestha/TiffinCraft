package com.tiffincraft.app.activities.customer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivitySubscriptionPaymentBinding;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.SubscriptionResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.CurrencyUtils;
import com.tiffincraft.app.utils.ImageUploadHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Customer-side "complete your subscription payment" screen — shown right
 * after subscribeToPlan() creates a 'pending_payment' subscription
 * (CookDetailsActivity no longer shows "Subscribed!" immediately). Mirrors
 * CommissionSettlementActivity's pay/upload flow: pay the cook's QR
 * (same eSewa app deep-link + save-to-gallery pattern used for orders),
 * upload a screenshot, then wait for the cook to verify it.
 */
public class SubscriptionPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_SUBSCRIPTION_ID = "subscription_id";
    public static final String EXTRA_PLAN_NAME = "plan_name";
    public static final String EXTRA_PLAN_PRICE = "plan_price";
    public static final String EXTRA_PLAN_DURATION = "plan_duration";
    public static final String EXTRA_COOK_ESEWA_QR_URL = "cook_esewa_qr_url";

    private static final String ESEWA_PACKAGE = "com.f1soft.esewa";
    private static final int REQUEST_STORAGE_PERMISSION = 2201;

    private ActivitySubscriptionPaymentBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private int subscriptionId;
    private String cookEsewaQrUrl;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubscriptionPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        subscriptionId = getIntent().getIntExtra(EXTRA_SUBSCRIPTION_ID, -1);
        String planName = getIntent().getStringExtra(EXTRA_PLAN_NAME);
        double planPrice = getIntent().getDoubleExtra(EXTRA_PLAN_PRICE, 0);
        String planDuration = getIntent().getStringExtra(EXTRA_PLAN_DURATION);
        cookEsewaQrUrl = getIntent().getStringExtra(EXTRA_COOK_ESEWA_QR_URL);

        if (subscriptionId == -1) {
            Toast.makeText(this, "Something went wrong — please try subscribing again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvPlanName.setText(planName != null ? planName : "Subscription");
        binding.tvPlanPrice.setText(CurrencyUtils.formatRupees(planPrice));
        binding.tvPlanDurationLabel.setText("one-time payment · " + ("weekly".equals(planDuration) ? "1 Week" : "1 Month"));

        // A cook who never uploaded a QR (bank_details NULL) and a stored URL
        // that no longer resolves both leave a blank 220dp box sitting under
        // the "Or scan the cook's eSewa QR" caption, which just reads as a
        // broken screen. Hide the whole affordance in either case and let
        // "Pay with eSewa" be the route.
        if (cookEsewaQrUrl != null && !cookEsewaQrUrl.isEmpty()) {
            Glide.with(this).asBitmap().load(cookEsewaQrUrl).into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                @Override
                public void onResourceReady(@NonNull android.graphics.Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                    binding.ivCookQr.setImageBitmap(bitmap);
                }

                @Override
                public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {}

                @Override
                public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                    hideCookQrBlock();
                }
            });
        } else {
            hideCookQrBlock();
        }

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
        binding.ivCookQr.setOnClickListener(v -> saveQrToGallery());
        binding.btnUploadProof.setOnClickListener(v -> openImagePicker());

        // Freshly created, so we already know the state: pending payment, nothing
        // submitted yet. No need for a network round-trip just to render this.
        renderState("pending", null);
    }

    private void renderState(String paymentStatus, String verificationNotes) {
        binding.layoutPaySection.setVisibility(View.GONE);
        binding.layoutStatusPanel.setVisibility(View.GONE);
        binding.layoutRejectedNotes.setVisibility(View.GONE);

        switch (paymentStatus) {
            case "submitted":
                binding.tvStatusChip.setText("Submitted");
                binding.layoutStatusPanel.setVisibility(View.VISIBLE);
                binding.tvStatusPanelTitle.setText("Waiting for the cook to verify");
                binding.tvStatusPanelSubtitle.setText("You'll be notified once your payment is confirmed.");
                break;
            case "verified":
                binding.tvStatusChip.setText("Verified ✅");
                binding.layoutStatusPanel.setVisibility(View.VISIBLE);
                binding.tvStatusPanelTitle.setText("Subscription Active!");
                binding.tvStatusPanelSubtitle.setText("Your first delivery is scheduled per the plan's cadence.");
                break;
            case "rejected":
                binding.tvStatusChip.setText("Rejected");
                binding.layoutPaySection.setVisibility(View.VISIBLE);
                binding.layoutRejectedNotes.setVisibility(View.VISIBLE);
                binding.tvRejectedNotes.setText(verificationNotes != null && !verificationNotes.isEmpty()
                        ? verificationNotes : "Please re-upload a clearer payment screenshot.");
                break;
            default: // pending
                binding.tvStatusChip.setText("Awaiting Payment");
                binding.layoutPaySection.setVisibility(View.VISIBLE);
                break;
        }
    }

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

    /** Drops the "scan the cook's QR" affordance when there is no QR to show. */
    private void hideCookQrBlock() {
        binding.tvScanToPay.setVisibility(View.GONE);
        binding.ivCookQr.setVisibility(View.GONE);
    }

    private void saveQrToGallery() {
        if (cookEsewaQrUrl == null || cookEsewaQrUrl.isEmpty()) return;

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            Toast.makeText(this, "Storage permission needed — tap the QR again after allowing.", Toast.LENGTH_LONG).show();
            return;
        }

        Glide.with(this).asBitmap().load(cookEsewaQrUrl).into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
            @Override
            public void onResourceReady(@NonNull android.graphics.Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                boolean saved = com.tiffincraft.app.utils.ImageUtils.saveBitmapToGallery(
                        SubscriptionPaymentActivity.this, bitmap, "esewa_qr_subscription_" + subscriptionId);
                Toast.makeText(SubscriptionPaymentActivity.this,
                        saved ? "QR saved to gallery" : "Could not save QR", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {}

            @Override
            public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                Toast.makeText(SubscriptionPaymentActivity.this, "Could not load QR", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadScreenshot(Uri imageUri) {
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

        apiService.uploadDocumentCloudinary(token, imagePart).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.btnUploadProof.setEnabled(true);
                    Toast.makeText(SubscriptionPaymentActivity.this, "Failed to upload screenshot", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitScreenshotUrl(response.body().getData().getUrl());
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.btnUploadProof.setEnabled(true);
                Toast.makeText(SubscriptionPaymentActivity.this, "Network error uploading screenshot", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitScreenshotUrl(String url) {
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("payment_screenshot_url", url);

        apiService.uploadSubscriptionScreenshot(token, subscriptionId, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.btnUploadProof.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(SubscriptionPaymentActivity.this, "Payment proof submitted — the cook will verify it shortly.", Toast.LENGTH_LONG).show();
                    renderState("submitted", null);
                } else {
                    Toast.makeText(SubscriptionPaymentActivity.this, "Failed to submit payment proof", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.btnUploadProof.setEnabled(true);
                Toast.makeText(SubscriptionPaymentActivity.this, "Network error submitting payment proof", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
