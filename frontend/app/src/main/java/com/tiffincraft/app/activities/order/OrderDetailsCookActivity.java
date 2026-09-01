package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.MediaViewerActivity;
import com.tiffincraft.app.activities.cook.UpdateStatusActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityCookOrderDetailsBinding;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsCookActivity extends AppCompatActivity {

    private ActivityCookOrderDetailsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int orderId = -1;
    private boolean isPaymentSectionVisible = false;
    private Order currentOrder;
    /** Guards a double-tap from sending two payment decisions for the same order. */
    private boolean actionInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCookOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        orderId = getIntent().getIntExtra("order_id", -1);

        if (binding.btnBackOrderDetails != null) {
            binding.btnBackOrderDetails.setOnClickListener(v -> finish());
        }

        if (binding.btnUpdateOrderStatus != null) {
            binding.btnUpdateOrderStatus.setOnClickListener(v -> {
                // Only block "delivered" status if payment is not verified for online payments
                // Allow confirming, preparing, and ready statuses without payment verification
                String currentStatus = currentOrder != null ? currentOrder.getStatus() : "";
                boolean isReadyToDeliver = "ready".equals(currentStatus);
                
                if (isReadyToDeliver && isPaymentSectionVisible && isPaymentNotVerified()) {
                    Snackbar.make(binding.getRoot(),
                            "Payment not verified yet! Verify payment screenshot before marking delivered.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(this, UpdateStatusActivity.class);
                intent.putExtra("order_id", orderId);
                startActivity(intent);
            });
        }

        if (orderId > 0) {
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from UpdateStatusActivity may have changed the status.
        if (orderId > 0) {
            loadOrderDetails();
        }
    }

    /** Blocks "mark complete" until money has actually been confirmed received. */
    private boolean isPaymentNotVerified() {
        if (currentOrder == null || !"online".equals(currentOrder.getPaymentMethod())) return false;
        String status = currentOrder.getPaymentStatus();
        if ("verified".equals(status)) return false;
        if ("paid".equals(status) && currentOrder.isEsewaConfirmed()) return false;
        return true;
    }

    private void loadOrderDetails() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getOrderDetails(token, orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getOrder() != null) {
                    currentOrder = response.body().getOrder();
                    populateOrder(currentOrder);
                } else {
                    Toast.makeText(OrderDetailsCookActivity.this, "Failed to load order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailsCookActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateOrder(Order order) {
        if (binding.tvOrderIdHeader != null) {
            binding.tvOrderIdHeader.setText("Order #TC" + order.getId());
        }

        bindStatusChip(order.getStatus());

        if (binding.tvOrderPlacedTime != null) {
            binding.tvOrderPlacedTime.setText(formatTime(order.getCreatedAt()));
        }
        if (binding.tvOrderPaymentMethod != null) {
            binding.tvOrderPaymentMethod.setText("cod".equals(order.getPaymentMethod()) ? "COD" : "Online");
        }

        if (binding.tvCustomerName != null) {
            binding.tvCustomerName.setText(order.getCustomerName() != null ? order.getCustomerName() : "Customer");
        }
        if (binding.ivCustomerAvatar != null) {
            com.tiffincraft.app.utils.ImageUrlHelper.load(binding.ivCustomerAvatar, order.getCustomerProfileImage(), R.drawable.avatar_customer);
        }
        if (binding.tvCustomerAddress != null) {
            binding.tvCustomerAddress.setText(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "—");
        }
        if (binding.btnCallCustomer != null) {
            binding.btnCallCustomer.setOnClickListener(v -> {
                if (order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + order.getCustomerPhone())));
                } else {
                    Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        bindItems(order);

        if (binding.layoutCustomerNote != null && binding.tvCustomerNote != null) {
            boolean hasNote = order.getSpecialInstructions() != null && !order.getSpecialInstructions().trim().isEmpty();
            binding.layoutCustomerNote.setVisibility(hasNote ? View.VISIBLE : View.GONE);
            if (hasNote) {
                binding.tvCustomerNote.setText(order.getSpecialInstructions());
            }
        }

        String amountStr = String.format(Locale.getDefault(), "₹%.0f", order.getTotalAmount());
        if (binding.tvItemTotal != null) binding.tvItemTotal.setText(amountStr);
        if (binding.tvYouEarn != null) binding.tvYouEarn.setText(amountStr);

        bindPaymentSection(order);
        bindTrackButton(order.getStatus());
    }

    private void bindTrackButton(String status) {
        if (binding.btnTrackDelivery == null) return;
        String s = status != null ? status.toLowerCase(Locale.ROOT) : "";
        boolean show = s.equals("preparing")
                || s.equals("ready")
                || s.equals("prepared")
                || s.equals("out_for_delivery")
                || s.equals("out for delivery")
                || s.equals("delivered")
                || s.equals("completed");
        binding.btnTrackDelivery.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnTrackDelivery.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrackOrderActivity.class);
            intent.putExtra("order_id", orderId);
            startActivity(intent);
        });
    }

    private void bindStatusChip(String status) {
        if (binding.tvOrderDetailsStatus == null) return;
        String s = status != null ? status.toLowerCase(Locale.ROOT) : "";
        String label;
        int bgRes;
        int textColor;

        switch (s) {
            case "confirmed":
                label = "Confirmed"; bgRes = R.drawable.status_chip_out_for_delivery; textColor = R.color.blue; break;
            case "preparing":
                label = "Preparing"; bgRes = R.drawable.status_chip_preparing; textColor = R.color.status_preparing_text; break;
            case "ready":
                label = "Ready"; bgRes = R.drawable.status_chip_preparing; textColor = R.color.status_preparing_text; break;
            case "out_for_delivery":
                label = "Out for Delivery"; bgRes = R.drawable.status_chip_out_for_delivery; textColor = R.color.blue; break;
            case "delivered":
                label = "Delivered"; bgRes = R.drawable.status_chip_delivered; textColor = R.color.status_delivered_text; break;
            case "completed":
                label = "Completed"; bgRes = R.drawable.status_chip_delivered; textColor = R.color.status_delivered_text; break;
            case "cancelled":
                label = "Cancelled"; bgRes = R.drawable.status_chip_sold_out; textColor = R.color.error_red; break;
            default:
                label = "Pending"; bgRes = R.drawable.status_chip_pending; textColor = R.color.status_pending_text;
        }

        binding.tvOrderDetailsStatus.setText(label);
        binding.tvOrderDetailsStatus.setBackgroundResource(bgRes);
        binding.tvOrderDetailsStatus.setTextColor(ContextCompat.getColor(this, textColor));
    }

    private void bindItems(Order order) {
        if (binding.layoutItemsContainer == null) return;
        LinearLayout container = binding.layoutItemsContainer;
        container.removeAllViews();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            int count = order.getItems().size();
            for (int i = 0; i < count; i++) {
                Order.OrderItem item = order.getItems().get(i);
                addItemRow(container, item.getQuantity(), item.getMealName(),
                        item.getPriceAtTime() * item.getQuantity(), i < count - 1);
            }
        } else if (order.getMealName() != null) {
            addItemRow(container, Math.max(order.getQuantity(), 1), order.getMealName(),
                    order.getMealPrice() * Math.max(order.getQuantity(), 1), false);
        } else {
            TextView empty = new TextView(this);
            empty.setText("No item details available.");
            empty.setTextColor(0xFF999999);
            empty.setTextSize(13f);
            container.addView(empty);
        }

        if (binding.tvOrderTimeLeft != null) {
            int itemCount = order.getItems() != null ? order.getItems().size() : (order.getMealName() != null ? 1 : 0);
            binding.tvOrderTimeLeft.setText(String.valueOf(itemCount));
        }
    }

    private void addItemRow(LinearLayout container, int quantity, String name, double lineTotal, boolean withDivider) {
        float density = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvQty = new TextView(this);
        tvQty.setText(quantity + "x");
        tvQty.setTextColor(0xFF4CAF50);
        tvQty.setTextSize(13.5f);
        tvQty.setTypeface(null, android.graphics.Typeface.BOLD);
        tvQty.setLayoutParams(new LinearLayout.LayoutParams((int) (28 * density), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(tvQty);

        TextView tvName = new TextView(this);
        tvName.setText(name != null ? name : "Item");
        tvName.setTextColor(0xFF111111);
        tvName.setTextSize(14f);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvName);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f", lineTotal));
        tvPrice.setTextColor(0xFF333333);
        tvPrice.setTextSize(13f);
        row.addView(tvPrice);

        container.addView(row);

        if (withDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * density));
            lp.topMargin = (int) (12 * density);
            lp.bottomMargin = (int) (12 * density);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(0xFFEFEFEF);
            container.addView(divider);
        }
    }

    private void bindPaymentSection(Order order) {
        boolean isOnline = "online".equals(order.getPaymentMethod());
        isPaymentSectionVisible = isOnline;

        if (binding.layoutPaymentInfo == null) return;
        binding.layoutPaymentInfo.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        if (!isOnline) return;

        String status = order.getPaymentStatus();
        boolean esewaConfirmed = order.isEsewaConfirmed();
        String label;
        int bgRes;
        switch (status != null ? status : "") {
            case "verified":
                label = "✅ Verified"; bgRes = R.drawable.status_chip_delivered; break;
            case "paid":
                label = esewaConfirmed ? "✅ Paid via eSewa" : "⏳ Awaiting Verification";
                bgRes = esewaConfirmed ? R.drawable.status_chip_delivered : R.drawable.status_chip_preparing;
                break;
            case "refunded":
                label = "🔄 Refunded"; bgRes = R.drawable.status_chip_out_for_delivery; break;
            default:
                // 'pending' means either "never paid" or "you rejected the proof
                // and they haven't sent a new one" — those need different labels.
                boolean wasRejected = order.getPaymentRejectedAt() != null;
                label = wasRejected ? "❌ You rejected the proof" : "⏳ Payment Pending";
                bgRes = wasRejected ? R.drawable.status_chip_sold_out : R.drawable.status_chip_pending;
        }
        if (binding.tvPaymentStatus != null) {
            binding.tvPaymentStatus.setText(label);
            binding.tvPaymentStatus.setBackgroundResource(bgRes);
        }

        if (binding.imgPaymentScreenshot != null) {
            final String url = order.getPaymentScreenshotUrl();
            boolean hasProof = url != null && !url.isEmpty();
            binding.imgPaymentScreenshot.setVisibility(hasProof ? View.VISIBLE : View.GONE);
            if (binding.tvTapToZoomHint != null) {
                binding.tvTapToZoomHint.setVisibility(hasProof ? View.VISIBLE : View.GONE);
            }
            if (hasProof) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(binding.imgPaymentScreenshot);
                // Pinch-zoom lives in the existing full-screen viewer; a
                // transaction ID is unreadable in a 180dp centerCrop preview.
                binding.imgPaymentScreenshot.setOnClickListener(v -> {
                    Intent viewer = new Intent(this, MediaViewerActivity.class);
                    viewer.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, url);
                    viewer.putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, false);
                    startActivity(viewer);
                });
            }
        }

        renderRetryWarning(order);

        // Both buttons appear and disappear together: a proof awaiting a manual
        // decision is exactly the state in which either answer is valid.
        boolean canDecide = "paid".equals(status) && !esewaConfirmed;
        if (binding.layoutPaymentActions != null) {
            binding.layoutPaymentActions.setVisibility(canDecide ? View.VISIBLE : View.GONE);
        }
        if (binding.btnVerifyPayment != null) {
            binding.btnVerifyPayment.setOnClickListener(v -> confirmVerify());
        }
        if (binding.btnRejectPayment != null) {
            binding.btnRejectPayment.setOnClickListener(v -> promptReject());
        }
        setPaymentButtonsEnabled(!actionInFlight);
    }

    /**
     * A rising attempt count is a dispute signal, and the reason the cook gave
     * last time is the context for judging this screenshot.
     */
    private void renderRetryWarning(Order order) {
        if (binding.tvPaymentRetryWarning == null) return;
        if (order.getPaymentProofAttempts() > 1) {
            String reason = order.getPaymentRejectionReason();
            binding.tvPaymentRetryWarning.setText("This is attempt " + order.getPaymentProofAttempts() + "."
                    + (reason != null && !reason.trim().isEmpty()
                        ? " You rejected the previous one: \"" + reason + "\""
                        : " An earlier screenshot was rejected."));
            binding.tvPaymentRetryWarning.setVisibility(View.VISIBLE);
        } else {
            binding.tvPaymentRetryWarning.setVisibility(View.GONE);
        }
    }

    /**
     * Verify. The dialog restates the amount because this is the step the
     * customer feels as irreversible — it confirms the order.
     */
    private void confirmVerify() {
        if (currentOrder == null) return;
        String amount = String.format(Locale.getDefault(), "₹%.0f", currentOrder.getTotalAmount());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.RoundedWhiteDialog)
                .setTitle("Confirm payment received?")
                .setMessage("Verify only if " + amount + " is in your account. Check amount, date, and sender name against the screenshot.")
                .setPositiveButton("Yes, payment received", (d, w) -> sendDecision(true, null))
                .setNegativeButton("Not yet", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the "Not yet" button with red color
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.error, null));
        }
    }

    /**
     * Reject, with a mandatory reason.
     *
     * The reason is what the customer sees and what they have to act on to send
     * a usable screenshot, so an empty one is refused rather than defaulted.
     */
    private void promptReject() {
        final EditText input = new EditText(this);
        input.setHint("e.g. amount is short, or this is an old transfer");
        input.setMinLines(2);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(pad, pad / 2, pad, 0);
        wrapper.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reject this screenshot?")
                .setMessage("The customer is told why and can upload a new one. Nothing is deleted — "
                        + "this image stays on the record in case the payment is disputed later.")
                .setView(wrapper)
                .setPositiveButton("Reject payment", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this,
                                "Give a reason — it's what the customer has to fix.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    sendDecision(false, reason);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void sendDecision(boolean verified, String reason) {
        if (actionInFlight) return;
        actionInFlight = true;
        setPaymentButtonsEnabled(false);

        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("verified", verified);
        if (reason != null) body.addProperty("reason", reason);

        apiService.verifyPayment(token, orderId, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                actionInFlight = false;
                setPaymentButtonsEnabled(true);

                RegisterResponse b = response.body();
                boolean ok = response.isSuccessful() && b != null && b.isSuccess();
                Toast.makeText(OrderDetailsCookActivity.this,
                        b != null && b.getMessage() != null
                                ? b.getMessage()
                                : (ok ? "Done." : "Couldn't record that. Please try again."),
                        Toast.LENGTH_LONG).show();

                // Re-read either way: on success to pick up the new state, on
                // failure because whatever state actually won is what matters.
                loadOrderDetails();
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                actionInFlight = false;
                setPaymentButtonsEnabled(true);
                Toast.makeText(OrderDetailsCookActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setPaymentButtonsEnabled(boolean enabled) {
        if (binding.btnVerifyPayment != null) binding.btnVerifyPayment.setEnabled(enabled);
        if (binding.btnRejectPayment != null) binding.btnRejectPayment.setEnabled(enabled);
    }

    /**
     * created_at is UTC ISO ("...T05:24:40.000Z"); this used to strip the 'Z' and
     * parse it as local time, showing an order placed at 11:09 AM NPT as 05:24 AM.
     */
    private String formatTime(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) return "—";
        return com.tiffincraft.app.utils.TimeFormat.time(isoDate);
    }
}
