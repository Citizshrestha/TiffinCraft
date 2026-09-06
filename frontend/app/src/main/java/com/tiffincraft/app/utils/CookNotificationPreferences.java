package com.tiffincraft.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.tiffincraft.app.session.SessionManager;

/**
 * Single source of truth for the cook-facing notification switches.
 * Preferences intentionally affect alerts (push, sound, banners), while the
 * notification inbox still keeps its history so important activity is not lost.
 */
public final class CookNotificationPreferences {

    public static final String PREFS_NAME = "NotificationPreferences";
    public static final String NEW_ORDERS = "new_orders";
    public static final String ORDER_STATUS = "order_status";
    public static final String CHAT_MESSAGES = "chat_messages";
    public static final String EARNINGS_SUMMARY = "earnings_summary";
    public static final String PROMOTIONS = "promotions";

    private CookNotificationPreferences() { }

    public static boolean get(Context context, String key) {
        return prefs(context).getBoolean(key, defaultFor(key));
    }

    public static void set(Context context, String key, boolean enabled) {
        prefs(context).edit().putBoolean(key, enabled).apply();
    }

    public static boolean alertsEnabledForType(Context context, String type) {
        // This settings screen belongs to cooks. Never carry a cook's device
        // preferences over to a customer session on the same phone.
        if (!"cook".equals(new SessionManager(context).getRole())) return true;
        if (type == null) return true;

        switch (type.toLowerCase()) {
            case "new_order":
                return get(context, NEW_ORDERS);
            case "order_status":
            case "order_cancelled":
            case "payment_verified":
            case "payment_rejected":
                return get(context, ORDER_STATUS);
            case "chat_message":
                return get(context, CHAT_MESSAGES);
            case "earnings_summary":
            case "weekly_earnings_summary":
                return get(context, EARNINGS_SUMMARY);
            case "promotion":
            case "promotions":
            case "offer":
            case "marketing":
                return get(context, PROMOTIONS);
            default:
                // Subscription requests, commission, account/security, and
                // other operational alerts are not controlled by these five switches.
                return true;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static boolean defaultFor(String key) {
        return !PROMOTIONS.equals(key);
    }
}
