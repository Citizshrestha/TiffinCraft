package com.tiffincraft.app.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Resolves the backend's network address.
 *
 * HISTORY — why this class looks the way it does:
 * The dev backend used to be exposed via localtunnel, whose public URL changed
 * on every tunnel restart. Hardcoding it in RetrofitClient meant a manual edit
 * plus an APK rebuild each time, so this class was added to discover the
 * current address at runtime from GET /api/config.
 *
 * NOW:
 * The backend is deployed at a stable HTTPS address (see DEFAULT_BASE_URL), so
 * there is nothing to discover in normal operation. Discovery is retained but
 * OFF by default — enable it with {@link #setLanDiscoveryEnabled} only when
 * pointing the app at a backend on the local network.
 *
 * Leaving discovery on against the hosted backend is not merely redundant, it
 * is harmful: /api/config reports the server's own view of itself, which on a
 * hosted box is a private address inside the provider's network. Adopting it
 * would cache a permanently unreachable host. {@link #discoverAndCacheSync}
 * guards against this by refusing any address whose host differs from the one
 * it just queried.
 */
public class ServerConfig {

    private static final String TAG = "ServerConfig";

    // Bumped to "…V2" when the backend moved from the dev PC to the hosted
    // deployment. Any device that ran an earlier build has the old LAN address
    // sitting in active_base_url, and that cache WINS over the constants below
    // — so without a new prefs file, upgrading installs would keep dialling a
    // PC that is no longer serving them. A new name orphans the old file and
    // every device falls through to DEFAULT_BASE_URL exactly once.
    private static final String PREFS_NAME = "TiffinCraftServerConfigV2";
    private static final String KEY_ACTIVE_BASE_URL = "active_base_url";
    private static final String KEY_ACTIVE_SERVER_URL = "active_server_url";
    private static final String KEY_LAN_HOST = "lan_host";

    // The hosted backend. Unlike the old localtunnel setup this address is
    // stable, so it is the real default rather than a seed value to be
    // discovered past.
    private static final String DEFAULT_BASE_URL = "https://tiffincraft-xsrh.onrender.com/api/";
    private static final String DEFAULT_SERVER_URL = "https://tiffincraft-xsrh.onrender.com";

    // Only consulted by discoverAndCacheSync(), which is now opt-in (see
    // setLanDiscoveryEnabled). Kept so a developer can still point the app at
    // a laptop running the backend locally.
    private static final String DEFAULT_LAN_HOST = "192.168.100.115:5000";

    private static final String KEY_LAN_DISCOVERY = "lan_discovery_enabled";

    // Render's free tier spins the instance down after 15 minutes idle; the
    // next request then pays a cold start. 3s was tuned for a LAN hop and
    // would abandon a waking instance as unreachable.
    private static final int DISCOVERY_TIMEOUT_MS = 8000;

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

    /**
     * LAN rediscovery is OFF by default. Turn it on only when running against a
     * backend on the local network; against the hosted deployment there is
     * nothing to discover and it can only do harm (see discoverAndCacheSync).
     */
    public static void setLanDiscoveryEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_LAN_DISCOVERY, enabled).apply();
    }

    public static boolean isLanDiscoveryEnabled(Context context) {
        return prefs(context).getBoolean(KEY_LAN_DISCOVERY, false);
    }

    /** Discards any cached address, so the next read falls back to DEFAULT_BASE_URL. */
    public static void resetToDefault(Context context) {
        prefs(context).edit()
                .remove(KEY_ACTIVE_BASE_URL)
                .remove(KEY_ACTIVE_SERVER_URL)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Opt-in developer aid: rediscovers a backend on the local network via
     * /api/config. MUST be called off the main thread.
     *
     * Returns null immediately unless LAN discovery has been explicitly
     * enabled. With the backend at a stable HTTPS address there is nothing to
     * discover, and leaving this on was actively dangerous — see the host-match
     * guard below.
     *
     * @return the newly discovered + cached base URL, or null if discovery is
     *         disabled, failed, or returned an address we refuse to adopt.
     */
    public static String discoverAndCacheSync(Context context) {
        if (!isLanDiscoveryEnabled(context)) {
            return null;
        }

        String lanHost = getLanHost(context);
        // Host portion only — lanHost carries a port, the discovered URL may not.
        int colon = lanHost.indexOf(':');
        String queriedHost = colon >= 0 ? lanHost.substring(0, colon) : lanHost;

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

            if (activeBaseUrl == null || activeServerUrl == null) {
                return null;
            }

            // Only adopt an address on the SAME host we just proved reachable.
            // /api/config reports the server's own view of itself, which on a
            // hosted box is a private address inside the provider's network —
            // the live deploy returns http://10.27.232.60:10000/api/. Adopting
            // that is unrecoverable: the app caches a dead host, and every
            // later request, including this discovery call, then fails against
            // it. Comparing hosts catches that without banning private ranges
            // outright, so a developer whose own LAN is 10.x still works.
            String discoveredHost = Uri.parse(activeBaseUrl).getHost();
            if (discoveredHost == null || !discoveredHost.equals(queriedHost)) {
                Log.w(TAG, "Refusing discovered host '" + discoveredHost
                        + "' — does not match the queried host '" + queriedHost + "'");
                return null;
            }

            saveActive(context, activeBaseUrl, activeServerUrl);
            return activeBaseUrl;
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
