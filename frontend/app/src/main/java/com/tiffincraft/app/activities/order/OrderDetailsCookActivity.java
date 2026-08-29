package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.tiffincraft.app.R;
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
                if (isPaymentSectionVisible && isPaymentNotVerified()) {
                    Snackbar.make(binding.getRoot(),
                            "Payment not verified yet! Verify payment screenshot before marking complete.",
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
                label = "⏳ Payment Pending"; bgRes = R.drawable.status_chip_pending;
        }
        if (binding.tvPaymentStatus != null) {
            binding.tvPaymentStatus.setText(label);
            binding.tvPaymentStatus.setBackgroundResource(bgRes);
        }

        if (binding.imgPaymentScreenshot != null) {
            String url = order.getPaymentScreenshotUrl();
            if (url != null && !url.isEmpty()) {
                binding.imgPaymentScreenshot.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(binding.imgPaymentScreenshot);
            } else {
                binding.imgPaymentScreenshot.setVisibility(View.GONE);
            }
        }

        if (binding.btnVerifyPayment != null) {
            boolean canVerify = "paid".equals(status) && !esewaConfirmed;
            binding.btnVerifyPayment.setVisibility(canVerify ? View.VISIBLE : View.GONE);
            binding.btnVerifyPayment.setOnClickListener(v -> verifyPayment());
        }
    }

    private void verifyPayment() {
        String token = "Bearer " + sessionManager.getToken();
        JsonObject body = new JsonObject();
        body.addProperty("verified", true);

        binding.btnVerifyPayment.setEnabled(false);
        apiService.verifyPayment(token, orderId, body).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                binding.btnVerifyPayment.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OrderDetailsCookActivity.this, "Payment verified!", Toast.LENGTH_SHORT).show();
                    loadOrderDetails();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to verify payment";
                    Toast.makeText(OrderDetailsCookActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                binding.btnVerifyPayment.setEnabled(true);
                Toast.makeText(OrderDetailsCookActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
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
