package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.customer.CustomerHomeActivity;
import com.tiffincraft.app.activities.customer.CustomerMenuActivity;
import com.tiffincraft.app.activities.customer.CustomerProfileActivity;
import com.tiffincraft.app.activities.customer.FavoritesActivity;
import com.tiffincraft.app.adapters.CustomerOrderAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Order;
import com.tiffincraft.app.models.OrderResponse;
import com.tiffincraft.app.models.RegisterResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity {

    private static final String TAG = "OrderHistoryActivity";

    private ChipGroup chipGroupStatus;
    private RecyclerView rvOrderHistory;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnBrowseMeals;
    private BottomNavigationView bottomNavigation;

    private ApiService apiService;
    private SessionManager sessionManager;

    private CustomerOrderAdapter adapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        init();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        loadOrders();
    }

    private void init() {
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        btnBrowseMeals = findViewById(R.id.btnBrowseMeals);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        apiService = RetrofitClient.getInstance(this).getApiService();
        sessionManager = new SessionManager(this);
    }

    private void setupRecyclerView() {
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerOrderAdapter(displayedOrders,
                order -> {
                    Intent intent = new Intent(this, OrderDetailsCustomerActivity.class);
                    intent.putExtra("order_id", order.getId());
                    startActivity(intent);
                },
                order -> {
                    Intent intent = new Intent(this, TrackOrderActivity.class);
                    intent.putExtra("order_id", order.getId());
                    startActivity(intent);
                },
                this::confirmCancelOrder);
        rvOrderHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedId = checkedIds.get(0);
                filterOrders(selectedId);
            }
        });

        btnBrowseMeals.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerMenuActivity.class));
            finish();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_orders);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, CustomerHomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                startActivity(new Intent(this, CustomerMenuActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(this, com.tiffincraft.app.activities.common.CartActivity.class));
                return false;
            } else if (itemId == R.id.nav_orders) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, CustomerProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void loadOrders() {
        showLoading(true);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getCustomerOrders(token).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allOrders.clear();
                    if (response.body().getOrders() != null) {
                        allOrders.addAll(response.body().getOrders());
                    }
                    applyCurrentFilter();
                } else {
                    Log.e(TAG, "Failed to load orders: " + response.code());
                    showEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading orders", t);
                Toast.makeText(OrderHistoryActivity.this, "Error loading orders", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private void applyCurrentFilter() {
        int checkedId = chipGroupStatus.getCheckedChipId();
        filterOrders(checkedId);
    }

    private void filterOrders(int chipId) {
        displayedOrders.clear();

        for (Order order : allOrders) {
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
            boolean matches;
            if (chipId == R.id.chipActive) {
                matches = status.equals("pending") || status.equals("confirmed")
                        || status.equals("preparing") || status.equals("ready");
            } else if (chipId == R.id.chipCompleted) {
                matches = status.equals("delivered") || status.equals("completed");
            } else if (chipId == R.id.chipCancelled) {
                matches = status.equals("cancelled");
            } else {
                matches = true; // chipAll
            }
            if (matches) displayedOrders.add(order);
        }

        adapter.notifyDataSetChanged();
        showEmptyState(displayedOrders.isEmpty());
    }

    private void confirmCancelOrder(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order #" + order.getId() + "?")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> performCancelOrder(order))
                .setNegativeButton("No", null)
                .show();
    }

    private void performCancelOrder(Order order) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.cancelOrder(token, order.getId(), new JsonObject()).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OrderHistoryActivity.this, "Order cancelled successfully.", Toast.LENGTH_SHORT).show();
                    loadOrders();
                } else {
                    Toast.makeText(OrderHistoryActivity.this, buildCancelDeniedMessage(order, response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error cancelling order", t);
                Toast.makeText(OrderHistoryActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * The order's status may have moved past "pending" between page load and tap (e.g. the cook
     * just accepted it), so the client can't assume why the backend refused — it always defers to
     * the backend's decision. It only picks a friendlier wording than the backend's raw message
     * when the order status we last saw explains the refusal (anything other than "pending");
     * for the true pending-but-outside-the-10-minute-window case, the backend's own message is
     * shown verbatim since only the server can know the elapsed time authoritatively.
     */
    private String buildCancelDeniedMessage(Order order, Response<RegisterResponse> response) {
        String status = order.getStatus() != null ? order.getStatus().toLowerCase(Locale.US) : "";
        if (!"pending".equals(status)) {
            String label = status.isEmpty() ? "This" : Character.toUpperCase(status.charAt(0)) + status.substring(1);
            return label + " orders cannot be cancelled.";
        }

        try {
            if (response.errorBody() != null) {
                JsonObject err = new JsonParser().parse(response.errorBody().string()).getAsJsonObject();
                if (err.has("message") && !err.get("message").isJsonNull()) {
                    return err.get("message").getAsString();
                }
            }
        } catch (Exception ignored) { }

        return "This order can no longer be cancelled.";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvOrderHistory.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvOrderHistory.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_orders);
        }
    }
}
