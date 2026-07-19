package com.tiffincraft.app.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Auto-discovery of the backend's current network address.
 *
 * PROBLEM THIS SOLVES:
 * The dev backend is exposed via localtunnel, whose public URL changes every
 * time the tunnel process restarts (PC reboot, network drop, crash, etc).
 * Previously the Android app had this URL hardcoded in RetrofitClient, which
 * meant every tunnel restart required manually editing the URL and rebuilding
 * the APK — the exact "backend error on login/register" issue.
 *
 * FIX:
 * The backend exposes GET /api/config (added specifically for this) which
 * reports both:
 *   - lan:    the backend's LAN address (http://<pc-lan-ip>:5000) — reachable
 *             directly whenever the phone is on the same WiFi as the PC.
 *   - tunnel: the current localtunnel public URL (for off-WiFi access).
 *
 * This class queries /api/config directly via the PC's LAN IP (fast, doesn't
 * depend on the tunnel at all while on the same WiFi) and caches whichever
 * URL is confirmed reachable. RetrofitClient reads from this cache, and
 * FailoverInterceptor re-runs discovery automatically if a request ever fails
 * — so the app self-heals without needing a rebuild or reinstall.
 */
public class ServerConfig {

    private static final String PREFS_NAME = "TiffinCraftServerConfig";
    private static final String KEY_ACTIVE_BASE_URL = "active_base_url";
    private static final String KEY_ACTIVE_SERVER_URL = "active_server_url";
    private static final String KEY_LAN_HOST = "lan_host";

    // Seed values — only used before the first successful discovery ever
    // happens (fresh install). After that, the cache is kept up to date
    // automatically. If your PC's LAN IP changes permanently (new router,
    // etc.), update DEFAULT_LAN_HOST here, or simply let it fail over once —
    // it will still recover automatically as long as this fallback tunnel URL
    // (or the LAN IP) is reachable at least once.
    // IMPORTANT: The LAN host is the primary route when both phones and PC
    // share the same WiFi — it's fast, doesn't depend on the tunnel, and is
    // the fallback when the tunnel URL goes stale.
    private static final String DEFAULT_LAN_HOST = "192.168.100.115:5000";
    private static final String DEFAULT_BASE_URL = "http://192.168.100.115:5000/api/";
    private static final String DEFAULT_SERVER_URL = "http://192.168.100.115:5000";

    private static final int DISCOVERY_TIMEOUT_MS = 3000;

    private ServerConfig() {
    }

    public static String getCachedBaseUrl(Context context) {
        return prefs(context).getString(KEY_ACTIVE_BASE_URL, DEFAULT_BASE_URL);
    }

    public static String getCachedServerUrl(Context context) {
        return prefs(context).getString(KEY_ACTIVE_SERVER_URL, DEFAULT_SERVER_URL);
    }

    private static String getLanHost(Context context) {
        return prefs(context).getString(KEY_LAN_HOST, DEFAULT_LAN_HOST);
    }

    private static void saveActive(Context context, String baseUrl, String serverUrl) {
        prefs(context).edit()
                .putString(KEY_ACTIVE_BASE_URL, baseUrl)
                .putString(KEY_ACTIVE_SERVER_URL, serverUrl)
                .apply();
    }

    /** Allows manually overriding the LAN discovery host, e.g. from a settings screen. */
    public static void saveLanHost(Context context, String hostPort) {
        prefs(context).edit().putString(KEY_LAN_HOST, hostPort).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Synchronously discovers the current live backend URL by querying
     * /api/config via the PC's LAN IP. MUST be called off the main thread.
     *
     * @return the newly discovered + cached base URL, or null if discovery failed
     *         (e.g. phone is not on the same WiFi as the PC right now).
     */
    public static String discoverAndCacheSync(Context context) {
        String lanHost = getLanHost(context);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(DISCOVERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DISCOVERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url("http://" + lanHost + "/api/config")
                .header("Bypass-Tunnel-Reminder", "true")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            JSONObject obj = new JSONObject(response.body().string());

            JSONObject lan = obj.optJSONObject("lan");
            JSONObject tunnel = obj.optJSONObject("tunnel");

            String activeBaseUrl = null;
            String activeServerUrl = null;

            // We just reached the backend directly via LAN, so LAN is
            // confirmed reachable right now — prefer it (faster, doesn't
            // depend on localtunnel being alive at all).
            if (lan != null && lan.has("baseUrl")) {
                activeBaseUrl = lan.optString("baseUrl", null);
                activeServerUrl = lan.optString("serverUrl", null);
            } else if (tunnel != null && tunnel.has("baseUrl")) {
                activeBaseUrl = tunnel.optString("baseUrl", null);
                activeServerUrl = tunnel.optString("serverUrl", null);
            }

            if (activeBaseUrl != null && activeServerUrl != null) {
                saveActive(context, activeBaseUrl, activeServerUrl);
                return activeBaseUrl;
            }
        } catch (Exception e) {
            // LAN unreachable right now (different network, PC off, etc).
            // Caller falls back to the last cached / compiled default URL.
        }
        return null;
    }

    public static String getActiveServerUrlAfterDiscovery(Context context) {
        return getCachedServerUrl(context);
    }
}
