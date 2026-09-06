package com.tiffincraft.app.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private TextView btnMarkAllRead;

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
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        btnBack.setOnClickListener(v -> finish());
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        updateMarkAllReadAction();
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
            case "payment_success":
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
            case "commission_due":
            case "commission_verified":
            case "commission_rejected":
            case "commission_submitted":
            case "commission_rate_change":
                // The screen fetches the cook's current/past-due settlement itself
                // on open, so refId is passed only as a hint for the day a cook has
                // more than one open settlement. A rate change lands here too: the
                // new rate only matters in the context of what is owed.
                intent = new Intent(this, com.tiffincraft.app.activities.cook.CommissionSettlementActivity.class);
                if (refId != null) {
                    intent.putExtra("settlement_id", (int) refId);
                }
                break;
            case "refund_feedback":
            case "refund_status":
                // Sent to the cook (refund_feedback) or customer (refund_status)
                // with the order id as the reference — same order details screen
                // as the order-status types above.
                if (refId != null) {
                    intent = new Intent(this, isCook ? OrderDetailsCookActivity.class : OrderDetailsCustomerActivity.class);
                    intent.putExtra("order_id", refId);
                }
                break;
            case "subscription_payment_submitted":
                // Cook: a customer submitted payment proof — open straight into
                // the "Needs Review" filter instead of the generic "All" list.
                intent = new Intent(this, com.tiffincraft.app.activities.cook.CookSubscribersActivity.class);
                intent.putExtra(com.tiffincraft.app.activities.cook.CookSubscribersActivity.EXTRA_INITIAL_FILTER, "submitted");
                break;
            case "subscription_verified":
            case "subscription_rejected":
            case "subscription_accepted":
            case "subscription_paid":
            case "subscription_payment_rejected":
            case "subscription_scheduled":
            case "subscription_completed":
            case "subscription_update":
            case "subscription_paused":
            case "subscription_cancelled":
            case "custom_meal_accepted":
            case "custom_meal_declined":
                // Customer: their profile's subscription card already reflects the
                // live status and re-opens SubscriptionPaymentActivity via "Manage"
                // if action is still needed — reuses that flow instead of trying to
                // reconstruct plan/price/QR details from just a notification.
                // The custom_meal_* replies land here too: their referenceId is a
                // custom_meal_requests row, so there is no subscription id to open
                // a calendar with (see the trap noted below).
                intent = new Intent(this, com.tiffincraft.app.activities.customer.CustomerProfileActivity.class);
                break;
            case "subscription_request":
                // Cook: the request that needs answering. The inbox scrolls to and
                // flashes this subscription's card rather than dumping a list.
                if (refId != null) {
                    intent = com.tiffincraft.app.activities.cook.SubscriptionRequestsActivity
                            .intentFor(this, refId);
                }
                break;
            case "subscription_meal_sent":
            case "subscription_meal_received":
            case "subscription_day_skipped":
            case "subscription_delivery_skipped":
            case "cook_unavailable":
                // Day-level events belong on the calendar, and it works for both
                // roles (the response's `viewer` field drives read-only mode).
                // Guarded on reference_type: some subscription-family types carry a
                // request id rather than a subscription id, and a calendar keyed on
                // the wrong id opens somebody else's schedule.
                if (refId != null && "subscription".equals(notification.getReferenceType())) {
                    intent = com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity
                            .intentFor(this, refId, null);
                }
                break;
            case "custom_meal_request":
                // referenceId here is a custom_meal_requests row id, NOT a
                // subscription id — must not go to the calendar.
                intent = new Intent(this, com.tiffincraft.app.activities.cook.CookSubscribersActivity.class);
                break;
            default:
                break; // system/unrecognized (e.g. admin-only refund_requested) — nothing to navigate to
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
                    updateMarkAllReadAction();
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
                    updateMarkAllReadAction();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to mark as read: " + t.getMessage());
            }
        });
    }

    private void markAllAsRead() {
        if (!hasUnreadNotifications()) return;

        btnMarkAllRead.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();
        apiService.markAllUserNotificationsAsRead(token).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    for (Notification notification : notificationList) {
                        notification.setIsRead(true);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(NotificationActivity.this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NotificationActivity.this, "Could not mark notifications as read", Toast.LENGTH_SHORT).show();
                }
                updateMarkAllReadAction();
            }

            @Override
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to mark all notifications as read: " + t.getMessage());
                Toast.makeText(NotificationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                updateMarkAllReadAction();
            }
        });
    }

    private boolean hasUnreadNotifications() {
        for (Notification notification : notificationList) {
            if (!notification.isRead()) return true;
        }
        return false;
    }

    private void updateMarkAllReadAction() {
        boolean hasUnread = hasUnreadNotifications();
        btnMarkAllRead.setEnabled(hasUnread);
        btnMarkAllRead.setAlpha(hasUnread ? 1f : 0.45f);
    }
}
