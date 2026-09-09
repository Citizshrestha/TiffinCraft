package com.tiffincraft.app.utils;

import android.content.Context;

import androidx.core.app.NotificationManagerCompat;

/**
 * Compatibility helper for dismissing chat notifications created by older app
 * builds. New chat alerts are displayed exclusively by FcmService so the
 * Socket.IO event and FCM push cannot create two notifications for one message.
 */
public final class ChatNotifier {

    private ChatNotifier() {}

    /** Dismiss a legacy socket-generated notification for an opened conversation. */
    public static void cancel(Context context, int conversationId) {
        NotificationManagerCompat.from(context).cancel(conversationId);
    }
}
