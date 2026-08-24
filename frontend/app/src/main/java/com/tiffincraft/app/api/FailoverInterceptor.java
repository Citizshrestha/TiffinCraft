package com.tiffincraft.app.api;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Retry-and-failover network interceptor.
 *
 * TWO JOBS, ONE OF WHICH IS NOW DORMANT:
 *
 * 1. Gateway-error retry (still active, still useful). A 502/503/504 is a valid
 *    HTTP response, not an IOException, so it would otherwise sail past as a
 *    "successful" gateway error. The hosted backend runs on a free tier that
 *    spins down when idle, so the first request after a quiet spell can land
 *    mid-wake. One same-request retry clears most of those.
 *
 * 2. Host failover (dormant by default). This was built for localtunnel, whose
 *    URL rotated whenever the tunnel process restarted; on failure it would
 *    re-run discovery against the dev PC's LAN IP and retry against whatever
 *    address came back. The backend now lives at a stable HTTPS address, so
 *    {@link ServerConfig#discoverAndCacheSync} returns null unless LAN
 *    discovery has been explicitly enabled, and this path becomes a no-op that
 *    rethrows the original error.
 *
 * Leaving (2) in place costs nothing and means pointing the app back at a
 * laptop is a one-line toggle rather than a revert.
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
            Response response = chain.proceed(originalRequest);

            // loca.lt (the free dev tunnel) intermittently answers with a bare
            // 502/503/504 instead of dropping the connection — that's a valid
            // HTTP response, not an IOException, so it would otherwise sail
            // straight past this interceptor as a "successful" gateway error.
            // One same-request retry clears most of these transient blips.
            if (isRetryableGatewayError(response.code())) {
                Log.w(TAG, "Gateway error " + response.code() + " from " + originalRequest.url() +
                        " — retrying once");
                response.close();
                Response retryResponse = chain.proceed(originalRequest);
                if (!isRetryableGatewayError(retryResponse.code())) {
                    return retryResponse;
                }
                if (!ServerConfig.isLanDiscoveryEnabled(context)) {
                    // No alternative host exists to fail over to, so return the
                    // real gateway response rather than synthesising an
                    // IOException from it. Callers can then surface "server
                    // unavailable" instead of a generic network error.
                    return retryResponse;
                }
                // Still failing — fall through to host failover below using
                // the retry's response (closed after we're done with it).
                retryResponse.close();
                return attemptHostFailover(chain, originalRequest,
                        new IOException("Gateway error " + retryResponse.code()));
            }

            return response;
        } catch (IOException firstAttemptError) {
            Log.w(TAG, "Request failed (" + originalRequest.url() +
                    "), attempting failover discovery: " + firstAttemptError.getMessage());
            return attemptHostFailover(chain, originalRequest, firstAttemptError);
        }
    }

    private boolean isRetryableGatewayError(int code) {
        return code == 502 || code == 503 || code == 504;
    }

    private Response attemptHostFailover(Chain chain, Request originalRequest, IOException firstAttemptError)
            throws IOException {
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
