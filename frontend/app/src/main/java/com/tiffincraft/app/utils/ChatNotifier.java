package com.tiffincraft.app.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.common.ChatActivity;
import com.tiffincraft.app.activities.common.NotificationActivity;

/**
 * In-app chat message notifications (heads-up + sound) driven by the
 * Socket.IO "chatNotification" event — no FCM involved, so they fire while
 * the app process is alive (foreground or backgrounded with a live socket).
 */
public final class ChatNotifier {

    public static final String CHANNEL_ID = "chat_messages";

    /** Conversation the user is currently looking at; its notifications are suppressed. */
    private static volatile int activeConversationId = 0;

    private ChatNotifier() {}

    public static void setActiveConversation(int conversationId) {
        activeConversationId = conversationId;
    }

    public static void clearActiveConversation() {
        activeConversationId = 0;
    }

    /** Call once from Application.onCreate — minSdk 26, so a channel is always required. */
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("New messages from cooks and customers");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }

    public static void showMessageNotification(Context context, int conversationId,
                                               String senderName, String preview) {
        if (!CookNotificationPreferences.alertsEnabledForType(context, "chat_message")) {
            return;
        }
        if (conversationId > 0 && conversationId == activeConversationId) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // conversation_id 0 marks a general alert (e.g. referral reward) — it
        // opens the bell/notifications screen instead of a chat thread.
        Intent intent;
        if (conversationId > 0) {
            intent = new Intent(context, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
            intent.putExtra(ChatActivity.EXTRA_CONTACT_NAME, senderName);
        } else {
            intent = new Intent(context, NotificationActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                conversationId, // distinct request code per conversation
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setColor(ContextCompat.getColor(context, R.color.forest))
                .setContentTitle(senderName != null && !senderName.isEmpty()
                        ? senderName : "New message")
                .setContentText(preview != null && !preview.isEmpty()
                        ? preview : "You have a new message")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(preview != null ? preview : ""))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            // One notification per conversation: a newer message replaces the older one.
            NotificationManagerCompat.from(context).notify(conversationId, builder.build());
        } catch (SecurityException ignored) {
            // Permission revoked between the check and notify — nothing to do.
        }
    }

    /** Dismiss the notification for a conversation the user just opened. */
    public static void cancel(Context context, int conversationId) {
        NotificationManagerCompat.from(context).cancel(conversationId);
    }
}
