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

import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.OrderAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.databinding.ActivityManageOrdersBinding;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.UpdateOrderStatusRequest;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageOrdersActivity extends AppCompatActivity
        implements OrderAdapter.OnOrderActionListener {

    private static final String TAG = "ManageOrdersActivity";

    private ActivityManageOrdersBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

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

        setupRecyclerView();
        setupFilterTabs();
        setupClickListeners();
        setupBottomNavigation();
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_orders);
        }
        loadOrders();
    }

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
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRefresh.setOnClickListener(v -> {
            binding.btnRefresh.animate().rotation(360f).setDuration(400).start();
            loadOrders();
        });

        binding.swipeRefresh.setColorSchemeColors(0xFF4CAF50);
        binding.swipeRefresh.setOnRefreshListener(this::loadOrders);
    }

    private void setupFilterTabs() {
        View[] tabs = {
                binding.tabAll, binding.tabNew, binding.tabAccepted,
                binding.tabPreparing, binding.tabDelivery, binding.tabDone
        };
        String[] filters = { null, "pending", "confirmed", "preparing", "ready", "delivered" };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            final String filter = filters[i];
            tabs[i].setOnClickListener(v -> {
                currentFilter = filter;
                for (View tab : tabs) setTabSelected(tab, false);
                setTabSelected(tabs[index], true);
                applyFilter();
            });
        }
        setTabSelected(binding.tabAll, true);
    }

    private void setTabSelected(View tab, boolean selected) {
        if (!(tab instanceof TextView)) return;
        TextView tv = (TextView) tab;
        if (selected) {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(0xFF4CAF50);
            gd.setCornerRadius(dpToPx(18));
            tv.setBackground(gd);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(0xFFF2F2F2);
            gd.setCornerRadius(dpToPx(18));
            tv.setBackground(gd);
            tv.setTextColor(0xFF555555);
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
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
                            Toast.makeText(ManageOrdersActivity.this,
                                    "Failed to update order", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                        Toast.makeText(ManageOrdersActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
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
}
