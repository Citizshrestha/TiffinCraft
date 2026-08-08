package com.tiffincraft.app.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.ChatActivity;
import com.tiffincraft.app.activities.common.NotificationActivity;
import com.tiffincraft.app.activities.cook.CookHomeActivity;
import com.tiffincraft.app.activities.cook.ManageOrdersActivity;
import com.tiffincraft.app.activities.order.OrderDetailsCookActivity;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;

import java.util.Map;

/**
 * Firebase Cloud Messaging service.
 * Handles incoming push notifications when the app is in background or killed.
 * For foreground FCM messages, the SDK delivers them via onMessageReceived;
 * for background, the system creates a notification automatically from the
 * notification payload (title + body) — data payload is available here.
 */
public class FcmService extends FirebaseMessagingService {

    private static final String TAG = "FcmService";
    public static final String CHANNEL_ID = "tiffincraft_alerts";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "🔥 New FCM token: " + token);

        SocketManager socketManager = SocketManager.getInstance(this);
        socketManager.setFcmToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "📩 FCM message received");

        Map<String, String> data = remoteMessage.getData();
        String title = null;
        String body = null;
        String type = null;
        String orderIdStr = null;
        String conversationIdStr = null;

        // Extract from data payload first (server-sent), fall back to notification payload
        if (data != null && !data.isEmpty()) {
            title = data.containsKey("title") ? data.get("title") : null;
            body = data.containsKey("body") ? data.get("body") : null;
            type = data.get("type");
            orderIdStr = data.get("orderId");
            conversationIdStr = data.get("conversationId");
        }

        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
        }
        if (body == null && remoteMessage.getNotification() != null) {
            body = remoteMessage.getNotification().getBody();
        }
        if (title == null) title = "TiffinCraft";
        if (body == null) body = "";

        showNotification(title, body, type, orderIdStr, conversationIdStr);
    }

    /**
     * Build and display a heads-up system notification.
     * Deep-links based on the type:
     * - "new_order" → OrderDetailsCookActivity
     * - "cook_approval" → CookHomeActivity (shows celebratory dialog)
     * - "chat_message" → ChatActivity (specific conversation)
     * - default → ManageOrdersActivity
     */
    private void showNotification(String title, String body, String type,
                                  String orderIdStr, String conversationIdStr) {
        if (title == null) title = "TiffinCraft";
        if (body == null) body = "";

        Intent intent;
        int requestCode = 1000;

        if ("new_order".equals(type) && orderIdStr != null && !orderIdStr.isEmpty()) {
            try {
                int orderId = Integer.parseInt(orderIdStr);
                intent = new Intent(this, OrderDetailsCookActivity.class);
                intent.putExtra("order_id", orderId);
                requestCode = orderId;
            } catch (NumberFormatException e) {
                intent = new Intent(this, ManageOrdersActivity.class);
            }

        } else if ("cook_approval".equals(type)) {
            intent = new Intent(this, CookHomeActivity.class);
            intent.putExtra("show_approval_dialog", true);

        } else if ("chat_message".equals(type) && conversationIdStr != null && !conversationIdStr.isEmpty()) {
            try {
                int convId = Integer.parseInt(conversationIdStr);
                intent = new Intent(this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, convId);
                intent.putExtra(ChatActivity.EXTRA_CONTACT_NAME, title);
                requestCode = 10000 + convId;
            } catch (NumberFormatException e) {
                intent = new Intent(this, NotificationActivity.class);
            }

        } else {
            // Anything without a hardcoded deep-link above (commission/refund/
            // subscription events, reviews, etc.) — ManageOrdersActivity is
            // cook-only and would be the wrong screen for a customer-facing
            // type here. NotificationActivity is role-agnostic and already
            // knows how to route every type correctly once tapped there
            // (see NotificationActivity.navigateForNotification).
            intent = new Intent(this, NotificationActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                CHANNEL_ID,
                "TiffinCraft Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("New orders, order updates, and kitchen notifications");
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setShowBadge(true);

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                   == PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(this).notify(requestCode, builder.build());
            } catch (SecurityException ignored) { }
        }
    }
}
