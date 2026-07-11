package com.tiffincraft.app.activities.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.customer.CustomerHomeActivity;
import com.tiffincraft.app.activities.customer.CustomerMenuActivity;
import com.tiffincraft.app.activities.customer.CustomerProfileActivity;
import com.tiffincraft.app.activities.customer.FavoritesActivity;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.session.SessionManager;

public class OrderHistoryActivity extends AppCompatActivity {
    
    private ChipGroup chipGroupStatus;
    private RecyclerView rvOrderHistory;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnBrowseMeals;
    private BottomNavigationView bottomNavigation;
    
    private ApiService apiService;
    private SessionManager sessionManager;
    
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
        // TODO: Set adapter when OrderAdapter is ready
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
            } else if (itemId == R.id.nav_orders) {
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                finish();
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
        
        // TODO: Replace with actual API call
        // For now, show empty state
        showLoading(false);
        showEmptyState(true);
        
        /*
        // Example API call structure:
        String token = "Bearer " + sessionManager.getToken();
        apiService.getOrders(token).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Update adapter with orders
                    showEmptyState(orders.isEmpty());
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(OrderHistoryActivity.this, "Error loading orders", Toast.LENGTH_SHORT).show();
            }
        });
        */
    }
    
    private void filterOrders(int chipId) {
        if (chipId == R.id.chipAll) {
            Toast.makeText(this, "Showing all orders", Toast.LENGTH_SHORT).show();
        } else if (chipId == R.id.chipActive) {
            Toast.makeText(this, "Showing active orders", Toast.LENGTH_SHORT).show();
        } else if (chipId == R.id.chipCompleted) {
            Toast.makeText(this, "Showing completed orders", Toast.LENGTH_SHORT).show();
        } else if (chipId == R.id.chipCancelled) {
            Toast.makeText(this, "Showing cancelled orders", Toast.LENGTH_SHORT).show();
        }
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
