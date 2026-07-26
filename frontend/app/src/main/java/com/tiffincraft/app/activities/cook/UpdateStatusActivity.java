package com.tiffincraft.app.activities.cook;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityUpdateStatusBinding;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.UpdateOrderStatusRequest;
import com.tiffincraft.app.session.SessionManager;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateStatusActivity extends AppCompatActivity {

    private ActivityUpdateStatusBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int orderId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);

        orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId <= 0) {
            Toast.makeText(this, "Invalid order.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCurrentStatus();

        binding.btnSaveStatus.setOnClickListener(v -> saveStatus());
    }

    /** Pre-selects the radio matching the order's real current status, so the
     *  cook sees where the order already is instead of an empty picker. */
    private void loadCurrentStatus() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getOrderDetails(token, orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getOrder() != null) {
                    Order order = response.body().getOrder();
                    String status = order.getStatus();
                    if ("confirmed".equals(status)) {
                        binding.rgStatus.check(binding.rbConfirmed.getId());
                    } else if ("preparing".equals(status)) {
                        binding.rgStatus.check(binding.rbPreparing.getId());
                    } else if ("ready".equals(status)) {
                        binding.rgStatus.check(binding.rbOutForDelivery.getId());
                    } else if ("delivered".equals(status)) {
                        binding.rgStatus.check(binding.rbDelivered.getId());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                // Non-fatal — the picker just opens with nothing pre-selected.
            }
        });
    }

    /** Radio labels use the customer-facing "Out for Delivery" wording, but the
     *  backend's actual lifecycle (see orderController.updateOrderStatus) has
     *  no such status — "ready" is its final pre-delivery state. */
    private String selectedBackendStatus() {
        int checkedId = binding.rgStatus.getCheckedRadioButtonId();
        if (checkedId == binding.rbConfirmed.getId()) return "confirmed";
        if (checkedId == binding.rbPreparing.getId()) return "preparing";
        if (checkedId == binding.rbOutForDelivery.getId()) return "ready";
        if (checkedId == binding.rbDelivered.getId()) return "delivered";
        return null;
    }

    private void saveStatus() {
        String status = selectedBackendStatus();
        if (status == null) {
            Toast.makeText(this, "Select a status first.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveStatus.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();

        apiService.updateOrderStatus(token, orderId, new UpdateOrderStatusRequest(status))
                .enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                        binding.btnSaveStatus.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(UpdateStatusActivity.this, "Order status updated!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            // e.g. "Cannot mark as delivered until payment is verified."
                            Toast.makeText(UpdateStatusActivity.this,
                                    extractErrorMessage(response.errorBody()), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                        binding.btnSaveStatus.setEnabled(true);
                        Toast.makeText(UpdateStatusActivity.this, "Network error — please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String extractErrorMessage(okhttp3.ResponseBody errorBody) {
        if (errorBody != null) {
            try {
                JSONObject json = new JSONObject(errorBody.string());
                if (json.has("message")) {
                    return json.getString("message");
                }
            } catch (Exception ignored) {}
        }
        return "Failed to update order status.";
    }
}
