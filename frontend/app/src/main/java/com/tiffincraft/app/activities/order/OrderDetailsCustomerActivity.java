package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.RateReviewActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityOrderDetailsCustomerBinding;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.models.UploadResponse;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.ImageUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsCustomerActivity extends AppCompatActivity {

    private ActivityOrderDetailsCustomerBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int orderId = -1;
    private int cookId = -1;
    private String kitchenName;
    private String paymentMethod, paymentStatus, paymentScreenshotUrl, cookEsewaQrUrl;
    private boolean esewaConfirmed;

    private static final int PICK_IMAGE_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsCustomerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        orderId = getIntent().getIntExtra("order_id", -1);

        binding.btnBack.setOnClickListener(v -> finish());

        try {
            binding.btnRateReview.setVisibility(android.view.View.GONE);
            binding.btnRateReview.setOnClickListener(v -> {
                Intent intent = new Intent(this, RateReviewActivity.class);
                intent.putExtra(RateReviewActivity.EXTRA_ORDER_ID, orderId);
                intent.putExtra(RateReviewActivity.EXTRA_COOK_ID, cookId);
                intent.putExtra(RateReviewActivity.EXTRA_KITCHEN_NAME, kitchenName);
                startActivity(intent);
            });
        } catch (NullPointerException ignored) {}

        try {
            binding.btnTrackOrder.setOnClickListener(v -> {
                Intent intent = new Intent(this, TrackOrderActivity.class);
                intent.putExtra("order_id", orderId);
                startActivity(intent);
            });
        } catch (NullPointerException ignored) {}

        // Wire Upload Payment Screenshot button
        if (binding.btnUploadPayment != null) {
            binding.btnUploadPayment.setOnClickListener(v -> openImagePicker());
        }

        // Payment is manual: the customer opens the real eSewa app (or scans
        // the cook's QR shown below, set up in populateOrder), pays in their
        // own eSewa app, and uploads the screenshot for the cook to verify.
        // No prefilled amount/deeplink — that needs a production eSewa
        // MERCHANT account (merchant.esewa.com.np), which this app doesn't
        // have.
        if (binding.btnPayWithEsewa != null) {
            binding.btnPayWithEsewa.setOnClickListener(v -> openEsewaApp());
        }
        if (binding.ivCookEsewaQr != null) {
            binding.ivCookEsewaQr.setOnClickListener(v -> saveQrToGallery());
        }
        if (binding.btnRequestRefund != null) {
            binding.btnRequestRefund.setOnClickListener(v -> showRequestRefundDialog());
        }

        // Load order details
        if (orderId > 0) {
            loadOrderDetails();
        }
    }

    private void loadOrderDetails() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getOrderDetails(token, orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getOrder() != null) {
                    populateOrder(response.body().getOrder());
                } else {
                    Toast.makeText(OrderDetailsCustomerActivity.this, "Failed to load order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailsCustomerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateOrder(Order order) {
        if (binding.tvOrderId != null)
            binding.tvOrderId.setText("Order #TC" + order.getId());
        if (binding.tvOrderDate != null && order.getCreatedAt() != null)
            binding.tvOrderDate.setText(order.getCreatedAt());
        if (binding.tvTotalAmount != null)
            binding.tvTotalAmount.setText("Total Amount: ₹" + String.format("%.0f", order.getTotalAmount()));
        if (binding.tvOrderItems != null) {
            String items = order.getItemsSummary();
            binding.tvOrderItems.setText(items != null && !items.isEmpty() ? items : "No item details available.");
        }

        cookId = order.getCookId();
        kitchenName = order.getKitchenName() != null && !order.getKitchenName().isEmpty()
                ? order.getKitchenName() : order.getCookName();

        try {
            binding.btnRateReview.setVisibility(
                    "delivered".equals(order.getStatus()) ? android.view.View.VISIBLE : android.view.View.GONE);
        } catch (NullPointerException ignored) {}

        paymentMethod = order.getPaymentMethod();
        paymentStatus = order.getPaymentStatus();
        paymentScreenshotUrl = order.getPaymentScreenshotUrl();
        esewaConfirmed = order.isEsewaConfirmed();

        // Show/hide payment section based on payment method
        if (binding.layoutPaymentSection != null) {
            boolean isOnline = "online".equals(paymentMethod);
            binding.layoutPaymentSection.setVisibility(isOnline ? android.view.View.VISIBLE : android.view.View.GONE);

            if (isOnline) {
                // Payment status badge
                if (binding.tvPaymentStatusBadge != null) {
                    binding.tvPaymentStatusBadge.setText(getPaymentStatusText(paymentStatus, esewaConfirmed));
                    binding.tvPaymentStatusBadge.setBackgroundResource(getPaymentStatusBg(paymentStatus, esewaConfirmed));
                    binding.tvPaymentStatusBadge.setTextColor(getColor(getPaymentStatusTextColor(paymentStatus, esewaConfirmed)));
                }

                // Show uploaded screenshot
                if (paymentScreenshotUrl != null && !paymentScreenshotUrl.isEmpty() && binding.ivPaymentScreenshot != null) {
                    binding.ivPaymentScreenshot.setVisibility(android.view.View.VISIBLE);
                    Glide.with(this)
                            .load(paymentScreenshotUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(binding.ivPaymentScreenshot);
                } else if (binding.ivPaymentScreenshot != null) {
                    binding.ivPaymentScreenshot.setVisibility(android.view.View.GONE);
                }

                // Upload button visibility: show only when pending (not yet paid/verified/rejected)
                boolean canPay = "pending".equals(paymentStatus);
                if (binding.btnUploadPayment != null) {
                    binding.btnUploadPayment.setVisibility(canPay ? android.view.View.VISIBLE : android.view.View.GONE);
                    binding.btnUploadPayment.setEnabled(canPay);
                }
                if (binding.btnPayWithEsewa != null) {
                    binding.btnPayWithEsewa.setVisibility(canPay ? android.view.View.VISIBLE : android.view.View.GONE);
                }
                // Cook's eSewa QR — shown while payment is still pending so the
                // customer can scan and pay. Hidden if the cook never uploaded one.
                cookEsewaQrUrl = order.getCookEsewaQrUrl();
                boolean showQr = canPay && cookEsewaQrUrl != null && !cookEsewaQrUrl.isEmpty();
                if (binding.ivCookEsewaQr != null) {
                    binding.ivCookEsewaQr.setVisibility(showQr ? android.view.View.VISIBLE : android.view.View.GONE);
                    if (showQr) {
                        Glide.with(this).load(cookEsewaQrUrl)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(binding.ivCookEsewaQr);
                    }
                }
                if (binding.tvScanToPay != null) {
                    binding.tvScanToPay.setVisibility(showQr ? android.view.View.VISIBLE : android.view.View.GONE);
                }
                if (binding.btnRequestRefund != null) {
                    boolean canRefund = "paid".equals(paymentStatus) || "verified".equals(paymentStatus);
                    binding.btnRequestRefund.setVisibility(canRefund ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }
        }
    }

    private static final String ESEWA_PACKAGE = "com.f1soft.esewa";

    /** Opens the real eSewa app (its own launcher entry) so the customer can pay from there. */
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

    private static final int REQUEST_STORAGE_PERMISSION = 2002;

    /** Downloads the cook's QR (already on-screen via Glide) and saves it into the gallery. */
    private void saveQrToGallery() {
        if (cookEsewaQrUrl == null || cookEsewaQrUrl.isEmpty()) return;

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P
                && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            Toast.makeText(this, "Storage permission needed — tap the QR again after allowing.", Toast.LENGTH_LONG).show();
            return;
        }

        Glide.with(this).asBitmap().load(cookEsewaQrUrl).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                boolean saved = ImageUtils.saveBitmapToGallery(
                        OrderDetailsCustomerActivity.this, bitmap, "esewa_qr_order_" + orderId);
                Toast.makeText(OrderDetailsCustomerActivity.this,
                        saved ? "QR saved to gallery" : "Could not save QR", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

            @Override
            public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                Toast.makeText(OrderDetailsCustomerActivity.this, "Could not load QR", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRequestRefundDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_request_refund, null);
        Spinner spinnerReason = dialogView.findViewById(R.id.spinnerRefundReason);
        EditText etNotes = dialogView.findViewById(R.id.etRefundNotes);

        String[] reasonLabels = {"Failed delivery", "Cook made a mistake", "I want to cancel", "Other"};
        String[] reasonValues = {"failed_delivery", "cook_mistake", "customer_cancelled", "other"};
        spinnerReason.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reasonLabels));

        new AlertDialog.Builder(this)
                .setTitle("Request a Refund")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String reason = reasonValues[spinnerReason.getSelectedItemPosition()];
                    String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : null;
                    submitRefundRequest(reason, notes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitRefundRequest(String reason, String notes) {
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("order_id", orderId);
        body.addProperty("reason", reason);
        if (notes != null && !notes.isEmpty()) {
            body.addProperty("reason_notes", notes);
        }

        apiService.requestRefund(token, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OrderDetailsCustomerActivity.this,
                            "Refund request submitted. An admin will review it shortly.", Toast.LENGTH_LONG).show();
                    if (binding.btnRequestRefund != null) {
                        binding.btnRequestRefund.setEnabled(false);
                        binding.btnRequestRefund.setText("Refund Requested");
                    }
                } else {
                    Toast.makeText(OrderDetailsCustomerActivity.this,
                            extractErrorMessage(response.errorBody(), "Failed to submit refund request"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailsCustomerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToCloudinary(imageUri);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create temp file
            File tempFile = new File(getCacheDir(), "payment_screenshot_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            // Upload to Cloudinary via the existing document upload endpoint.
            // The backend's fileFilter whitelists exact mimetypes (image/jpeg,
            // image/png, ...) and rejects the literal wildcard "image/*" that
            // was hardcoded here before — resolve the picked file's real type.
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType == null) mimeType = "image/jpeg";

            String token = "Bearer " + sessionManager.getToken();
            RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), tempFile);
            // Backend's /upload/document route is configured with multer's
            // uploadSingle('document') — any other field name is rejected with
            // "Unexpected field" (was "image", which always 400'd here).
            MultipartBody.Part part = MultipartBody.Part.createFormData("document", tempFile.getName(), requestBody);

            binding.btnUploadPayment.setEnabled(false);
            binding.btnUploadPayment.setText("Uploading...");

            apiService.uploadDocumentCloudinary(token, part).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                    binding.btnUploadPayment.setEnabled(true);
                    binding.btnUploadPayment.setText("Upload Payment Screenshot");
                    tempFile.delete();

                    if (response.isSuccessful() && response.body() != null
                            && response.body().getData() != null && response.body().getData().getUrl() != null) {
                        String uploadedUrl = response.body().getData().getUrl();
                        submitPaymentScreenshot(uploadedUrl);
                    } else {
                        Toast.makeText(OrderDetailsCustomerActivity.this,
                                extractErrorMessage(response.errorBody(), "Upload failed"), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                    binding.btnUploadPayment.setEnabled(true);
                    binding.btnUploadPayment.setText("Upload Payment Screenshot");
                    tempFile.delete();
                    Toast.makeText(OrderDetailsCustomerActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void submitPaymentScreenshot(String url) {
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("payment_screenshot_url", url);

        apiService.uploadPaymentScreenshot(token, orderId, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OrderDetailsCustomerActivity.this, "Payment screenshot submitted! Waiting for cook verification.", Toast.LENGTH_LONG).show();
                    // Refresh to show updated status
                    loadOrderDetails();
                } else {
                    Toast.makeText(OrderDetailsCustomerActivity.this,
                            extractErrorMessage(response.errorBody(), "Failed to submit payment screenshot"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailsCustomerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Retrofit leaves response.body() null on non-2xx responses — the real
    // reason (e.g. "Invalid file type...") lives in errorBody() and was
    // previously discarded in favor of generic toasts.
    private String extractErrorMessage(okhttp3.ResponseBody errorBody, String fallback) {
        if (errorBody != null) {
            try {
                JSONObject json = new JSONObject(errorBody.string());
                if (json.has("message")) {
                    return json.getString("message");
                }
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    // ── Payment status helpers ────────────────────────────
    // "paid" is reached by two different paths: an eSewa auto-confirmation
    // (esewaConfirmed=true, nothing left to do) or the manual screenshot flow
    // (still awaiting the cook's verification) — the label must say which.
    private String getPaymentStatusText(String status, boolean esewaConfirmed) {
        if (status == null) return "Pending";
        switch (status) {
            case "verified": return "✅ Payment Verified";
            case "paid": return esewaConfirmed ? "✅ Paid via eSewa" : "⏳ Awaiting Verification";
            case "pending": return "⏳ Payment Pending";
            case "refunded": return "🔄 Refunded";
            default: return status;
        }
    }

    private int getPaymentStatusBg(String status, boolean esewaConfirmed) {
        if (status == null) return R.drawable.status_chip_pending;
        switch (status) {
            case "verified": return R.drawable.status_chip_delivered;
            case "paid": return esewaConfirmed ? R.drawable.status_chip_delivered : R.drawable.status_chip_preparing;
            case "pending": return R.drawable.status_chip_pending;
            default: return R.drawable.status_chip_pending;
        }
    }

    private int getPaymentStatusTextColor(String status, boolean esewaConfirmed) {
        if (status == null) return R.color.status_pending_text;
        switch (status) {
            case "verified": return R.color.status_delivered_text;
            case "paid": return esewaConfirmed ? R.color.status_delivered_text : R.color.status_preparing_text;
            default: return R.color.status_pending_text;
        }
    }
}
