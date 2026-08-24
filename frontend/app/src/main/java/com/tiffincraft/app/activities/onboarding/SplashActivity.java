package com.tiffincraft.app.activities.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.tiffincraft.app.R;
import com.tiffincraft.app.activities.cook.CookHomeActivity;
import com.tiffincraft.app.activities.customer.CustomerHomeActivity;
import com.tiffincraft.app.api.RetrofitClient;
import com.tiffincraft.app.api.ServerConfig;
import com.tiffincraft.app.session.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    // Must be >= ServerConfig's own discovery timeout (3000ms) so we never
    // cut the network call off early and fall back to a stale cached URL
    // when a fresh one was actually about to arrive.
    private static final int DISCOVERY_WAIT_MS = 3500;

    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final long startTime = System.currentTimeMillis();
        executor = Executors.newSingleThreadExecutor();

        // Startup URL discovery. Only does real work when LAN discovery has been
        // explicitly enabled (see ServerConfig) — against the hosted backend it
        // returns null immediately, which is why the resetInstance() below is
        // the part that still matters: it rebuilds Retrofit from whatever
        // address is currently cached.
        Future<String> discoveryFuture = executor.submit(
                () -> ServerConfig.discoverAndCacheSync(getApplicationContext()));

        executor.submit(() -> {
            try {
                discoveryFuture.get(DISCOVERY_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                // Discovery failed or timed out (phone off PC's WiFi, PC
                // asleep, etc). We proceed with whatever URL was already
                // cached from a previous successful discovery / the
                // compiled-in default — FailoverInterceptor will still
                // recover mid-session if that turns out to be stale.
            }

            // Rebuild the Retrofit singleton so it picks up whatever URL is
            // now cached (freshly discovered, or previous cache untouched).
            RetrofitClient.resetInstance();

            long elapsed = System.currentTimeMillis() - startTime;
            long remainingDelay = Math.max(0, SPLASH_DELAY - elapsed);

            new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, remainingDelay);
        });
    }

    private void navigateNext() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        SessionManager sessionManager = new SessionManager(SplashActivity.this);

        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getRole();
            Intent intent;
            if ("cook".equals(role)) {
                intent = new Intent(SplashActivity.this, CookHomeActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, CustomerHomeActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Intent i = new Intent(SplashActivity.this, OnboardingActivity1.class);
            startActivity(i);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
