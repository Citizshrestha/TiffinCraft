package com.tiffincraft.app.api;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    /**
     * BASE_URL / SERVER_URL are now MUTABLE and backed by {@link ServerConfig}'s
     * cache instead of a hardcoded literal. This is the permanent fix for
     * "backend error on login/register even though backend + tunnel are
     * running": the previous hardcoded tunnel URL went stale every time the
     * tunnel restarted (new random loca.lt subdomain), requiring a manual
     * source edit + APK rebuild.
     *
     * Now:
     *  1. On first use (see getInstance), a background discovery call hits
     *     the PC's LAN IP at /api/config to fetch the CURRENT tunnel/LAN URL.
     *  2. FailoverInterceptor below detects failed requests (stale URL) and
     *     triggers re-discovery + retry automatically, mid-session.
     *  3. resetInstance() rebuilds Retrofit with whatever URL is cached now.
     *
     * No more manual RetrofitClient edits or rebuilds are needed when the
     * tunnel URL rotates — as long as the phone is on the same WiFi as the
     * PC (per user's confirmed setup), discovery always succeeds.
     */
    public static String BASE_URL;
    public static String SERVER_URL;

    private static RetrofitClient instance;
    private final Retrofit retrofit;
    private final Context context;

    private RetrofitClient(Context context) {
        this.context = context.getApplicationContext();

        // Load whatever URL is currently cached (or compiled-in default on
        // very first run before discovery has ever completed).
        BASE_URL = ServerConfig.getCachedBaseUrl(this.context);
        SERVER_URL = ServerConfig.getCachedServerUrl(this.context);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Changed from HEADERS to BODY

        Interceptor authInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();

                Request.Builder builder = originalRequest.newBuilder()
                        .header("Bypass-Tunnel-Reminder", "true");

                if (originalRequest.header("Authorization") == null) {
                    SharedPreferences prefs = context.getSharedPreferences(
                            "TiffinCraftSession", Context.MODE_PRIVATE);
                    String token = prefs.getString("token", null);
                    if (token != null && !token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                }

                return chain.proceed(builder.build());
            }
        };

        Interceptor authErrorInterceptor = new AuthErrorInterceptor(this.context);
        Interceptor failoverInterceptor = new FailoverInterceptor(this.context);

        CookieJar cookieJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);

                // Save token from cookie to SessionManager prefs for consistency
                for (Cookie cookie : cookies) {
                    if ("auth_token".equals(cookie.name())) {
                        SharedPreferences prefs = context.getSharedPreferences(
                                "TiffinCraftSession", Context.MODE_PRIVATE);
                        prefs.edit().putString("token", cookie.value()).apply();
                    }
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<>();
            }
        };

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .cookieJar(cookieJar)
                .addInterceptor(authInterceptor)
                .addInterceptor(authErrorInterceptor)  // Add auth error handling
                .addInterceptor(failoverInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(
                    new com.google.gson.GsonBuilder()
                        .setLenient()
                        .create()
                ))
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    /** Call this after tunnel URL changes so the singleton is rebuilt with new URLs. */
    public static synchronized void resetInstance() {
        instance = null;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    public static String getAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftSession", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);
        return token != null ? "Bearer " + token : null;
    }

    public static void saveAuthToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftSession", Context.MODE_PRIVATE);
        prefs.edit().putString("token", token).apply();
    }

    public static void clearAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftSession", Context.MODE_PRIVATE);
        prefs.edit().remove("token").apply();
    }

    /** Get the current server URL for Socket.IO connections */
    public static String getServerUrl(Context context) {
        return ServerConfig.getCachedServerUrl(context);
    }
}
