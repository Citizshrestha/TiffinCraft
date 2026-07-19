package com.tiffincraft.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.tiffincraft.app.utils.ChatNotifier;

/**
 * Application entry point. TiffinCraft is a light-theme-only app, so night mode
 * is forced off here — this guarantees no screen ever resolves dark resources,
 * regardless of the device's system dark-mode setting.
 */
public class TiffinCraftApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        ChatNotifier.createChannel(this);
        createAlertChannel();
    }

    /**
     * Create the high-importance channel used by FCM push notifications
     * for new orders, order updates, and kitchen approvals.
     */
    private void createAlertChannel() {
        NotificationChannel channel = new NotificationChannel(
                com.tiffincraft.app.services.FcmService.CHANNEL_ID,
                "TiffinCraft Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("New orders, order updates, and kitchen notifications");
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setShowBadge(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
