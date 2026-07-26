package com.tiffincraft.app.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.cook.CookProfileActivity;
import com.tiffincraft.app.activities.cook.CookReviewsActivity;
import com.tiffincraft.app.activities.order.OrderDetailsCookActivity;
import com.tiffincraft.app.activities.order.OrderDetailsCustomerActivity;
import com.tiffincraft.app.adapters.NotificationAdapter;
import com.tiffincraft.app.api.ApiService;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.models.Notification;
import com.tiffincraft.app.models.NotificationResponse;
import com.tiffincraft.app.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private RecyclerView rvNotifications;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        notificationList = new ArrayList<>();

        initViews();
        setupRecyclerView();
        fetchNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notificationList, (notification, position) -> {
            if (!notification.isRead()) {
                markAsRead(notification.getId(), position);
            }
            navigateForNotification(notification);
        });
        rvNotifications.setAdapter(adapter);
    }

    /** Routes to wherever this notification is actually about — mirrors how
     *  Gmail/most professional apps behave (tap a notification, land on the
     *  thing it describes, not just a flat list). */
    private void navigateForNotification(Notification notification) {
        String type = notification.getType() != null ? notification.getType() : "";
        Integer refId = notification.getReferenceId();
        boolean isCook = "cook".equals(sessionManager.getRole());

        Intent intent = null;

        switch (type) {
            case "new_order":
            case "order_status":
            case "payment_verified":
            case "payment_verification":
            case "payment_rejected":
                if (refId != null) {
                    intent = new Intent(this, isCook ? OrderDetailsCookActivity.class : OrderDetailsCustomerActivity.class);
                    intent.putExtra("order_id", refId);
                }
                break;
            case "review":
                if (isCook) {
                    intent = new Intent(this, CookReviewsActivity.class);
                }
                // No single-review view exists on the customer side yet — a
                // "reply" notification just stays on this list for them.
                break;
            case "cook_approved":
            case "cook_rejected":
                intent = new Intent(this, CookProfileActivity.class);
                break;
            case "chat_message":
                if (refId != null) {
                    intent = new Intent(this, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, refId);
                }
                break;
            default:
                break; // system/unrecognized — nothing to navigate to
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private void fetchNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        String token = "Bearer " + sessionManager.getToken();

        apiService.getNotifications(token).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    notificationList.clear();
                    if (response.body().getNotifications() != null) {
                        notificationList.addAll(response.body().getNotifications());
                    }
                    
                    if (notificationList.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(NotificationActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                Log.e(TAG, "Error fetching notifications: " + t.getMessage());
                Toast.makeText(NotificationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAsRead(int notificationId, int position) {
        String token = "Bearer " + sessionManager.getToken();

        apiService.markNotificationAsRead(token, notificationId).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    adapter.markAsRead(position);
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to mark as read: " + t.getMessage());
            }
        });
    }
}
