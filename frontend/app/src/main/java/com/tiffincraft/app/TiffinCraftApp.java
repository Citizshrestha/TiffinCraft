package com.tiffincraft.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

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
    }
}
