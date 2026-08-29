package com.tiffincraft.app.activities.cook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.OrderAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.activities.order.OrderDetailsCookActivity;
import com.tiffincraft.app.databinding.ActivityManageOrdersBinding;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.UpdateOrderStatusRequest;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageOrdersActivity extends AppCompatActivity
        implements OrderAdapter.OnOrderActionListener {

    private static final String TAG = "ManageOrdersActivity";

    private ActivityManageOrdersBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private SocketManager socketManager;

    private OrderAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private String currentFilter = null; // null = all

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        socketManager = SocketManager.getInstance(this);

        setupRecyclerView();
        setupFilterTabs();
        setupClickListeners();
        setupBottomNavigation();
        setupSocketListeners();
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_orders);
        }
        loadOrders();

        // Connect socket and re-claim the order events for this screen
        connectSocket();
        setupSocketListeners();
    }

    // Note: no socket disconnect in onPause — SocketManager is an app-wide
    // singleton, so disconnecting here killed real-time chat/order updates for
    // every other screen. The socket now lives until logout.

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new OrderAdapter(this, new ArrayList<>(), this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
        binding.rvOrders.setHasFixedSize(false);
    }

    private void setupClickListeners() {
        binding.btnRefresh.setOnClickListener(v -> {
            binding.btnRefresh.setIconResource(R.drawable.ic_refresh);
            loadOrders();
        });

        binding.swipeRefresh.setColorSchemeColors(0xFF4CAF50);
        binding.swipeRefresh.setOnRefreshListener(this::loadOrders);
    }

    private void setupFilterTabs() {
        MaterialButton[] tabs = {
                binding.tabAll, binding.tabNew, binding.tabAccepted, binding.tabPreparing, binding.tabCompleted
        };
        // Order statuses are pending/confirmed/preparing/ready/delivered/cancelled —
        // there is no "completed" status, so the "Completed" tab must filter "delivered".
        String[] filters = { null, "pending", "preparing", "ready", "delivered" };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            final String filter = filters[i];
            tabs[i].setOnClickListener(v -> {
                currentFilter = filter;
                for (MaterialButton tab : tabs) setTabSelected(tab, false);
                setTabSelected(tabs[index], true);
                applyFilter();
            });
        }
        setTabSelected(binding.tabAll, true);
    }

    private void setTabSelected(MaterialButton tab, boolean selected) {
        if (selected) {
            tab.setBackgroundColor(0xFF4CAF50);
            tab.setTextColor(0xFFFFFFFF);
            tab.setIconTint(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            tab.setStrokeWidth(0);
        } else {
            tab.setBackgroundColor(0xFFFFFFFF);
            tab.setTextColor(0xFF111111);
            tab.setIconTint(android.content.res.ColorStateList.valueOf(0xFF111111));
            tab.setStrokeWidth(dpToPx(1));
            tab.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
        }
    }

    private void setupBottomNavigation() {
        if (binding.bottomNavigation == null) return;
        binding.bottomNavigation.setSelectedItemId(R.id.nav_orders);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, CookHomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_meals) {
                startActivity(new Intent(this, CookMealActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                return true; // already here
            } else if (id == R.id.nav_earnings) {
                startActivity(new Intent(this, CookEarningsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, CookProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data Loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadOrders() {
        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            showEmpty(true);
            return;
        }

        showLoading(true);
        String authHeader = "Bearer " + token;
        Log.d(TAG, "Loading cook orders for user: " + sessionManager.getUserId());

        apiService.getCookOrders(authHeader).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call,
                                   @NonNull Response<OrderResponse> response) {
                showLoading(false);
                Log.d(TAG, "Orders response code: " + response.code());

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {

                    allOrders = response.body().getOrders();
                    if (allOrders == null) allOrders = new ArrayList<>();
                    Log.d(TAG, "Loaded " + allOrders.size() + " orders");
                    updateStatsBar();
                    applyFilter();

                } else {
                    String err = "Failed to load orders";
                    if (response.code() == 401) err = "Session expired. Please login again.";
                    else if (response.code() == 403) err = "Access denied.";
                    else if (response.body() != null && response.body().getMessage() != null) {
                        err = response.body().getMessage();
                    }
                    Log.e(TAG, "Error: " + response.code() + " body=" + response.errorBody());
                    Toast.makeText(ManageOrdersActivity.this, err, Toast.LENGTH_SHORT).show();
                    showEmpty(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(ManageOrdersActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty(true);
            }
        });
    }

    private void applyFilter() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : allOrders) {
            if (currentFilter == null || currentFilter.equals(o.getStatus())) {
                filtered.add(o);
            }
        }
        adapter.updateOrders(filtered);
        showEmpty(filtered.isEmpty());
    }

    private void updateStatsBar() {
        int newCount = 0, activeCount = 0, doneCount = 0;
        for (Order o : allOrders) {
            switch (o.getStatus() == null ? "" : o.getStatus()) {
                case "pending":          newCount++;    break;
                case "confirmed":
                case "preparing":
                case "ready":            activeCount++; break;
                case "delivered":        doneCount++;   break;
            }
        }
        binding.tvCountNew.setText(String.valueOf(newCount));
        binding.tvCountActive.setText(String.valueOf(activeCount));
        binding.tvCountDone.setText(String.valueOf(doneCount));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order Action (status update)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onActionClick(Order order, String nextStatus) {
        String token = "Bearer " + sessionManager.getToken();
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(nextStatus);

        apiService.updateOrderStatus(token, order.getId(), request)
                .enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderResponse> call,
                                           @NonNull Response<OrderResponse> response) {
                        if (response.isSuccessful()) {
                            String label = statusLabel(nextStatus);
                            Toast.makeText(ManageOrdersActivity.this,
                                    "Order #" + order.getId() + " → " + label,
                                    Toast.LENGTH_SHORT).show();
                            loadOrders(); // Refresh
                        } else {
                            // Retrofit leaves response.body() null on non-2xx — the real reason
                            // (e.g. "Cannot mark as delivered until payment is verified") lives in
                            // errorBody() and was previously discarded in favor of a generic toast.
                            Toast.makeText(ManageOrdersActivity.this,
                                    extractErrorMessage(response.errorBody()), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                        Toast.makeText(ManageOrdersActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
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
        return "Failed to update order";
    }

    @Override
    public void onVerifyPaymentClick(Order order) {
        // Opens the full order details screen, where the cook can actually see
        // the uploaded screenshot before confirming it's genuine — verifying
        // blind from the list row would defeat the point of the check.
        Intent intent = new Intent(this, OrderDetailsCookActivity.class);
        intent.putExtra("order_id", order.getId());
        startActivity(intent);
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, OrderDetailsCookActivity.class);
        intent.putExtra("order_id", order.getId());
        startActivity(intent);
    }

    @Override
    public void onTrackOrder(Order order) {
        Intent intent = new Intent(this, com.tiffincraft.app.activities.order.TrackOrderActivity.class);
        intent.putExtra("order_id", order.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteOrder(Order order) {
        String token = "Bearer " + sessionManager.getToken();

        apiService.deleteOrder(token, order.getId())
                .enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call,
                                           @NonNull Response<com.tiffincraft.app.models.RegisterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(ManageOrdersActivity.this,
                                    "Order #" + order.getId() + " deleted successfully",
                                    Toast.LENGTH_SHORT).show();
                            
                            // Remove order from list and update UI
                            allOrders.remove(order);
                            applyFilter();
                        } else {
                            String errorMsg = "Failed to delete order";
                            if (response.body() != null && response.body().getMessage() != null) {
                                errorMsg = response.body().getMessage();
                            }
                            Toast.makeText(ManageOrdersActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.tiffincraft.app.models.RegisterResponse> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(ManageOrdersActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String statusLabel(String status) {
        switch (status) {
            case "confirmed":        return "Confirmed";
            case "preparing":        return "Preparing";
            case "ready":            return "Ready for Delivery";
            case "delivered":        return "Delivered";
            default:                 return status;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        binding.swipeRefresh.setRefreshing(false);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.rvOrders.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.GONE);
        } else {
            binding.rvOrders.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(boolean empty) {
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Socket.IO Real-time Updates
    // ─────────────────────────────────────────────────────────────────────────

    private void connectSocket() {
        if (socketManager != null) {
            socketManager.connect();

            // Join cook room
            String userIdStr = sessionManager.getUserId();
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    int cookId = Integer.parseInt(userIdStr);
                    socketManager.joinCookRoom(cookId);
                    Log.d(TAG, "Joined cook room for real-time order updates: " + cookId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid user ID format: " + userIdStr);
                }
            }
        }
    }

    private void setupSocketListeners() {
        if (socketManager == null) return;

        // Listen for new orders
        socketManager.onNewOrder(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        if (args.length > 0 && args[0] != null) {
                            JSONObject data = (JSONObject) args[0];
                            handleNewOrderEvent(data);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling new order event", e);
                    }
                });
            }
        });

        // Listen for cancelled orders
        socketManager.onOrderCancelled(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        if (args.length > 0 && args[0] != null) {
                            JSONObject data = (JSONObject) args[0];
                            handleOrderCancelledEvent(data);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling order cancelled event", e);
                    }
                });
            }
        });
    }

    private void handleNewOrderEvent(JSONObject data) {
        try {
            int orderId = data.optInt("orderId");
            double totalAmount = data.optDouble("total_amount", 0);

            Log.d(TAG, "🔔 New Order Event! Order #" + orderId);

            // Show toast
            Toast.makeText(this,
                    "🔔 New Order #" + orderId + " - ₹" + String.format("%.0f", totalAmount),
                    Toast.LENGTH_LONG).show();

            // Play notification sound and vibrate
            playNotificationSound();

            // Refresh orders list
            loadOrders();

        } catch (Exception e) {
            Log.e(TAG, "Error processing new order event", e);
        }
    }

    private void handleOrderCancelledEvent(JSONObject data) {
        try {
            int orderId = data.optInt("orderId");
            String message = data.optString("message", "Order cancelled");

            Log.d(TAG, "Order #" + orderId + " cancelled by customer");

            Toast.makeText(this,
                    "Order #" + orderId + " was cancelled by customer",
                    Toast.LENGTH_SHORT).show();

            // Refresh orders list
            loadOrders();

        } catch (Exception e) {
            Log.e(TAG, "Error processing order cancelled event", e);
        }
    }

    private void playNotificationSound() {
        try {
            // Vibrate
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(500);
                }
            }

            // Play default notification sound
            android.media.RingtoneManager.getRingtone(
                    this,
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            ).play();

        } catch (Exception e) {
            Log.e(TAG, "Error playing notification", e);
        }
    }
}
