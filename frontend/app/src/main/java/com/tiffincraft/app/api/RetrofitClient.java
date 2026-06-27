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

    private static final String BASE_URL = "http://192.168.1.4:5000/api/";

    private static RetrofitClient instance;
    private final Retrofit retrofit;
    private final Context context;

    private RetrofitClient(Context context) {
        this.context = context.getApplicationContext();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();

                if (originalRequest.header("Authorization") != null) {
                    return chain.proceed(originalRequest);
                }

                SharedPreferences prefs = context.getSharedPreferences("TiffinCraftPrefs", Context.MODE_PRIVATE);
                String token = prefs.getString("auth_token", null);

                if (token == null || token.isEmpty()) {
                    return chain.proceed(originalRequest);
                }

                Request newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();

                return chain.proceed(newRequest);
            }
        };

        CookieJar cookieJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);

                for (Cookie cookie : cookies) {
                    if ("auth_token".equals(cookie.name())) {
                        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("auth_token", cookie.value()).apply();
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
                .cookieJar(cookieJar)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    public static String getAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        return token != null ? "Bearer " + token : null;
    }

    public static void saveAuthToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("auth_token", token).apply();
    }

    public static void clearAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TiffinCraftPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("auth_token").apply();
    }
}
