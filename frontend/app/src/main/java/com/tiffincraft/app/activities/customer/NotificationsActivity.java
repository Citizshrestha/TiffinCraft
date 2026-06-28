package com.tiffincraft.app.activities.customer;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.adapters.NotificationAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.CustomerDashboardResponse;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {
    
    private static final String TAG = "NotificationsActivity";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), this::onNotificationClick);
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        String token = "Bearer " + sessionManager.getToken();

        Call<NotificationResponse> call = apiService.getNotifications(token);
        call.enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CustomerDashboardResponse.Notification> notifications = response.body().getNotifications();
                    
                    if (notifications != null && !notifications.isEmpty()) {
                        adapter.updateNotifications(notifications);
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e(TAG, "Failed to load notifications: " + response.code());
                    Toast.makeText(NotificationsActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Log.e(TAG, "Error loading notifications", t);
                Toast.makeText(NotificationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onNotificationClick(CustomerDashboardResponse.Notification notification) {
        // Mark notification as read
        String token = "Bearer " + sessionManager.getToken();
        
        Call<com.tiffincraft.app.models.RegisterResponse> call = apiService.markNotificationAsRead(token, notification.getId());
        call.enqueue(new Callback<com.tiffincraft.app.models.RegisterResponse>() {
            @Override
            public void onResponse(Call<com.tiffincraft.app.models.RegisterResponse> call, Response<com.tiffincraft.app.models.RegisterResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification marked as read");
                    // Reload notifications
                    loadNotifications();
                }
            }

            @Override
            public void onFailure(Call<com.tiffincraft.app.models.RegisterResponse> call, Throwable t) {
                Log.e(TAG, "Failed to mark notification as read", t);
            }
        });

        // Handle different notification types
        if (notification.getOrderId() != null) {
            // Navigate to order details
            Toast.makeText(this, "Navigate to order #" + notification.getOrderId(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, notification.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
