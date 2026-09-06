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
import com.tiffincraft.app.activities.cook.SubscriptionRequestsActivity;
import com.tiffincraft.app.activities.order.OrderDetailsCookActivity;
import com.tiffincraft.app.session.SessionManager;
import com.tiffincraft.app.utils.SocketManager;
import com.tiffincraft.app.utils.CookNotificationPreferences;

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
        String subscriptionIdStr = null;
        String requestIdStr = null;

        // Extract from data payload first (server-sent), fall back to notification payload
        if (data != null && !data.isEmpty()) {
            title = data.containsKey("title") ? data.get("title") : null;
            body = data.containsKey("body") ? data.get("body") : null;
            type = data.get("type");
            orderIdStr = data.get("orderId");
            conversationIdStr = data.get("conversationId");
            // announceSubscriptionEvent puts this on every subscription push
            // (utils/subscriptionEvents.js) — without reading it, every
            // subscription alert fell through to the generic list.
            subscriptionIdStr = data.get("subscriptionId");
            // custom_meal_request / accepted / declined also carry requestId
            requestIdStr = data.get("requestId");
        }

        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
        }
        if (body == null && remoteMessage.getNotification() != null) {
            body = remoteMessage.getNotification().getBody();
        }
        if (title == null) title = "TiffinCraft";
        if (body == null) body = "";

        if (!CookNotificationPreferences.alertsEnabledForType(this, type)) {
            Log.d(TAG, "Push suppressed by cook notification preference: " + type);
            return;
        }

        showNotification(title, body, type, orderIdStr, conversationIdStr, subscriptionIdStr, requestIdStr);
    }

    /**
     * Build and display a heads-up system notification.
     * Deep-links based on the type:
     * - "new_order"               → OrderDetailsCookActivity
     * - "cook_approval"           → CookHomeActivity (shows celebratory dialog)
     * - "chat_message"            → ChatActivity (specific conversation)
     * - "subscription_request" / proof types → SubscriptionRequestsActivity (cook)
     * - "subscription_day_skipped" → SubscriptionCalendarActivity
     * - "custom_meal_request"     → SubscriptionRequestsActivity (cook inbox)
     * - "custom_meal_accepted" / "custom_meal_declined" → SubscriptionCalendarActivity (customer)
     * - default                   → NotificationActivity
     */
    private void showNotification(String title, String body, String type,
                                  String orderIdStr, String conversationIdStr,
                                  String subscriptionIdStr, String requestIdStr) {
        if (title == null) title = "TiffinCraft";
        if (body == null) body = "";

        Intent intent;
        // requestCode doubles as the system notification ID. Using a unique
        // value per notification means consecutive alerts (e.g. several
        // skipped-day pushes) stack in the shade rather than overwriting the
        // previous one.  The ranges below are spaced far enough apart that
        // they never collide across notification types.
        int requestCode;

        if ("new_order".equals(type) && orderIdStr != null && !orderIdStr.isEmpty()) {
            try {
                int orderId = Integer.parseInt(orderIdStr);
                intent = new Intent(this, OrderDetailsCookActivity.class);
                intent.putExtra("order_id", orderId);
                requestCode = orderId;                    // range: order IDs
            } catch (NumberFormatException e) {
                intent = new Intent(this, ManageOrdersActivity.class);
                requestCode = (int) System.currentTimeMillis();
            }

        } else if (isCookSubscriptionInbox(type)
                && subscriptionIdStr != null && !subscriptionIdStr.isEmpty()) {
            // Cook-facing subscription work: land on the request itself, scrolled
            // to and highlighted, instead of the flat notification list.
            try {
                int subscriptionId = Integer.parseInt(subscriptionIdStr);
                intent = SubscriptionRequestsActivity.intentFor(this, subscriptionId);
                requestCode = 20000 + subscriptionId;    // range: 20 000 +
            } catch (NumberFormatException e) {
                intent = new Intent(this, NotificationActivity.class);
                requestCode = (int) System.currentTimeMillis();
            }

        } else if ("custom_meal_request".equals(type)
                && subscriptionIdStr != null && !subscriptionIdStr.isEmpty()) {
            // Cook receives this when a customer requests a different meal.
            // Land on the subscription requests inbox scrolled to this subscription
            // so the cook can tap Accept/Decline immediately.
            try {
                int subscriptionId = Integer.parseInt(subscriptionIdStr);
                intent = SubscriptionRequestsActivity.intentFor(this, subscriptionId);
                // Use requestId for uniqueness so back-to-back requests for the
                // same subscription don't collapse into a single notification.
                int rId = 0;
                try { rId = requestIdStr != null ? Integer.parseInt(requestIdStr) : 0; } catch (NumberFormatException ignored) {}
                requestCode = 50000 + (rId > 0 ? rId : (int) (System.currentTimeMillis() % 10000));
            } catch (NumberFormatException e) {
                intent = new Intent(this, NotificationActivity.class);
                requestCode = (int) System.currentTimeMillis();
            }

        } else if (("custom_meal_accepted".equals(type) || "custom_meal_declined".equals(type))
                && subscriptionIdStr != null && !subscriptionIdStr.isEmpty()) {
            // Customer receives the cook's response. Open the calendar for that
            // subscription so the customer can see the updated day status.
            try {
                int subscriptionId = Integer.parseInt(subscriptionIdStr);
                intent = com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity
                        .intentFor(this, subscriptionId, null);
                int rId = 0;
                try { rId = requestIdStr != null ? Integer.parseInt(requestIdStr) : 0; } catch (NumberFormatException ignored) {}
                requestCode = 60000 + (rId > 0 ? rId : (int) (System.currentTimeMillis() % 10000));
            } catch (NumberFormatException e) {
                intent = new Intent(this, NotificationActivity.class);
                requestCode = (int) System.currentTimeMillis();
            }

        } else if ("subscription_day_skipped".equals(type)
                && subscriptionIdStr != null && !subscriptionIdStr.isEmpty()) {
            // Cook receives this when a customer skips a delivery day.
            // Land on the subscription calendar so the cook can see which
            // day was skipped at a glance.
            try {
                int subscriptionId = Integer.parseInt(subscriptionIdStr);
                intent = com.tiffincraft.app.activities.customer.SubscriptionCalendarActivity
                        .intentFor(this, subscriptionId, null);
                // Use a time-based suffix so two rapid skip pushes for the
                // same subscription don't collapse to a single notification.
                requestCode = 30000 + (int) (System.currentTimeMillis() % 10000);
            } catch (NumberFormatException e) {
                intent = new Intent(this, NotificationActivity.class);
                requestCode = (int) System.currentTimeMillis();
            }

        } else if ("cook_approval".equals(type)) {
            intent = new Intent(this, CookHomeActivity.class);
            intent.putExtra("show_approval_dialog", true);
            requestCode = 40000;

        } else if ("chat_message".equals(type) && conversationIdStr != null && !conversationIdStr.isEmpty()) {
            try {
                int convId = Integer.parseInt(conversationIdStr);
                intent = new Intent(this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, convId);
                intent.putExtra(ChatActivity.EXTRA_CONTACT_NAME, title);
                requestCode = 10000 + convId;            // range: 10 000 +
            } catch (NumberFormatException e) {
                intent = new Intent(this, NotificationActivity.class);
                requestCode = (int) System.currentTimeMillis();
            }

        } else {
            // Anything without a hardcoded deep-link above (commission/refund/
            // subscription verified/rejected, reviews, etc.) — NotificationActivity
            // is role-agnostic and already routes every type correctly once tapped
            // (see NotificationActivity.navigateForNotification).
            // Use a time-based ID so multiple general notifications stack.
            intent = new Intent(this, NotificationActivity.class);
            requestCode = (int) System.currentTimeMillis();
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

    /**
     * Types whose recipient is the cook and whose next step lives in the
     * subscription requests inbox. Deliberately narrow: customer-facing
     * subscription types (verified/rejected/paid) belong on the customer's own
     * screens, and NotificationActivity routes those correctly once tapped.
     */
    private static boolean isCookSubscriptionInbox(String type) {
        if (type == null) return false;
        switch (type) {
            case "subscription_request":
            case "subscription_payment_submitted":
                return true;
            default:
                return false;
        }
    }
}
