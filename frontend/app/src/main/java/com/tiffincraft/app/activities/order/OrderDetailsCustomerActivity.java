package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
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
    private String paymentMethod, paymentStatus, paymentScreenshotUrl;

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
            binding.btnRateReview.setOnClickListener(v ->
                startActivity(new Intent(this, RateReviewActivity.class))
            );
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

        paymentMethod = order.getPaymentMethod();
        paymentStatus = order.getPaymentStatus();
        paymentScreenshotUrl = order.getPaymentScreenshotUrl();

        // Show/hide payment section based on payment method
        if (binding.layoutPaymentSection != null) {
            boolean isOnline = "online".equals(paymentMethod);
            binding.layoutPaymentSection.setVisibility(isOnline ? android.view.View.VISIBLE : android.view.View.GONE);

            if (isOnline) {
                // Payment status badge
                if (binding.tvPaymentStatusBadge != null) {
                    String statusText = getPaymentStatusText(paymentStatus);
                    binding.tvPaymentStatusBadge.setText(statusText);
                    int bgRes = getPaymentStatusBg(paymentStatus);
                    int textColor = getPaymentStatusTextColor(paymentStatus);
                    binding.tvPaymentStatusBadge.setBackgroundResource(bgRes);
                    binding.tvPaymentStatusBadge.setTextColor(getColor(textColor));
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
                if (binding.btnUploadPayment != null) {
                    boolean canUpload = "pending".equals(paymentStatus);
                    binding.btnUploadPayment.setVisibility(canUpload ? android.view.View.VISIBLE : android.view.View.GONE);
                    binding.btnUploadPayment.setEnabled(canUpload);
                }
            }
        }
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

            // Upload to Cloudinary via the existing document upload endpoint
            String token = "Bearer " + sessionManager.getToken();
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part part = MultipartBody.Part.createFormData("image", tempFile.getName(), requestBody);

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
                        Toast.makeText(OrderDetailsCustomerActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
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
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to submit";
                    Toast.makeText(OrderDetailsCustomerActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailsCustomerActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Payment status helpers ────────────────────────────
    private String getPaymentStatusText(String status) {
        if (status == null) return "Pending";
        switch (status) {
            case "verified": return "✅ Payment Verified";
            case "paid": return "⏳ Awaiting Verification";
            case "pending": return "⏳ Payment Pending";
            case "refunded": return "🔄 Refunded";
            default: return status;
        }
    }

    private int getPaymentStatusBg(String status) {
        if (status == null) return R.drawable.status_chip_pending;
        switch (status) {
            case "verified": return R.drawable.status_chip_delivered;
            case "paid": return R.drawable.status_chip_preparing;
            case "pending": return R.drawable.status_chip_pending;
            default: return R.drawable.status_chip_pending;
        }
    }

    private int getPaymentStatusTextColor(String status) {
        if (status == null) return R.color.status_pending_text;
        switch (status) {
            case "verified": return R.color.status_delivered_text;
            case "paid": return R.color.status_preparing_text;
            default: return R.color.status_pending_text;
        }
    }
}
