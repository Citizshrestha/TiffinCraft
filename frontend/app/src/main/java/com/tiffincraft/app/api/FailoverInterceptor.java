package com.tiffincraft.app.api;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Self-healing network interceptor.
 *
 * WHY THIS EXISTS:
 * Even with startup discovery (see ServerConfig + SplashActivity), the
 * backend's tunnel URL can rotate WHILE the app is already running (tunnel
 * process restarts, PC sleeps/wakes, etc). Without this, every request made
 * with the stale cached URL would fail with a connection error, and the user
 * would see "backend error" on login/register until they force-close and
 * reopen the app.
 *
 * WHAT IT DOES:
 * If a request fails (IOException, e.g. host unreachable / DNS failure /
 * connection refused — exactly what happens when a loca.lt subdomain dies),
 * this interceptor:
 *   1. Runs a fresh discovery call against the PC's LAN IP (/api/config).
 *   2. If a new URL is found and differs from the one just used, rewrites
 *      the failed request to point at the new host and retries ONCE.
 *   3. Updates RetrofitClient.BASE_URL/SERVER_URL so subsequent requests
 *      (and image URL construction elsewhere in the app) use the fresh URL
 *      immediately, without requiring an app restart.
 *
 * This makes login/register (and every other API call) recover automatically
 * from a stale tunnel URL — the core permanent fix for the reported issue.
 */
class FailoverInterceptor implements Interceptor {

    private static final String TAG = "FailoverInterceptor";

    private final Context context;

    FailoverInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        try {
            return chain.proceed(originalRequest);
        } catch (IOException firstAttemptError) {
            Log.w(TAG, "Request failed (" + originalRequest.url() +
                    "), attempting failover discovery: " + firstAttemptError.getMessage());

            String newBaseUrl = ServerConfig.discoverAndCacheSync(context);

            if (newBaseUrl == null) {
                // Discovery itself failed (e.g. phone not on same WiFi right
                // now, or PC backend is genuinely down). Nothing we can do —
                // surface the original error.
                throw firstAttemptError;
            }

            HttpUrl newHttpBaseUrl = HttpUrl.parse(newBaseUrl);
            if (newHttpBaseUrl == null) {
                throw firstAttemptError;
            }

            // Keep RetrofitClient's public fields in sync so image URL
            // helpers (RetrofitClient.SERVER_URL + path) also use the fresh
            // host immediately.
            RetrofitClient.BASE_URL = newBaseUrl;
            RetrofitClient.SERVER_URL = ServerConfig.getCachedServerUrl(context);

            HttpUrl originalUrl = originalRequest.url();

            // If discovery returned the SAME host we already tried, there's
            // no point retrying — it would fail identically.
            boolean sameHost = originalUrl.host().equals(newHttpBaseUrl.host())
                    && originalUrl.scheme().equals(newHttpBaseUrl.scheme())
                    && originalUrl.port() == newHttpBaseUrl.port();
            if (sameHost) {
                throw firstAttemptError;
            }

            HttpUrl rewrittenUrl = originalUrl.newBuilder()
                    .scheme(newHttpBaseUrl.scheme())
                    .host(newHttpBaseUrl.host())
                    .port(newHttpBaseUrl.port())
                    .build();

            Request retryRequest = originalRequest.newBuilder()
                    .url(rewrittenUrl)
                    .build();

            Log.i(TAG, "Retrying with freshly discovered host: " + rewrittenUrl);
            return chain.proceed(retryRequest);
        }
    }
}
